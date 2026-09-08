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

@RestController
@RequestMapping("/api/server-service-configurations")
public class ServerServiceConfigurationController {

    private final ServerServiceConfigurationRepository repository;

    public ServerServiceConfigurationController(ServerServiceConfigurationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/server/{serverId}")
    public ResponseEntity<List<ServerServiceConfigurationResponse>> findByServer(@PathVariable Integer serverId) {
        List<ServerServiceConfigurationResponse> configurations = repository.findByServerId(serverId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(configurations);
    }

    private ServerServiceConfigurationResponse toResponse(ServerServiceConfiguration configuration) {
        return new ServerServiceConfigurationResponse(
                configuration.getId(),
                configuration.getServer().getId(),
                configuration.getService().getId(),
                configuration.getHealthCheckUrl());
    }
}
