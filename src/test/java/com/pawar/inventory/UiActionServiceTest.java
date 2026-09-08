package com.pawar.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pawar.todo.amt.model.UiAction;
import com.pawar.todo.amt.respository.UiActionRepository;
import com.pawar.todo.amt.service.UiActionService;

@ExtendWith(MockitoExtension.class)
class UiActionServiceTest {
    @Mock private UiActionRepository repository;

    @Test
    void groupsActionsByCategoryAndPanel() {
        UiAction action = new UiAction();
        action.setSidebarCategory("Infrastructure"); action.setPanelTitle("Docker"); action.setActionLabel("Restart container");
        when(repository.findByViewContextOrderBySidebarCategoryAscPanelTitleAscActionLabelAsc("operations")).thenReturn(List.of(action));

        Map<String, Map<String, List<UiAction>>> grouped = new UiActionService(repository).getGroupedActions("operations");

        assertEquals(action, grouped.get("Infrastructure").get("Docker").get(0));
    }
}