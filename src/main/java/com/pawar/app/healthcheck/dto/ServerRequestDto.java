package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record ServerRequestDto(Integer id, String hostname, String ipAddress, String osType,
        String status, LocalDateTime lastHealthChecked, Set<PathRequestDto> paths,
        Set<ServiceResponseDto> services, LocalDateTime createdDttm, LocalDateTime lastUpdatedDttm,
        String createdSource, String lastUpdatedSource) {
}
