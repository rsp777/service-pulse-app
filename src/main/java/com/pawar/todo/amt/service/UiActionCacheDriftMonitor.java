package com.pawar.todo.amt.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pawar.todo.amt.model.UiAction;
import com.pawar.todo.amt.respository.UiActionRepository;

@Component
public class UiActionCacheDriftMonitor {
    private final UiActionRepository repository;
    private final UiActionService service;
    private final CacheManager cacheManager;
    private final AtomicBoolean driftDetected = new AtomicBoolean(false);

    public UiActionCacheDriftMonitor(UiActionRepository repository, UiActionService service, CacheManager cacheManager) {
        this.repository = repository;
        this.service = service;
        this.cacheManager = cacheManager;
    }

    @Scheduled(fixedDelayString = "${sdui.cache-drift-check-millis:60000}")
    public void detectDrift() {
        Cache cache = cacheManager.getCache("sdui_configs");
        if (cache == null) return;
        driftDetected.set(repository.findDistinctViewContexts().stream().anyMatch(viewContext -> differs(cache, viewContext)));
    }

    private boolean differs(Cache cache, String viewContext) {
        Cache.ValueWrapper cached = cache.get(viewContext);
        if (!(cached != null && cached.get() instanceof List<?>)) return false;
        @SuppressWarnings("unchecked") List<UiAction> cachedActions = (List<UiAction>) cached.get();
        List<UiAction> databaseActions = repository.findByViewContextOrderBySidebarCategoryAscPanelTitleAscActionLabelAsc(viewContext);
        return !service.md5(cachedActions).equals(service.md5(databaseActions));
    }

    public boolean isDriftDetected() { return driftDetected.get(); }
    public void clearDrift() { driftDetected.set(false); }
}