package com.pawar.todo.amt.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.util.concurrent.ListenableFuture;

import com.pawar.app.healthcheck.dto.CommandRequestDto;
import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.HealthCheckRequestDto;
import com.pawar.app.healthcheck.dto.HealthCheckResponseDto;
import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.constants.CommandStatus;
import com.pawar.todo.amt.constants.HealthCheckStatus;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.exceptions.CommandOperationException;
import com.pawar.todo.amt.exceptions.HealthCheckNotFoundException;
import com.pawar.todo.amt.exceptions.HealthCheckOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServerOperationException;
import com.pawar.todo.amt.model.Server;

public interface HealthCheckService {
	 public HealthCheckResponseDto createHealthCheck(HealthCheckRequestDto healthCheckRequestDto)throws NoSuchElementException, HealthCheckOperationException;
	 public Optional<HealthCheckResponseDto> findHealthCheckById(Integer id) throws HealthCheckOperationException,HealthCheckNotFoundException;
	 public CompletableFuture<List<HealthCheckResponseDto>> findAllHealthChecksAsync() throws HealthCheckOperationException;
	 public HealthCheckResponseDto updateHealthCheck(Integer id, HealthCheckRequestDto healthCheckRequestDto) throws HealthCheckOperationException;
	 public ListenableFuture<Void> deleteHealthCheckAsync(Integer id) throws HealthCheckOperationException;
//	 public List<HealthCheckResponseDto> findHealthChecksByStatus(HealthCheckStatus status) throws HealthCheckOperationException;
}