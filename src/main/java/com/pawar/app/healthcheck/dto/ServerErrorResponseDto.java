package com.pawar.app.healthcheck.dto;

import java.time.Instant;
import java.util.Map;

public record ServerErrorResponseDto(Instant timestamp, int status, String error, String message,
        String path, Map<String, String> validationErrors) {
    public ServerErrorResponseDto(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, null);
    }

    public ServerErrorResponseDto withValidationErrors(Map<String, String> validationErrors) {
        return new ServerErrorResponseDto(timestamp, status, error, message, path, validationErrors);
    }
}
