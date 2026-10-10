package com.example.collector;

public class ProbeResult {
    private final boolean success;
    private final double value; // Latency in ms
    private final String status;

    public ProbeResult(boolean success, double value, String status) {
        this.success = success;
        this.value = value;
        this.status = status;
    }

    public boolean isSuccess() { return success; }
    public double getValue() { return value; }
    public String getStatus() { return status; }
}
