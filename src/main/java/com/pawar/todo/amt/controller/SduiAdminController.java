package com.pawar.todo.amt.controller;

import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.UiActionCacheDriftMonitor;

@RestController
@RequestMapping("/api/admin/cache")
public class SduiAdminController {
    private final CacheManager cacheManager;
    private final UiActionCacheDriftMonitor driftMonitor;
    public SduiAdminController(CacheManager cacheManager, UiActionCacheDriftMonitor driftMonitor) {
        this.cacheManager = cacheManager; this.driftMonitor = driftMonitor;
    }
    @PostMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearSduiCache() {
        cacheManager.getCache("sdui_configs").clear();
        driftMonitor.clearDrift();
        return ResponseEntity.ok(new ApiResponse<>(true, "SDUI cache cleared", null));
    }
}