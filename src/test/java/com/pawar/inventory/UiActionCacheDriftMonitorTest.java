package com.pawar.inventory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import com.pawar.todo.amt.model.UiAction;
import com.pawar.todo.amt.respository.UiActionRepository;
import com.pawar.todo.amt.service.UiActionCacheDriftMonitor;
import com.pawar.todo.amt.service.UiActionService;

@ExtendWith(MockitoExtension.class)
class UiActionCacheDriftMonitorTest {
    @Mock private UiActionRepository repository;

    @Test
    void detectsDatabaseChangesToCachedActions() {
        UiAction cachedAction = action("Restart container");
        UiAction databaseAction = action("Stop container");
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("sdui_configs");
        cacheManager.getCache("sdui_configs").put("operations", List.of(cachedAction));
        when(repository.findDistinctViewContexts()).thenReturn(List.of("operations"));
        when(repository.findByViewContextOrderBySidebarCategoryAscPanelTitleAscActionLabelAsc("operations"))
                .thenReturn(List.of(databaseAction));

        UiActionCacheDriftMonitor monitor = new UiActionCacheDriftMonitor(repository,
                new UiActionService(repository, cacheManager, null), cacheManager);
        monitor.detectDrift();

        assertTrue(monitor.isDriftDetected());
    }

    private UiAction action(String label) {
        UiAction action = new UiAction();
        action.setId(1);
        action.setViewContext("operations");
        action.setActionLabel(label);
        action.setActionEndpoint("/api/commands/execute?serverId=1&command=status");
        return action;
    }
}