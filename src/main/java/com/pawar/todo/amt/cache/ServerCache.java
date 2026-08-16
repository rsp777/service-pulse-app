package com.pawar.todo.amt.cache;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.ServerResponseDto;


public interface ServerCache {
	Optional<ServerResponseDto> get(Integer id);

	void put(Integer id, ServerResponseDto dto);

	void evict(Integer id);
}
