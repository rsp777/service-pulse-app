package com.pawar.todo.amt.response;

public record RuntimeConfigurationResponse(
        boolean healthCheckEnabled,
        boolean serviceManagementEnabled,
        boolean alertManagementEnabled,
        boolean healthStatusPopulationEnabled,
        String healthCheckCron) {
}
