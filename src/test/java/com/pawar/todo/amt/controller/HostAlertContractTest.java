package com.pawar.todo.amt.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.pawar.todo.amt.respository.AlertConfigurationRepository;
import com.pawar.todo.amt.respository.AlertEventRepository;
import com.pawar.todo.amt.service.AlertConfigurationService;

// Issue #34: Exercise JSON-to-rule persistence and rejected thresholds through Spring MVC.
class HostAlertContractTest {
    @Test void hostRuleIsAcceptedAndInvalidThresholdIsRejected() throws Exception {
        var rules = mock(AlertConfigurationRepository.class);
        when(rules.save(any())).thenAnswer(call -> call.getArgument(0));
        var mvc = MockMvcBuilders.standaloneSetup(new AlertConfigurationController(
                new AlertConfigurationService(rules, mock(AlertEventRepository.class)))).build();
        mvc.perform(post("/api/alerts").contentType("application/json").content("""
                {"name":"Host down","targetType":"SERVER","serverId":7,"conditionType":"SERVER_STATUS",
                 "operator":"EQUALS","conditionValue":"OFFLINE","enabled":true}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        mvc.perform(post("/api/alerts").contentType("application/json").content("""
                {"name":"Disk","targetType":"SERVER","serverId":7,"conditionType":"RESOURCE_USAGE",
                 "operator":"GREATER_THAN","conditionValue":"DISK:NaN","enabled":true}
                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verify(rules, times(1)).save(any());
    }
}
