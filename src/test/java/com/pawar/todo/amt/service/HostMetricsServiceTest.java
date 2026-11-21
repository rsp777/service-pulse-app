package com.pawar.todo.amt.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.ssh.SshCommandService;

// Issue #33: Valid telemetry, invalid output and SSH failures have different outcomes.
class HostMetricsServiceTest {
    @Test void parsesCompleteSample() {
        var value = HostMetricsService.parse(7, "CPU=12.5\nMEMORY=40\nDISK=80\nUPTIME=90061\n");
        assertTrue(value.available());
        assertEquals(7, value.serverId());
        assertEquals(12.5, value.cpuPercent());
        assertEquals(40, value.memoryPercent());
        assertEquals(80, value.diskPercent());
        assertEquals(90061, value.uptimeSeconds());
    }

    @ParameterizedTest @ValueSource(strings = {"NaN", "Infinity", "-1", "101", "oops"})
    void rejectsInvalidPercentages(String invalid) {
        assertThrows(RuntimeException.class, () -> HostMetricsService.parse(1,
                "CPU=" + invalid + "\nMEMORY=40\nDISK=80\nUPTIME=100"));
    }

    @Test void rejectsIncompleteAndDuplicateOutput() {
        assertThrows(RuntimeException.class, () -> HostMetricsService.parse(1, "CPU=1"));
        assertThrows(RuntimeException.class, () -> HostMetricsService.parse(1,
                "CPU=1\nCPU=2\nMEMORY=4\nDISK=8\nUPTIME=100"));
    }

    @Test void executionUsesFixedCommandAndSanitizesErrors() throws Exception {
        var ssh = mock(SshCommandService.class);
        var server = new ServerResponseDto(7, "host", "127.0.0.1", "Linux", "ONLINE", 22, "user", null);
        when(ssh.execute(server, HostMetricsService.METRICS_COMMAND)).thenThrow(new IOException("sensitive detail"));
        var sample = new HostMetricsService(ssh).sample(server);
        assertFalse(sample.available());
        assertNull(sample.cpuPercent());
        assertFalse(sample.message().contains("sensitive"));
        verify(ssh).execute(server, HostMetricsService.METRICS_COMMAND);
    }
}
