package com.pawar.todo.amt.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;

import com.pawar.app.healthcheck.dto.CommandRequestDto;
import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.todo.amt.cache.CommandCache;
import com.pawar.todo.amt.constants.CommandStatus;
import com.pawar.todo.amt.exceptions.CommandOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.mapper.CommandMapper;
import com.pawar.todo.amt.model.Command;
import com.pawar.todo.amt.respository.CommandRepository;

@Service
public class CommandServiceImpl implements CommandService {

    private final static Logger logger = LoggerFactory.getLogger(CommandServiceImpl.class.getName());

    private final CommandRepository commandRepository;
    private final CommandMapper commandMapper;
    private final CommandCache commandCache;

    public CommandServiceImpl(CommandRepository commandRepository,
            CommandMapper commandMapper,
            CommandCache commandCache) {
        this.commandRepository = commandRepository;
        this.commandMapper = commandMapper;
        this.commandCache = commandCache;
    }

    @Override
    @Transactional
    public CommandResponseDto createCommand(CommandRequestDto request) throws CommandOperationException {
        LocalDateTime now = LocalDateTime.now();
        try {
            logger.info("Creating new Command: {}", request.name());

            Command command = commandMapper.toEntity(request);
            command.setCreatedDttm(now);
            command.setLastUpdatedDttm(now);

            Command savedcommand = commandRepository.save(command);
            logger.debug("Service created successfully: ID={}", savedcommand.getId());

            return commandMapper.toDto(savedcommand);
        } catch (Exception e) {
            logger.error("Failed to create command: ", e);
            throw new CommandOperationException("Failed to create command", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommandResponseDto> findCommandById(Integer id) throws CommandOperationException {
        try {
            logger.debug("Fetching command by ID: {}", id);
            return commandCache.get(id)
                    .or(() -> {
                        try {
                            Command command = commandRepository.findById(id)
                                    .orElseThrow(
                                            () -> new ResourceNotFoundException("Command not found with ID: " + id));
                            CommandResponseDto dto = commandMapper.toDto(command);
                            commandCache.put(id, dto);
                            return Optional.of(dto);
                        } catch (ResourceNotFoundException e) {
                            logger.error("Error fetching service with ID: {}", id, e);
                            return Optional.empty(); // Return empty if not found
                        }
                    });
        } catch (Exception e) {
            logger.error("Error fetching command with ID: {}", id, e);
            throw new CommandOperationException("Failed to fetch command", e);
        }
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<CommandResponseDto>> findAllCommandsAsync() {
        try {
            logger.info("Async fetching of all commands initiated");
            List<Command> commands = commandRepository.findAll();
            List<CommandResponseDto> response = commands.stream()
                    .peek(s -> logger.debug("Processing command: {}", s.getId()))
                    .map(commandMapper::toDto)
                    .collect(Collectors.toList());

            logger.info("Async command fetch completed successfully");
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            logger.error("Async command fetch failed", e);
            throw new CompletionException("Failed to fetch commands asynchronously", e);
        }
    }

    @Override
    @Transactional
    public CommandResponseDto updateCommand(Integer id, CommandRequestDto request) throws CommandOperationException {
        LocalDateTime now = LocalDateTime.now();
        try {
            logger.info("Updating command ID: {}", id);
            Command command = commandRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Command not found with ID: " + id));

            commandMapper.updateFromDto(request, command);
            command.setLastUpdatedDttm(now);

            Command updatedCommand = commandRepository.save(command);
            commandCache.evict(id);
            logger.debug("Command updated successfully: ID={}", id);

            return commandMapper.toDto(updatedCommand);
        } catch (Exception e) {
            logger.error("Failed to update Command ID: {}", id, e);
            throw new CommandOperationException("Failed to update command", e);
        }
    }

    @Override
    @Async
    @Transactional
    public ListenableFuture<Void> deleteCommandAsync(Integer id) throws CommandOperationException {
        try {
            logger.info("Async deletion initiated for command ID: {}", id);
            commandRepository.deleteById(id);
            commandCache.evict(id);
            logger.debug("Async deletion completed for command ID: {}", id);
            return new AsyncResult<>(null);
        } catch (Exception e) {
            logger.error("Async deletion failed for command ID: {}", id, e);
            throw new CommandOperationException("Failed to delete command asynchronously", e);
        }
    }

    @Override
    public List<CommandResponseDto> findCommandsByStatus(CommandStatus status) throws CommandOperationException {
        try {
            logger.debug("Fetching commands with status: {}", status.name());

            CommandStatus commandStatus = CommandStatus.valueOf(status.name().toUpperCase());

            List<CommandResponseDto> dtos = commandRepository.findByStatus(commandStatus)
                    .stream()
                    .map(commandMapper::toDto)
                    .collect(Collectors.toList());

            return dtos;
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid command status requested: {}", status);
            throw new IllegalArgumentException("Invalid command status: " + status);
        } catch (Exception e) {
            logger.error("Error fetching commands by status: {}", status, e);
            throw new CommandOperationException("Failed to fetch commands by status", e);
        }
    }

    @Override
    public Optional<CommandResponseDto> findCommandByDescription(String description) throws CommandOperationException {
        try {
            logger.debug("Fetching command by description: {}", description);
            return commandCache.get(description)
                    .or(() -> {
                        try {
                            Command command = commandRepository.findByDescription(description)
                                    .orElseThrow(() -> new ResourceNotFoundException(
                                            "Command not found with description: " + description));
                            CommandResponseDto dto = commandMapper.toDto(command);
                            commandCache.put(description, dto);
                            return Optional.of(dto);
                        } catch (ResourceNotFoundException e) {
                            logger.error("Error fetching service with ID: {}", description, e);
                            return Optional.empty(); // Return empty if not found
                        }
                    });
        } catch (Exception e) {
            logger.error("Error fetching command with description: {}", description, e);
            throw new CommandOperationException("Failed to fetch command", e);
        }
    }
}