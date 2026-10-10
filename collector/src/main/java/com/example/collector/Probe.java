package com.example.collector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Probe {
    ProbeResult execute(ServiceEntity service);
}

class ProbeResult {
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

class HttpProbe implements Probe {
    private static final Logger LOG = LoggerFactory.getLogger(HttpProbe.class);
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public ProbeResult execute(ServiceEntity service) {
        long start = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(service.getUrl()))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            long latency = System.currentTimeMillis() - start;

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new ProbeResult(true, latency, "UP");
            } else {
                return new ProbeResult(false, latency, "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            LOG.error("HTTP probe failed for {}: {}", service.getUrl(), e.getMessage());
            return new ProbeResult(false, System.currentTimeMillis() - start, "DOWN");
        }
    }
}

class TcpProbe implements Probe {
    private static final Logger LOG = LoggerFactory.getLogger(TcpProbe.class);

    @Override
    public ProbeResult execute(ServiceEntity service) {
        String[] parts = service.getUrl().split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 80;

        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            long latency = System.currentTimeMillis() - start;
            return new ProbeResult(true, latency, "UP");
        } catch (IOException e) {
            LOG.error("TCP probe failed for {}: {}", service.getUrl(), e.getMessage());
            return new ProbeResult(false, System.currentTimeMillis() - start, "DOWN");
        }
    }
}
