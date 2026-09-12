package com.pawar.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import com.pawar.todo.amt.controller.UniversalScreenController;
import com.pawar.todo.amt.model.UiAction;
import com.pawar.todo.amt.service.UiActionService;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class UniversalScreenControllerTest {

    @Mock
    private UiActionService uiActionService;

    @Mock
    private HttpServletRequest request;

    @Test
    void mapsContextToTheUniversalDashboardModel() {
        Map<String, Map<String, List<UiAction>>> actions = Map.of("Infrastructure", Map.of("Docker", List.of(new UiAction())));
        when(uiActionService.getGroupedActions("operations")).thenReturn(actions);
        when(request.getContextPath()).thenReturn("/service-pulse-app");
        ExtendedModelMap model = new ExtendedModelMap();

        String view = new UniversalScreenController(uiActionService).renderDashboard("operations", model, request);

        assertEquals("generic-screen", view);
        assertEquals("operations", model.get("viewContext"));
        assertEquals("/service-pulse-app", model.get("contextPath"));
        assertEquals(actions, model.get("dashboardActions"));
    }
}