package com.pawar.todo.amt.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawar.todo.amt.model.UiAction;
import com.pawar.todo.amt.respository.UiActionRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UiActionService {

    private static final String CACHE_NAME = "sdui_configs";

    private final UiActionRepository repository;
    private final CacheManager cacheManager;
    private final UiActionCacheDriftMonitor driftMonitor;

    public UiActionService(UiActionRepository repository, CacheManager cacheManager,
            @Lazy UiActionCacheDriftMonitor driftMonitor) {
        this.repository = repository;
        this.cacheManager = cacheManager;
        this.driftMonitor = driftMonitor;
    }

    @Cacheable(value = CACHE_NAME, key = "#viewContext")
    @Transactional(readOnly = true)
    public List<UiAction> getActions(String viewContext) {
        List<UiAction> actions = repository.findByViewContextOrderBySidebarCategoryAscPanelTitleAscActionLabelAsc(viewContext);
        log.info("Loaded {} SDUI actions for view context={}", actions.size(), viewContext);
        return actions;
    }

    public Map<String, Map<String, List<UiAction>>> getGroupedActions(String viewContext) {
        Map<String, Map<String, List<UiAction>>> groupedActions = getActions(viewContext).stream().collect(Collectors.groupingBy(
                action -> valueOrDefault(action.getSidebarCategory(), "General"), TreeMap::new,
                Collectors.groupingBy(action -> valueOrDefault(action.getPanelTitle(), "Actions"), TreeMap::new,
                        Collectors.toList())));
        log.debug("Grouped SDUI actions for view context={} into {} categories", viewContext, groupedActions.size());
        return groupedActions;
    }

    @Transactional(readOnly = true)
    public List<String> getViewContexts() {
        return repository.findDistinctViewContexts().stream().sorted().toList();
    }

    @Transactional(readOnly = true)
    public List<UiAction> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public UiAction findById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("SDUI action not found with ID: " + id));
    }

    @Transactional
    public UiAction create(UiAction action) {
        action.setId(null);
        UiAction saved = repository.save(action);
        refreshCache(saved.getViewContext());
        log.info("Created SDUI action id={} for view context={}", saved.getId(), saved.getViewContext());
        return saved;
    }

    @Transactional
    public UiAction update(Integer id, UiAction changes) {
        UiAction existing = findById(id);
        String previousViewContext = existing.getViewContext();
        existing.setViewContext(changes.getViewContext());
        existing.setActionLabel(changes.getActionLabel());
        existing.setActionEndpoint(changes.getActionEndpoint());
        existing.setComponentType(changes.getComponentType());
        existing.setComponentConfig(changes.getComponentConfig());
        existing.setRequestPayload(changes.getRequestPayload());
        existing.setSidebarCategory(changes.getSidebarCategory());
        existing.setPanelTitle(changes.getPanelTitle());
        existing.setGridSpan(changes.getGridSpan());
        existing.setIconClass(changes.getIconClass());
        UiAction saved = repository.save(existing);
        refreshCache(previousViewContext, saved.getViewContext());
        log.info("Updated SDUI action id={} for view context={}", saved.getId(), saved.getViewContext());
        return saved;
    }

    @Transactional
    public void delete(Integer id) {
        UiAction existing = findById(id);
        repository.delete(existing);
        refreshCache(existing.getViewContext());
        log.info("Deleted SDUI action id={} for view context={}", id, existing.getViewContext());
    }

    /**
     * Evicts the cached SDUI actions for the given view contexts so the next dashboard request
     * loads fresh data from the database. This keeps the rendered menus/fields automatically in
     * sync whenever a screen configuration is created, updated, or deleted through the API,
     * without requiring a manual cache clear or application restart.
     */
    private void refreshCache(String... viewContexts) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            log.warn("Unable to refresh SDUI cache because cache {} is unavailable", CACHE_NAME);
            return;
        }
        for (String viewContext : viewContexts) {
            if (viewContext == null || viewContext.isBlank()) continue;
            cache.evict(viewContext);
        }
        driftMonitor.clearDrift();
        log.info("SDUI cache refreshed for view context(s)={}", String.join(",", viewContexts));
    }

    public String md5(List<UiAction> actions) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            for (UiAction action : actions) {
                String value = String.join("|", String.valueOf(action.getId()), action.getViewContext(),
                        action.getActionLabel(), action.getActionEndpoint(), valueOrDefault(action.getSidebarCategory(), ""),
                        valueOrDefault(action.getPanelTitle(), ""), valueOrDefault(action.getGridSpan(), "span-12"),
                    valueOrDefault(action.getIconClass(), ""), valueOrDefault(action.getComponentType(), "ACTION"),
                    action.getComponentConfig() == null ? "" : action.getComponentConfig(),
                    action.getRequestPayload() == null ? "" : action.getRequestPayload());
                digest.update(value.getBytes(StandardCharsets.UTF_8));
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 algorithm is unavailable", exception);
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String toHex(byte[] hash) {
        StringBuilder result = new StringBuilder(hash.length * 2);
        for (byte value : hash) result.append(String.format("%02x", value));
        return result.toString();
    }
}