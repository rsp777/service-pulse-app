package com.pawar.todo.amt.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.response.HostMetricsResponse;
import com.pawar.todo.amt.service.HostMetricsService;
import com.pawar.todo.amt.service.ServerService;

// Issue #33: Verify actual HTTP mapping and safe absence/failure responses.
class HostMetricsControllerTest {
    @Test void registeredHostReturnsUnavailableInsteadOfFabricatedZeros() throws Exception {
        var servers = mock(ServerService.class);
        var metrics = mock(HostMetricsService.class);
        var server = new ServerResponseDto(1, "host", "127.0.0.1", "Linux", "UNKNOWN", 22, "user", null);
        when(servers.findServerById(1)).thenReturn(Optional.of(server));
        when(metrics.sample(server)).thenReturn(HostMetricsResponse.unavailable(1));
        MockMvcBuilders.standaloneSetup(new HostMetricsController(servers, metrics)).build()
            .perform(get("/api/servers/1/metrics")).andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(false)).andExpect(jsonPath("$.cpuPercent").doesNotExist());
    }

    @Test void missingHostNeverExecutesSsh() throws Exception {
        var servers = mock(ServerService.class);
        var metrics = mock(HostMetricsService.class);
        when(servers.findServerById(99)).thenReturn(Optional.empty());
        MockMvcBuilders.standaloneSetup(new HostMetricsController(servers, metrics)).build()
            .perform(get("/api/servers/99/metrics")).andExpect(status().isNotFound());
        verifyNoInteractions(metrics);
    }
}
