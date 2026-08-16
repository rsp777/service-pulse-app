package com.pawar.todo.amt.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.ServerResponseDto;

@Component
public class InMemoryCacheServer implements ServerCache {

	private final Map<Integer, ServerResponseDto> cache = new ConcurrentHashMap();

	@Override
	public Optional<ServerResponseDto> get(Integer id) {
		return Optional.ofNullable(cache.get(id));
	}

	@Override
	public void put(Integer id, ServerResponseDto dto) {
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
