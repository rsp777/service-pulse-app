package com.pawar.todo.amt.controller;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pawar.app.healthcheck.dto.CommandRequestDto;
import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.todo.amt.exceptions.CommandOperationException;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.CommandService;
import com.pawar.todo.amt.service.ServerService;
import com.pawar.todo.amt.ssh.SshCommandService;

@RestController
@RequestMapping("/api/commands")
public class CommandController {

	private static final Logger logger = LoggerFactory.getLogger(CommandController.class);
	private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

	private final CommandService commandService;
	private final ServerService serverService;
	private final SshCommandService sshCommandService;

	public CommandController(CommandService commandService, ServerService serverService, SshCommandService sshCommandService) {
		this.commandService = commandService;
		this.serverService = serverService;
		this.sshCommandService = sshCommandService;
	}

	@PostMapping("/execute")
	public ResponseEntity<ApiResponse<String>> execute(@RequestParam Integer serverId, @RequestParam String command) {
		try {
			String output = sshCommandService.execute(serverService.findServerById(serverId)
					.orElseThrow(() -> new IllegalArgumentException("Server not found: " + serverId)), command);
			return ResponseEntity.ok(new ApiResponse<>(true, "Command completed", output));
		} catch (Exception exception) {
			logger.error("Command execution failed for server {}", serverId, exception);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponse<>(false, exception.getMessage(), null));
		}
	}

	@PostMapping
	public ResponseEntity<CommandResponseDto> createCommand(@RequestBody CommandRequestDto request) {
		logger.info("Received request to create new command: {}", request.name());
		logger.debug("Command creation request details: {}", request);

		try {
			CommandResponseDto createdCommand = commandService.createCommand(request);
			auditLogger.info("Command created successfully - ID: {}, Name: {}", createdCommand.id(),
					createdCommand.name());
			logger.debug("Created command details: {}", createdCommand);

			return ResponseEntity.status(HttpStatus.CREATED).body(createdCommand);
		} catch (CommandOperationException e) {
			logger.error("Failed to create command: {} - Error: {}", request.name(), e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping("/{id}")
	public ResponseEntity<CommandResponseDto> getCommandById(@PathVariable Integer id) {
		logger.info("Fetching command by ID: {}", id);

		try {
			Optional<CommandResponseDto> command = commandService.findCommandById(id);

			if (command.isPresent()) {
				logger.debug("Retrieved command details for ID {}: {}", id, command.get());
				return ResponseEntity.ok(command.get());
			} else {
				logger.warn("Command not found with ID: {}", id);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}
		} catch (CommandOperationException e) {
			logger.error("Error fetching service ID {}: {}", id, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping
	public ResponseEntity<List<CommandResponseDto>> getAllCommands() {
		logger.info("Fetching all commands");

		try {
			CompletableFuture<List<CommandResponseDto>> commandsFuture = commandService.findAllCommandsAsync();
			List<CommandResponseDto> commands = commandsFuture.join();

			logger.debug("Retrieved {} commands", commands.size());
			return ResponseEntity.ok(commands);
		} catch (Exception e) {
			logger.error("Error fetching all commands: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity<CommandResponseDto> updateCommand(@PathVariable Integer id,
			@RequestBody CommandRequestDto request) {
		logger.info("Updating command ID: {}", id);
		logger.debug("Update details for command ID {}: {}", id, request);

		try {
			CommandResponseDto updatedCommand = commandService.updateCommand(id, request);
			auditLogger.info("Command updated - ID: {}, Name: {}", id, updatedCommand.name());
			logger.debug("Updated command details: {}", updatedCommand);

			return ResponseEntity.ok(updatedCommand);
		} catch (CommandOperationException e) {
			logger.error("Failed to update command ID {}: {}", id, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteCommand(@PathVariable Integer id) {
		logger.info("Deleting command ID: {}", id);

		try {
			commandService.deleteCommandAsync(id);
			auditLogger.warn("Command deleted - ID: {}", id);
			return ResponseEntity.noContent().build();
		} catch (CommandOperationException e) {
			logger.error("Failed to delete command ID {}: {}", id, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
