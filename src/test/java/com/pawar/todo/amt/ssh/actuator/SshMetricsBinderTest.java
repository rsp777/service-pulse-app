package com.pawar.todo.amt.ssh.actuator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pawar.todo.amt.ssh.auth.SshAuthMode;
import com.pawar.todo.amt.ssh.auth.SshCredentialMetadata;
import com.pawar.todo.amt.ssh.auth.SshCredentialProvider;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class SshMetricsBinderTest {

    private SshCredentialProvider credentialProvider;
    private SimpleMeterRegistry meterRegistry;
    private SshMetricsBinder metricsBinder;

    @BeforeEach
    void setUp() {
        credentialProvider = mock(SshCredentialProvider.class);
        meterRegistry = new SimpleMeterRegistry();
        metricsBinder = new SshMetricsBinder(credentialProvider);
    }

    @Test
    void bindTo_registersExpectedGaugesWithRegistry() {
        SshCredentialMetadata metadata = new SshCredentialMetadata(
                SshAuthMode.CA_CERTIFICATE,
                "ED25519",
                "9999",
                "key-metric-test",
                List.of("ubuntu"),
                Instant.now().plusSeconds(1800),
                1800L,
                7,
                Instant.now().minusSeconds(600),
                true
        );

        when(credentialProvider.isReady()).thenReturn(true);
        when(credentialProvider.getMetadata()).thenReturn(metadata);

        metricsBinder.bindTo(meterRegistry);

        Gauge ttlGauge = meterRegistry.find("ssh.cert.ttl.seconds").gauge();
        assertNotNull(ttlGauge);
        assertEquals(1800.0, ttlGauge.value());

        Gauge validGauge = meterRegistry.find("ssh.cert.active.valid").gauge();
        assertNotNull(validGauge);
        assertEquals(1.0, validGauge.value());

        Gauge rotationsGauge = meterRegistry.find("ssh.cert.rotations.total").gauge();
        assertNotNull(rotationsGauge);
        assertEquals(7.0, rotationsGauge.value());
    }

    @Test
    void bindTo_whenUnreadyOrExpired_reportsZeroValid() {
        when(credentialProvider.isReady()).thenReturn(false);
        when(credentialProvider.getMetadata()).thenReturn(null);

        metricsBinder.bindTo(meterRegistry);

        Gauge validGauge = meterRegistry.find("ssh.cert.active.valid").gauge();
        assertNotNull(validGauge);
        assertEquals(0.0, validGauge.value());

        Gauge ttlGauge = meterRegistry.find("ssh.cert.ttl.seconds").gauge();
        assertNotNull(ttlGauge);
        assertEquals(0.0, ttlGauge.value());
    }
}
