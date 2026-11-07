// Issue #28: Added serviceType field to ServiceResponseDto with backward-compatible constructor [Author: Ravindra Singh Pawar / Antigravity]
package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record ServiceResponseDto(
        Integer id,
        Set<ServerResponseDto> servers,
        String serviceName,
        String serviceType,
        LocalDateTime createdDttm,
        LocalDateTime lastUpdatedDttm,
        String createdSource,
        String lastUpdatedSource
) {
    public ServiceResponseDto(
            Integer id,
            Set<ServerResponseDto> servers,
            String serviceName,
            LocalDateTime createdDttm,
            LocalDateTime lastUpdatedDttm,
            String createdSource,
            String lastUpdatedSource
    ) {
        this(id, servers, serviceName, null, createdDttm, lastUpdatedDttm, createdSource, lastUpdatedSource);
    }
}
