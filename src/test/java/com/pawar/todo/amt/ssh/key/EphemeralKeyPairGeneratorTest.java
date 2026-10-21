package com.pawar.todo.amt.ssh.key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EphemeralKeyPairGeneratorTest {

    private DefaultEphemeralKeyPairGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new DefaultEphemeralKeyPairGenerator();
    }

    @Test
    void generateKeyPair_ed25519_createsValidKeyPairWithoutDiskPersistence() {
        EphemeralKeyPair keyPair = generator.generateKeyPair(KeyAlgorithm.ED25519);

        assertNotNull(keyPair);
        assertEquals(KeyAlgorithm.ED25519, keyPair.getAlgorithm());
        assertNotNull(keyPair.getPublicKey());
        assertNotNull(keyPair.getPrivateKey());
        assertTrue(keyPair.getOpenSshPublicKey().startsWith("ssh-ed25519 "));
        assertFalse(keyPair.isDestroyed());
        assertNotNull(keyPair.getCreatedAt());

        // Verify toString does not leak private key
        String str = keyPair.toString();
        assertTrue(str.contains("[REDACTED]"));
        assertFalse(str.contains(keyPair.getPrivateKey().toString()));
    }

    @Test
    void generateKeyPair_rsa_createsValidKeyPair() {
        EphemeralKeyPair keyPair = generator.generateKeyPair(KeyAlgorithm.RSA);

        assertNotNull(keyPair);
        assertEquals(KeyAlgorithm.RSA, keyPair.getAlgorithm());
        assertNotNull(keyPair.getPublicKey());
        assertNotNull(keyPair.getPrivateKey());
        assertTrue(keyPair.getOpenSshPublicKey().startsWith("ssh-rsa "));
        assertFalse(keyPair.isDestroyed());
    }

    @Test
    void close_destroysKeyPair_andThrowsOnAccess() {
        EphemeralKeyPair keyPair = generator.generateKeyPair(KeyAlgorithm.ED25519);
        assertFalse(keyPair.isDestroyed());

        keyPair.close();
        assertTrue(keyPair.isDestroyed());

        assertThrows(IllegalStateException.class, keyPair::getPublicKey);
        assertThrows(IllegalStateException.class, keyPair::getPrivateKey);
        assertThrows(IllegalStateException.class, keyPair::getOpenSshPublicKey);
    }
}
