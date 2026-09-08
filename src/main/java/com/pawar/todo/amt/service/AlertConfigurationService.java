package com.pawar.todo.amt.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawar.todo.amt.model.AlertConfiguration;
import com.pawar.todo.amt.model.AlertEvent;
import com.pawar.todo.amt.respository.AlertConfigurationRepository;
import com.pawar.todo.amt.respository.AlertEventRepository;
import com.pawar.todo.amt.response.AlertConfigurationRequest;

@Service
public class AlertConfigurationService {
    private final AlertConfigurationRepository configurationRepository;
    private final AlertEventRepository eventRepository;

    public AlertConfigurationService(AlertConfigurationRepository configurationRepository,
            AlertEventRepository eventRepository) {
        this.configurationRepository = configurationRepository;
        this.eventRepository = eventRepository;
    }

    public List<AlertConfiguration> findAll() { return configurationRepository.findAll(); }

    public List<AlertEvent> findRecentEvents() { return eventRepository.findTop50ByOrderByTriggeredDttmDesc(); }

    @Transactional
    public AlertConfiguration save(Integer id, AlertConfigurationRequest request) {
        validate(request);
        AlertConfiguration alert = id == null ? new AlertConfiguration() : configurationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));
        alert.setName(request.name().trim());
        alert.setTargetType(request.targetType());
        alert.setServerId(request.serverId());
        alert.setServiceId(request.serviceId());
        alert.setConditionType(request.conditionType());
        alert.setOperator(request.operator());
        alert.setConditionValue(request.conditionValue().trim());
        alert.setEnabled(request.enabled() == null || request.enabled());
        return configurationRepository.saveAndFlush(alert);
    }

    public void delete(Integer id) { configurationRepository.deleteById(id); }

    @Transactional
    public void evaluate(Integer serverId, Integer serviceId, String status, Long responseTime) {
        for (AlertConfiguration alert : configurationRepository.findByEnabledTrue()) {
            if (!matchesTarget(alert, serverId, serviceId) || !matchesCondition(alert, status, responseTime)) continue;
            AlertEvent event = new AlertEvent();
            event.setAlertId(alert.getId());
            event.setServerId(serverId);
            event.setServiceId(serviceId);
            event.setStatus("TRIGGERED");
            event.setMessage(String.format("Alert '%s' triggered: %s %s %s", alert.getName(),
                    alert.getConditionType(), alert.getOperator(), alert.getConditionValue()));
            event.setTriggeredDttm(LocalDateTime.now());
            eventRepository.save(event);
        }
    }

    private boolean matchesTarget(AlertConfiguration alert, Integer serverId, Integer serviceId) {
        if ("SERVER".equals(alert.getTargetType())) return alert.getServerId() != null && alert.getServerId().equals(serverId);
        return alert.getServiceId() != null && alert.getServiceId().equals(serviceId)
                && (alert.getServerId() == null || alert.getServerId().equals(serverId));
    }

    private boolean matchesCondition(AlertConfiguration alert, String status, Long responseTime) {
        String actual = "STATUS".equals(alert.getConditionType()) ? status : responseTime == null ? null : responseTime.toString();
        if (actual == null) return false;
        return "EQUALS".equals(alert.getOperator()) ? actual.equalsIgnoreCase(alert.getConditionValue())
                : "NOT_EQUALS".equals(alert.getOperator()) && !actual.equalsIgnoreCase(alert.getConditionValue());
    }

    private void validate(AlertConfigurationRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) throw new IllegalArgumentException("Alert name is required");
        if (!"SERVER".equals(request.targetType()) && !"SERVICE".equals(request.targetType())) throw new IllegalArgumentException("Target type must be SERVER or SERVICE");
        if ("SERVER".equals(request.targetType()) && request.serverId() == null) throw new IllegalArgumentException("Server target is required");
        if ("SERVICE".equals(request.targetType()) && request.serviceId() == null) throw new IllegalArgumentException("Service target is required");
        if (!"STATUS".equals(request.conditionType()) && !"RESPONSE_TIME".equals(request.conditionType())) throw new IllegalArgumentException("Unsupported condition type");
        if (!"EQUALS".equals(request.operator()) && !"NOT_EQUALS".equals(request.operator())) throw new IllegalArgumentException("Unsupported operator");
        if (request.conditionValue() == null || request.conditionValue().isBlank()) throw new IllegalArgumentException("Condition value is required");
    }
}
