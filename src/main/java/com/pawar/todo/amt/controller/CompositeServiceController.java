package com.pawar.todo.amt.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pawar.app.healthcheck.dto.CompositeServiceRequestDto;
import com.pawar.app.healthcheck.dto.PathRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceRequestDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.exceptions.PathOperationException;
import com.pawar.todo.amt.exceptions.ServiceOperationException;
import com.pawar.todo.amt.service.PathService;
import com.pawar.todo.amt.service.ServerService;
import com.pawar.todo.amt.service.ServiceService;

@RestController
@RequestMapping("/api/services/composite")
public class CompositeServiceController {

    private static final Logger logger = LoggerFactory.getLogger(CompositeServiceController.class);

    private final ServiceService serviceService;
    private final ServerService serverService;
    private final PathService pathService;

    public CompositeServiceController(ServiceService serviceService, ServerService serverService,
            PathService pathService) {
        this.serviceService = serviceService;
        this.serverService = serverService;
        this.pathService = pathService;
    }

    @PostMapping
    public ResponseEntity<Void> createCompositeService(@RequestBody CompositeServiceRequestDto request) {
        return processCompositeService(null, request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCompositeService(@PathVariable Integer id,
            @RequestBody CompositeServiceRequestDto request) {
        return processCompositeService(id, request);
    }

    private ResponseEntity<Void> processCompositeService(Integer id, CompositeServiceRequestDto request) {
        try {
            // 1. Fetch Server to calculate health check URL
            Optional<ServerResponseDto> serverOpt = serverService.findServerById(request.serverId());
            if (serverOpt.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            ServerResponseDto server = serverOpt.get();

            // 2. Compute Health Check URL
            String healthCheckUrl = "";
            if ("SPRING_BOOT".equals(request.serviceType())) {
                String host = server.ipAddress() != null && !server.ipAddress().isBlank() ? server.ipAddress()
                        : server.hostname();
                Integer port = request.actuatorPort() != null ? request.actuatorPort() : 8080;
                String context = request.contextPath() != null ? request.contextPath().trim().replaceAll("^/+|/+$", "")
                        : "";
                healthCheckUrl = "http://" + host + ":" + port + (context.isEmpty() ? "" : "/" + context)
                        + "/actuator/health";
            }

            // 3. Create or Update Service
            ServiceRequestDto serviceRequest = new ServiceRequestDto(
                    id,
                    Set.of(server),
                    request.serviceName(),
                    request.serviceType(),
                    healthCheckUrl,
                    null, null, "UI", "UI");

            if (id == null) {
                serviceService.createService(serviceRequest);
            } else {
                serviceService.updateService(id, serviceRequest);
            }

            // 4. Create Paths
            createPathIfProvided(server, "LOGS_HOME", request.logsPath());
            createPathIfProvided(server, "SCRIPTS_HOME", request.scriptsPath());

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error processing composite service: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void createPathIfProvided(ServerResponseDto server, String pathName, String pathDescription) {
        if (pathDescription == null || pathDescription.trim().isEmpty()) {
            return;
        }

        try {
            com.pawar.app.healthcheck.dto.ServerRequestDto serverReq = new com.pawar.app.healthcheck.dto.ServerRequestDto(
                    server.id(), server.hostname(), server.ipAddress(), server.osType(),
                    server.status(), server.lastHealthChecked(), null, null,
                    server.createdDttm(), server.lastUpdatedDttm(), server.createdSource(), server.lastUpdatedSource());

            PathRequestDto pathRequest = new PathRequestDto(
                    null,
                    pathName,
                    pathDescription.trim(),
                    Set.of(serverReq),
                    Collections.emptySet(),
                    null, null, "UI", "UI");
            pathService.createPath(pathRequest);
        } catch (Exception e) {
            logger.warn("Failed to create path: {}", pathName, e);
        }
    }
}
