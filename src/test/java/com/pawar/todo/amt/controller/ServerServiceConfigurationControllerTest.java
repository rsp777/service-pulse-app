// Issue #27: Comprehensive unit tests for ServerServiceConfigurationController [Author: Ravindra Singh Pawar / Antigravity]
package com.pawar.todo.amt.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.pawar.todo.amt.model.Path;
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.model.ServerServiceConfiguration;
import com.pawar.todo.amt.model.Service;
import com.pawar.todo.amt.response.ServerServiceConfigurationResponse;
import com.pawar.todo.amt.respository.ServerServiceConfigurationRepository;
import com.pawar.todo.amt.respository.ServiceHealthStatusRepository;
import com.pawar.todo.amt.service.PathService;

class ServerServiceConfigurationControllerTest {

    private ServerServiceConfigurationRepository repository;
    private PathService pathService;
    private ServiceHealthStatusRepository serviceHealthStatusRepository;
    private ServerServiceConfigurationController controller;

    @BeforeEach
    void setUp() {
        repository = mock(ServerServiceConfigurationRepository.class);
        pathService = mock(PathService.class);
        serviceHealthStatusRepository = mock(ServiceHealthStatusRepository.class);
        controller = new ServerServiceConfigurationController(repository, pathService, serviceHealthStatusRepository);
    }

    private ServerServiceConfiguration buildSampleConfig(Integer id, Integer serverId, Integer serviceId, String healthUrl) {
        Server server = new Server();
        server.setId(serverId);
        server.setHostname("app-server-01");
        Path logsPath = new Path();
        logsPath.setPathName("LOGS_HOME");
        logsPath.setPathDescription("/opt/apps/logs");
        server.setPaths(Set.of(logsPath));

        Service service = new Service();
        service.setId(serviceId);
        service.setServiceName("auth-service");
        service.setServiceType("SPRING_BOOT");

        ServerServiceConfiguration config = new ServerServiceConfiguration();
        config.setId(id);
        config.setServer(server);
        config.setService(service);
        config.setHealthCheckUrl(healthUrl);
        return config;
    }

    @Test
    @DisplayName("+ve: findAll returns all configurations across servers with properly parsed healthCheckUrl")
    void findAll_success() {
        ServerServiceConfiguration config1 = buildSampleConfig(1, 10, 100, "http://10.0.0.1:8085/auth/actuator/health");
        ServerServiceConfiguration config2 = buildSampleConfig(2, 20, 200, "http://10.0.0.2:9090/actuator/health");

        when(repository.findAll()).thenReturn(List.of(config1, config2));

        ResponseEntity<List<ServerServiceConfigurationResponse>> response = controller.findAll();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<ServerServiceConfigurationResponse> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());

        ServerServiceConfigurationResponse item1 = body.get(0);
        assertEquals(1, item1.id());
        assertEquals("http://10.0.0.1:8085/auth/actuator/health", item1.healthCheckUrl());
        assertEquals(8085, item1.actuatorPort());
        assertEquals("/auth", item1.contextPath());
        assertEquals("/opt/apps/logs", item1.logsPath());

        ServerServiceConfigurationResponse item2 = body.get(1);
        assertEquals(2, item2.id());
        assertEquals(9090, item2.actuatorPort());
        assertEquals("", item2.contextPath());
    }

    @Test
    @DisplayName("-ve: findAll returns empty list when no configurations exist")
    void findAll_empty() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        ResponseEntity<List<ServerServiceConfigurationResponse>> response = controller.findAll();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<ServerServiceConfigurationResponse> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isEmpty());
    }

    @Test
    @DisplayName("+ve: findByServer returns configurations matching serverId")
    void findByServer_success() {
        ServerServiceConfiguration config = buildSampleConfig(5, 42, 105, "http://192.168.1.50:8080/actuator/health");
        when(repository.findByServerId(42)).thenReturn(List.of(config));

        ResponseEntity<List<ServerServiceConfigurationResponse>> response = controller.findByServer(42);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(42, response.getBody().get(0).serverId());
        assertEquals("http://192.168.1.50:8080/actuator/health", response.getBody().get(0).healthCheckUrl());
    }

    @Test
    @DisplayName("-ve: findByServer returns empty list when serverId has no services")
    void findByServer_empty() {
        when(repository.findByServerId(999)).thenReturn(Collections.emptyList());

        ResponseEntity<List<ServerServiceConfigurationResponse>> response = controller.findByServer(999);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("+ve: findById returns configuration response when found")
    void findById_found() {
        ServerServiceConfiguration config = buildSampleConfig(1, 10, 100, "http://10.0.0.1:8080/actuator/health");
        when(repository.findById(1)).thenReturn(Optional.of(config));

        ResponseEntity<ServerServiceConfigurationResponse> response = controller.findById(1);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().id());
    }

    @Test
    @DisplayName("-ve: findById returns 404 NOT FOUND when configuration id does not exist")
    void findById_notFound() {
        when(repository.findById(999)).thenReturn(Optional.empty());

        ResponseEntity<ServerServiceConfigurationResponse> response = controller.findById(999);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
