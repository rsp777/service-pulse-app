// Issue #26: Comprehensive test suite for Server SSH configuration & connection testing [Author: Ravindra Singh Pawar / Antigravity]
package com.pawar.todo.amt.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.pawar.app.healthcheck.dto.ConnectionTestResult;
import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.converter.ServerStatusConverter;
import com.pawar.todo.amt.mapper.ServerMapper;
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.ServerService;

class ServerSshConfigurationTest {

    private ServerService serverService;
    private ServerController serverController;
    private ServerMapper serverMapper;

    @BeforeEach
    void setUp() {
        serverService = mock(ServerService.class);
        serverController = new ServerController(serverService);
        serverMapper = new ServerMapper(new ServerStatusConverter());
    }

    @Test
    @DisplayName("+ve: Controller testConnection returns success when target is reachable")
    void testConnection_controller_success() throws Exception {
        ServerRequestDto request = new ServerRequestDto(1, "prod-app-01", "10.0.0.1", "Linux", "ONLINE", 22, "ubuntu", null);
        when(serverService.testConnection(any(ServerRequestDto.class)))
                .thenReturn(new ConnectionTestResult(true, "SSH connection successful to 10.0.0.1:22"));

        ResponseEntity<ApiResponse<ConnectionTestResult>> response = serverController.testConnection(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getData().connected());
        assertTrue(response.getBody().getData().message().contains("successful"));
    }

    @Test
    @DisplayName("-ve: Controller testConnection returns failure when target is unreachable")
    void testConnection_controller_failure() throws Exception {
        ServerRequestDto request = new ServerRequestDto(2, "dead-host", "192.168.254.99", "Linux", "OFFLINE", 9999, "baduser", null);
        when(serverService.testConnection(any(ServerRequestDto.class)))
                .thenReturn(new ConnectionTestResult(false, "Connection refused to 192.168.254.99:9999"));

        ResponseEntity<ApiResponse<ConnectionTestResult>> response = serverController.testConnection(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertFalse(response.getBody().getData().connected());
        assertTrue(response.getBody().getData().message().contains("refused"));
    }

    @Test
    @DisplayName("+ve: Controller testConnectionForServer returns success for existing server ID")
    void testConnectionForServer_controller_success() throws Exception {
        when(serverService.testConnection(1))
                .thenReturn(new ConnectionTestResult(true, "SSH connection successful to server ID 1"));

        ResponseEntity<ApiResponse<ConnectionTestResult>> response = serverController.testConnectionForServer(1);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getData().connected());
    }

    @Test
    @DisplayName("-ve: Controller testConnectionForServer handles service exception gracefully")
    void testConnectionForServer_controller_exceptionHandled() throws Exception {
        when(serverService.testConnection(999))
                .thenThrow(new RuntimeException("Server not found"));

        ResponseEntity<ApiResponse<ConnectionTestResult>> response = serverController.testConnectionForServer(999);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertFalse(response.getBody().getData().connected());
        assertTrue(response.getBody().getMessage().contains("Server not found"));
    }

    @Test
    @DisplayName("+ve: ServerMapper correctly maps SSH fields between DTO and Entity")
    void serverMapper_mapsSshFields() throws Exception {
        ServerRequestDto request = new ServerRequestDto(10, "web-node", "172.16.0.5", "Linux", "ONLINE", 2222, "deployer", "key-vault-01");
        Server entity = serverMapper.toEntity(request);

        assertNotNull(entity);
        assertEquals(2222, entity.getSshPort());
        assertEquals("deployer", entity.getSshUsername());
        assertEquals("key-vault-01", entity.getSshKeyId());

        ServerResponseDto response = serverMapper.toDto(entity);
        assertNotNull(response);
        assertEquals(2222, response.sshPort());
        assertEquals("deployer", response.sshUsername());
        assertEquals("key-vault-01", response.sshKeyId());
    }

    @Test
    @DisplayName("+ve: Server entity defaults sshPort to 22 and sshUsername to ubuntu")
    void serverEntity_defaults() {
        Server server = new Server();
        assertEquals(22, server.getSshPort());
        assertEquals("ubuntu", server.getSshUsername());
    }
}
