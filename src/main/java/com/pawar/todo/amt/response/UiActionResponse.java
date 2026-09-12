package com.pawar.todo.amt.response;

import com.fasterxml.jackson.databind.JsonNode;

public record UiActionResponse(
        Integer id,
        String viewContext,
        String actionLabel,
        String actionEndpoint,
        String componentType,
        JsonNode componentConfig,
        JsonNode requestPayload,
        String sidebarCategory,
        String panelTitle,
        String gridSpan,
        String iconClass) {
}
