package com.pawar.todo.amt.ssh.rotation;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;

import com.pawar.todo.amt.ssh.SshProperties;
import com.pawar.todo.amt.ssh.auth.CaCertificateCredentialProvider;
import com.pawar.todo.amt.ssh.auth.SshAuthMode;
import com.pawar.todo.amt.ssh.ca.CertificateSigningOptions;
import com.pawar.todo.amt.ssh.ca.SshCertificateAuthorityClient;
import com.pawar.todo.amt.ssh.ca.SshSignedCertificate;
import com.pawar.todo.amt.ssh.key.EphemeralKeyPair;
import com.pawar.todo.amt.ssh.key.EphemeralKeyPairGenerator;
import com.pawar.todo.amt.ssh.key.KeyAlgorithm;

/**
 * SmartLifecycle background rotation engine for OpenSSH User Certificates.
 * Monitors TTL, generates in-memory ephemeral keypairs, requests CA signatures proactively (at 75% TTL),
 * and hot-swaps credentials in CaCertificateCredentialProvider without disruption.
 */
public class SshCertificateRotationEngine implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(SshCertificateRotationEngine.class);

    private final SshProperties properties;
    private final EphemeralKeyPairGenerator keyPairGenerator;
    private final SshCertificateAuthorityClient caClient;
    private final CaCertificateCredentialProvider credentialProvider;
    private final ApplicationEventPublisher eventPublisher;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "ssh-cert-rotator");
        thread.setDaemon(true);
        return thread;
    });

    private ScheduledFuture<?> nextRotationTask;

    public SshCertificateRotationEngine(SshProperties properties,
                                        EphemeralKeyPairGenerator keyPairGenerator,
                                        SshCertificateAuthorityClient caClient,
                                        CaCertificateCredentialProvider credentialProvider,
                                        ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.keyPairGenerator = keyPairGenerator;
        this.caClient = caClient;
        this.credentialProvider = credentialProvider;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void start() {
        if (properties.getAuthMode() != SshAuthMode.CA_CERTIFICATE) {
            logger.info("SSH AuthMode is {}, SshCertificateRotationEngine will remain inactive", properties.getAuthMode());
            return;
        }

        if (running.compareAndSet(false, true)) {
            logger.info("Starting proactive SshCertificateRotationEngine...");
            rotateNow();
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("Stopping SshCertificateRotationEngine...");
            if (nextRotationTask != null && !nextRotationTask.isDone()) {
                nextRotationTask.cancel(true);
            }
            scheduler.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE + 1000;
    }

    public synchronized CompletableFuture<ActiveSshCredential> rotate() {
        return CompletableFuture.supplyAsync(this::doRotate, scheduler);
    }

    public synchronized ActiveSshCredential rotateNow() {
        try {
            ActiveSshCredential credential = doRotate();
            consecutiveFailures.set(0);
            scheduleNextProactiveRotation(credential);
            return credential;
        } catch (Exception e) {
            handleRotationFailure(e);
            return null;
        }
    }

    private ActiveSshCredential doRotate() {
        long startTime = System.currentTimeMillis();
        logger.info("Initiating SSH certificate rotation cycle...");

        SshProperties.CaProperties ca = properties.getCa();
        KeyAlgorithm algorithm = KeyAlgorithm.fromString(ca.getKeyAlgorithm());

        EphemeralKeyPair keyPair = keyPairGenerator.generateKeyPair(algorithm);

        CertificateSigningOptions options = CertificateSigningOptions.builder()
                .validPrincipals(ca.getPrincipals())
                .ttl(Duration.ofMinutes(ca.getCertTtlMinutes() > 0 ? ca.getCertTtlMinutes() : 60))
                .keyId("servicepulse-" + Instant.now().getEpochSecond())
                .certType("user")
                .build();

        SshSignedCertificate signedCert = caClient.sign(keyPair.getOpenSshPublicKey(), options);

        ActiveSshCredential newCredential = new ActiveSshCredential(keyPair, signedCert.getParsedCertificate());

        // Atomically hot swap with 60-second grace period for old keys
        credentialProvider.updateCredential(newCredential, Duration.ofSeconds(60));

        long durationMs = System.currentTimeMillis() - startTime;
        RotationEvent event = RotationEvent.success(newCredential.getSerial(), newCredential.getKeyId(), durationMs);
        publishEvent(event);

        logger.info("SSH certificate rotation completed in {}ms (serial={}, expiresAt={})",
                durationMs, newCredential.getSerial(), newCredential.getExpiresAt());

        return newCredential;
    }

    private void scheduleNextProactiveRotation(ActiveSshCredential credential) {
        if (!running.get()) {
            return;
        }

        Instant validAfter = credential.getCertificate().getValidAfter();
        Instant validBefore = credential.getCertificate().getValidBefore();
        Duration totalTtl = Duration.between(validAfter != null ? validAfter : Instant.now(), validBefore);

        double threshold = (properties.getCa().getRenewThresholdPercentage() > 0)
                ? properties.getCa().getRenewThresholdPercentage() / 100.0
                : 0.75;

        long delaySeconds = Math.max((long) (totalTtl.toSeconds() * threshold), 30L);
        logger.info("Next proactive SSH certificate rotation scheduled in {} seconds (at {}% TTL)",
                delaySeconds, (int) (threshold * 100));

        if (nextRotationTask != null && !nextRotationTask.isDone()) {
            nextRotationTask.cancel(false);
        }

        nextRotationTask = scheduler.schedule(this::rotateNow, delaySeconds, TimeUnit.SECONDS);
    }

    private void handleRotationFailure(Exception exception) {
        int failures = consecutiveFailures.incrementAndGet();
        long retryDelaySeconds = calculateRetryBackoff(failures);

        logger.error("SSH certificate rotation failed (attempt #{}). Retrying in {} seconds: {}",
                failures, retryDelaySeconds, exception.getMessage());

        RotationEvent event = RotationEvent.failure(exception.getMessage(), 0L);
        publishEvent(event);

        // Check if current certificate is near expiration
        ActiveSshCredential active = credentialProvider.getCurrentCredential();
        if (active != null) {
            long remainingSec = active.getRemainingTtl().toSeconds();
            if (remainingSec < 300) {
                logger.warn("CRITICAL: Active SSH certificate expires in {}s and renewal is failing!", remainingSec);
            }
        }

        if (running.get()) {
            nextRotationTask = scheduler.schedule(this::rotateNow, retryDelaySeconds, TimeUnit.SECONDS);
        }
    }

    private long calculateRetryBackoff(int failures) {
        // Exponential backoff: 10s, 30s, 60s max
        if (failures <= 1) {
            return 10L;
        } else if (failures == 2) {
            return 30L;
        } else {
            return 60L;
        }
    }

    private void publishEvent(RotationEvent event) {
        if (eventPublisher != null) {
            try {
                eventPublisher.publishEvent(event);
            } catch (Exception e) {
                logger.debug("Failed to publish RotationEvent: {}", e.getMessage());
            }
        }
    }
}
