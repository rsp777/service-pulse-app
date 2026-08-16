package com.pawar.todo.amt.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.AgentResponseDto;

@Component
public class InMemoryCacheAgent implements AgentCache {

    private final int MAX_CACHE_SIZE = 100; // Set your desired cache size
    private final Map<Integer, AgentResponseDto> cache = new LinkedHashMap<Integer, AgentResponseDto>(MAX_CACHE_SIZE, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Integer, AgentResponseDto> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    @Override
    public Optional<AgentResponseDto> get(Integer id) {
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public void put(Integer id, AgentResponseDto dto) {
        if (id != null && dto != null) {
            cache.put(id, dto);
        }
    }

    @Override
    public void evict(Integer id) {
        cache.remove(id);
    }

    // Additional helper method to clear entire cache (optional)
    public void clear() {
        cache.clear();
    }
}
