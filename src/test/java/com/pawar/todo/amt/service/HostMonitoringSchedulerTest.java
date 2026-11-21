package com.pawar.todo.amt.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.pawar.todo.amt.cache.ServerCache;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.model.ApplicationConfiguration;
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.respository.ApplicationConfigurationRepository;
import com.pawar.todo.amt.respository.ServerRepository;
import com.pawar.todo.amt.ssh.SshCommandService;

// Issue #34: Scheduler probes hosts without requiring any registered services.
class HostMonitoringSchedulerTest {
    ServerRepository servers = mock(ServerRepository.class);
    ServerCache cache = mock(ServerCache.class);
    SshCommandService ssh = mock(SshCommandService.class);
    HostMetricsService metrics = mock(HostMetricsService.class);
    AlertConfigurationService alerts = mock(AlertConfigurationService.class);
    ApplicationConfigurationRepository flags = mock(ApplicationConfigurationRepository.class);
    HostMonitoringScheduler monitor = new HostMonitoringScheduler(servers, cache, ssh, metrics, alerts, flags);

    @BeforeEach void setup() {
        var host = new Server(); host.setId(7); host.setHostname("node"); host.setStatus(ServerStatus.UNKNOWN);
        when(servers.findAll()).thenReturn(List.of(host));
        when(servers.recordHealth(eq(7), any(), any())).thenReturn(1);
        when(flags.findById(anyString())).thenReturn(Optional.empty());
    }

    @Test void offlineHostIsPersistedAndEvaluatedWithoutCollectingResources() {
        monitor.checkHosts();
        verify(servers).recordHealth(eq(7), eq(ServerStatus.OFFLINE), any());
        verify(cache).evict(7);
        verify(alerts).evaluateHost(7, "OFFLINE", null);
        verifyNoInteractions(metrics);
    }

    @Test void onlineHostWithResourceRuleIsSampled() {
        when(ssh.isReachable(any())).thenReturn(true);
        when(alerts.hasResourceAlerts(7)).thenReturn(true);
        var sample = new com.pawar.todo.amt.response.HostMetricsResponse(7, true, 90.0, 40.0, 60.0,
                100L, java.time.Instant.now(), null);
        when(metrics.sample(any())).thenReturn(sample);
        monitor.checkHosts();
        verify(servers).recordHealth(eq(7), eq(ServerStatus.ONLINE), any());
        verify(metrics).sample(any());
        verify(alerts).evaluateHost(7, "ONLINE", sample);
    }

    @Test void healthFlagDisablesAllRemoteCalls() {
        disable("healthcheck.enabled");
        monitor.checkHosts();
        verifyNoInteractions(servers, ssh, metrics, alerts);
    }

    @Test void alertFlagDisablesOnlyEvaluation() {
        disable("alert-management.enabled");
        monitor.checkHosts();
        verify(ssh).isReachable(any());
        verifyNoInteractions(alerts, metrics);
    }

    @Test void aDeletedHostDoesNotProduceAlerts() {
        when(servers.recordHealth(eq(7), any(), any())).thenReturn(0);
        monitor.checkHosts();
        verifyNoInteractions(alerts, cache);
    }

    @Test void failureOfOneHostDoesNotSkipOthers() {
        var other = new Server(); other.setId(8);
        var first = new Server(); first.setId(7);
        when(servers.findAll()).thenReturn(List.of(first, other));
        when(ssh.isReachable(argThat(target -> target.id() == 7))).thenThrow(new IllegalStateException());
        monitor.checkHosts();
        verify(servers).recordHealth(eq(8), eq(ServerStatus.OFFLINE), any());
    }

    private void disable(String key) {
        var flag = new ApplicationConfiguration(); flag.setValue("false");
        when(flags.findById(key)).thenReturn(Optional.of(flag));
    }
}
