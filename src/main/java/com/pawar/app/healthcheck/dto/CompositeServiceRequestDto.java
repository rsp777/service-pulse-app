package com.pawar.app.healthcheck.dto;

public record CompositeServiceRequestDto(
        Integer id,
        Integer serverId,
        String serviceName,
        String serviceType,
        String logsPath,
        String scriptsPath,
        Integer actuatorPort,
        String contextPath,
        String healthCheckUrl
) {}
