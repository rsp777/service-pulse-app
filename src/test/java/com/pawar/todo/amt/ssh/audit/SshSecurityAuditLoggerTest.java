package com.pawar.todo.amt.ssh.audit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pawar.todo.amt.ssh.auth.SshAuthMode;
import com.pawar.todo.amt.ssh.auth.SshCredentialMetadata;

class SshSecurityAuditLoggerTest {

    private SshSecurityAuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        auditLogger = new SshSecurityAuditLogger();
    }

    @Test
    void logSessionStart_withValidMetadata_doesNotThrow() {
        SshCredentialMetadata metadata = new SshCredentialMetadata(
                SshAuthMode.CA_CERTIFICATE,
                "ED25519",
                "98765",
                "test-key",
                List.of("ubuntu"),
                null,
                null,
                1,
                null,
                true
        );

        assertDoesNotThrow(() ->
                auditLogger.logSessionStart("192.168.1.100", "ubuntu", metadata, "echo 'hello world'")
        );
    }

    @Test
    void logSessionStart_withNullMetadataAndNullCommand_doesNotThrow() {
        assertDoesNotThrow(() ->
                auditLogger.logSessionStart("10.0.0.1", "admin", null, null)
        );
    }

    @Test
    void logSessionEnd_executesCleanly() {
        assertDoesNotThrow(() ->
                auditLogger.logSessionEnd("10.0.0.1", "admin", 0, 150L)
        );
    }

    @Test
    void logSessionFailure_executesCleanly() {
        assertDoesNotThrow(() ->
                auditLogger.logSessionFailure("10.0.0.1", "admin", "Connection refused\nHost unreachable", 300L)
        );
    }
}
