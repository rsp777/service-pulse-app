package com.pawar.todo.amt.response;

public record RuntimeConfigurationUpdate(
        Boolean healthCheckEnabled,
        Boolean serviceManagementEnabled,
        Boolean alertManagementEnabled,
        Boolean backFillDataPopulationEnabled) {
}
