package com.pawar.todo.amt.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawar.todo.amt.model.AlertConfiguration;
import com.pawar.todo.amt.model.AlertEvent;
import com.pawar.todo.amt.response.AlertConfigurationRequest;
import com.pawar.todo.amt.response.HostMetricsResponse;
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

    // Issue #34: Service samples cannot trigger or resolve a host-level rule.
    @Transactional
    public void evaluate(Integer serverId, Integer serviceId, String status, Long responseTime) {
        for (AlertConfiguration alert : configurationRepository.findByEnabledTrue()) {
            if (!"SERVICE".equals(alert.getTargetType()) || !matchesTarget(alert, serverId, serviceId)) continue;
            String actual = "RESPONSE_TIME".equals(alert.getConditionType())
                    ? (responseTime == null ? null : responseTime.toString()) : status;
            if (!List.of("STATUS", "SERVICE_STATUS", "RESPONSE_TIME").contains(alert.getConditionType()) || actual == null) continue;
            recordEvaluation(alert, serverId, serviceId, matches(alert, actual, alert.getConditionValue()), actual);
        }
    }

    public boolean hasResourceAlerts(Integer serverId) {
        return configurationRepository.findByEnabledTrue().stream().anyMatch(alert ->
                "SERVER".equals(alert.getTargetType()) && serverId.equals(alert.getServerId())
                && "RESOURCE_USAGE".equals(alert.getConditionType()));
    }

    @Transactional
    public void evaluateHost(Integer serverId, String status, HostMetricsResponse metrics) {
        for (AlertConfiguration alert : configurationRepository.findByEnabledTrue()) {
            if (!"SERVER".equals(alert.getTargetType()) || !serverId.equals(alert.getServerId())) continue;
            String actual;
            String expected = alert.getConditionValue();
            if ("RESOURCE_USAGE".equals(alert.getConditionType())) {
                if (metrics == null || !metrics.available()) continue; // No measurement cannot mean recovery.
                String[] threshold = expected.split(":", -1);
                if (threshold.length != 2) continue;
                Double value = switch (threshold[0].toUpperCase(Locale.ROOT)) {
                    case "CPU" -> metrics.cpuPercent();
                    case "MEMORY" -> metrics.memoryPercent();
                    case "DISK" -> metrics.diskPercent();
                    default -> null;
                };
                if (value == null || !Double.isFinite(value)) continue;
                actual = value.toString();
                expected = threshold[1];
            } else if (List.of("SERVER_STATUS", "STATUS").contains(alert.getConditionType())) {
                actual = status;
            } else continue;
            recordEvaluation(alert, serverId, null, matches(alert, actual, expected), actual);
        }
    }

    private void recordEvaluation(AlertConfiguration alert, Integer serverId, Integer serviceId, boolean matched, String actual) {
        List<AlertEvent> active = eventRepository.findByAlertIdAndServerIdAndServiceIdAndStatus(
                alert.getId(), serverId, serviceId, "TRIGGERED");
        if (matched && active.isEmpty()) {
            AlertEvent event = new AlertEvent();
            event.setAlertId(alert.getId());
            event.setServerId(serverId);
            event.setServiceId(serviceId);
            event.setStatus("TRIGGERED");
            event.setRead(false);
            event.setMessage(String.format("Alert '%s' triggered: %s %s %s (actual %s)", alert.getName(),
                    alert.getConditionType(), alert.getOperator(), alert.getConditionValue(), actual));
            event.setTriggeredDttm(LocalDateTime.now());
            eventRepository.save(event);
            logEvent(alert, serverId, serviceId, "TRIGGERED", event.getMessage());
        } else if (!matched && !active.isEmpty()) {
            String message = String.format("Alert '%s' resolved: %s is now %s", alert.getName(), alert.getConditionType(), actual);
            for (AlertEvent event : active) {
                event.setStatus("RESOLVED");
                event.setMessage(message);
                event.setRead(false);
                event.setTriggeredDttm(LocalDateTime.now());
                eventRepository.save(event);
            }
            logEvent(alert, serverId, serviceId, "RESOLVED", message);
        }
    }

    private void logEvent(AlertConfiguration alert, Integer serverId, Integer serviceId, String status, String message) {
        if (alertEventService != null)
            alertEventService.logAlertEvent(alert.getId(), serverId, serviceId, alert.getConditionType(), status, message);
    }

    private boolean matchesTarget(AlertConfiguration alert, Integer serverId, Integer serviceId) {
        return alert.getServiceId() != null && alert.getServiceId().equals(serviceId)
                && (alert.getServerId() == null || alert.getServerId().equals(serverId));
    }

    private boolean matches(AlertConfiguration alert, String actual, String expected) {
        if (List.of("STATUS", "SERVICE_STATUS", "SERVER_STATUS").contains(alert.getConditionType())) {
            boolean equal = normalizeStatus(actual).equals(normalizeStatus(expected));
            return "EQUALS".equals(alert.getOperator()) ? equal : "NOT_EQUALS".equals(alert.getOperator()) && !equal;
        }
        try {
            double a = finiteNumber(actual), b = finiteNumber(expected);
            return switch (alert.getOperator()) {
                case "EQUALS" -> a == b;
                case "NOT_EQUALS" -> a != b;
                case "GREATER_THAN" -> a > b;
                case "LESS_THAN" -> a < b;
                default -> false;
            };
        } catch (IllegalArgumentException exception) { return false; }
    }

    private static String normalizeStatus(String value) {
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "UP" -> "ONLINE";
            case "DOWN" -> "OFFLINE";
            default -> value.toUpperCase(Locale.ROOT);
        };
    }

    private static double finiteNumber(String value) {
        double number = Double.parseDouble(value);
        if (!Double.isFinite(number)) throw new IllegalArgumentException("Threshold must be finite");
        return number;
    }

    private void validate(AlertConfigurationRequest request) {
        if (request == null || request.name() == null || request.name().isBlank())
            throw new IllegalArgumentException("Alert name is required");
        if (!"SERVER".equals(request.targetType()) && !"SERVICE".equals(request.targetType()))
            throw new IllegalArgumentException("Target type must be SERVER or SERVICE");
        if ("SERVER".equals(request.targetType()) && (request.serverId() == null || request.serverId() <= 0))
            throw new IllegalArgumentException("Server target is required");
        if ("SERVICE".equals(request.targetType()) && (request.serviceId() == null || request.serviceId() <= 0))
            throw new IllegalArgumentException("Service target is required");
        if (request.enabled() == null) throw new IllegalArgumentException("Enabled flag is required");
        if (request.conditionType() == null || !List.of("STATUS", "SERVICE_STATUS", "SERVER_STATUS", "RESPONSE_TIME", "RESOURCE_USAGE").contains(request.conditionType()))
            throw new IllegalArgumentException("Unsupported condition type");
        boolean host = "SERVER".equals(request.targetType());
        if (host && List.of("SERVICE_STATUS", "RESPONSE_TIME").contains(request.conditionType())
                || !host && List.of("SERVER_STATUS", "RESOURCE_USAGE").contains(request.conditionType()))
            throw new IllegalArgumentException("Condition type does not match target scope");
        if (request.conditionValue() == null || request.conditionValue().isBlank())
            throw new IllegalArgumentException("Condition value is required");
        boolean numeric = List.of("RESPONSE_TIME", "RESOURCE_USAGE").contains(request.conditionType());
        if (request.operator() == null || !(numeric
                ? List.of("EQUALS", "NOT_EQUALS", "GREATER_THAN", "LESS_THAN")
                : List.of("EQUALS", "NOT_EQUALS")).contains(request.operator()))
            throw new IllegalArgumentException("Unsupported operator");
        String threshold = request.conditionValue().trim();
        if ("RESOURCE_USAGE".equals(request.conditionType())) {
            String[] parts = threshold.split(":", -1);
            if (parts.length != 2 || !List.of("CPU", "MEMORY", "DISK").contains(parts[0].toUpperCase(Locale.ROOT)))
                throw new IllegalArgumentException("Use CPU:80, MEMORY:85 or DISK:90 for resource thresholds");
            double value = finiteNumber(parts[1]);
            if (value < 0 || value > 100) throw new IllegalArgumentException("Resource percentage must be between 0 and 100");
        } else if ("RESPONSE_TIME".equals(request.conditionType())) {
            if (finiteNumber(threshold) < 0) throw new IllegalArgumentException("Response time must be nonnegative");
        } else if (!List.of("UP", "DOWN", "ONLINE", "OFFLINE", "UNKNOWN").contains(threshold.toUpperCase(Locale.ROOT)))
            throw new IllegalArgumentException("Unsupported status value");
    }
}
