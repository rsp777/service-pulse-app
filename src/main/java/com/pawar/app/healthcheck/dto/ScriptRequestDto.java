package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record ScriptRequestDto(Integer id, String scriptName, String scriptExtension,
        Set<PathRequestDto> paths, LocalDateTime createdDttm, LocalDateTime lastUpdatedDttm,
        String createdSource, String lastUpdatedSource) {
}
