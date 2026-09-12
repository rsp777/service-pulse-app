package com.pawar.todo.amt.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pawar.todo.amt.model.AlertConditionType;
import com.pawar.todo.amt.model.AlertConditionMapping;
import com.pawar.todo.amt.model.AlertEventLog;
import com.pawar.todo.amt.service.AlertEventService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertEventController {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertEventController.class);
    
    private final AlertEventService alertEventService;

    public AlertEventController(AlertEventService alertEventService) {
        this.alertEventService = alertEventService;
    }

    /**
     * Log a new alert event
     */
    @PostMapping("/events/log")
    public ResponseEntity<AlertEventLog> logAlertEvent(@RequestBody Map<String, Object> payload) {
        try {
            Integer alertId = ((Number) payload.get("alertId")).intValue();
            Integer serverId = payload.get("serverId") != null ? ((Number) payload.get("serverId")).intValue() : null;
            Integer serviceId = payload.get("serviceId") != null ? ((Number) payload.get("serviceId")).intValue() : null;
            String triggerCondition = (String) payload.get("triggerCondition");
            String alertStatus = (String) payload.get("alertStatus");
            String alertMessage = (String) payload.get("alertMessage");
            
            AlertEventLog eventLog = alertEventService.logAlertEvent(alertId, serverId, serviceId, 
                                                                      triggerCondition, alertStatus, alertMessage);
            logger.info("Alert event logged via API: alertId={}", alertId);
            return ResponseEntity.status(HttpStatus.CREATED).body(eventLog);
        } catch (Exception error) {
            logger.warn("Failed to log alert event: {}", error.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get alert events for a specific alert
     */
    @GetMapping("/event-logs")
    public ResponseEntity<List<AlertEventLog>> getAlertEvents(@RequestParam Integer alertId) {
        try {
            List<AlertEventLog> events = alertEventService.getAlertEvents(alertId);
            return ResponseEntity.ok(events);
        } catch (Exception error) {
            logger.warn("Failed to fetch alert events: {}", error.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all condition types
     */
    @GetMapping("/condition-types")
    public ResponseEntity<List<AlertConditionType>> getConditionTypes() {
        try {
            List<AlertConditionType> types = alertEventService.getAllConditionTypes();
            return ResponseEntity.ok(types);
        } catch (Exception error) {
            logger.warn("Failed to fetch condition types: {}", error.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get condition mappings for a type
     */
    @GetMapping("/condition-types/{typeId}/mappings")
    public ResponseEntity<List<AlertConditionMapping>> getConditionMappings(@PathVariable Integer typeId) {
        try {
            List<AlertConditionMapping> mappings = alertEventService.getConditionMappings(typeId);
            return ResponseEntity.ok(mappings);
        } catch (Exception error) {
            logger.warn("Failed to fetch condition mappings: {}", error.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create a new condition mapping
     */
    @PostMapping("/condition-types/{typeId}/mappings")
    public ResponseEntity<AlertConditionMapping> createConditionMapping(@PathVariable Integer typeId,
                                                                         @RequestBody Map<String, String> payload) {
        try {
            String conditionKey = payload.get("conditionKey");
            String conditionValue = payload.get("conditionValue");
            String description = payload.get("description");
            
            AlertConditionMapping mapping = alertEventService.createConditionMapping(typeId, conditionKey, conditionValue, description);
            logger.info("Condition mapping created: typeId={}, key={}", typeId, conditionKey);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapping);
        } catch (Exception error) {
            logger.warn("Failed to create condition mapping: {}", error.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update a condition mapping
     */
    @PutMapping("/condition-mappings/{mappingId}")
    public ResponseEntity<AlertConditionMapping> updateConditionMapping(@PathVariable Integer mappingId,
                                                                         @RequestBody Map<String, String> payload) {
        try {
            String conditionValue = payload.get("conditionValue");
            String description = payload.get("description");
            
            AlertConditionMapping mapping = alertEventService.updateConditionMapping(mappingId, conditionValue, description);
            logger.info("Condition mapping updated: mappingId={}", mappingId);
            return ResponseEntity.ok(mapping);
        } catch (Exception error) {
            logger.warn("Failed to update condition mapping: {}", error.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a condition mapping
     */
    @DeleteMapping("/condition-mappings/{mappingId}")
    public ResponseEntity<Void> deleteConditionMapping(@PathVariable Integer mappingId) {
        try {
            alertEventService.deleteConditionMapping(mappingId);
            logger.info("Condition mapping deleted: mappingId={}", mappingId);
            return ResponseEntity.noContent().build();
        } catch (Exception error) {
            logger.warn("Failed to delete condition mapping: {}", error.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
