package com.pawar.todo.amt.response;

import java.time.LocalDateTime;

public record AlertConfigurationResponse(Integer id, String name, String targetType, Integer serverId,
        Integer serviceId, String conditionType, String operator, String conditionValue, boolean enabled,
        LocalDateTime createdDttm, LocalDateTime lastUpdatedDttm) {
}
