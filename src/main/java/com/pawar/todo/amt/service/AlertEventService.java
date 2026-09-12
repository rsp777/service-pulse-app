package com.pawar.todo.amt.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pawar.todo.amt.model.AlertConditionType;
import com.pawar.todo.amt.model.AlertConditionMapping;
import com.pawar.todo.amt.model.AlertEventLog;
import com.pawar.todo.amt.repository.AlertConditionTypeRepository;
import com.pawar.todo.amt.repository.AlertConditionMappingRepository;
import com.pawar.todo.amt.repository.AlertEventLogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AlertEventService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertEventService.class);
    
    private final AlertConditionTypeRepository conditionTypeRepository;
    private final AlertConditionMappingRepository conditionMappingRepository;
    private final AlertEventLogRepository eventLogRepository;

    public AlertEventService(AlertConditionTypeRepository conditionTypeRepository,
                           AlertConditionMappingRepository conditionMappingRepository,
                           AlertEventLogRepository eventLogRepository) {
        this.conditionTypeRepository = conditionTypeRepository;
        this.conditionMappingRepository = conditionMappingRepository;
        this.eventLogRepository = eventLogRepository;
    }

    /**
     * Log an alert event when triggered
     */
    public AlertEventLog logAlertEvent(Integer alertId, Integer serverId, Integer serviceId, 
                                       String triggerCondition, String alertStatus, String alertMessage) {
        AlertEventLog eventLog = new AlertEventLog(alertId, serverId, serviceId, triggerCondition, alertStatus, alertMessage);
        eventLog = eventLogRepository.save(eventLog);
        
        logger.info("Alert event logged: alertId={}, serverId={}, serviceId={}, status={}, condition={}", 
                    alertId, serverId, serviceId, alertStatus, triggerCondition);
        return eventLog;
    }

    /**
     * Get all alert events for a specific alert
     */
    public List<AlertEventLog> getAlertEvents(Integer alertId) {
        return eventLogRepository.findByAlertIdOrderByTriggeredDttmDesc(alertId);
    }

    /**
     * Get alert events within a date range
     */
    public List<AlertEventLog> getAlertEventsByDateRange(LocalDateTime start, LocalDateTime end) {
        return eventLogRepository.findByTriggeredDttmBetweenOrderByTriggeredDttmDesc(start, end);
    }

    /**
     * Get all alert events by status
     */
    public List<AlertEventLog> getAlertEventsByStatus(String alertStatus) {
        return eventLogRepository.findByAlertStatusOrderByTriggeredDttmDesc(alertStatus);
    }

    /**
     * Get all alert condition types
     */
    public List<AlertConditionType> getAllConditionTypes() {
        return conditionTypeRepository.findAll();
    }

    /**
     * Get condition type by ID
     */
    public Optional<AlertConditionType> getConditionType(Integer typeId) {
        return conditionTypeRepository.findById(typeId);
    }

    /**
     * Get condition type by name
     */
    public Optional<AlertConditionType> getConditionTypeByName(String name) {
        return conditionTypeRepository.findByName(name);
    }

    /**
     * Get all condition mappings for a type
     */
    public List<AlertConditionMapping> getConditionMappings(Integer typeId) {
        return conditionMappingRepository.findByConditionTypeIdOrderByConditionKeyAsc(typeId);
    }

    /**
     * Create a new condition mapping
     */
    public AlertConditionMapping createConditionMapping(Integer typeId, String conditionKey, 
                                                        String conditionValue, String description) {
        Optional<AlertConditionType> typeOpt = conditionTypeRepository.findById(typeId);
        if (!typeOpt.isPresent()) {
            throw new IllegalArgumentException("Condition type not found: " + typeId);
        }
        
        AlertConditionMapping mapping = new AlertConditionMapping(typeOpt.get(), conditionKey, conditionValue, description);
        mapping = conditionMappingRepository.save(mapping);
        
        logger.info("Condition mapping created: typeId={}, key={}, value={}", typeId, conditionKey, conditionValue);
        return mapping;
    }

    /**
     * Update a condition mapping
     */
    public AlertConditionMapping updateConditionMapping(Integer mappingId, String conditionValue, String description) {
        AlertConditionMapping mapping = conditionMappingRepository.findById(mappingId)
                .orElseThrow(() -> new IllegalArgumentException("Mapping not found: " + mappingId));
        
        mapping.setConditionValue(conditionValue);
        mapping.setDescription(description);
        mapping = conditionMappingRepository.save(mapping);
        
        logger.info("Condition mapping updated: mappingId={}, value={}", mappingId, conditionValue);
        return mapping;
    }

    /**
     * Delete a condition mapping
     */
    public void deleteConditionMapping(Integer mappingId) {
        conditionMappingRepository.deleteById(mappingId);
        logger.info("Condition mapping deleted: mappingId={}", mappingId);
    }
}
