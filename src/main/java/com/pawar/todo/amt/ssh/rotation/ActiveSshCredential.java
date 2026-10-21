package com.pawar.todo.amt.ssh.rotation;

import java.time.Duration;
import java.time.Instant;

import com.pawar.todo.amt.ssh.cert.CertificateKeyProvider;
import com.pawar.todo.amt.ssh.cert.OpenSshCertificate;
import com.pawar.todo.amt.ssh.key.EphemeralKeyPair;

import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

/**
 * Encapsulates an active ephemeral keypair coupled with its signed OpenSSH User Certificate.
 */
public class ActiveSshCredential {

    private final EphemeralKeyPair keyPair;
    private final OpenSshCertificate certificate;
    private final Instant createdAt;

    public ActiveSshCredential(EphemeralKeyPair keyPair, OpenSshCertificate certificate) {
        if (keyPair == null || certificate == null) {
            throw new IllegalArgumentException("KeyPair and Certificate cannot be null");
        }
        this.keyPair = keyPair;
        this.certificate = certificate;
        this.createdAt = Instant.now();
    }

    public EphemeralKeyPair getKeyPair() {
        return keyPair;
    }

    public OpenSshCertificate getCertificate() {
        return certificate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return certificate.getValidBefore();
    }

    public Duration getRemainingTtl() {
        return certificate.getRemainingTtl();
    }

    public boolean isExpired() {
        return certificate.isExpired();
    }

    public boolean isExpiringSoon(double thresholdPercentage) {
        Instant validAfter = certificate.getValidAfter();
        Instant validBefore = certificate.getValidBefore();
        if (validAfter == null || validBefore == null) {
            return false;
        }
        Duration totalDuration = Duration.between(validAfter, validBefore);
        Duration elapsed = Duration.between(validAfter, Instant.now());
        if (totalDuration.isZero() || totalDuration.isNegative()) {
            return true;
        }
        double elapsedRatio = (double) elapsed.toMillis() / totalDuration.toMillis();
        return elapsedRatio >= (thresholdPercentage / 100.0);
    }

    public long getSerial() {
        return certificate.getSerial();
    }

    public String getKeyId() {
        return certificate.getKeyId();
    }

    public KeyProvider toKeyProvider() {
        return new CertificateKeyProvider(keyPair, certificate);
    }

    public void destroy() {
        keyPair.close();
    }

    @Override
    public String toString() {
        return "ActiveSshCredential{" +
                "serial=" + getSerial() +
                ", keyId='" + getKeyId() + '\'' +
                ", expiresAt=" + getExpiresAt() +
                ", remainingSeconds=" + getRemainingTtl().toSeconds() +
                '}';
    }
}
