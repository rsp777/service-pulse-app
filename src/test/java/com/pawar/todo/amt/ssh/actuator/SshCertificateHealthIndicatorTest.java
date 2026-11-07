package com.pawar.todo.amt.ssh.actuator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import com.pawar.todo.amt.ssh.SshProperties;
import com.pawar.todo.amt.ssh.auth.SshAuthMode;
import com.pawar.todo.amt.ssh.auth.SshCredentialMetadata;
import com.pawar.todo.amt.ssh.auth.SshCredentialProvider;

class SshCertificateHealthIndicatorTest {

    private SshProperties properties;
    private SshCredentialProvider credentialProvider;
    private SshCertificateHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        properties = new SshProperties();
        credentialProvider = mock(SshCredentialProvider.class);
        healthIndicator = new SshCertificateHealthIndicator(properties, credentialProvider);
    }

    @Test
    void health_staticMode_whenReady_returnsUp() {
        properties.setAuthMode(SshAuthMode.STATIC);
        when(credentialProvider.isReady()).thenReturn(true);
        when(credentialProvider.getMetadata()).thenReturn(SshCredentialMetadata.staticKey("STATIC", true));

        Health health = healthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("STATIC", health.getDetails().get("authMode"));
    }

    @Test
    void health_staticMode_whenNotReady_returnsDown() {
        properties.setAuthMode(SshAuthMode.STATIC);
        when(credentialProvider.isReady()).thenReturn(false);
        when(credentialProvider.getMetadata()).thenReturn(SshCredentialMetadata.staticKey("STATIC", false));

        Health health = healthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("KEY_FILE_MISSING_OR_UNREADABLE", health.getDetails().get("status"));
    }

    @Test
    void health_caMode_whenHealthy_returnsUpWithDetails() {
        properties.setAuthMode(SshAuthMode.CA_CERTIFICATE);
        when(credentialProvider.isReady()).thenReturn(true);

        SshCredentialMetadata metadata = new SshCredentialMetadata(
                SshAuthMode.CA_CERTIFICATE,
                "ED25519",
                "12345",
                "key-id-1",
                List.of("ubuntu"),
                Instant.now().plusSeconds(3600),
                3600L,
                5,
                Instant.now().minusSeconds(1800),
                true
        );
        when(credentialProvider.getMetadata()).thenReturn(metadata);

        Health health = healthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("12345", health.getDetails().get("certificateSerial"));
        assertEquals(3600L, health.getDetails().get("remainingSeconds"));
        assertEquals(5L, health.getDetails().get("totalRotations"));
    }

    @Test
    void health_caMode_whenRenewalUrgent_returnsOutOfService() {
        properties.setAuthMode(SshAuthMode.CA_CERTIFICATE);
        when(credentialProvider.isReady()).thenReturn(true);

        SshCredentialMetadata metadata = new SshCredentialMetadata(
                SshAuthMode.CA_CERTIFICATE,
                "ED25519",
                "12345",
                "key-id-urgent",
                List.of("ubuntu"),
                Instant.now().plusSeconds(120),
                120L, // only 2 minutes remaining!
                3,
                Instant.now(),
                true
        );
        when(credentialProvider.getMetadata()).thenReturn(metadata);

        Health health = healthIndicator.health();

        assertEquals(Status.OUT_OF_SERVICE, health.getStatus());
        assertEquals("CERTIFICATE_RENEWAL_URGENT", health.getDetails().get("status"));
    }

    @Test
    void health_caMode_whenExpiredOrUnavailable_returnsDown() {
        properties.setAuthMode(SshAuthMode.CA_CERTIFICATE);
        when(credentialProvider.isReady()).thenReturn(false);
        when(credentialProvider.getMetadata()).thenReturn(null);

        Health health = healthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("CERTIFICATE_EXPIRED_OR_UNAVAILABLE", health.getDetails().get("status"));
    }
}
