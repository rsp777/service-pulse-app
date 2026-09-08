package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record ScriptResponseDto(Integer id, String scriptName, String scriptExtension,
        Set<PathResponseDto> paths, LocalDateTime createdDttm, LocalDateTime lastUpdatedDttm,
        String createdSource, String lastUpdatedSource) {
}
