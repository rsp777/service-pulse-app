package com.pawar.todo.amt.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(AlertConfigurationController.class);
    private final AlertConfigurationService service;

    public AlertConfigurationController(AlertConfigurationService service) { this.service = service; }

    @GetMapping
    public List<AlertConfigurationResponse> findAll() { return service.findAll().stream().map(this::toResponse).toList(); }

    @GetMapping("/events")
    public List<AlertEventResponse> events() { return service.findRecentEvents().stream().map(this::toResponse).toList(); }

    @GetMapping("/events/unread")
    public List<AlertEventResponse> unreadEvents() { return service.findUnreadEvents().stream().map(this::toResponse).toList(); }

    @PutMapping("/events/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Integer id) {
        service.markAsRead(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Marked as read", null));
    }

    @PutMapping("/events/read-all")
    public ResponseEntity<ApiResponse<String>> markAllAsRead() {
        service.markAllAsRead();
        return ResponseEntity.ok(new ApiResponse<>(true, "Marked all as read", null));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> create(@RequestBody AlertConfigurationRequest request) {
        log.info("Creating new alert rule: name='{}'", request.name());
        service.save(null, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Alert created successfully", null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> update(@PathVariable Integer id, @RequestBody AlertConfigurationRequest request) {
        log.info("Updating alert rule id={}", id);
        service.save(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Alert updated successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Integer id) {
        log.info("Deleting alert rule id={}", id);
        service.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Alert deleted successfully", null));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<String>> toggle(@PathVariable Integer id) {
        log.info("Toggling alert rule id={}", id);
        boolean newState = service.toggle(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Alert " + (newState ? "enabled" : "disabled") + " successfully", null));
    }

    private AlertConfigurationResponse toResponse(AlertConfiguration alert) {
        return new AlertConfigurationResponse(alert.getId(), alert.getName(), alert.getTargetType(), alert.getServerId(), alert.getServiceId(),
                alert.getConditionType(), alert.getOperator(), alert.getConditionValue(), alert.isEnabled(), alert.getCreatedDttm(), alert.getLastUpdatedDttm());
    }

    private AlertEventResponse toResponse(AlertEvent event) {
        return new AlertEventResponse(event.getId(), event.getAlertId(), event.getServerId(), event.getServiceId(), event.getMessage(), event.getStatus(), event.isRead(), event.getTriggeredDttm());
    }
}
