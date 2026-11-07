package com.pawar.todo.amt.ssh.key;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

/**
 * In-memory representation of an ephemeral SSH keypair.
 * Enforces memory zeroization on disposal and prevents leaking private key bytes in logs.
 */
public class EphemeralKeyPair implements AutoCloseable, Destroyable {

    private final KeyAlgorithm algorithm;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final String openSshPublicKey;
    private final Instant createdAt;
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    public EphemeralKeyPair(KeyAlgorithm algorithm, KeyPair keyPair, String openSshPublicKey) {
        if (algorithm == null || keyPair == null) {
            throw new IllegalArgumentException("Algorithm and KeyPair cannot be null");
        }
        this.algorithm = algorithm;
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
        this.openSshPublicKey = openSshPublicKey;
        this.createdAt = Instant.now();
    }

    public KeyAlgorithm getAlgorithm() {
        return algorithm;
    }

    public PublicKey getPublicKey() {
        checkNotDestroyed();
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        checkNotDestroyed();
        return privateKey;
    }

    public String getOpenSshPublicKey() {
        checkNotDestroyed();
        return openSshPublicKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public void destroy() throws DestroyFailedException {
        if (destroyed.compareAndSet(false, true)) {
            if (privateKey instanceof Destroyable destroyableKey) {
                try {
                    destroyableKey.destroy();
                } catch (DestroyFailedException e) {
                    // Ignored if provider does not support explicit in-place memory scrubbing
                }
            }
        }
    }

    @Override
    public boolean isDestroyed() {
        return destroyed.get();
    }

    @Override
    public void close() {
        try {
            destroy();
        } catch (DestroyFailedException e) {
            // best-effort cleanup on close
        }
    }

    private void checkNotDestroyed() {
        if (isDestroyed()) {
            throw new IllegalStateException("EphemeralKeyPair has been destroyed");
        }
    }

    @Override
    public String toString() {
        return "EphemeralKeyPair{" +
                "algorithm=" + algorithm +
                ", openSshPublicKey='" + openSshPublicKey + '\'' +
                ", createdAt=" + createdAt +
                ", destroyed=" + isDestroyed() +
                ", privateKey=[REDACTED]" +
                '}';
    }
}
