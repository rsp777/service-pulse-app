package com.pawar.todo.amt.ssh.auth;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class SshCredentialMetadata {

    private final SshAuthMode authMode;
    private final String keyAlgorithm;
    private final String certificateSerial;
    private final String keyId;
    private final List<String> principals;
    private final Instant validUntil;
    private final Long remainingSeconds;
    private final long totalRotations;
    private final Instant lastRotatedAt;
    private final boolean valid;

    public SshCredentialMetadata(SshAuthMode authMode,
                                 String keyAlgorithm,
                                 String certificateSerial,
                                 String keyId,
                                 List<String> principals,
                                 Instant validUntil,
                                 Long remainingSeconds,
                                 long totalRotations,
                                 Instant lastRotatedAt,
                                 boolean valid) {
        this.authMode = authMode;
        this.keyAlgorithm = keyAlgorithm;
        this.certificateSerial = certificateSerial;
        this.keyId = keyId;
        this.principals = principals != null ? Collections.unmodifiableList(principals) : Collections.emptyList();
        this.validUntil = validUntil;
        this.remainingSeconds = remainingSeconds;
        this.totalRotations = totalRotations;
        this.lastRotatedAt = lastRotatedAt;
        this.valid = valid;
    }

    public static SshCredentialMetadata staticKey(String keyAlgorithm, boolean valid) {
        return new SshCredentialMetadata(
                SshAuthMode.STATIC,
                keyAlgorithm,
                null,
                "static-disk-key",
                Collections.emptyList(),
                null,
                null,
                0,
                null,
                valid
        );
    }

    public SshAuthMode getAuthMode() {
        return authMode;
    }

    public String getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public String getCertificateSerial() {
        return certificateSerial;
    }

    public String getKeyId() {
        return keyId;
    }

    public List<String> getPrincipals() {
        return principals;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public long getTotalRotations() {
        return totalRotations;
    }

    public Instant getLastRotatedAt() {
        return lastRotatedAt;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public String toString() {
        return "SshCredentialMetadata{" +
                "authMode=" + authMode +
                ", keyAlgorithm='" + keyAlgorithm + '\'' +
                ", certificateSerial='" + certificateSerial + '\'' +
                ", keyId='" + keyId + '\'' +
                ", principals=" + principals +
                ", validUntil=" + validUntil +
                ", remainingSeconds=" + remainingSeconds +
                ", totalRotations=" + totalRotations +
                ", lastRotatedAt=" + lastRotatedAt +
                ", valid=" + valid +
                '}';
    }
}
