package com.pawar.todo.amt.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.util.concurrent.ListenableFuture;

import com.pawar.app.healthcheck.dto.CommandRequestDto;
import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.constants.CommandStatus;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.exceptions.CommandOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServerOperationException;
import com.pawar.todo.amt.model.Server;

public interface CommandService {
	 public CommandResponseDto createCommand(CommandRequestDto commandRequestDto )throws CommandOperationException;
	 public Optional<CommandResponseDto> findCommandById(Integer id) throws CommandOperationException;
	 public CompletableFuture<List<CommandResponseDto>> findAllCommandsAsync();
	 public CommandResponseDto updateCommand(Integer id, CommandRequestDto commandRequestDto) throws CommandOperationException;
	 public ListenableFuture<Void> deleteCommandAsync(Integer id) throws CommandOperationException;
	 public List<CommandResponseDto> findCommandsByStatus(CommandStatus status) throws CommandOperationException;
	 public Optional<CommandResponseDto> findCommand(String nameOrDescription) throws CommandOperationException;
	 public Optional<CommandResponseDto> findCommandByDescription(String string) throws CommandOperationException;
}