package com.pawar.todo.amt.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawar.todo.amt.model.AlertConfiguration;
import com.pawar.todo.amt.model.AlertEvent;
import com.pawar.todo.amt.response.AlertConfigurationRequest;
import com.pawar.todo.amt.respository.AlertConfigurationRepository;
import com.pawar.todo.amt.respository.AlertEventRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AlertConfigurationService {
    private final AlertConfigurationRepository configurationRepository;
    private final AlertEventRepository eventRepository;
    private final AlertEventService alertEventService;


    public AlertConfigurationService(AlertConfigurationRepository configurationRepository,
            AlertEventRepository eventRepository) {
        this(configurationRepository, eventRepository, null);
    }

    @Autowired
    public AlertConfigurationService(AlertConfigurationRepository configurationRepository,
            AlertEventRepository eventRepository,
            @Autowired(required = false) AlertEventService alertEventService) {
        this.configurationRepository = configurationRepository;
        this.eventRepository = eventRepository;
        this.alertEventService = alertEventService;
    }

    public List<AlertConfiguration> findAll() {
        return configurationRepository.findAll();
    }

    public List<AlertEvent> findRecentEvents() {
        return eventRepository.findTop50ByStatusOrderByTriggeredDttmDesc("TRIGGERED");
    }

    public List<AlertEvent> findUnreadEvents() {
        return eventRepository.findByIsReadFalseOrderByTriggeredDttmDesc();
    }

    public long countUnreadEvents() {
        return eventRepository.countByIsReadFalse();
    }

    @Transactional
    public void markAsRead(Integer id) {
        eventRepository.findById(id).ifPresent(event -> {
            event.setRead(true);
            eventRepository.save(event);
        });
    }

    @Transactional
    public void markAllAsRead() {
        List<AlertEvent> unread = eventRepository.findByIsReadFalseOrderByTriggeredDttmDesc();
        unread.forEach(event -> event.setRead(true));
        eventRepository.saveAll(unread);
    }

    @Transactional
    public AlertConfiguration save(Integer id, AlertConfigurationRequest request) {
        validate(request);
        AlertConfiguration alert = id == null ? new AlertConfiguration()
                : configurationRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));
        alert.setName(request.name().trim());
        alert.setTargetType(request.targetType());
        alert.setServerId(request.serverId());
        alert.setServiceId(request.serviceId());
        alert.setConditionType(request.conditionType());
        alert.setOperator(request.operator());
        alert.setConditionValue(request.conditionValue().trim());
        alert.setEnabled(request.enabled());
        alert.setLastUpdatedDttm(LocalDateTime.now());
        return configurationRepository.save(alert);
    }

    public void delete(Integer id) {
        configurationRepository.deleteById(id);
    }

    @Transactional
    public boolean toggle(Integer id) {
        AlertConfiguration alert = configurationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));
        alert.setEnabled(!alert.isEnabled());
        alert.setLastUpdatedDttm(LocalDateTime.now());
        configurationRepository.save(alert);
        return alert.isEnabled();
    }

    public boolean hasActiveAlert(Integer serverId, Integer serviceId) {
        return eventRepository.existsByServerIdAndServiceIdAndStatus(serverId, serviceId, "TRIGGERED");
    }

    @Transactional
    public void evaluate(Integer serverId, Integer serviceId, String status, Long responseTime) {
        for (AlertConfiguration alert : configurationRepository.findByEnabledTrue()) {
            if (!matchesTarget(alert, serverId, serviceId))
                continue;

            boolean conditionMatched = matchesCondition(alert, status, responseTime);
            List<AlertEvent> activeEvents = eventRepository.findByAlertIdAndServerIdAndServiceIdAndStatus(
                    alert.getId(), serverId, serviceId, "TRIGGERED");

            if (conditionMatched) {
                // If an alert is already in TRIGGERED state, do NOT log another event
                // (deduplicate)
                if (activeEvents.isEmpty()) {
                    AlertEvent event = new AlertEvent();
                    event.setAlertId(alert.getId());
                    event.setServerId(serverId);
                    event.setServiceId(serviceId);
                    event.setStatus("TRIGGERED");
                    event.setMessage(String.format("Alert '%s' triggered: %s %s %s", alert.getName(),
                            alert.getConditionType(), alert.getOperator(), alert.getConditionValue()));
                    event.setTriggeredDttm(LocalDateTime.now());
                    eventRepository.save(event);

                    if (alertEventService != null) {
                        alertEventService.logAlertEvent(alert.getId(), serverId, serviceId,
                                alert.getConditionType(), "TRIGGERED", event.getMessage());
                    }
                }
            } else {
                // Condition no longer holds (e.g. Service is UP) -> Close open alert
                if (!activeEvents.isEmpty()) {
                    for (AlertEvent activeEvent : activeEvents) {
                        activeEvent.setStatus("RESOLVED");
                        activeEvent.setMessage(
                                String.format("Alert '%s' closed: Service is now %s", alert.getName(), status));
                        activeEvent.setRead(false);
                        activeEvent.setTriggeredDttm(LocalDateTime.now());
                        eventRepository.save(activeEvent);
                    }

                    if (alertEventService != null) {
                        alertEventService.logAlertEvent(alert.getId(), serverId, serviceId,
                                alert.getConditionType(), "RESOLVED",
                                String.format("Alert '%s' closed: Service is now %s", alert.getName(), status));
                    }
                }
            }
        }
    }

    private boolean matchesTarget(AlertConfiguration alert, Integer serverId, Integer serviceId) {
        log.debug("Target Type : {}, Alert Server ID : {}, Alert Service ID : {}, Server ID : {}, Service ID : {}",
                alert.getTargetType(), alert.getServerId(), alert.getServiceId(), serverId, serviceId);
        if ("SERVER".equals(alert.getTargetType()))
            return alert.getServerId() != null && alert.getServerId().equals(serverId);
        return alert.getServiceId() != null && alert.getServiceId().equals(serviceId)
                && (alert.getServerId() == null || alert.getServerId().equals(serverId));
    }

    private boolean matchesCondition(AlertConfiguration alert, String status, Long responseTime) {
        log.debug("Condition Actual : {}, Condition Type : {}, Condition Operator : {}, Condition Value : {}", status,
                alert.getConditionType(), alert.getOperator(), alert.getConditionValue());
        String actual = "STATUS".equals(alert.getConditionType()) ? status
                : responseTime == null ? null : responseTime.toString();
        if (actual == null)
            return false;
        return "EQUALS".equals(alert.getOperator()) ? actual.equalsIgnoreCase(alert.getConditionValue())
                : "NOT_EQUALS".equals(alert.getOperator()) && !actual.equalsIgnoreCase(alert.getConditionValue());
    }

    private void validate(AlertConfigurationRequest request) {
        if (request == null || request.name() == null || request.name().isBlank())
            throw new IllegalArgumentException("Alert name is required");
        if (!"SERVER".equals(request.targetType()) && !"SERVICE".equals(request.targetType()))
            throw new IllegalArgumentException("Target type must be SERVER or SERVICE");
        if ("SERVER".equals(request.targetType()) && request.serverId() == null)
            throw new IllegalArgumentException("Server target is required");
        if ("SERVICE".equals(request.targetType()) && request.serviceId() == null)
            throw new IllegalArgumentException("Service target is required");
        if (!"STATUS".equals(request.conditionType()) && !"RESPONSE_TIME".equals(request.conditionType()))
            throw new IllegalArgumentException("Unsupported condition type");
        if (!"EQUALS".equals(request.operator()) && !"NOT_EQUALS".equals(request.operator()))
            throw new IllegalArgumentException("Unsupported operator");
        if (request.conditionValue() == null || request.conditionValue().isBlank())
            throw new IllegalArgumentException("Condition value is required");
    }
}
