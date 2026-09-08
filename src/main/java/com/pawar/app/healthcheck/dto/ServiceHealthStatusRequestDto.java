package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;

public record ServiceHealthStatusRequestDto(Integer id, ServiceRequestDto service, String status,
        LocalDateTime timestamp, Long responseTime, String errorMessage, LocalDateTime createdDttm,
        LocalDateTime lastUpdatedDttm, String createdSource, String lastUpdatedSource) {
}
