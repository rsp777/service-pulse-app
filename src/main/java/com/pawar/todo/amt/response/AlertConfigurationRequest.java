package com.pawar.todo.amt.response;

public record AlertConfigurationRequest(String name, String targetType, Integer serverId, Integer serviceId,
        String conditionType, String operator, String conditionValue, Boolean enabled) {
}
