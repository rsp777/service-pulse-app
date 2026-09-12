package com.pawar.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawar.todo.amt.controller.UiActionController;
import com.pawar.todo.amt.model.UiAction;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.response.UiActionRequest;
import com.pawar.todo.amt.response.UiActionResponse;
import com.pawar.todo.amt.service.UiActionService;

@ExtendWith(MockitoExtension.class)
class UiActionControllerTest {

    @Mock
    private UiActionService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsNewMenuActionWithFieldsAndReturnsCreated() {
        UiActionRequest request = new UiActionRequest("reports", "Generate Report", "/api/reports", "FORM",
                objectMapper.createObjectNode().put("submitLabel", "Generate"), null, "Reporting", "Reports", null, null);
        UiAction saved = new UiAction();
        saved.setId(1); saved.setViewContext("reports"); saved.setActionLabel("Generate Report");
        saved.setActionEndpoint("/api/reports"); saved.setComponentType("FORM");
        saved.setComponentConfig("{\"submitLabel\":\"Generate\"}"); saved.setGridSpan("span-12");
        when(service.create(any(UiAction.class))).thenReturn(saved);

        ResponseEntity<ApiResponse<UiActionResponse>> response = new UiActionController(service, objectMapper).create(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("reports", response.getBody().getData().viewContext());
        verify(service).create(any(UiAction.class));
    }

    @Test
    void rejectsCreateWhenViewContextMissing() {
        UiActionRequest request = new UiActionRequest(null, "Label", "/api/x", null, null, null, null, null, null, null);

        ResponseEntity<ApiResponse<UiActionResponse>> response = new UiActionController(service, objectMapper).create(request);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void returnsNotFoundWhenActionMissing() {
        when(service.findById(42)).thenThrow(new NoSuchElementException("SDUI action not found with ID: 42"));

        ResponseEntity<ApiResponse<UiActionResponse>> response = new UiActionController(service, objectMapper).findById(42);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void listsAllActionsWhenNoViewContextProvided() {
        UiAction action = new UiAction();
        action.setId(5); action.setViewContext("dashboard"); action.setActionLabel("Servers"); action.setActionEndpoint("/api/servers");
        when(service.findAll()).thenReturn(List.of(action));

        ResponseEntity<ApiResponse<List<UiActionResponse>>> response = new UiActionController(service, objectMapper).findAll(null);

        assertEquals(1, response.getBody().getData().size());
    }
}
