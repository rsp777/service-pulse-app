package com.pawar.todo.amt.service;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.cache.ServerCache;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.respository.ApplicationConfigurationRepository;
import com.pawar.todo.amt.respository.ServerRepository;
import com.pawar.todo.amt.ssh.SshCommandService;

// Issue #34: Host health is sampled independently of service health and UI activity.
@Component
public class HostMonitoringScheduler {
    private final ServerRepository servers;
    private final ServerCache cache;
    private final SshCommandService ssh;
    private final HostMetricsService metrics;
    private final AlertConfigurationService alerts;
    private final ApplicationConfigurationRepository configuration;
    @Value("${healthcheck.enabled:true}") private boolean healthEnabled = true;
    @Value("${alert-management.enabled:true}") private boolean alertsEnabled = true;

    public HostMonitoringScheduler(ServerRepository servers, ServerCache cache, SshCommandService ssh,
            HostMetricsService metrics, AlertConfigurationService alerts, ApplicationConfigurationRepository configuration) {
        this.servers = servers;
        this.cache = cache;
        this.ssh = ssh;
        this.metrics = metrics;
        this.alerts = alerts;
        this.configuration = configuration;
    }

    @Scheduled(fixedDelayString = "${host-monitor.interval-millis:60000}",
            initialDelayString = "${host-monitor.initial-delay-millis:15000}")
    public void checkHosts() {
        if (!enabled("healthcheck.enabled", healthEnabled)) return;
        for (var host : servers.findAll()) {
            try {
                var target = new ServerResponseDto(host.getId(), host.getHostname(), host.getIpAddress(),
                        host.getOsType(), host.getStatus() == null ? "UNKNOWN" : host.getStatus().name(),
                        host.getSshPort(), host.getSshUsername(), host.getSshKeyId());
                boolean reachable = ssh.isReachable(target);
                ServerStatus status = reachable ? ServerStatus.ONLINE : ServerStatus.OFFLINE;
                if (servers.recordHealth(host.getId(), status, LocalDateTime.now()) == 0) continue;
                cache.evict(host.getId());
                if (enabled("alert-management.enabled", alertsEnabled)) {
                    var sample = reachable && alerts.hasResourceAlerts(host.getId()) ? metrics.sample(target) : null;
                    alerts.evaluateHost(host.getId(), status.name(), sample);
                }
            } catch (Exception exception) {
                // One failed host/store must not skip remaining registered hosts.
                org.slf4j.LoggerFactory.getLogger(HostMonitoringScheduler.class)
                        .warn("Host health evaluation failed for serverId={}", host.getId());
            }
        }
    }

    private boolean enabled(String key, boolean fallback) {
        return configuration.findById(key).map(value -> Boolean.parseBoolean(value.getValue())).orElse(fallback);
    }
}
