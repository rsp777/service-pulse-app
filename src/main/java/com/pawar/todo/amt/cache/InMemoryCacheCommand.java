package com.pawar.todo.amt.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.CommandResponseDto;

@Component
public class InMemoryCacheCommand implements CommandCache {

    private final int MAX_CACHE_SIZE = 100; // Set your desired cache size
    private final Map<Integer, CommandResponseDto> cache = new LinkedHashMap<Integer, CommandResponseDto>(MAX_CACHE_SIZE, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Integer, CommandResponseDto> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private final Map<String, CommandResponseDto> cacheString = new LinkedHashMap<String, CommandResponseDto>(MAX_CACHE_SIZE, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, CommandResponseDto> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    @Override
    public Optional<CommandResponseDto> get(Integer id) {
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public void put(Integer id, CommandResponseDto dto) {
        if (id != null && dto != null) {
            cache.put(id, dto);
        }
    }

    @Override
    public void evict(Integer id) {
        cache.remove(id);
    }

    @Override
    public Optional<CommandResponseDto> get(String description) {
        return Optional.ofNullable(cacheString.get(description));
    }

    @Override
    public void put(String description, CommandResponseDto dto) {
        if (description != null && dto != null) {
            cacheString.put(description, dto);
        }
    }

    @Override
    public void evict(String description) {
        cacheString.remove(description);
    }

    public void clear() {
        cache.clear();
    }

    public void clearr() {
        cacheString.clear();
    }
}

