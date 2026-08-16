package com.pawar.todo.amt.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.ScriptResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;

@Component
public class InMemoryCacheScript implements ScriptCache {

	private final Map<Integer, ScriptResponseDto> cache = new ConcurrentHashMap();
	private final Map<String, ScriptResponseDto> cacheString = new ConcurrentHashMap();

	
	@Override
	public Optional<ScriptResponseDto> get(Integer id) {
		return Optional.ofNullable(cache.get(id));
	}

	@Override
	public void put(Integer id, ScriptResponseDto dto) {
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

	@Override
	public Optional<ScriptResponseDto> get(String name) {
		return Optional.ofNullable(cacheString.get(name));
	}

	@Override
	public void put(String name, ScriptResponseDto dto) {
		if (name != null && dto != null) {
			cacheString.put(name, dto);
		}
	}

	@Override
	public void evict(String name) {
		cacheString.remove(name);
		
	}

}
