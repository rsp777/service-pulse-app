package com.pawar.todo.amt.ssh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.ssh.audit.SshSecurityAuditLogger;
import com.pawar.todo.amt.ssh.auth.SshAuthMode;
import com.pawar.todo.amt.ssh.auth.SshCredentialProvider;

class SshCommandServiceTest {

    private SshProperties properties;
    private SshCredentialProvider credentialProvider;
    private SshSecurityAuditLogger auditLogger;
    private SshCommandService service;

    @BeforeEach
    void setUp() {
        properties = new SshProperties();
        credentialProvider = mock(SshCredentialProvider.class);
        auditLogger = mock(SshSecurityAuditLogger.class);
        service = new SshCommandService(properties, credentialProvider, auditLogger);
    }

    @Test
    void execute_nullServer_throwsIOException() {
        assertThrows(IOException.class, () -> service.execute(null, "uptime"));
    }

    @Test
    void execute_serverWithoutHostOrIp_throwsIOException() {
        ServerResponseDto server = new ServerResponseDto(1, null, null, null, null, null, null, null, null, null, null, null);
        assertThrows(IOException.class, () -> service.execute(server, "uptime"));
    }

    @Test
    void execute_missingUsername_throwsIOExceptionAndLogsAuditFailure() {
        properties.setUsername("");
        properties.setPrivateKeyPath("/path/to/key");
        ServerResponseDto server = new ServerResponseDto(1, "test-host", "192.168.1.50", null, null, null, null, null, null, null, null, null);

        assertThrows(IOException.class, () -> service.execute(server, "uptime"));
        verify(auditLogger).logSessionFailure(eq("192.168.1.50"), any(), any(), anyLong());
    }

    @Test
    void execute_staticModeMissingPrivateKeyPath_throwsIOException() {
        properties.setUsername("servicepulse");
        properties.setAuthMode(SshAuthMode.STATIC);
        properties.setPrivateKeyPath("");
        ServerResponseDto server = new ServerResponseDto(1, "test-host", "192.168.1.50", null, null, null, null, null, null, null, null, null);

        assertThrows(IOException.class, () -> service.execute(server, "uptime"));
    }

    @Test
    void getters_returnConfiguredDependencies() {
        assertEquals(credentialProvider, service.getCredentialProvider());
        assertEquals(auditLogger, service.getAuditLogger());
    }

    // Issue #26: Unit tests for testConnection (+ve and -ve) [Author: Ravindra Singh Pawar / Antigravity]
    @Test
    void testConnection_whenUnreachable_returnsFailureResult() {
        properties.setUsername("servicepulse");
        properties.setPrivateKeyPath("invalid-path");
        ServerResponseDto server = new ServerResponseDto(1, "unreachable-host", "127.0.0.99", null, null, null, null, null, null, null, null, null, 2222, "custom-user", null);

        com.pawar.app.healthcheck.dto.ConnectionTestResult result = service.testConnection(server);
        assertNotNull(result);
        assertFalse(result.connected());
        assertNotNull(result.message());
    }

    @Test
    void testConnection_whenNullServer_returnsFailureGracefully() {
        com.pawar.app.healthcheck.dto.ConnectionTestResult result = service.testConnection(null);
        assertNotNull(result);
        assertFalse(result.connected());
    }
}
