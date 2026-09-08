package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record ServiceResponseDto(Integer id, Set<ServerResponseDto> servers, String serviceName,
        LocalDateTime createdDttm, LocalDateTime lastUpdatedDttm,
        String createdSource, String lastUpdatedSource) {
}
