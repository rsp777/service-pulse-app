package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;

public record HealthCheckResponseDto(Integer id, String url, Integer operationalPort,
        HttpMethodsResponseDto httpMethod, String expectedResponse, ServiceResponseDto serviceResponseDto,
        LocalDateTime createdDttm, LocalDateTime lastUpdatedDttm, String createdSource,
        String lastUpdatedSource) {
}
