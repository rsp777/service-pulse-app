package com.example.collector;

import com.example.collector.model.ServiceEntity;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpProbe implements Probe {
    @Override
    public ProbeResult execute(ServiceEntity service) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            // Assume URL is in format "ip:port" for TCP
            String[] parts = service.getUrl().split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            socket.connect(new InetSocketAddress(host, port), 5000);
            long latency = System.currentTimeMillis() - start;
            return new ProbeResult(true, latency, "UP");
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return new ProbeResult(false, latency, "DOWN: " + e.getMessage());
        }
    }
}
