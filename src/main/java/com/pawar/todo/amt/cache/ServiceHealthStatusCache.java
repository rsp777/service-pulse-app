package com.pawar.todo.amt.cache;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.HealthCheckResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceHealthStatusResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;


public interface ServiceHealthStatusCache {
	Optional<ServiceHealthStatusResponseDto> get(Integer id);

	void put(Integer id, ServiceHealthStatusResponseDto dto);

	void evict(Integer id);
}
