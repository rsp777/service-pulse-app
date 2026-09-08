package com.pawar.app.healthcheck.dto;

public record ServerStatusDto(String status) {
    public static ServerStatusDto fromEnum(Object status) {
        return new ServerStatusDto(status == null ? null : status.toString());
    }

    public static Object toEnum(String status) {
        return status;
    }
}
