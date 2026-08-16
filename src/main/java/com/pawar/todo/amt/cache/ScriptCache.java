package com.pawar.todo.amt.cache;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.app.healthcheck.dto.ScriptResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;


public interface ScriptCache {
	Optional<ScriptResponseDto> get(Integer id);

	void put(Integer id, ScriptResponseDto dto);

	void evict(Integer id);
	
	Optional<ScriptResponseDto> get(String name);

	void put(String name, ScriptResponseDto dto);

	void evict(String name);
}
