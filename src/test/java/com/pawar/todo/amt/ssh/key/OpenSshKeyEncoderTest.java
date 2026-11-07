package com.pawar.todo.amt.ssh.key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class OpenSshKeyEncoderTest {

    @Test
    void encodePublicKey_withEd25519Key_formatsRFC4253WireString() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        KeyPair kp = kpg.generateKeyPair();

        String comment = "test-host@domain";
        String encoded = OpenSshKeyEncoder.encodePublicKey(kp.getPublic(), comment);

        assertNotNull(encoded);
        assertTrue(encoded.startsWith("ssh-ed25519 "));
        assertTrue(encoded.endsWith(" " + comment));

        String[] parts = encoded.split(" ");
        assertEquals(3, parts.length);
        assertEquals("ssh-ed25519", parts[0]);
        assertEquals(comment, parts[2]);

        byte[] wireBytes = Base64.getDecoder().decode(parts[1]);
        ByteBuffer buf = ByteBuffer.wrap(wireBytes);
        int typeLen = buf.getInt();
        byte[] typeBytes = new byte[typeLen];
        buf.get(typeBytes);
        assertEquals("ssh-ed25519", new String(typeBytes, StandardCharsets.US_ASCII));

        int keyLen = buf.getInt();
        assertEquals(32, keyLen);
        byte[] rawKey = new byte[keyLen];
        buf.get(rawKey);
        assertEquals(0, buf.remaining());
    }

    @Test
    void encodePublicKey_withRsaKey_formatsRFC4253WireString() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        String comment = "test-rsa-comment";
        String encoded = OpenSshKeyEncoder.encodePublicKey(kp.getPublic(), comment);

        assertNotNull(encoded);
        assertTrue(encoded.startsWith("ssh-rsa "));
        assertTrue(encoded.endsWith(" " + comment));

        String[] parts = encoded.split(" ");
        assertEquals(3, parts.length);
        assertEquals("ssh-rsa", parts[0]);

        byte[] wireBytes = Base64.getDecoder().decode(parts[1]);
        ByteBuffer buf = ByteBuffer.wrap(wireBytes);
        int typeLen = buf.getInt();
        byte[] typeBytes = new byte[typeLen];
        buf.get(typeBytes);
        assertEquals("ssh-rsa", new String(typeBytes, StandardCharsets.US_ASCII));

        // Read public exponent mpint
        int eLen = buf.getInt();
        assertTrue(eLen > 0);
        byte[] eBytes = new byte[eLen];
        buf.get(eBytes);

        // Read modulus mpint
        int nLen = buf.getInt();
        assertTrue(nLen > 0);
        byte[] nBytes = new byte[nLen];
        buf.get(nBytes);

        assertEquals(0, buf.remaining());
    }
}
