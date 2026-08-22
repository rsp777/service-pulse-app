package com.pawar.todo.amt.service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.exceptions.AgentOperationException;
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

	public LogsServiceImpl(SshCommandService sshCommandService,
			ServiceService serviceService,
			ServerService serverService,
			CommandService commandService) {
		this.sshCommandService = sshCommandService;
		this.serviceService = serviceService;
		this.serverService = serverService;
		this.commandService = commandService;
	}

	@Override
	public String viewLogsByService(Integer serverId, Integer serviceId)
			throws AgentOperationException, IOException, ServiceOperationException, CommandOperationException,
			PathOperationException {

		ServerResponseDto server;
		try {
			server = serverService.findServerById(serverId)
					.orElseThrow(() -> new AgentOperationException("Server does not exist"));
		} catch (com.pawar.todo.amt.exceptions.ServerOperationException exception) {
			throw new AgentOperationException("Failed to load server", exception);
		}

		ServiceResponseDto service = serviceService.findServiceById(serviceId)
				.orElseThrow(() -> new ServiceOperationException("Service not found"));

		CommandResponseDto commandResponseDto = commandService.findCommand("StreamLogs")
				.orElseThrow(() -> new CommandOperationException("Command does not exist"));

		log.info("Server: {}", server);

		if (!server.status().equals(ServerStatus.ONLINE.toString())) {
			log.info("Server {} is OFFLINE", server.hostname());
			return "Server is OFFLINE";
		}

		String command = buildCommandString(commandResponseDto, server.hostname(), service.serviceName());
		log.info("Command: {}", command);
		log.info("Starting Service: {}", service.serviceName());

		String response = sshCommandService.execute(server, command);
		if (response == null) {
			log.error("No response received from server for command: {}", command);
			return "Failed to fetch logs: No response from server.";
		}

		log.info("Logs for {} fetched successfully. Response: {}", service.serviceName(), response);
		return response;
	}

	@Override
	public void streamLogsByService(Integer serverId, Integer serviceId, Consumer<String> lineConsumer,
			AtomicBoolean stopped)
			throws AgentOperationException, IOException, ServiceOperationException, CommandOperationException,
			PathOperationException {
		ServerResponseDto server = loadServer(serverId);
		ServiceResponseDto service = serviceService.findServiceById(serviceId)
				.orElseThrow(() -> new ServiceOperationException("Service not found"));

		if (!ServerStatus.ONLINE.toString().equals(server.status())) {
			lineConsumer.accept("Server is OFFLINE");
			return;
		}

		CommandResponseDto command = commandService.findCommand("StreamLogs")
				.orElseThrow(() -> new CommandOperationException("Command does not exist"));
		sshCommandService.stream(server, buildStreamCommand(command, server.hostname(), service.serviceName()),
				lineConsumer, stopped);
	}

	private ServerResponseDto loadServer(Integer serverId) throws AgentOperationException {
		try {
			return serverService.findServerById(serverId)
					.orElseThrow(() -> new AgentOperationException("Server does not exist"));
		} catch (com.pawar.todo.amt.exceptions.ServerOperationException exception) {
			throw new AgentOperationException("Failed to load server", exception);
		}
	}

	private String buildCommandString(CommandResponseDto command, String serverName, String serviceName) {
		String parameters = command.parameters() != null ? command.parameters() : "";
		return String
				.format("%s %s ~/apps/%s/logs/%s/%s.log", command.name(), parameters, serverName, serviceName,
						serviceName)
				.trim();
	}

	private String buildStreamCommand(CommandResponseDto command, String serverName, String serviceName) {
		String parameters = command.parameters() != null ? command.parameters().trim() : "";
		if (parameters.isBlank() || !parameters.matches(".*(?:^|\\s)-[nN]\\s+\\d+.*")) {
			parameters = "-n 100 " + parameters;
		}
		return String.format("%s %s -F -- $HOME/apps/%s/logs/%s/%s.log", command.name(), parameters, serverName,
				serviceName, serviceName).trim();
	}
}
