package com.pawar.todo.amt.ssh.rotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.pawar.todo.amt.ssh.SshProperties;
import com.pawar.todo.amt.ssh.auth.CaCertificateCredentialProvider;
import com.pawar.todo.amt.ssh.auth.SshAuthMode;
import com.pawar.todo.amt.ssh.ca.SshCaException;
import com.pawar.todo.amt.ssh.ca.SshCertificateAuthorityClient;
import com.pawar.todo.amt.ssh.ca.SshSignedCertificate;
import com.pawar.todo.amt.ssh.cert.OpenSshCertificate;
import com.pawar.todo.amt.ssh.cert.OpenSshCertificateParser;
import com.pawar.todo.amt.ssh.key.DefaultEphemeralKeyPairGenerator;
import com.pawar.todo.amt.ssh.key.EphemeralKeyPair;
import com.pawar.todo.amt.ssh.key.EphemeralKeyPairGenerator;
import com.pawar.todo.amt.ssh.key.KeyAlgorithm;

class SshCertificateRotationEngineTest {

    private SshProperties properties;
    private EphemeralKeyPairGenerator keyPairGenerator;
    private SshCertificateAuthorityClient caClient;
    private CaCertificateCredentialProvider credentialProvider;
    private ApplicationEventPublisher eventPublisher;
    private SshCertificateRotationEngine engine;

    @BeforeEach
    void setUp() {
        properties = new SshProperties();
        properties.setAuthMode(SshAuthMode.CA_CERTIFICATE);
        properties.getCa().setKeyAlgorithm("ED25519");
        properties.getCa().setCertTtlMinutes(60);
        properties.getCa().setRenewThresholdPercentage(75);

        keyPairGenerator = new DefaultEphemeralKeyPairGenerator();
        caClient = mock(SshCertificateAuthorityClient.class);
        credentialProvider = new CaCertificateCredentialProvider();
        eventPublisher = mock(ApplicationEventPublisher.class);

        engine = new SshCertificateRotationEngine(
                properties, keyPairGenerator, caClient, credentialProvider, eventPublisher
        );
    }

    @AfterEach
    void tearDown() {
        engine.stop();
        credentialProvider.close();
    }

    @Test
    void rotateNow_successfulSigning_updatesCredentialProvider() throws Exception {
        String certString = createValidCertString(5555L, 3600);
        OpenSshCertificate parsedCert = OpenSshCertificateParser.parse(certString);
        SshSignedCertificate signedCert = new SshSignedCertificate(certString, parsedCert);

        when(caClient.sign(any(), any())).thenReturn(signedCert);

        ActiveSshCredential result = engine.rotateNow();

        assertNotNull(result);
        assertEquals(5555L, result.getSerial());
        assertTrue(credentialProvider.isReady());
        assertEquals(5555L, credentialProvider.getCurrentCredential().getSerial());

        verify(eventPublisher).publishEvent(any(RotationEvent.class));
    }

    @Test
    void rotateNow_whenCaFails_returnsNullAndSchedulesRetry() {
        when(caClient.sign(any(), any())).thenThrow(new SshCaException("CA service timeout", 504));

        ActiveSshCredential result = engine.rotateNow();

        assertNull(result);
        assertFalse(credentialProvider.isReady());
        verify(eventPublisher).publishEvent(any(RotationEvent.class));
    }

    @Test
    void lifecycle_startAndStop_updatesRunningState() throws Exception {
        String certString = createValidCertString(1234L, 3600);
        OpenSshCertificate parsedCert = OpenSshCertificateParser.parse(certString);
        when(caClient.sign(any(), any())).thenReturn(new SshSignedCertificate(certString, parsedCert));

        engine.start();
        assertTrue(engine.isRunning());

        engine.stop();
        assertFalse(engine.isRunning());
    }

    private String createValidCertString(long serial, long ttlSec) throws IOException {
        String keyType = "ssh-ed25519-cert-v01@openssh.com";
        ByteArrayOutputStream certWire = new ByteArrayOutputStream();
        writeWireString(certWire, keyType.getBytes(StandardCharsets.US_ASCII));
        writeWireString(certWire, new byte[32]);
        writeWireString(certWire, new byte[32]);
        certWire.write(ByteBuffer.allocate(8).putLong(serial).array());
        certWire.write(ByteBuffer.allocate(4).putInt(OpenSshCertificate.TYPE_USER).array());
        writeWireString(certWire, "rotator-test-identity".getBytes(StandardCharsets.UTF_8));
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
