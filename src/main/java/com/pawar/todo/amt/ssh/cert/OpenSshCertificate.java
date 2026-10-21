package com.pawar.todo.amt.ssh.cert;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Parsed representation of an OpenSSH certificate according to PROTOCOL.certkeys.
 */
public class OpenSshCertificate {

    public static final int TYPE_USER = 1;
    public static final int TYPE_HOST = 2;

    private final String rawString;
    private final String keyType;
    private final long serial;
    private final int type;
    private final String keyId;
    private final List<String> validPrincipals;
    private final Instant validAfter;
    private final Instant validBefore;
    private final List<String> criticalOptions;
    private final List<String> extensions;
    private final byte[] nonce;
    private final byte[] publicKeyBytes;
    private final byte[] signatureKeyBytes;

    public OpenSshCertificate(String rawString,
                              String keyType,
                              long serial,
                              int type,
                              String keyId,
                              List<String> validPrincipals,
                              Instant validAfter,
                              Instant validBefore,
                              List<String> criticalOptions,
                              List<String> extensions,
                              byte[] nonce,
                              byte[] publicKeyBytes,
                              byte[] signatureKeyBytes) {
        this.rawString = rawString;
        this.keyType = keyType;
        this.serial = serial;
        this.type = type;
        this.keyId = keyId;
        this.validPrincipals = validPrincipals != null ? Collections.unmodifiableList(validPrincipals) : Collections.emptyList();
        this.validAfter = validAfter;
        this.validBefore = validBefore;
        this.criticalOptions = criticalOptions != null ? Collections.unmodifiableList(criticalOptions) : Collections.emptyList();
        this.extensions = extensions != null ? Collections.unmodifiableList(extensions) : Collections.emptyList();
        this.nonce = nonce != null ? nonce.clone() : new byte[0];
        this.publicKeyBytes = publicKeyBytes != null ? publicKeyBytes.clone() : new byte[0];
        this.signatureKeyBytes = signatureKeyBytes != null ? signatureKeyBytes.clone() : new byte[0];
    }

    public String getRawString() {
        return rawString;
    }

    public String getKeyType() {
        return keyType;
    }

    public long getSerial() {
        return serial;
    }

    public int getType() {
        return type;
    }

    public boolean isUserCertificate() {
        return type == TYPE_USER;
    }

    public boolean isHostCertificate() {
        return type == TYPE_HOST;
    }

    public String getKeyId() {
        return keyId;
    }

    public List<String> getValidPrincipals() {
        return validPrincipals;
    }

    public Instant getValidAfter() {
        return validAfter;
    }

    public Instant getValidBefore() {
        return validBefore;
    }

    public List<String> getCriticalOptions() {
        return criticalOptions;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public byte[] getNonce() {
        return nonce.clone();
    }

    public byte[] getPublicKeyBytes() {
        return publicKeyBytes.clone();
    }

    public byte[] getSignatureKeyBytes() {
        return signatureKeyBytes.clone();
    }

    public boolean isExpired() {
        return validBefore != null && Instant.now().isAfter(validBefore);
    }

    public Duration getRemainingTtl() {
        if (validBefore == null) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(Instant.now(), validBefore);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    @Override
    public String toString() {
        return "OpenSshCertificate{" +
                "keyType='" + keyType + '\'' +
                ", serial=" + serial +
                ", type=" + (type == TYPE_USER ? "USER" : "HOST") +
                ", keyId='" + keyId + '\'' +
                ", validPrincipals=" + validPrincipals +
                ", validAfter=" + validAfter +
                ", validBefore=" + validBefore +
                ", remainingSeconds=" + getRemainingTtl().toSeconds() +
                '}';
    }
}
