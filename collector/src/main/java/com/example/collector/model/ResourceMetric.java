package com.example.collector.model;

public class ResourceMetric {
    private final double cpuUsage;
    private final double ramUsage;

    public ResourceMetric(double cpuUsage, double ramUsage) {
        this.cpuUsage = cpuUsage;
        this.ramUsage = ramUsage;
    }

    public double getCpuUsage() { return cpuUsage; }
    public double getRamUsage() { return ramUsage; }
}
