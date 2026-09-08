package com.pawar.todo.amt.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawar.todo.amt.model.UiAction;
import com.pawar.todo.amt.respository.UiActionRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UiActionService {

    private final UiActionRepository repository;

    public UiActionService(UiActionRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "sdui_configs", key = "#viewContext")
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

    public String md5(List<UiAction> actions) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            for (UiAction action : actions) {
                String value = String.join("|", String.valueOf(action.getId()), action.getViewContext(),
                        action.getActionLabel(), action.getActionEndpoint(), valueOrDefault(action.getSidebarCategory(), ""),
                        valueOrDefault(action.getPanelTitle(), ""), valueOrDefault(action.getGridSpan(), "span-12"),
                        valueOrDefault(action.getIconClass(), ""), action.getRequestPayload() == null ? "" : action.getRequestPayload().toString());
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