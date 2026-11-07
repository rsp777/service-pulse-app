package com.pawar.todo.amt.ssh.key;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import org.springframework.stereotype.Component;

/**
 * Generates cryptographic keypairs entirely in memory without writing to disk.
 * Uses native Ed25519 (RFC 8410) and RSA-4096.
 */
@Component
public class DefaultEphemeralKeyPairGenerator implements EphemeralKeyPairGenerator {

    private static final int RSA_KEY_SIZE = 4096;

    @Override
    public EphemeralKeyPair generateKeyPair(KeyAlgorithm algorithm) {
        KeyAlgorithm selectedAlgorithm = algorithm != null ? algorithm : KeyAlgorithm.ED25519;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(selectedAlgorithm.getJcaName());
            if (selectedAlgorithm == KeyAlgorithm.RSA) {
                generator.initialize(RSA_KEY_SIZE);
            }
            KeyPair keyPair = generator.generateKeyPair();
            String comment = "service-pulse-app-ephemeral@" + Instant.now().getEpochSecond();
            String openSshPublicKey = OpenSshKeyEncoder.encodePublicKey(keyPair.getPublic(), comment);
            return new EphemeralKeyPair(selectedAlgorithm, keyPair, openSshPublicKey);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cryptographic algorithm not available: " + selectedAlgorithm, e);
        }
    }
}
