package com.example.collector;

import com.example.collector.model.ServiceEntity;

public interface Probe {
    ProbeResult execute(ServiceEntity service);
}
