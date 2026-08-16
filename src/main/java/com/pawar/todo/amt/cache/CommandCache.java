package com.pawar.todo.amt.cache;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;


public interface CommandCache {
	Optional<CommandResponseDto> get(Integer id);

	void put(Integer id, CommandResponseDto dto);

	void evict(Integer id);
	
	Optional<CommandResponseDto> get(String description);

	void put(String description, CommandResponseDto dto);

	void evict(String description);
}
