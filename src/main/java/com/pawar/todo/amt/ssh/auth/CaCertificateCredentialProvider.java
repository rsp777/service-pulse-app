package com.pawar.todo.amt.ssh.auth;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pawar.todo.amt.ssh.rotation.ActiveSshCredential;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

/**
 * Thread-safe, lock-free SshCredentialProvider backed by an in-memory rotating OpenSSH certificate.
 * Atomically hot-swaps active credentials without terminating in-flight SSH operations.
 */
public class CaCertificateCredentialProvider implements SshCredentialProvider {

    private static final Logger logger = LoggerFactory.getLogger(CaCertificateCredentialProvider.class);

    private final AtomicReference<ActiveSshCredential> currentCredential = new AtomicReference<>();
    private final AtomicLong totalRotations = new AtomicLong(0);
    private final AtomicReference<Instant> lastRotatedAt = new AtomicReference<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "ssh-credential-cleanup");
        thread.setDaemon(true);
        return thread;
    });

    public CaCertificateCredentialProvider() {
    }

    public CaCertificateCredentialProvider(ActiveSshCredential initialCredential) {
        if (initialCredential != null) {
            updateCredential(initialCredential, Duration.ZERO);
        }
    }

    @Override
    public KeyProvider getKeyProvider(SSHClient client) throws IOException {
        ActiveSshCredential credential = currentCredential.get();
        if (credential == null) {
            throw new IOException("No active SSH certificate credential available");
        }
        if (credential.isExpired()) {
            throw new IOException("Active SSH certificate has expired at " + credential.getExpiresAt());
        }
        return credential.toKeyProvider();
    }

    @Override
    public SshCredentialMetadata getMetadata() {
        ActiveSshCredential credential = currentCredential.get();
        if (credential == null) {
            return new SshCredentialMetadata(
                    SshAuthMode.CA_CERTIFICATE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0L,
                    totalRotations.get(),
                    lastRotatedAt.get(),
                    false
            );
        }

        return new SshCredentialMetadata(
                SshAuthMode.CA_CERTIFICATE,
                credential.getKeyPair().getAlgorithm().name(),
                String.valueOf(credential.getSerial()),
                credential.getKeyId(),
                credential.getCertificate().getValidPrincipals(),
                credential.getExpiresAt(),
                credential.getRemainingTtl().toSeconds(),
                totalRotations.get(),
                lastRotatedAt.get(),
                !credential.isExpired()
        );
    }

    @Override
    public boolean isReady() {
        ActiveSshCredential credential = currentCredential.get();
        return credential != null && !credential.isExpired();
    }

    /**
     * Atomically swaps the active credential. Schedules previous credential for deferred
     * destruction after the specified grace period so running operations can conclude.
     */
    public void updateCredential(ActiveSshCredential newCredential, Duration gracePeriod) {
        if (newCredential == null) {
            throw new IllegalArgumentException("New credential cannot be null");
        }

        ActiveSshCredential previous = currentCredential.getAndSet(newCredential);
        totalRotations.incrementAndGet();
        lastRotatedAt.set(Instant.now());

        logger.info("Atomically updated active SSH credential (serial={}, keyId={}, expiresAt={})",
                newCredential.getSerial(), newCredential.getKeyId(), newCredential.getExpiresAt());

        if (previous != null) {
            long delayMs = (gracePeriod != null && !gracePeriod.isNegative()) ? gracePeriod.toMillis() : 60_000L;
            cleanupExecutor.schedule(() -> {
                try {
                    logger.debug("Executing deferred cleanup for retired SSH credential serial={}", previous.getSerial());
                    previous.destroy();
                } catch (Exception e) {
                    logger.warn("Error destroying retired SSH credential: {}", e.getMessage());
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    public ActiveSshCredential getCurrentCredential() {
        return currentCredential.get();
    }

    public void close() {
        cleanupExecutor.shutdownNow();
        ActiveSshCredential current = currentCredential.getAndSet(null);
        if (current != null) {
            current.destroy();
        }
    }
}
