package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record PathResponseDto(Integer id, String pathName, String pathDescription,
        Set<ServerResponseDto> servers, Set<ScriptResponseDto> scripts, LocalDateTime createdDttm,
        LocalDateTime lastUpdatedDttm, String createdSource, String lastUpdatedSource) {
}
