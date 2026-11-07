package com.pawar.todo.amt.ssh.verifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pawar.todo.amt.ssh.cert.OpenSshCertificate;
import com.pawar.todo.amt.ssh.cert.OpenSshCertificateParser;

class HostCertificateVerifierTest {

    private byte[] hostCaRawKey;
    private String hostCaPublicKeyString;
    private HostCertificateVerifier verifier;

    @BeforeEach
    void setUp() {
        hostCaRawKey = new byte[32];
        for (int i = 0; i < 32; i++) {
            hostCaRawKey[i] = (byte) (i + 1);
        }
        hostCaPublicKeyString = "ssh-ed25519 " + Base64.getEncoder().encodeToString(hostCaRawKey) + " ca-2026";
        verifier = new HostCertificateVerifier(hostCaPublicKeyString);
    }

    @Test
    void verifyHostCertificate_validCertWithMatchingHostname_returnsTrue() throws IOException {
        String certString = createHostCertString(
                List.of("node1.internal.net", "*.internal.net"),
                hostCaRawKey,
                3600,
                OpenSshCertificate.TYPE_HOST
        );
        OpenSshCertificate cert = OpenSshCertificateParser.parse(certString);

        assertTrue(verifier.verifyHostCertificate("node1.internal.net", 22, cert));
        assertTrue(verifier.verifyHostCertificate("node2.internal.net", 22, cert)); // matches wildcard
    }

    @Test
    void verifyHostCertificate_mismatchedHostname_returnsFalse() throws IOException {
        String certString = createHostCertString(
                List.of("node1.internal.net"),
                hostCaRawKey,
                3600,
                OpenSshCertificate.TYPE_HOST
        );
        OpenSshCertificate cert = OpenSshCertificateParser.parse(certString);

        assertFalse(verifier.verifyHostCertificate("rogue.external.com", 22, cert));
    }

    @Test
    void verifyHostCertificate_expiredCertificate_returnsFalse() throws IOException {
        String certString = createExpiredHostCertString(List.of("node1.internal.net"), hostCaRawKey);
        OpenSshCertificate cert = OpenSshCertificateParser.parse(certString);

        assertFalse(verifier.verifyHostCertificate("node1.internal.net", 22, cert));
    }

    @Test
    void verifyHostCertificate_untrustedCaKey_returnsFalse() throws IOException {
        byte[] differentCaKey = new byte[32];
        differentCaKey[0] = 99;

        String certString = createHostCertString(
                List.of("node1.internal.net"),
                differentCaKey,
                3600,
                OpenSshCertificate.TYPE_HOST
        );
        OpenSshCertificate cert = OpenSshCertificateParser.parse(certString);

        assertFalse(verifier.verifyHostCertificate("node1.internal.net", 22, cert));
    }

    @Test
    void verifyHostCertificate_userCertPresentedAsHostCert_returnsFalse() throws IOException {
        String certString = createHostCertString(
                List.of("node1.internal.net"),
                hostCaRawKey,
                3600,
                OpenSshCertificate.TYPE_USER // USER cert instead of HOST cert!
        );
        OpenSshCertificate cert = OpenSshCertificateParser.parse(certString);

        assertFalse(verifier.verifyHostCertificate("node1.internal.net", 22, cert));
    }

    private String createHostCertString(List<String> principals, byte[] caKeyBytes, long ttlSec, int certType) throws IOException {
        String keyType = "ssh-ed25519-cert-v01@openssh.com";
        ByteArrayOutputStream certWire = new ByteArrayOutputStream();
        writeWireString(certWire, keyType.getBytes(StandardCharsets.US_ASCII));
        writeWireString(certWire, new byte[32]);
        writeWireString(certWire, new byte[32]);
        certWire.write(ByteBuffer.allocate(8).putLong(999L).array());
        certWire.write(ByteBuffer.allocate(4).putInt(certType).array());
        writeWireString(certWire, "host-id".getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream principalsBuf = new ByteArrayOutputStream();
        for (String p : principals) {
            writeWireString(principalsBuf, p.getBytes(StandardCharsets.UTF_8));
        }
        writeWireString(certWire, principalsBuf.toByteArray());

        long now = Instant.now().getEpochSecond();
        certWire.write(ByteBuffer.allocate(8).putLong(now).array());
        certWire.write(ByteBuffer.allocate(8).putLong(now + ttlSec).array());
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, caKeyBytes);
        writeWireString(certWire, new byte[64]);

        return keyType + " " + Base64.getEncoder().encodeToString(certWire.toByteArray()) + " host-cert";
    }

    private String createExpiredHostCertString(List<String> principals, byte[] caKeyBytes) throws IOException {
        String keyType = "ssh-ed25519-cert-v01@openssh.com";
        ByteArrayOutputStream certWire = new ByteArrayOutputStream();
        writeWireString(certWire, keyType.getBytes(StandardCharsets.US_ASCII));
        writeWireString(certWire, new byte[32]);
        writeWireString(certWire, new byte[32]);
        certWire.write(ByteBuffer.allocate(8).putLong(1L).array());
        certWire.write(ByteBuffer.allocate(4).putInt(OpenSshCertificate.TYPE_HOST).array());
        writeWireString(certWire, "expired-host".getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream principalsBuf = new ByteArrayOutputStream();
        for (String p : principals) {
            writeWireString(principalsBuf, p.getBytes(StandardCharsets.UTF_8));
        }
        writeWireString(certWire, principalsBuf.toByteArray());

        certWire.write(ByteBuffer.allocate(8).putLong(100L).array());
        certWire.write(ByteBuffer.allocate(8).putLong(500L).array()); // expired long ago
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, caKeyBytes);
        writeWireString(certWire, new byte[64]);

        return keyType + " " + Base64.getEncoder().encodeToString(certWire.toByteArray()) + " expired";
    }

    private static void writeWireString(ByteArrayOutputStream out, byte[] bytes) throws IOException {
        out.write(ByteBuffer.allocate(4).putInt(bytes.length).array());
        out.write(bytes);
    }
}
