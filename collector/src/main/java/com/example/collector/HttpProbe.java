package com.example.collector;

import com.example.collector.model.ServiceEntity;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpProbe implements Probe {
    private final HttpClient httpClient = HttpClient.newBuilder()
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

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long latency = System.currentTimeMillis() - start;

            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                return new ProbeResult(true, latency, "UP");
            } else {
                return new ProbeResult(false, latency, "DOWN (" + response.statusCode() + ")");
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return new ProbeResult(false, latency, "ERROR: " + e.getMessage());
        }
    }
}
