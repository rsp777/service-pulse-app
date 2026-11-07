package com.pawar.todo.amt.ssh.cert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

class OpenSshCertificateParserTest {

    @Test
    void parse_validEd25519UserCertificate_extractsAllFields() throws IOException {
        String keyType = "ssh-ed25519-cert-v01@openssh.com";
        byte[] nonce = new byte[32];
        byte[] pubKey = new byte[32];
        long serial = 9876543210L;
        int type = OpenSshCertificate.TYPE_USER;
        String keyId = "servicepulse-test-identity";
        long validAfterSec = 1700000000L;
        long validBeforeSec = Instant.now().getEpochSecond() + 3600; // 1 hour in future

        ByteArrayOutputStream principalsBuf = new ByteArrayOutputStream();
        writeWireString(principalsBuf, "ubuntu".getBytes(StandardCharsets.UTF_8));
        writeWireString(principalsBuf, "servicepulse".getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream certWire = new ByteArrayOutputStream();
        writeWireString(certWire, keyType.getBytes(StandardCharsets.US_ASCII));
        writeWireString(certWire, nonce);
        writeWireString(certWire, pubKey);
        certWire.write(ByteBuffer.allocate(8).putLong(serial).array());
        certWire.write(ByteBuffer.allocate(4).putInt(type).array());
        writeWireString(certWire, keyId.getBytes(StandardCharsets.UTF_8));
        writeWireString(certWire, principalsBuf.toByteArray());
        certWire.write(ByteBuffer.allocate(8).putLong(validAfterSec).array());
        certWire.write(ByteBuffer.allocate(8).putLong(validBeforeSec).array());
        writeWireString(certWire, new byte[0]); // critical options
        writeWireString(certWire, new byte[0]); // extensions
        writeWireString(certWire, new byte[0]); // reserved
        writeWireString(certWire, new byte[32]); // signature key
        writeWireString(certWire, new byte[64]); // signature

        String base64 = Base64.getEncoder().encodeToString(certWire.toByteArray());
        String certString = keyType + " " + base64 + " comment-test";

        OpenSshCertificate cert = OpenSshCertificateParser.parse(certString);

        assertNotNull(cert);
        assertEquals(keyType, cert.getKeyType());
        assertEquals(serial, cert.getSerial());
        assertEquals(type, cert.getType());
        assertTrue(cert.isUserCertificate());
        assertFalse(cert.isHostCertificate());
        assertEquals(keyId, cert.getKeyId());
        assertEquals(List.of("ubuntu", "servicepulse"), cert.getValidPrincipals());
        assertEquals(Instant.ofEpochSecond(validAfterSec), cert.getValidAfter());
        assertEquals(Instant.ofEpochSecond(validBeforeSec), cert.getValidBefore());
        assertFalse(cert.isExpired());
        assertTrue(cert.getRemainingTtl().toSeconds() > 0);
    }

    @Test
    void parse_expiredCertificate_reportsExpired() throws IOException {
        String keyType = "ssh-ed25519-cert-v01@openssh.com";
        ByteArrayOutputStream certWire = new ByteArrayOutputStream();
        writeWireString(certWire, keyType.getBytes(StandardCharsets.US_ASCII));
        writeWireString(certWire, new byte[32]);
        writeWireString(certWire, new byte[32]);
        certWire.write(ByteBuffer.allocate(8).putLong(1L).array());
        certWire.write(ByteBuffer.allocate(4).putInt(OpenSshCertificate.TYPE_USER).array());
        writeWireString(certWire, "test".getBytes(StandardCharsets.UTF_8));
        writeWireString(certWire, new byte[0]);
        certWire.write(ByteBuffer.allocate(8).putLong(1000L).array());
        certWire.write(ByteBuffer.allocate(8).putLong(2000L).array()); // expired in 1970
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, new byte[32]);
        writeWireString(certWire, new byte[64]);

        String certString = keyType + " " + Base64.getEncoder().encodeToString(certWire.toByteArray());
        OpenSshCertificate cert = OpenSshCertificateParser.parse(certString);

        assertTrue(cert.isExpired());
        assertEquals(0, cert.getRemainingTtl().toSeconds());
    }

    @Test
    void parse_invalidFormat_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> OpenSshCertificateParser.parse(null));
        assertThrows(IllegalArgumentException.class, () -> OpenSshCertificateParser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> OpenSshCertificateParser.parse("single-token"));
        assertThrows(IllegalArgumentException.class, () -> OpenSshCertificateParser.parse("ssh-ed25519-cert-v01 not-base-64!!!"));
    }

    private static void writeWireString(ByteArrayOutputStream out, byte[] bytes) throws IOException {
        out.write(ByteBuffer.allocate(4).putInt(bytes.length).array());
        out.write(bytes);
    }
}
