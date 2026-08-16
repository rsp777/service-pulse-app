package com.pawar.todo.amt.cache;

import java.util.Optional;

import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceHealthStatusResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;

public interface ServiceCache {
	Optional<ServiceResponseDto> get(Integer id);

	void put(Integer id, ServiceResponseDto dto);

	void evict(Integer id);
}
