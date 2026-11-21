package com.pawar.todo.amt.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.pawar.todo.amt.exceptions.ServerOperationException;
import com.pawar.todo.amt.response.HostMetricsResponse;
import com.pawar.todo.amt.service.HostMetricsService;
import com.pawar.todo.amt.service.ServerService;

// Issue #33: Probe only registered server IDs through the existing SSH trust boundary.
@RestController
public class HostMetricsController {
    private final ServerService servers;
    private final HostMetricsService metrics;
    public HostMetricsController(ServerService servers, HostMetricsService metrics) {
        this.servers = servers;
        this.metrics = metrics;
    }

    @GetMapping("/api/servers/{id}/metrics")
    public ResponseEntity<HostMetricsResponse> metrics(@PathVariable Integer id) throws ServerOperationException {
        return servers.findServerById(id).map(server -> ResponseEntity.ok(metrics.sample(server)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
