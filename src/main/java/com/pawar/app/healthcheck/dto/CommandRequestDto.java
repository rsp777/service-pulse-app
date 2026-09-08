package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;

public record CommandRequestDto(String name, String description, String parameters, String status,
        String result, LocalDateTime createdDttm, LocalDateTime lastUpdatedDttm, String createdSource,
        String lastUpdatedSource) {
}
