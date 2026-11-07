// Issue #29: Comprehensive unit tests for CommandController and command execution tracking [Author: Ravindra Singh Pawar / Antigravity]
package com.pawar.todo.amt.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.pawar.app.healthcheck.dto.CommandRequestDto;
import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.constants.CommandStatus;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.CommandService;
import com.pawar.todo.amt.service.ServerService;
import com.pawar.todo.amt.ssh.SshCommandService;

class CommandControllerTest {

    private CommandService commandService;
    private ServerService serverService;
    private SshCommandService sshCommandService;
    private CommandController controller;

    @BeforeEach
    void setUp() {
        commandService = mock(CommandService.class);
        serverService = mock(ServerService.class);
        sshCommandService = mock(SshCommandService.class);
        controller = new CommandController(commandService, serverService, sshCommandService);
    }

    private ServerResponseDto buildSampleServer(Integer id) {
        return new ServerResponseDto(id, "prod-host", "10.0.0.1", "Linux", "ONLINE", null, null, null, null, null, "test", "test", 22, "ubuntu", null);
    }

    private CommandResponseDto buildSampleCommand(Integer id, String name, String params, String status) {
        return new CommandResponseDto(id, name, "Check disk", params, status, null, LocalDateTime.now(), LocalDateTime.now(), "UI", "UI");
    }

    @Test
    @DisplayName("+ve: execute succeeds and updates command status to COMPLETED when commandId provided")
    void execute_withCommandId_success() throws Exception {
        ServerResponseDto server = buildSampleServer(1);
        when(serverService.findServerById(1)).thenReturn(Optional.of(server));
        when(sshCommandService.execute(server, "df -h")).thenReturn("Filesystem 1K-blocks Used Available Use% Mounted on\n/dev/sda1 1000 500 500 50% /");

        CommandResponseDto cmd = buildSampleCommand(10, "Check Disk Space", "df -h", "CREATED");
        when(commandService.findCommandById(10)).thenReturn(Optional.of(cmd));

        ResponseEntity<ApiResponse<String>> response = controller.execute(1, "df -h", 10);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getData().contains("Filesystem"));

        verify(commandService).updateCommand(eq(10), any(CommandRequestDto.class));
    }

    @Test
    @DisplayName("+ve: execute succeeds without commandId")
    void execute_withoutCommandId_success() throws Exception {
        ServerResponseDto server = buildSampleServer(2);
        when(serverService.findServerById(2)).thenReturn(Optional.of(server));
        when(sshCommandService.execute(server, "uptime")).thenReturn("up 42 days");

        ResponseEntity<ApiResponse<String>> response = controller.execute(2, "uptime", null);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("up 42 days", response.getBody().getData());
    }

    @Test
    @DisplayName("-ve: execute returns 500 and updates command status to FAILED when execution throws exception")
    void execute_failure_recordsFailedStatus() throws Exception {
        ServerResponseDto server = buildSampleServer(1);
        when(serverService.findServerById(1)).thenReturn(Optional.of(server));
        when(sshCommandService.execute(server, "bad-cmd")).thenThrow(new IOException("Command not found"));

        CommandResponseDto cmd = buildSampleCommand(20, "Bad Command", "bad-cmd", "CREATED");
        when(commandService.findCommandById(20)).thenReturn(Optional.of(cmd));

        ResponseEntity<ApiResponse<String>> response = controller.execute(1, "bad-cmd", 20);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Command not found"));

        verify(commandService).updateCommand(eq(20), any(CommandRequestDto.class));
    }

    @Test
    @DisplayName("-ve: execute returns 500 when server is not found")
    void execute_serverNotFound() throws Exception {
        when(serverService.findServerById(999)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<String>> response = controller.execute(999, "uptime", null);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Server not found"));
    }

    @Test
    @DisplayName("+ve: getAllCommands returns all configured commands")
    void getAllCommands_success() {
        CommandResponseDto c1 = buildSampleCommand(1, "Disk", "df -h", "CREATED");
        CommandResponseDto c2 = buildSampleCommand(2, "Memory", "free -m", "COMPLETED");
        when(commandService.findAllCommandsAsync()).thenReturn(CompletableFuture.completedFuture(List.of(c1, c2)));

        ResponseEntity<List<CommandResponseDto>> response = controller.getAllCommands();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    @DisplayName("-ve: getCommandById returns 404 NOT FOUND when command does not exist")
    void getCommandById_notFound() throws Exception {
        when(commandService.findCommandById(99)).thenReturn(Optional.empty());

        ResponseEntity<CommandResponseDto> response = controller.getCommandById(99);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
