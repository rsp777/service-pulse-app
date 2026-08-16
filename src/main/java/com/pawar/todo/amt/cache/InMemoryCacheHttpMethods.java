package com.pawar.todo.amt.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.HttpMethodsResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.service.CommandService;

@Component
public class InMemoryCacheHttpMethods implements HttpMethodsCache {

	private final Map<Integer, HttpMethodsResponseDto> cache = new ConcurrentHashMap();

	@Override
	public Optional<HttpMethodsResponseDto> get(Integer id) {
		return Optional.ofNullable(cache.get(id));
	}

	@Override
	public void put(Integer id, HttpMethodsResponseDto dto) {
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
