package com.pawar.todo.amt.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawar.todo.amt.model.AlertConfiguration;
import com.pawar.todo.amt.model.AlertEvent;
import com.pawar.todo.amt.response.AlertConfigurationRequest;
import com.pawar.todo.amt.response.AlertConfigurationResponse;
import com.pawar.todo.amt.response.AlertEventResponse;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.AlertConfigurationService;

@RestController
@RequestMapping("/api/alerts")
public class AlertConfigurationController {
    private final AlertConfigurationService service;

    public AlertConfigurationController(AlertConfigurationService service) { this.service = service; }

    @GetMapping
    public List<AlertConfigurationResponse> findAll() { return service.findAll().stream().map(this::toResponse).toList(); }

    @GetMapping("/events")
    public List<AlertEventResponse> events() { return service.findRecentEvents().stream().map(this::toResponse).toList(); }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> create(@RequestBody AlertConfigurationRequest request) {
        service.save(null, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Alert created successfully", null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> update(@PathVariable Integer id, @RequestBody AlertConfigurationRequest request) {
        service.save(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Alert updated successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Alert deleted successfully", null));
    }

    private AlertConfigurationResponse toResponse(AlertConfiguration alert) {
        return new AlertConfigurationResponse(alert.getId(), alert.getName(), alert.getTargetType(), alert.getServerId(), alert.getServiceId(),
                alert.getConditionType(), alert.getOperator(), alert.getConditionValue(), alert.isEnabled(), alert.getCreatedDttm(), alert.getLastUpdatedDttm());
    }

    private AlertEventResponse toResponse(AlertEvent event) {
        return new AlertEventResponse(event.getId(), event.getAlertId(), event.getServerId(), event.getServiceId(), event.getMessage(), event.getStatus(), event.getTriggeredDttm());
    }
}
