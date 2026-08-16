package com.pawar.todo.amt.cache;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.PathRequestDto;
import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;


public interface PathCache {
	Optional<PathResponseDto> get(Integer id);

	void put(Integer id, PathResponseDto dto);

	void evict(Integer id);
	
	Optional<PathResponseDto> get(String pathName);

	void put(String pathName, PathResponseDto dto);

	void evict(String pathName);
	
	public Optional<PathRequestDto> gett(Integer id);
	public void put(Integer id, PathRequestDto dto);

	void evictt(String pathName);

	Optional<PathRequestDto> gett(String pathName);

	void putt(String pathName, PathRequestDto dto);

	void evictt(Integer id);
	
}
