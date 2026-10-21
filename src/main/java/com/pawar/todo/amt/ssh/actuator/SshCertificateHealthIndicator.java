package com.pawar.todo.amt.ssh.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.pawar.todo.amt.ssh.SshProperties;
import com.pawar.todo.amt.ssh.auth.SshAuthMode;
import com.pawar.todo.amt.ssh.auth.SshCredentialMetadata;
import com.pawar.todo.amt.ssh.auth.SshCredentialProvider;

/**
 * Spring Boot Actuator HealthIndicator reporting SSH authentication and certificate health.
 */
@Component
public class SshCertificateHealthIndicator implements HealthIndicator {

    private final SshProperties properties;
    private final SshCredentialProvider credentialProvider;

    public SshCertificateHealthIndicator(SshProperties properties, SshCredentialProvider credentialProvider) {
        this.properties = properties;
        this.credentialProvider = credentialProvider;
    }

    @Override
    public Health health() {
        SshCredentialMetadata metadata = credentialProvider.getMetadata();
        SshAuthMode authMode = properties.getAuthMode();

        if (authMode == SshAuthMode.STATIC) {
            if (credentialProvider.isReady()) {
                return Health.up()
                        .withDetail("authMode", "STATIC")
                        .withDetail("status", "READY")
                        .withDetail("keyConfigured", true)
                        .build();
            } else {
                return Health.down()
                        .withDetail("authMode", "STATIC")
                        .withDetail("status", "KEY_FILE_MISSING_OR_UNREADABLE")
                        .withDetail("keyConfigured", false)
                        .build();
            }
        }

        // CA_CERTIFICATE mode
        if (metadata == null || !credentialProvider.isReady()) {
            return Health.down()
                    .withDetail("authMode", "CA_CERTIFICATE")
                    .withDetail("status", "CERTIFICATE_EXPIRED_OR_UNAVAILABLE")
                    .withDetail("totalRotations", metadata != null ? metadata.getTotalRotations() : 0)
                    .build();
        }

        Long remainingSeconds = metadata.getRemainingSeconds();
        if (remainingSeconds != null && remainingSeconds < 300) {
            // Less than 5 minutes remaining
            return Health.outOfService()
                    .withDetail("authMode", "CA_CERTIFICATE")
                    .withDetail("status", "CERTIFICATE_RENEWAL_URGENT")
                    .withDetail("certificateSerial", metadata.getCertificateSerial())
                    .withDetail("keyId", metadata.getKeyId())
                    .withDetail("remainingSeconds", remainingSeconds)
                    .withDetail("totalRotations", metadata.getTotalRotations())
                    .build();
        }

        return Health.up()
                .withDetail("authMode", "CA_CERTIFICATE")
                .withDetail("keyAlgorithm", metadata.getKeyAlgorithm())
                .withDetail("certificateSerial", metadata.getCertificateSerial())
                .withDetail("keyId", metadata.getKeyId())
                .withDetail("principals", metadata.getPrincipals())
                .withDetail("validUntil", metadata.getValidUntil())
                .withDetail("remainingSeconds", remainingSeconds)
                .withDetail("totalRotations", metadata.getTotalRotations())
                .withDetail("lastRotatedAt", metadata.getLastRotatedAt())
                .build();
    }
}
