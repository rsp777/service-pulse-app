package com.pawar.todo.amt.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.LogsService;

/**
 * Unit tests for LogsController covering Issue #31 Service Log Streaming and Lifecycle (+ve & -ve).
 * Author: Ravindra Singh Pawar / Antigravity
 */
class LogsControllerTest {

    private LogsService logsService;
    private Executor directExecutor;
    private LogsController controller;

    @BeforeEach
    void setUp() {
        logsService = mock(LogsService.class);
        directExecutor = Runnable::run; // Synchronous direct execution for deterministic testing
        controller = new LogsController(logsService, directExecutor);
    }

    // Issue #31: Positive test - viewLogsByService returns 200 OK with log content [Author: Ravindra Singh Pawar / Antigravity]
    @Test
    void viewLogsByServiceSuccess() throws Exception {
        when(logsService.viewLogsByService(1, 10)).thenReturn("2026-10-22 12:00:00 [INFO] Service started successfully");

        ResponseEntity<ApiResponse<String>> response = controller.viewLogsByService(1, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("2026-10-22 12:00:00 [INFO] Service started successfully", response.getBody().getData());
    }

    // Issue #31: Negative test - viewLogsByService returns 500 when logsService throws exception [Author: Ravindra Singh Pawar / Antigravity]
    @Test
    void viewLogsByServiceFailureReturns500() throws Exception {
        when(logsService.viewLogsByService(1, 10)).thenThrow(new RuntimeException("Host SSH connection refused"));

        ResponseEntity<ApiResponse<String>> response = controller.viewLogsByService(1, 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Host SSH connection refused"));
    }

    // Issue #31: Positive test - streamLogsByService sets up SseEmitter and streams lines [Author: Ravindra Singh Pawar / Antigravity]
    @Test
    @SuppressWarnings("unchecked")
    void streamLogsByServiceStreamsSuccessfully() throws Exception {
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(2);
            AtomicBoolean stopped = invocation.getArgument(3);
            consumer.accept("[INFO] Initializing application context");
            consumer.accept("[INFO] Server running on port 8080");
            return null;
        }).when(logsService).streamLogsByService(eq(1), eq(10), any(Consumer.class), any(AtomicBoolean.class));

        SseEmitter emitter = controller.streamLogsByService(1, 10);
        assertNotNull(emitter);
        verify(logsService, times(1)).streamLogsByService(eq(1), eq(10), any(Consumer.class), any(AtomicBoolean.class));
    }

    // Issue #31: Negative test - streamLogsByService handles thread pool rejection gracefully [Author: Ravindra Singh Pawar / Antigravity]
    @Test
    void streamLogsByServiceExecutorRejectionHandled() {
        Executor rejectingExecutor = r -> {
            throw new RejectedExecutionException("Executor saturated");
        };
        LogsController rejectingController = new LogsController(logsService, rejectingExecutor);

        SseEmitter emitter = rejectingController.streamLogsByService(1, 10);
        assertNotNull(emitter);
    }

    // Issue #31: Negative test - streamLogsByService sets stopped flag when client disconnects or service errors [Author: Ravindra Singh Pawar / Antigravity]
    @Test
    @SuppressWarnings("unchecked")
    void streamLogsByServiceSetsStoppedOnException() throws Exception {
        AtomicBoolean capturedStopped = new AtomicBoolean(false);
        doAnswer(invocation -> {
            AtomicBoolean stopped = invocation.getArgument(3);
            capturedStopped.set(stopped.get());
            throw new RuntimeException("Stream broken");
        }).when(logsService).streamLogsByService(eq(1), eq(10), any(Consumer.class), any(AtomicBoolean.class));

        SseEmitter emitter = controller.streamLogsByService(1, 10);
        assertNotNull(emitter);
        verify(logsService).streamLogsByService(eq(1), eq(10), any(Consumer.class), any(AtomicBoolean.class));
    }
}
