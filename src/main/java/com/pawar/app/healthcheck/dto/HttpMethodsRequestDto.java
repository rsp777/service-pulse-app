package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;

public record HttpMethodsRequestDto(Integer id, String methodName, boolean allowedInHealthCheck,
        LocalDateTime createdDttm, LocalDateTime lastUpdatedDttm, String createdSource,
        String lastUpdatedSource) {
}
