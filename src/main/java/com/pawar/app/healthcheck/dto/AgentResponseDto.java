package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;

public record AgentResponseDto(Integer id, String name, String host, Integer port, String status,
        String agentWebSocketUrl, ServerResponseDto server, String jarFilePath, String jarVersion,
        LocalDateTime lastDeployment, LocalDateTime lastHeartbeat, LocalDateTime createdDttm,
        LocalDateTime lastUpdatedDttm, String createdSource, String lastUpdatedSource) {
}
