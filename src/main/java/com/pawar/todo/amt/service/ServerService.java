package com.pawar.todo.amt.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.util.concurrent.ListenableFuture;

import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServerOperationException;
import com.pawar.todo.amt.model.Server;

public interface ServerService {

	 public ServerResponseDto createServer(ServerRequestDto serverRequestDto)throws ServerOperationException;
	 public Optional<ServerResponseDto> findServerById(Integer id) throws ServerOperationException;
	 public CompletableFuture<List<ServerResponseDto>> findAllServersAsync();
	 public ServerResponseDto updateServer(Integer id, ServerRequestDto serverRequestDto) throws ServerOperationException;
	 public ListenableFuture<Void> deleteServerAsync(Integer id) throws ServerOperationException;
	 public List<ServerResponseDto> findServersByStatus(ServerStatus status) throws ServerOperationException;

	
	
}
