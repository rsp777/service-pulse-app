package com.pawar.todo.amt.response;

import java.time.LocalDateTime;

public record AlertEventResponse(Integer id, Integer alertId, Integer serverId, Integer serviceId, String message,
        String status, LocalDateTime triggeredDttm) {
}
