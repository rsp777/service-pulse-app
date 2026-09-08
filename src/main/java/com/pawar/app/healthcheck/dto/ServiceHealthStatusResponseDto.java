package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;

public record ServiceHealthStatusResponseDto(Integer id, ServiceResponseDto service, String status,
        LocalDateTime timeStamp, Long responseTime, String errorMessage, LocalDateTime createdDttm,
        LocalDateTime lastUpdatedDttm, String createdSource, String lastUpdatedSource) {
}
