package com.pawar.todo.amt.controller;

import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.UiActionCacheDriftMonitor;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/cache")
@Slf4j
public class SduiAdminController {
    private final CacheManager cacheManager;
    private final UiActionCacheDriftMonitor driftMonitor;
    public SduiAdminController(CacheManager cacheManager, UiActionCacheDriftMonitor driftMonitor) {
        this.cacheManager = cacheManager; this.driftMonitor = driftMonitor;
    }
    @PostMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearSduiCache() {
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache("sdui_configs");
            if (cache == null) {
                log.warn("SDUI cache clear requested but cache sdui_configs is unavailable");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new ApiResponse<>(false, "SDUI cache is unavailable", null));
            }
            cache.clear();
            driftMonitor.clearDrift();
            log.info("SDUI cache cleared through the administration endpoint");
            return ResponseEntity.ok(new ApiResponse<>(true, "SDUI cache cleared", null));
        } catch (RuntimeException exception) {
            log.error("Unable to clear SDUI cache", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to clear SDUI cache", null));
        }
    }
}