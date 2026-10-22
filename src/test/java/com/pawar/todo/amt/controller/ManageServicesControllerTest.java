package com.pawar.todo.amt.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.ManageServices;

/**
 * Unit tests for ManageServicesController covering Issue #30 Tiered Health Evaluation (+ve & -ve).
 * Author: Ravindra Singh Pawar / Antigravity
 */
class ManageServicesControllerTest {

    private ManageServices manageServices;
    private ManageServicesController controller;
    private Executor executor;

    @BeforeEach
    void setUp() {
        manageServices = mock(ManageServices.class);
        executor = mock(Executor.class);
        controller = new ManageServicesController(executor);
        controller.setManageServices(manageServices);
    }

    // Issue #30: Positive test - evaluate-service-health returns true when service is running
    @Test
    void evaluateServiceHealthReturnsUpWhenServiceRunning() throws Exception {
        when(manageServices.evaluateServiceHealth(1, 5)).thenReturn(true);

        ResponseEntity<ApiResponse<Boolean>> response = controller.evaluateServiceHealth(1, 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Service is UP", response.getBody().getMessage());
        assertEquals(Boolean.TRUE, response.getBody().getData());
    }

    // Issue #30: Negative test - evaluate-service-health returns false when service is down
    @Test
    void evaluateServiceHealthReturnsDownWhenServiceStopped() throws Exception {
        when(manageServices.evaluateServiceHealth(1, 5)).thenReturn(false);

        ResponseEntity<ApiResponse<Boolean>> response = controller.evaluateServiceHealth(1, 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Service is DOWN", response.getBody().getMessage());
        assertEquals(Boolean.FALSE, response.getBody().getData());
    }

    // Issue #30: Negative test - evaluate-service-health handles exception and returns 500
    @Test
    void evaluateServiceHealthReturnsErrorOnException() throws Exception {
        when(manageServices.evaluateServiceHealth(1, 5)).thenThrow(new RuntimeException("Host connection failure"));

        ResponseEntity<ApiResponse<Boolean>> response = controller.evaluateServiceHealth(1, 5);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Failed to evaluate service health"));
    }
}
