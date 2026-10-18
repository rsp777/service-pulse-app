package com.example.collector.factory;

import com.example.collector.*;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class ProbeFactory {
    private final Map<String, Probe> probeMap = new HashMap<>();

    public ProbeFactory() {
        probeMap.put("http", new HttpProbe());
        probeMap.put("tcp", new TcpProbe());
        probeMap.put("ssh", new SshProbe());
    }

    public Optional<Probe> getProbe(String type) {
        if (type == null) return Optional.empty();
        return Optional.ofNullable(probeMap.get(type.toLowerCase()));
    }
}
