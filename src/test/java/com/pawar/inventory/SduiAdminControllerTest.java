package com.pawar.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;

import com.pawar.todo.amt.controller.SduiAdminController;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.UiActionCacheDriftMonitor;

@ExtendWith(MockitoExtension.class)
class SduiAdminControllerTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private UiActionCacheDriftMonitor driftMonitor;

    @Test
    void returnsServiceUnavailableWhenSduiCacheIsNotConfigured() {
        when(cacheManager.getCache("sdui_configs")).thenReturn(null);

        ResponseEntity<ApiResponse<Void>> response = new SduiAdminController(cacheManager, driftMonitor).clearSduiCache();

        assertEquals(503, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals("SDUI cache is unavailable", response.getBody().getMessage());
        verifyNoInteractions(driftMonitor);
    }
}