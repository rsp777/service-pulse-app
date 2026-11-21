package com.pawar.todo.amt.response;

import java.time.Instant;

// Issue #33: Missing measurements remain null rather than looking like zero usage.
public record HostMetricsResponse(Integer serverId, boolean available, Double cpuPercent,
        Double memoryPercent, Double diskPercent, Long uptimeSeconds, Instant sampledAt, String message) {
    public static HostMetricsResponse unavailable(Integer serverId) {
        return new HostMetricsResponse(serverId, false, null, null, null, null, Instant.now(),
                "Metrics unavailable. Check SSH connectivity and Linux metric support.");
    }
}
