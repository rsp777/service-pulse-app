package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record PathRequestDto(Integer id, String pathName, String pathDescription,
        Set<ServerRequestDto> servers, Set<ScriptRequestDto> scripts, LocalDateTime createdDttm,
        LocalDateTime lastUpdatedDttm, String createdSource, String lastUpdatedSource) {
}
