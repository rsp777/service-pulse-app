package com.pawar.todo.amt.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.response.HostMetricsResponse;
import com.pawar.todo.amt.ssh.SshCommandService;

// Issue #33: A fixed read-only command; client values are never interpolated into the shell.
@Service
public class HostMetricsService {
    static final String METRICS_COMMAND = """
            set -eu
            LC_ALL=C
            export LC_ALL
            snapshot() { awk '/^cpu / {printf "%.0f %.0f\\n", $2+$3+$4+$5+$6+$7+$8+$9, $5+$6; exit}' /proc/stat; }
            set -- $(snapshot)
            total1=$1
            idle1=$2
            sleep 1
            set -- $(snapshot)
            awk -v total="$(( $1 - total1 ))" -v idle="$(( $2 - idle1 ))" 'BEGIN {
              if (total <= 0) exit 1;
              printf "CPU=%.2f\\n", 100 * (total-idle)/total
            }'
            awk '/^MemTotal:/ {total=$2} /^MemAvailable:/ {available=$2; found=1}
              END {if (total <= 0 || !found) exit 1; printf "MEMORY=%.2f\\n", 100*(total-available)/total}' /proc/meminfo
            df -P / | awk 'NR==2 {gsub(/%/, "", $5); printf "DISK=%s\\n", $5}'
            awk '{printf "UPTIME=%.0f\\n", int($1)}' /proc/uptime
            """;

    private final SshCommandService ssh;
    public HostMetricsService(SshCommandService ssh) { this.ssh = ssh; }

    public HostMetricsResponse sample(ServerResponseDto server) {
        try {
            return parse(server.id(), ssh.execute(server, METRICS_COMMAND));
        } catch (Exception exception) {
            return HostMetricsResponse.unavailable(server.id());
        }
    }

    static HostMetricsResponse parse(Integer serverId, String output) {
        Map<String, String> values = new HashMap<>();
        for (String line : output.split("\\R")) {
            String[] pair = line.trim().split("=", 2);
            if (pair.length == 2 && values.putIfAbsent(pair[0], pair[1]) != null)
                throw new IllegalArgumentException("Duplicate metric");
        }
        double cpu = percentage(values.get("CPU"));
        double memory = percentage(values.get("MEMORY"));
        double disk = percentage(values.get("DISK"));
        long uptime = Long.parseLong(values.get("UPTIME"));
        if (uptime < 0) throw new IllegalArgumentException("Invalid uptime");
        return new HostMetricsResponse(serverId, true, cpu, memory, disk, uptime, Instant.now(), null);
    }

    private static double percentage(String value) {
        double number = Double.parseDouble(value);
        if (!Double.isFinite(number) || number < 0 || number > 100)
            throw new IllegalArgumentException("Invalid percentage");
        return number;
    }
}
