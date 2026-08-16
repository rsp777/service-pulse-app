package com.pawar.todo.amt.cache;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.AgentResponseDto;
import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;


public interface AgentCache {
	Optional<AgentResponseDto> get(Integer id);

	void put(Integer id, AgentResponseDto dto);

	void evict(Integer id);
}
