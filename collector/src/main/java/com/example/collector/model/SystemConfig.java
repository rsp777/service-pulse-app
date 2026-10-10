package com.example.collector.model;

public class SystemConfig {
    private final long pollInterval;
    private final int timeout;

    public SystemConfig(long pollInterval, int timeout) {
        this.pollInterval = pollInterval;
        this.timeout = timeout;
    }

    public long getPollInterval() { return pollInterval; }
    public int getTimeout() { return timeout; }
}
