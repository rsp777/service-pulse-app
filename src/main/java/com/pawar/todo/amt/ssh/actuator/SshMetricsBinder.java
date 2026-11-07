package com.pawar.todo.amt.ssh.actuator;

import org.springframework.stereotype.Component;

import com.pawar.todo.amt.ssh.auth.SshCredentialMetadata;
import com.pawar.todo.amt.ssh.auth.SshCredentialProvider;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Micrometer metrics binder exposing OpenSSH certificate TTL and rotation statistics.
 */
@Component
public class SshMetricsBinder implements MeterBinder {

    private final SshCredentialProvider credentialProvider;

    public SshMetricsBinder(SshCredentialProvider credentialProvider) {
        this.credentialProvider = credentialProvider;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("ssh.cert.ttl.seconds", credentialProvider, this::extractRemainingSeconds)
                .description("Remaining validity Time-To-Live in seconds for the active OpenSSH certificate")
                .register(registry);

        Gauge.builder("ssh.cert.active.valid", credentialProvider, p -> p.isReady() ? 1.0 : 0.0)
                .description("Indicator whether active SSH credential is valid and unexpired (1.0 = valid, 0.0 = expired or unavailable)")
                .register(registry);

        Gauge.builder("ssh.cert.rotations.total", credentialProvider, this::extractTotalRotations)
                .description("Total number of completed SSH certificate rotation cycles")
                .register(registry);
    }

    private double extractRemainingSeconds(SshCredentialProvider provider) {
        if (provider == null) {
            return 0.0;
        }
        SshCredentialMetadata metadata = provider.getMetadata();
        if (metadata == null || metadata.getRemainingSeconds() == null) {
            return 0.0;
        }
        return Math.max(0.0, metadata.getRemainingSeconds());
    }

    private double extractTotalRotations(SshCredentialProvider provider) {
        if (provider == null) {
            return 0.0;
        }
        SshCredentialMetadata metadata = provider.getMetadata();
        return metadata != null ? (double) metadata.getTotalRotations() : 0.0;
    }
}
