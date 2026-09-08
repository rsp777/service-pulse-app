package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;

public record HealthCheckRequestDto(Integer id, String url, Integer operationalPort,
        HttpMethodsRequestDto httpMethod, String expectedResponse, ServiceRequestDto service,
        LocalDateTime createdDttm, LocalDateTime lastUpdatedDttm, String createdSource,
        String lastUpdatedSource) {
}
