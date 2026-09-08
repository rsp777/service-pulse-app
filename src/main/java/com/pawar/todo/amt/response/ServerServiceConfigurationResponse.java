package com.pawar.todo.amt.response;

public record ServerServiceConfigurationResponse(
        Integer id,
        Integer serverId,
        Integer serviceId,
        String healthCheckUrl) {
}
