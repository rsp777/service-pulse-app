package com.pawar.todo.amt.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawar.todo.amt.model.UiAction;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.response.UiActionRequest;
import com.pawar.todo.amt.response.UiActionResponse;
import com.pawar.todo.amt.service.UiActionService;

/**
 * Administration API for the SDUI screen/menu configurations stored in ui_actions.
 * Creating, updating, or deleting a row here evicts the affected view context from the
 * SDUI cache immediately, so new menus, fields, dropdowns, and config flags become
 * visible on the dashboard without an application restart or a manual cache clear.
 */
@RestController
@RequestMapping("/api/sdui/actions")
public class UiActionController {

    private static final Logger logger = LoggerFactory.getLogger(UiActionController.class);

    private final UiActionService service;
    private final ObjectMapper objectMapper;

    public UiActionController(UiActionService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UiActionResponse>>> findAll(
            @RequestParam(required = false) String viewContext) {
        List<UiAction> actions = viewContext == null || viewContext.isBlank()
                ? service.findAll()
                : service.getActions(viewContext);
        return ResponseEntity.ok(new ApiResponse<>(true, "SDUI actions retrieved successfully",
                actions.stream().map(this::toResponse).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UiActionResponse>> findById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(new ApiResponse<>(true, "SDUI action retrieved successfully",
                    toResponse(service.findById(id))));
        } catch (NoSuchElementException exception) {
            logger.warn("SDUI action not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, exception.getMessage(), null));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UiActionResponse>> create(@RequestBody UiActionRequest request) {
        try {
            UiAction created = service.create(toEntity(request, new UiAction()));
            logger.info("Created SDUI action id={} for view context={}", created.getId(), created.getViewContext());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "SDUI action created successfully", toResponse(created)));
        } catch (IllegalArgumentException exception) {
            logger.warn("Invalid create SDUI action request: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, exception.getMessage(), null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UiActionResponse>> update(@PathVariable Integer id,
            @RequestBody UiActionRequest request) {
        try {
            UiAction updated = service.update(id, toEntity(request, new UiAction()));
            logger.info("Updated SDUI action id={} for view context={}", updated.getId(), updated.getViewContext());
            return ResponseEntity.ok(new ApiResponse<>(true, "SDUI action updated successfully", toResponse(updated)));
        } catch (NoSuchElementException exception) {
            logger.warn("SDUI action not found for update with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, exception.getMessage(), null));
        } catch (IllegalArgumentException exception) {
            logger.warn("Invalid update SDUI action request for ID {}: {}", id, exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, exception.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        try {
            service.delete(id);
            logger.info("Deleted SDUI action id={}", id);
            return ResponseEntity.ok(new ApiResponse<>(true, "SDUI action deleted successfully", null));
        } catch (NoSuchElementException exception) {
            logger.warn("SDUI action not found for deletion with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, exception.getMessage(), null));
        }
    }

    private UiAction toEntity(UiActionRequest request, UiAction action) {
        if (request == null || request.viewContext() == null || request.viewContext().isBlank())
            throw new IllegalArgumentException("View context is required");
        if (request.actionLabel() == null || request.actionLabel().isBlank())
            throw new IllegalArgumentException("Action label is required");
        if (request.actionEndpoint() == null || request.actionEndpoint().isBlank())
            throw new IllegalArgumentException("Action endpoint is required");
        action.setViewContext(request.viewContext());
        action.setActionLabel(request.actionLabel());
        action.setActionEndpoint(request.actionEndpoint());
        action.setComponentType(request.componentType() == null || request.componentType().isBlank()
                ? "ACTION" : request.componentType());
        action.setComponentConfig(writeJson(request.componentConfig()));
        action.setRequestPayload(writeJson(request.requestPayload()));
        action.setSidebarCategory(request.sidebarCategory());
        action.setPanelTitle(request.panelTitle());
        action.setGridSpan(request.gridSpan() == null || request.gridSpan().isBlank() ? "span-12" : request.gridSpan());
        action.setIconClass(request.iconClass());
        return action;
    }

    private UiActionResponse toResponse(UiAction action) {
        return new UiActionResponse(action.getId(), action.getViewContext(), action.getActionLabel(),
                action.getActionEndpoint(), action.getComponentType(), readJson(action.getComponentConfig()),
                readJson(action.getRequestPayload()), action.getSidebarCategory(), action.getPanelTitle(),
                action.getGridSpan(), action.getIconClass());
    }

    private String writeJson(JsonNode node) {
        if (node == null || node.isNull()) return null;
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid JSON configuration", exception);
        }
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            logger.warn("Unable to parse stored SDUI JSON configuration: {}", exception.getMessage());
            return null;
        }
    }
}
