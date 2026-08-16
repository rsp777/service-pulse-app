package com.pawar.todo.amt.cache;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.HealthCheckResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;


public interface HealthCheckCache {
	Optional<HealthCheckResponseDto> get(Integer id);

	void put(Integer id, HealthCheckResponseDto dto);

	void evict(Integer id);
}
