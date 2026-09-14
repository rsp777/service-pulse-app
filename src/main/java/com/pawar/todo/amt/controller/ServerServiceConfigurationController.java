package com.pawar.todo.amt.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawar.todo.amt.model.ServerServiceConfiguration;
import com.pawar.todo.amt.response.ServerServiceConfigurationResponse;
import com.pawar.todo.amt.respository.ServerServiceConfigurationRepository;

import java.net.URL;
import java.util.Optional;
import com.pawar.app.healthcheck.dto.PathRequestDto;
import com.pawar.todo.amt.service.PathService;

@RestController
@RequestMapping("/api/server-service-configurations")
public class ServerServiceConfigurationController {

    private final ServerServiceConfigurationRepository repository;
    private final PathService pathService;
    private final com.pawar.todo.amt.respository.ServiceHealthStatusRepository serviceHealthStatusRepository;

    public ServerServiceConfigurationController(ServerServiceConfigurationRepository repository, PathService pathService,
            com.pawar.todo.amt.respository.ServiceHealthStatusRepository serviceHealthStatusRepository) {
        this.repository = repository;
        this.pathService = pathService;
        this.serviceHealthStatusRepository = serviceHealthStatusRepository;
    }

    @GetMapping("/server/{serverId}")
    public ResponseEntity<List<ServerServiceConfigurationResponse>> findByServer(@PathVariable Integer serverId) {
        List<ServerServiceConfigurationResponse> configurations = repository.findByServerId(serverId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(configurations);
    }

    private ServerServiceConfigurationResponse toResponse(ServerServiceConfiguration configuration) {
        String logsPath = "";
        String scriptsPath = "";
        
        try {
            if (configuration.getServer() != null && configuration.getServer().getPaths() != null) {
                for (com.pawar.todo.amt.model.Path path : configuration.getServer().getPaths()) {
                    if ("LOGS_HOME".equals(path.getPathName())) {
                        logsPath = path.getPathDescription();
                    } else if ("SCRIPTS_HOME".equals(path.getPathName())) {
                        scriptsPath = path.getPathDescription();
                    }
                }
            }
        } catch(Exception e) {}
        
        Integer actuatorPort = 8080;
        String contextPath = "";
        if (configuration.getHealthCheckUrl() != null && !configuration.getHealthCheckUrl().isBlank()) {
            try {
                URL url = new URL(configuration.getHealthCheckUrl());
                actuatorPort = url.getPort() != -1 ? url.getPort() : 80;
                contextPath = url.getPath().replaceAll("/actuator/health/?$", "");
            } catch (Exception e) {}
        }

        String persistedStatus = "UNKNOWN";
        try {
            if (configuration.getServer() != null && configuration.getService() != null) {
                persistedStatus = serviceHealthStatusRepository
                        .findByServiceIdAndServerId(configuration.getService().getId(), configuration.getServer().getId())
                        .map(s -> s.getStatus() != null ? s.getStatus().name() : "UNKNOWN")
                        .orElse("UNKNOWN");
            }
        } catch (Exception ignored) {}
        
        return new ServerServiceConfigurationResponse(
                configuration.getId(),
                configuration.getServer().getId(),
                configuration.getService().getId(),
                configuration.getService().getServiceName(),
                configuration.getService().getServiceType(),
                configuration.getHealthCheckUrl(),
                logsPath,
                scriptsPath,
                actuatorPort,
                contextPath,
                persistedStatus,
                null);
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}")
    public ResponseEntity<ServerServiceConfigurationResponse> findById(@PathVariable Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ResponseEntity<Void> updateConfiguration(@PathVariable Integer id, @org.springframework.web.bind.annotation.RequestBody ServerServiceConfigurationResponse request) {
        ServerServiceConfiguration configuration = repository.findById(id).orElseThrow();
        configuration.setHealthCheckUrl(request.healthCheckUrl());
        repository.save(configuration);
        return ResponseEntity.ok().build();
    }
}
