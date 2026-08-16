package com.pawar.todo.amt.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.util.concurrent.ListenableFuture;

import com.pawar.app.healthcheck.dto.AgentRequestDto;
import com.pawar.app.healthcheck.dto.AgentResponseDto;
import com.pawar.app.healthcheck.dto.CommandRequestDto;
import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.constants.AgentStatus;
import com.pawar.todo.amt.constants.CommandStatus;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.exceptions.AgentOperationException;
import com.pawar.todo.amt.exceptions.CommandOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServerOperationException;
import com.pawar.todo.amt.model.Server;

public interface AgentService {
	 public AgentResponseDto createAgent(AgentRequestDto agentRequestDto )throws AgentOperationException;
	 public Optional<AgentResponseDto> findAgentById(Integer id) throws AgentOperationException;
	 public boolean checkAgentStatus(Integer id) throws AgentOperationException;
	 public CompletableFuture<List<AgentResponseDto>> findAllAgentsAsync();
	 public AgentResponseDto updateAgent(Integer id, AgentRequestDto agentRequestDto) throws AgentOperationException;
	 public ListenableFuture<Void> deleteAgentAsync(Integer id) throws AgentOperationException;
	 public List<AgentResponseDto> findAgentsByStatus(AgentStatus status) throws AgentOperationException;
	public Optional<AgentResponseDto> findAgentByServerId(Integer id) throws AgentOperationException;
}