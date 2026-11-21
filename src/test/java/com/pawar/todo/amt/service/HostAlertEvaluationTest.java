package com.pawar.todo.amt.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import com.pawar.todo.amt.model.AlertConfiguration;
import com.pawar.todo.amt.model.AlertEvent;
import com.pawar.todo.amt.response.AlertConfigurationRequest;
import com.pawar.todo.amt.response.HostMetricsResponse;
import com.pawar.todo.amt.respository.AlertConfigurationRepository;
import com.pawar.todo.amt.respository.AlertEventRepository;

// Issue #34: Host isolation, recovery, thresholds and legacy service-rule compatibility.
class HostAlertEvaluationTest {
    AlertConfigurationRepository rules;
    AlertEventRepository events;
    AlertConfigurationService service;
    AlertConfiguration rule;

    @BeforeEach void setup() {
        rules = mock(AlertConfigurationRepository.class);
        events = mock(AlertEventRepository.class);
        service = new AlertConfigurationService(rules, events);
        rule = new AlertConfiguration();
        rule.setId(10); rule.setName("Host down"); rule.setTargetType("SERVER");
        rule.setServerId(7); rule.setConditionType("SERVER_STATUS");
        rule.setOperator("EQUALS"); rule.setConditionValue("OFFLINE"); rule.setEnabled(true);
        when(rules.findByEnabledTrue()).thenReturn(List.of(rule));
        when(events.findByAlertIdAndServerIdAndServiceIdAndStatus(10, 7, null, "TRIGGERED")).thenReturn(List.of());
        when(rules.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test void offlineCreatesOneUnreadHostEventWithoutServiceId() {
        service.evaluateHost(7, "OFFLINE", null);
        var capture = ArgumentCaptor.forClass(AlertEvent.class);
        verify(events).save(capture.capture());
        assertNull(capture.getValue().getServiceId());
        assertEquals(7, capture.getValue().getServerId());
        assertFalse(capture.getValue().isRead());
        assertEquals("TRIGGERED", capture.getValue().getStatus());
    }

    @Test void repeatedOfflineDoesNotDuplicateAndRecoveryResolves() {
        var active = new AlertEvent(); active.setStatus("TRIGGERED"); active.setRead(true);
        when(events.findByAlertIdAndServerIdAndServiceIdAndStatus(10, 7, null, "TRIGGERED")).thenReturn(List.of(active));
        service.evaluateHost(7, "OFFLINE", null);
        verify(events, never()).save(any());
        service.evaluateHost(7, "ONLINE", null);
        verify(events).save(active);
        assertEquals("RESOLVED", active.getStatus());
        assertFalse(active.isRead());
    }

    @Test void anotherHostAndServiceSamplesCannotAffectHostAlert() {
        service.evaluateHost(8, "OFFLINE", null);
        service.evaluate(7, 1, "DOWN", 10L);
        verifyNoInteractions(events);
    }

    @Test void resourceGreaterThanHonorsBoundaryAndMissingSamplesDoNotResolve() {
        rule.setConditionType("RESOURCE_USAGE"); rule.setOperator("GREATER_THAN"); rule.setConditionValue("CPU:80");
        service.evaluateHost(7, "ONLINE", metrics(81));
        verify(events).save(any());
        reset(events);
        when(events.findByAlertIdAndServerIdAndServiceIdAndStatus(10, 7, null, "TRIGGERED")).thenReturn(List.of());
        service.evaluateHost(7, "ONLINE", metrics(80));
        verify(events, never()).save(any());
        reset(events);
        service.evaluateHost(7, "OFFLINE", HostMetricsResponse.unavailable(7));
        verifyNoInteractions(events);
    }

    @ParameterizedTest @ValueSource(strings = {"DISK:80", "MEMORY:80"})
    void memoryAndDiskThresholdsTrigger(String condition) {
        rule.setConditionType("RESOURCE_USAGE"); rule.setOperator("GREATER_THAN"); rule.setConditionValue(condition);
        service.evaluateHost(7, "ONLINE", new HostMetricsResponse(7, true, 0.0, 90.0, 90.0, 1L, Instant.now(), null));
        verify(events).save(any());
    }

    @Test void legacyServiceStatusAndResponseTimeRulesStillEvaluate() {
        rule.setTargetType("SERVICE"); rule.setServiceId(1); rule.setConditionType("STATUS"); rule.setConditionValue("DOWN");
        when(events.findByAlertIdAndServerIdAndServiceIdAndStatus(10, 7, 1, "TRIGGERED")).thenReturn(List.of());
        service.evaluate(7, 1, "DOWN", 12L);
        verify(events).save(any());
        reset(events);
        rule.setConditionType("RESPONSE_TIME"); rule.setOperator("GREATER_THAN"); rule.setConditionValue("10");
        service.evaluate(7, 1, "UP", 12L);
        verify(events).save(any());
    }

    @Test void resourceRecoveryResolvesAnActiveEvent() {
        rule.setConditionType("RESOURCE_USAGE"); rule.setOperator("GREATER_THAN"); rule.setConditionValue("CPU:80");
        var active = new AlertEvent(); active.setStatus("TRIGGERED"); active.setRead(true);
        when(events.findByAlertIdAndServerIdAndServiceIdAndStatus(10, 7, null, "TRIGGERED")).thenReturn(List.of(active));
        service.evaluateHost(7, "ONLINE", metrics(30));
        verify(events).save(active);
        assertEquals("RESOLVED", active.getStatus());
        assertFalse(active.isRead());
    }

    @Test void hostLegacyStatusAliasesAreSupported() {
        rule.setConditionType("STATUS"); rule.setConditionValue("DOWN");
        service.evaluateHost(7, "OFFLINE", null);
        verify(events).save(any());
    }

    @ParameterizedTest @ValueSource(strings = {"CPU:NaN", "DISK:101", "MEMORY:-1", "BOGUS:80", "80"})
    void invalidResourceThresholdsAreRejected(String condition) {
        assertThrows(IllegalArgumentException.class, () -> service.save(null,
                new AlertConfigurationRequest("resource", "SERVER", 7, null, "RESOURCE_USAGE", "GREATER_THAN", condition, true)));
        verify(rules, never()).save(any());
    }

    @Test void validServerStatusAndResourceRulesCanBeSaved() {
        service.save(null, new AlertConfigurationRequest("down", "SERVER", 7, null, "SERVER_STATUS", "EQUALS", "OFFLINE", true));
        service.save(null, new AlertConfigurationRequest("disk", "SERVER", 7, null, "RESOURCE_USAGE", "GREATER_THAN", "DISK:90", true));
        verify(rules, times(2)).save(any());
    }

    private HostMetricsResponse metrics(double cpu) {
        return new HostMetricsResponse(7, true, cpu, 10.0, 10.0, 100L, Instant.now(), null);
    }
}
