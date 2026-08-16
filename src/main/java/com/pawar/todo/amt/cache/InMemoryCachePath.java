package com.pawar.todo.amt.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.PathRequestDto;
import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;

@Component
public class InMemoryCachePath implements PathCache {

	private final Map<Integer, PathResponseDto> cache = new ConcurrentHashMap();
	private final Map<String, PathResponseDto> cacheString = new ConcurrentHashMap();
	private final Map<Integer, PathRequestDto> cacheReq = new ConcurrentHashMap();
	private final Map<String, PathRequestDto> cacheReqString = new ConcurrentHashMap();


	@Override
	public Optional<PathResponseDto> get(Integer id) {
		return Optional.ofNullable(cache.get(id));
	}

	@Override
	public void put(Integer id, PathResponseDto dto) {
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
	public Optional<PathResponseDto> get(String pathName) {
		return Optional.ofNullable(cacheString.get(pathName));
	}

	@Override
	public void put(String pathName, PathResponseDto dto) {
		if (pathName != null && dto != null) {
			cacheString.put(pathName, dto);
		}
	}

	@Override
	public void evict(String pathName) {
		cacheString.remove(pathName);
	}

	// Additional helper method to clear entire cache (optional)
	public void clearr() {
		cacheString.clear();
	}
	
	////
	@Override
	public Optional<PathRequestDto> gett(Integer id) {
		return Optional.ofNullable(cacheReq.get(id));
	}

	@Override
	public void put(Integer id, PathRequestDto dto) {
		if (id != null && dto != null) {
			cacheReq.put(id, dto);
		}
	}

	@Override
	public void evictt(Integer id) {
		cacheReq.remove(id);
	}

	// Additional helper method to clear entire cache (optional)
	public void clearrrr() {
		cacheReq.clear();
	}
	
	@Override
	public Optional<PathRequestDto> gett(String pathName) {
		return Optional.ofNullable(cacheReqString.get(pathName));
	}

	@Override
	public void putt(String pathName, PathRequestDto dto) {
		if (pathName != null && dto != null) {
			cacheReqString.put(pathName, dto);
		}
	}

	@Override
	public void evictt(String pathName) {
		cacheReqString.remove(pathName);
	}

	// Additional helper method to clear entire cache (optional)
	public void clearrr() {
		cacheReqString.clear();
	}

}
