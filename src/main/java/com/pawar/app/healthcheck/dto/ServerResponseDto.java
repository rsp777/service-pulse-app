// Issue #26: Add SSH connection configuration fields [Author: Ravindra Singh Pawar / Antigravity]
package com.pawar.app.healthcheck.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record ServerResponseDto(Integer id, String hostname, String ipAddress, String osType,
        String status, LocalDateTime lastHealthChecked, Set<PathResponseDto> paths,
        Set<ServiceResponseDto> services, LocalDateTime createdDttm, LocalDateTime lastUpdatedDttm,
        String createdSource, String lastUpdatedSource,
        Integer sshPort, String sshUsername, String sshKeyId) {

    public ServerResponseDto(Integer id, String hostname, String ipAddress, String osType,
            String status, LocalDateTime lastHealthChecked, Set<PathResponseDto> paths,
            Set<ServiceResponseDto> services, LocalDateTime createdDttm, LocalDateTime lastUpdatedDttm,
            String createdSource, String lastUpdatedSource) {
        this(id, hostname, ipAddress, osType, status, lastHealthChecked, paths, services,
                createdDttm, lastUpdatedDttm, createdSource, lastUpdatedSource, 22, "ubuntu", null);
    }

    // Issue #26: Convenience constructor for tests and simple responses [Author: Ravindra Singh Pawar / Antigravity]
    public ServerResponseDto(Integer id, String hostname, String ipAddress, String osType,
            String status, Integer sshPort, String sshUsername, String sshKeyId) {
        this(id, hostname, ipAddress, osType, status, null, null, null, null, null, "TEST", "TEST", sshPort, sshUsername, sshKeyId);
    }
}
