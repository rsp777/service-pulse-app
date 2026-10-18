package com.pawar.todo.amt.service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.exceptions.ServerOperationException;
import com.pawar.todo.amt.exceptions.CommandOperationException;
import com.pawar.todo.amt.exceptions.PathOperationException;
import com.pawar.todo.amt.exceptions.ServiceOperationException;
import com.pawar.todo.amt.ssh.SshCommandService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LogsServiceImpl implements LogsService {

	private final SshCommandService sshCommandService;
	private final ServiceService serviceService;
	private final ServerService serverService;
	private final CommandService commandService;
	private final PathService pathService;

	public LogsServiceImpl(SshCommandService sshCommandService,
			ServiceService serviceService,
			ServerService serverService,
			CommandService commandService,
			PathService pathService) {
		this.sshCommandService = sshCommandService;
		this.serviceService = serviceService;
		this.serverService = serverService;
		this.commandService = commandService;
		this.pathService = pathService;
	}

	@Override
	public String viewLogsByService(Integer serverId, Integer serviceId)
			throws ServerOperationException, IOException, ServiceOperationException, CommandOperationException,
			PathOperationException {

		ServerResponseDto server = loadServer(serverId);
		ServiceResponseDto service = serviceService.findServiceById(serviceId)
				.orElseThrow(() -> new ServiceOperationException("Service not found"));

		log.info("Server: {}", server);

		if (!server.status().equals(ServerStatus.ONLINE.toString())) {
			log.info("Server {} is OFFLINE", server.hostname());
			return "Server is OFFLINE";
		}

		CommandResponseDto command = resolveStreamCommand();
		String commandStr = buildCommandString(command, server, service.serviceName());
		log.info("Command: {}", commandStr);

		String response = sshCommandService.execute(server, commandStr);
		if (response == null) {
			log.error("No response received from server for command: {}", commandStr);
			return "Failed to fetch logs: No response from server.";
		}

		log.info("Logs for {} fetched successfully.", service.serviceName());
		return response;
	}

	@Override
	public void streamLogsByService(Integer serverId, Integer serviceId, Consumer<String> lineConsumer,
			AtomicBoolean stopped)
			throws ServerOperationException, IOException, ServiceOperationException, CommandOperationException,
			PathOperationException {
		ServerResponseDto server = loadServer(serverId);
		ServiceResponseDto service = serviceService.findServiceById(serviceId)
				.orElseThrow(() -> new ServiceOperationException("Service not found"));

		if (!ServerStatus.ONLINE.toString().equals(server.status())) {
			lineConsumer.accept("Server is OFFLINE");
			return;
		}

		CommandResponseDto command = resolveStreamCommand();
		String streamCommand = buildStreamCommand(command, server, service.serviceName());
		log.info("Executing stream logs command: {}", streamCommand);
		sshCommandService.stream(server, streamCommand, lineConsumer, stopped);
	}

	private ServerResponseDto loadServer(Integer serverId) throws ServerOperationException {
		try {
			return serverService.findServerById(serverId)
					.orElseThrow(() -> new ServerOperationException("Server does not exist"));
		} catch (com.pawar.todo.amt.exceptions.ServerOperationException exception) {
			throw new ServerOperationException("Failed to load server", exception);
		}
	}

	private CommandResponseDto resolveStreamCommand() {
		try {
			return commandService.findCommand("StreamLogs")
					.orElseGet(this::defaultStreamCommand);
		} catch (Exception e) {
			log.debug("StreamLogs command lookup failed, using default: {}", e.getMessage());
			return defaultStreamCommand();
		}
	}

	private CommandResponseDto defaultStreamCommand() {
		return new CommandResponseDto(null, "tail", "-n 100", "Stream service logs via tail", "READY", null, null, null, "SYSTEM", "SYSTEM");
	}

	private String resolveLogFilePath(ServerResponseDto server, String serviceName) {
		String logsHome = null;

		// 1. Check server-associated paths for LOGS_HOME
		if (server.paths() != null) {
			logsHome = server.paths().stream()
					.filter(p -> "LOGS_HOME".equalsIgnoreCase(p.pathName()))
					.map(p -> p.pathDescription())
					.filter(desc -> desc != null && !desc.isBlank())
					.findFirst()
					.orElse(null);
		}

		// 2. Check global PathService for LOGS_HOME
		if (logsHome == null && pathService != null) {
			try {
				logsHome = pathService.findByPathName("LOGS_HOME")
						.map(p -> p.pathDescription())
						.filter(desc -> desc != null && !desc.isBlank())
						.orElse(null);
			} catch (Exception e) {
				log.debug("PathService lookup for LOGS_HOME failed: {}", e.getMessage());
			}
		}

		// 3. If a configured path is found, use it
		if (logsHome != null && !logsHome.isBlank()) {
			String cleanHome = logsHome.trim().replaceAll("/+$", "");
			if (cleanHome.endsWith(".log")) {
				return cleanHome;
			}
			return String.format("%s/%s/%s.log", cleanHome, serviceName, serviceName);
		}

		// 4. Default fallback convention
		return String.format("$HOME/apps/%s/logs/%s/%s.log", server.hostname(), serviceName, serviceName);
	}

	private String buildCommandString(CommandResponseDto command, ServerResponseDto server, String serviceName) {
		String cmdName = (command != null && command.name() != null) ? command.name() : "tail";
		String parameters = (command != null && command.parameters() != null) ? command.parameters().trim() : "-n 200";
		String logPath = resolveLogFilePath(server, serviceName);
		return String.format("%s %s %s", cmdName, parameters, logPath).trim();
	}

	private String buildStreamCommand(CommandResponseDto command, ServerResponseDto server, String serviceName) {
		String cmdName = (command != null && command.name() != null) ? command.name() : "tail";
		String parameters = (command != null && command.parameters() != null) ? command.parameters().trim() : "";
		if (parameters.isBlank() || !parameters.matches(".*(?:^|\\s)-[nN]\\s+\\d+.*")) {
			parameters = "-n 100 " + parameters;
		}
		String logPath = resolveLogFilePath(server, serviceName);
		return String.format("%s %s -F -- %s", cmdName, parameters.trim(), logPath).trim();
	}
}
