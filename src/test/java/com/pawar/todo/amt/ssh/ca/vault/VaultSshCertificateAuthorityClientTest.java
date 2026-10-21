package com.pawar.todo.amt.ssh.ca.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.pawar.todo.amt.ssh.SshProperties;
import com.pawar.todo.amt.ssh.ca.CertificateSigningOptions;
import com.pawar.todo.amt.ssh.ca.SshCaException;
import com.pawar.todo.amt.ssh.ca.SshSignedCertificate;
import com.pawar.todo.amt.ssh.cert.OpenSshCertificate;

class VaultSshCertificateAuthorityClientTest {

    private SshProperties properties;
    private RestTemplate restTemplate;
    private VaultSshCertificateAuthorityClient client;

    @BeforeEach
    void setUp() {
        properties = new SshProperties();
        properties.getCa().setEndpoint("http://vault.internal:8200");
        properties.getCa().setToken("hvs.test-vault-token");
        properties.getCa().setMount("ssh-client-signer");
        properties.getCa().setRole("servicepulse-runner");

        restTemplate = mock(RestTemplate.class);
        client = new VaultSshCertificateAuthorityClient(properties, restTemplate);
    }

    @Test
    void sign_successfulVaultResponse_returnsSshSignedCertificate() throws Exception {
        String certString = createSampleCertString();
        String vaultResponseJson = "{\"data\": {\"signed_key\": \"" + certString + "\", \"serial_number\": 1001}}";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(vaultResponseJson, HttpStatus.OK);
        when(restTemplate.exchange(
                eq("http://vault.internal:8200/v1/ssh-client-signer/sign/servicepulse-runner"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        CertificateSigningOptions options = CertificateSigningOptions.builder()
                .principal("servicepulse")
                .ttl(Duration.ofMinutes(30))
                .build();

        SshSignedCertificate result = client.sign("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5... test", options);

        assertNotNull(result);
        assertEquals(certString, result.getRawCertificate());
        assertEquals(1001L, result.getSerial());
        assertEquals("servicepulse-vault-test", result.getParsedCertificate().getKeyId());
    }

    @Test
    void sign_httpError_throwsSshCaExceptionWithStatusCode() {
        HttpClientErrorException forbidden = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "Forbidden", null, null, null
        );
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(forbidden);

        CertificateSigningOptions options = CertificateSigningOptions.builder().build();
        SshCaException exception = assertThrows(SshCaException.class, () ->
                client.sign("ssh-ed25519 AAA... test", options)
        );

        assertEquals(403, exception.getStatusCode());
    }

    @Test
    void sign_missingEndpoint_throwsSshCaException() {
        properties.getCa().setEndpoint("");
        CertificateSigningOptions options = CertificateSigningOptions.builder().build();

        assertThrows(SshCaException.class, () -> client.sign("ssh-ed25519 AAA... test", options));
    }

    private String createSampleCertString() throws IOException {
        String keyType = "ssh-ed25519-cert-v01@openssh.com";
        ByteArrayOutputStream certWire = new ByteArrayOutputStream();
        writeWireString(certWire, keyType.getBytes(StandardCharsets.US_ASCII));
        writeWireString(certWire, new byte[32]);
        writeWireString(certWire, new byte[32]);
        certWire.write(ByteBuffer.allocate(8).putLong(1001L).array());
        certWire.write(ByteBuffer.allocate(4).putInt(OpenSshCertificate.TYPE_USER).array());
        writeWireString(certWire, "servicepulse-vault-test".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream principals = new ByteArrayOutputStream();
        writeWireString(principals, "servicepulse".getBytes(StandardCharsets.UTF_8));
        writeWireString(certWire, principals.toByteArray());
        certWire.write(ByteBuffer.allocate(8).putLong(Instant.now().getEpochSecond()).array());
        certWire.write(ByteBuffer.allocate(8).putLong(Instant.now().getEpochSecond() + 1800).array());
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, new byte[0]);
        writeWireString(certWire, new byte[32]);
        writeWireString(certWire, new byte[64]);

        return keyType + " " + Base64.getEncoder().encodeToString(certWire.toByteArray()) + " vault-signed";
    }

    private static void writeWireString(ByteArrayOutputStream out, byte[] bytes) throws IOException {
        out.write(ByteBuffer.allocate(4).putInt(bytes.length).array());
        out.write(bytes);
    }
}
