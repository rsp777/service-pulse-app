package com.pawar.todo.amt.ssh.it;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.pawar.todo.amt.ssh.cert.OpenSshCertificate;
import com.pawar.todo.amt.ssh.key.OpenSshKeyEncoder;

/**
 * In-memory SSH Certificate Authority for integration testing.
 * Cryptographically signs OpenSSH User and Host certificates using Ed25519.
 */
public class TestSshCertificateAuthority {

    private final KeyPair caKeyPair;
    private final String caPublicKeyWire;
    private final AtomicLong serialGenerator = new AtomicLong(1000);
    private final SecureRandom random = new SecureRandom();

    public TestSshCertificateAuthority() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
            this.caKeyPair = kpg.generateKeyPair();
            this.caPublicKeyWire = OpenSshKeyEncoder.encodePublicKey(caKeyPair.getPublic(), "test-ca@servicepulse");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize TestSshCertificateAuthority", e);
        }
    }

    public PublicKey getCaPublicKey() {
        return caKeyPair.getPublic();
    }

    public PrivateKey getCaPrivateKey() {
        return caKeyPair.getPrivate();
    }

    public String getCaPublicKeyWire() {
        return caPublicKeyWire;
    }

    public String signUserCertificate(PublicKey subjectPublicKey, String keyId, List<String> principals, Duration ttl) {
        return signCertificate(subjectPublicKey, keyId, principals, ttl, OpenSshCertificate.TYPE_USER, false);
    }

    public String signHostCertificate(PublicKey subjectPublicKey, String keyId, List<String> principals, Duration ttl) {
        return signCertificate(subjectPublicKey, keyId, principals, ttl, OpenSshCertificate.TYPE_HOST, false);
    }

    public String signExpiredCertificate(PublicKey subjectPublicKey, String keyId, List<String> principals) {
        return signCertificate(subjectPublicKey, keyId, principals, Duration.ofMinutes(10), OpenSshCertificate.TYPE_USER, true);
    }

    public String signCertificate(PublicKey subjectPublicKey,
                                  String keyId,
                                  List<String> principals,
                                  Duration ttl,
                                  int certType,
                                  boolean expired) {
        try {
            String certKeyType = "ssh-ed25519-cert-v01@openssh.com";
            byte[] nonce = new byte[32];
            random.nextBytes(nonce);

            byte[] subjectKeyBytes = OpenSshKeyEncoder.encodePublicKeyBytes(subjectPublicKey);
            // Extract raw public key (last 32 bytes)
            byte[] rawSubjectKey = new byte[32];
            System.arraycopy(subjectKeyBytes, subjectKeyBytes.length - 32, rawSubjectKey, 0, 32);

            long serial = serialGenerator.incrementAndGet();

            ByteArrayOutputStream principalsBuf = new ByteArrayOutputStream();
            if (principals != null) {
                for (String p : principals) {
                    writeWireString(principalsBuf, p.getBytes(StandardCharsets.UTF_8));
                }
            }

            Instant now = Instant.now();
            long validAfterSec;
            long validBeforeSec;

            if (expired) {
                validAfterSec = now.minusSeconds(7200).getEpochSecond();
                validBeforeSec = now.minusSeconds(3600).getEpochSecond();
            } else {
                validAfterSec = now.minusSeconds(60).getEpochSecond();
                validBeforeSec = now.plus(ttl != null ? ttl : Duration.ofHours(1)).getEpochSecond();
            }

            ByteArrayOutputStream certBody = new ByteArrayOutputStream();
            writeWireString(certBody, certKeyType.getBytes(StandardCharsets.US_ASCII));
            writeWireString(certBody, nonce);
            writeWireString(certBody, rawSubjectKey);
            certBody.write(ByteBuffer.allocate(8).putLong(serial).array());
            certBody.write(ByteBuffer.allocate(4).putInt(certType).array());
            writeWireString(certBody, (keyId != null ? keyId : "servicepulse").getBytes(StandardCharsets.UTF_8));
            writeWireString(certBody, principalsBuf.toByteArray());
            certBody.write(ByteBuffer.allocate(8).putLong(validAfterSec).array());
            certBody.write(ByteBuffer.allocate(8).putLong(validBeforeSec).array());

            // Critical options (empty)
            writeWireString(certBody, new byte[0]);

            // Extensions (e.g. permit-pty)
            ByteArrayOutputStream extBuf = new ByteArrayOutputStream();
            writeWireString(extBuf, "permit-pty".getBytes(StandardCharsets.UTF_8));
            writeWireString(extBuf, new byte[0]); // empty value for standard boolean flag extension
            writeWireString(certBody, extBuf.toByteArray());

            // Reserved (empty)
            writeWireString(certBody, new byte[0]);

            // Signature key (wire format of CA public key)
            byte[] caPubKeyWireBytes = OpenSshKeyEncoder.encodePublicKeyBytes(caKeyPair.getPublic());
            writeWireString(certBody, caPubKeyWireBytes);

            byte[] bodyToSign = certBody.toByteArray();

            // Sign using Ed25519
            Signature sig = Signature.getInstance("Ed25519");
            sig.initSign(caKeyPair.getPrivate());
            sig.update(bodyToSign);
            byte[] rawSig = sig.sign();

            // Signature blob format: string("ssh-ed25519") + string(rawSig)
            ByteArrayOutputStream sigBlob = new ByteArrayOutputStream();
            writeWireString(sigBlob, "ssh-ed25519".getBytes(StandardCharsets.US_ASCII));
            writeWireString(sigBlob, rawSig);

            // Append signature blob as string to cert
            ByteArrayOutputStream fullCert = new ByteArrayOutputStream();
            fullCert.write(bodyToSign);
            writeWireString(fullCert, sigBlob.toByteArray());

            String base64 = Base64.getEncoder().encodeToString(fullCert.toByteArray());
            return certKeyType + " " + base64 + " " + (keyId != null ? keyId : "servicepulse-cert");

        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign OpenSSH certificate", e);
        }
    }

    private static void writeWireString(ByteArrayOutputStream out, byte[] bytes) throws IOException {
        out.write(ByteBuffer.allocate(4).putInt(bytes.length).array());
        out.write(bytes);
    }
}
