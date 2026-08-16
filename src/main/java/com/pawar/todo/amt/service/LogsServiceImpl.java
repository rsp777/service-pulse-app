package com.pawar.todo.amt.service;

import java.io.IOException;
import org.springframework.stereotype.Service;
import com.pawar.app.healthcheck.dto.AgentResponseDto;
import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.constants.AgentStatus;
import com.pawar.todo.amt.exceptions.AgentOperationException;
import com.pawar.todo.amt.exceptions.CommandOperationException;
import com.pawar.todo.amt.exceptions.PathOperationException;
import com.pawar.todo.amt.exceptions.ServiceOperationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LogsServiceImpl implements LogsService {

	private final WebSocketAgentService webSocketAgentService;
	private final ServiceService serviceService;
	private final AgentService agentService;
	private final CommandService commandService;
	private final PathService pathService;

	public LogsServiceImpl(WebSocketAgentService webSocketAgentService,
			ServiceService serviceService,
			AgentService agentService,
			CommandService commandService,
			PathService pathService) {
		this.webSocketAgentService = webSocketAgentService;
		this.serviceService = serviceService;
		this.agentService = agentService;
		this.commandService = commandService;
		this.pathService = pathService;
	}

	@Override
	public String viewLogsByService(Integer agentId, Integer serviceId)
			throws AgentOperationException, IOException, ServiceOperationException, CommandOperationException,
			PathOperationException {

		AgentResponseDto agent = agentService.findAgentById(agentId)
				.orElseThrow(() -> new AgentOperationException("Agent does not exist"));

		ServiceResponseDto service = serviceService.findServiceById(serviceId)
				.orElseThrow(() -> new ServiceOperationException("Service not found"));

		CommandResponseDto commandResponseDto = commandService.findCommandByDescription("StreamLogs")
				.orElseThrow(() -> new CommandOperationException("Command does not exist"));

		log.info("Agent: {}", agent);

		if (!agent.status().equals(AgentStatus.ONLINE.toString())) {
			log.info("Agent {} is OFFLINE", agent.name());
			return "Agent is OFFLINE";
		}

		PathResponseDto path = pathService.findByPathName("LOGS_HOME")
				.orElseThrow(() -> new PathOperationException("Path not found"));

		String command = buildCommandString(commandResponseDto, path, service.serviceName());
		log.info("Command: {}", command);
		log.info("Starting Service: {}", service.serviceName());

		String response = webSocketAgentService.sendCommand(agentId, command);
		if (response == null) {
			log.error("No response received from agent for command: {}", command);
			return "Failed to start service: No response from agent.";
		}

		log.info("Logs for {} fetched successfully. Response: {}", service.serviceName(), response);
		return response;
	}

	private String buildCommandString(CommandResponseDto command, PathResponseDto path, String serviceName) {
		String parameters = command.parameters() != null ? command.parameters() : "";
		return String
				.format("%s %s %s/%s/%s.log", command.name(), parameters, path.pathName(), serviceName, serviceName)
				.trim();
	}
}
