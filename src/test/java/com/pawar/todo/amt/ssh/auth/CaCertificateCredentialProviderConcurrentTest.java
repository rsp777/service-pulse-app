package com.pawar.todo.amt.ssh.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pawar.todo.amt.ssh.cert.OpenSshCertificate;
import com.pawar.todo.amt.ssh.cert.OpenSshCertificateParser;
import com.pawar.todo.amt.ssh.key.DefaultEphemeralKeyPairGenerator;
import com.pawar.todo.amt.ssh.key.EphemeralKeyPair;
import com.pawar.todo.amt.ssh.key.KeyAlgorithm;
import com.pawar.todo.amt.ssh.rotation.ActiveSshCredential;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

class CaCertificateCredentialProviderConcurrentTest {

    private CaCertificateCredentialProvider provider;
    private DefaultEphemeralKeyPairGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        provider = new CaCertificateCredentialProvider();
        keyGenerator = new DefaultEphemeralKeyPairGenerator();
    }

    @AfterEach
    void tearDown() {
        provider.close();
    }

    @Test
    void getKeyProvider_whenNoCredential_throwsIOException() {
        assertThrows(IOException.class, () -> provider.getKeyProvider(new SSHClient()));
        assertFalse(provider.isReady());
    }

    @Test
    void updateCredential_andConcurrentReads_preservesSessionContinuity() throws Exception {
        ActiveSshCredential initial = createCredential(1001L, 3600);
        provider.updateCredential(initial, Duration.ofSeconds(10));
        assertTrue(provider.isReady());

        int threadCount = 16;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        KeyProvider kp = provider.getKeyProvider(new SSHClient());
                        if (kp != null && kp.getPublic() != null) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    // failed read
                } finally {
                    latch.countDown();
                }
            });
        }

        // Mid-flight rotation
        Thread.sleep(20);
        ActiveSshCredential rotated = createCredential(1002L, 3600);
        provider.updateCredential(rotated, Duration.ofSeconds(10));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdownNow();

        assertEquals(threadCount * operationsPerThread, successCount.get());
        assertEquals(1002L, provider.getCurrentCredential().getSerial());
        assertEquals(2, provider.getMetadata().getTotalRotations());
    }

    private ActiveSshCredential createCredential(long serial, long ttlSec) throws IOException {
        EphemeralKeyPair kp = keyGenerator.generateKeyPair(KeyAlgorithm.ED25519);
        String certString = createValidCertString(serial, ttlSec);
        OpenSshCertificate cert = OpenSshCertificateParser.parse(certString);
        return new ActiveSshCredential(kp, cert);
    }

    private String createValidCertString(long serial, long ttlSec) throws IOException {
        String keyType = "ssh-ed25519-cert-v01@openssh.com";
        ByteArrayOutputStream certWire = new ByteArrayOutputStream();
        writeWireString(certWire, keyType.getBytes(StandardCharsets.US_ASCII));
        writeWireString(certWire, new byte[32]);
        writeWireString(certWire, new byte[32]);
        certWire.write(ByteBuffer.allocate(8).putLong(serial).array());
        certWire.write(ByteBuffer.allocate(4).putInt(OpenSshCertificate.TYPE_USER).array());
        writeWireString(certWire, "concurrent-test".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream principals = new ByteArrayOutputStream();
        writeWireString(principals, "servicepulse".getBytes(StandardCharsets.UTF_8));
        writeWireString(certWire, principals.toByteArray());
        long now = Instant.now().getEpochSecond();
        certWire.write(ByteBuffer.allocate(8).putLong(now).array());
        certWire.write(ByteBuffer.allocate(8).putLong(now + ttlSec).array());
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, new byte[32]);
        writeWireString(certWire, new byte[64]);

        return keyType + " " + Base64.getEncoder().encodeToString(certWire.toByteArray()) + " test";
    }

    private static void writeWireString(ByteArrayOutputStream out, byte[] bytes) throws IOException {
        out.write(ByteBuffer.allocate(4).putInt(bytes.length).array());
        out.write(bytes);
    }
}
