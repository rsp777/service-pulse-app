package com.pawar.todo.amt.cache;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.HttpMethodsResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;


public interface HttpMethodsCache {
	Optional<HttpMethodsResponseDto> get(Integer id);

	void put(Integer id, HttpMethodsResponseDto dto);

	void evict(Integer id);
}
