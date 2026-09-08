package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record ServiceRequestDto(Integer id, Set<ServerResponseDto> servers, String serviceName,
        String healthCheckUrl, LocalDateTime createdDttm, LocalDateTime lastUpdatedDttm,
        String createdSource, String lastUpdatedSource) {
}
