package com.pawar.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.pawar.todo.amt.model.UiAction;
import com.pawar.todo.amt.respository.UiActionRepository;
import com.pawar.todo.amt.service.UiActionCacheDriftMonitor;
import com.pawar.todo.amt.service.UiActionService;

@ExtendWith(MockitoExtension.class)
class UiActionServiceTest {
    @Mock private UiActionRepository repository;
    @Mock private CacheManager cacheManager;
    @Mock private UiActionCacheDriftMonitor driftMonitor;

    @Test
    void groupsActionsByCategoryAndPanel() {
        UiAction action = new UiAction();
        action.setSidebarCategory("Infrastructure"); action.setPanelTitle("Docker"); action.setActionLabel("Restart container");
        when(repository.findByViewContextOrderBySidebarCategoryAscPanelTitleAscActionLabelAsc("operations")).thenReturn(List.of(action));

        Map<String, Map<String, List<UiAction>>> grouped = new UiActionService(repository, cacheManager, driftMonitor).getGroupedActions("operations");

        assertEquals(action, grouped.get("Infrastructure").get("Docker").get(0));
    }

    @Test
    void createEvictsCacheForNewViewContextAndClearsDrift() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("sdui_configs")).thenReturn(cache);
        UiAction action = new UiAction();
        action.setViewContext("reports"); action.setActionLabel("Generate Report"); action.setActionEndpoint("/api/reports");
        when(repository.save(action)).thenAnswer(invocation -> {
            UiAction saved = invocation.getArgument(0);
            saved.setId(1);
            return saved;
        });

        UiAction created = new UiActionService(repository, cacheManager, driftMonitor).create(action);

        assertEquals(1, created.getId());
        verify(cache).evict("reports");
        verify(driftMonitor).clearDrift();
    }

    @Test
    void updateEvictsCacheForBothPreviousAndNewViewContext() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("sdui_configs")).thenReturn(cache);
        UiAction existing = new UiAction();
        existing.setId(2); existing.setViewContext("dashboard"); existing.setActionLabel("Old"); existing.setActionEndpoint("/api/old");
        when(repository.findById(2)).thenReturn(Optional.of(existing));
        when(repository.save(any(UiAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UiAction changes = new UiAction();
        changes.setViewContext("reports"); changes.setActionLabel("New"); changes.setActionEndpoint("/api/new");
        UiAction updated = new UiActionService(repository, cacheManager, driftMonitor).update(2, changes);

        assertEquals("reports", updated.getViewContext());
        verify(cache).evict("dashboard");
        verify(cache).evict("reports");
        verify(driftMonitor, times(1)).clearDrift();
    }

    @Test
    void deleteEvictsCacheForViewContext() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("sdui_configs")).thenReturn(cache);
        UiAction existing = new UiAction();
        existing.setId(3); existing.setViewContext("alerts");
        when(repository.findById(3)).thenReturn(Optional.of(existing));

        new UiActionService(repository, cacheManager, driftMonitor).delete(3);

        verify(repository).delete(existing);
        verify(cache).evict("alerts");
        verify(driftMonitor).clearDrift();
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> new UiActionService(repository, cacheManager, driftMonitor).findById(99));
    }

    @Test
    void refreshCacheSkipsEvictionWhenCacheUnavailable() {
        when(cacheManager.getCache("sdui_configs")).thenReturn(null);
        UiAction existing = new UiAction();
        existing.setId(4); existing.setViewContext("settings");
        when(repository.findById(4)).thenReturn(Optional.of(existing));

        new UiActionService(repository, cacheManager, driftMonitor).delete(4);

        verify(driftMonitor, never()).clearDrift();
    }
}