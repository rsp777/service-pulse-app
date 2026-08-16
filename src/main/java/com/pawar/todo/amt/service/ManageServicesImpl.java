package com.pawar.todo.amt.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawar.app.healthcheck.dto.AgentResponseDto;
import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.app.healthcheck.dto.ScriptResponseDto;
import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceHealthStatusRequestDto;
import com.pawar.app.healthcheck.dto.ServiceHealthStatusResponseDto;
import com.pawar.app.healthcheck.dto.ServiceRequestDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.sop.http.service.HttpService;
import com.pawar.todo.amt.constants.AgentStatus;
import com.pawar.todo.amt.constants.CommandResult;
import com.pawar.todo.amt.constants.HealthCheckStatus;
import com.pawar.todo.amt.constants.ScriptExtension;
import com.pawar.todo.amt.controller.ManageServicesController;
import com.pawar.todo.amt.converter.ScriptExtensionConverter;
import com.pawar.todo.amt.exceptions.AgentOperationException;
import com.pawar.todo.amt.exceptions.PathOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServiceHealthStatusOperationException;
import com.pawar.todo.amt.exceptions.ServiceOperationException;
import com.pawar.todo.amt.mapper.ServerMapper;
import com.pawar.todo.amt.mapper.ServiceHealthStatusMapper;
import com.pawar.todo.amt.mapper.ServiceMapper;
import com.pawar.todo.amt.model.ServiceHealthStatus;
import com.pawar.todo.amt.respository.ServiceRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ManageServicesImpl implements ManageServices {

	private final WebSocketAgentService webSocketAgentService;
	private ScriptService scriptService;
	private ServiceService serviceService;
	private ServiceHealthStatusService serviceHealthStatusService;
	private AgentService agentService;
	private final ScriptExtensionConverter scriptExtensionConverter;
	private ServiceHealthStatusMapper serviceHealthStatusMapper;
	private ServiceMapper serviceMapper;
	private final ManageServicesController manageServicesController;
	private final HttpService httpService;

	private PathService pathService;

	@Autowired
	private ServiceRepository serviceRepository;

	public ManageServicesImpl(WebSocketAgentService webSocketAgentService,
			ScriptExtensionConverter scriptExtensionConverter, ManageServicesController manageServicesController,
			HttpService httpService) {
		this.webSocketAgentService = webSocketAgentService;
		this.scriptExtensionConverter = scriptExtensionConverter;
		this.manageServicesController = manageServicesController;
		this.httpService = httpService;
	}

	@Autowired
	public void setScriptService(ScriptService scriptService) {
		this.scriptService = scriptService;
	}

	@Autowired
	public void setPathService(PathService pathService) {
		this.pathService = pathService;
	}

	@Autowired
	public void setServiceService(ServiceService serviceService) {
		this.serviceService = serviceService;
	}

	@Autowired
	public void setServiceHealthStatusService(ServiceHealthStatusService serviceHealthStatusService) {
		this.serviceHealthStatusService = serviceHealthStatusService;
	}

	@Autowired
	public void setAgentService(AgentService agentService) {
		this.agentService = agentService;
	}

	@Autowired
	public void setServiceHealthStatusMapper(ServiceHealthStatusMapper serviceHealthStatusMapper) {
		this.serviceHealthStatusMapper = serviceHealthStatusMapper;

	}

	@Autowired
	public void setServiceMapper(ServiceMapper serviceMapper) {
		this.serviceMapper = serviceMapper;

	}

	// @Autowired
	// public void setServerMapper(ServerMapper serverMapper) {
	// 	this.serverMapper = serverMapper;

	// }

	@Override
	public String startService(Integer agentId, Integer serviceId) throws AgentOperationException,
			ServiceOperationException, IOException, ServiceHealthStatusOperationException, PathOperationException,
			ResourceNotFoundException {
		AgentResponseDto agent = agentService.findAgentById(agentId)
				.orElseThrow(() -> new AgentOperationException("Agent does not exist"));
		ServiceResponseDto service = serviceService.findServiceById(serviceId)
				.orElseThrow(() -> new ServiceOperationException("Service not found"));
		ServiceHealthStatusResponseDto serviceHealthStatus = serviceHealthStatusService
				.findServiceHealthStatusByServiceId(serviceId)
				.orElseThrow(() -> new ServiceHealthStatusOperationException("Service health status not found"));

		log.info("Agent: {}", agent);

		if (!agent.status().equals(AgentStatus.ONLINE.toString())) {
			log.info("Agent {} is OFFLINE", agent.name());
			return "Agent is OFFLINE";
		}
		log.info("Service : {}", service);
		String command = startStopCommand(service, "SCRIPTS_HOME", "start");// buildStartCommand(agentId, service);
		log.info("Command: {}", command);
		String response = webSocketAgentService.sendCommand(agentId, command);
		String responseMessage = manageServicesController.waitForResponse().getPayload();
		log.info("responseMessage : {}", responseMessage);

		CommandResult commandResult = mapCommandResult(responseMessage);

		if (commandResult == null && response == null) {
			log.error("No response received from agent for command: {}", command);
			return "Failed to start service: No response from agent.";
		} else if (commandResult != null && responseMessage.contains("Service started")) {
			updateServiceHealthStatus(serviceHealthStatus, "UP", "startService");
			log.info("Service {} started successfully for Agent ID {}. Response: {}", service.serviceName(), agentId,
					response);
			return response;
		} else if (commandResult != null
				&& responseMessage.contains("WebSocket session is closed. Cannot send message.")) {
			log.info("Failed to start the service : {}", service.serviceName(), response);
			return response;
		}

		log.info("Service {} started successfully for Agent ID {}. Response: {}", service.serviceName(), agentId,
				response);
		return response;
	}

	@Override
	public String stopService(Integer agentId, Integer serviceId) throws AgentOperationException,
			ServiceOperationException, IOException, ResourceNotFoundException, ServiceHealthStatusOperationException,
			PathOperationException {

		AgentResponseDto agent = agentService.findAgentById(agentId)
				.orElseThrow(() -> new AgentOperationException("Agent does not exist"));
		ServiceResponseDto service = serviceService.findServiceById(serviceId)
				.orElseThrow(() -> new ServiceOperationException("Service not found"));

		ServiceHealthStatusResponseDto serviceHealthStatus = serviceHealthStatusService
				.findServiceHealthStatusByServiceId(serviceId)
				.orElseThrow(() -> new ServiceHealthStatusOperationException("Service health status not found"));

		if (!agent.status().equals(AgentStatus.ONLINE.toString())) {
			log.info("Agent {} is OFFLINE", agent.name());
			return "Agent is OFFLINE";
		}

		String command = startStopCommand(service, "SCRIPTS_HOME", "stop");// buildStopCommand(service, script);
		log.info("Command: {}", command);
		String response = webSocketAgentService.sendCommand(agentId, command);
		String responseMessage = manageServicesController.waitForResponse().getPayload();
		log.info("responseMessage : {}", responseMessage);

		CommandResult commandResult = mapCommandResult(responseMessage);

		if (commandResult == null && response == null) {
			log.error("No response received from agent for command: {}", command);
			return "Failed to stop service: No response from agent.";
		} else if (commandResult != null && commandResult.getOutput().contains("Service stopped")) {
			updateServiceHealthStatus(serviceHealthStatus, "DOWN", "stopService");
			log.info("Service {} stopped successfully for Agent ID {}. Response: {}", service.serviceName(), agentId,
					response);
			return response;
		} else if (commandResult != null
				&& commandResult.getOutput().contains("WebSocket session is closed. Cannot send message.")) {
			log.info("Failed to stop the service : {}", service.serviceName(), response);
			return response;
		}
		return response;
	}

	private CommandResult mapCommandResult(String responseMessage)
			throws JsonMappingException, JsonProcessingException {

		if (responseMessage.contains("requestId") && responseMessage.contains("status")
				&& responseMessage.contains("output") && responseMessage.contains("error")) {

			ObjectMapper objectMapper = new ObjectMapper();
			CommandResult commandResult = objectMapper.readValue(responseMessage, CommandResult.class);
			return commandResult;
		}
		return null;

	}

	private String startStopCommand(ServiceResponseDto service, String scriptsHome, String scriptName)
			throws PathOperationException, ResourceNotFoundException {
		String pathAndScript = getPathAndScript(scriptsHome, scriptName);
		if (service == null) {

			return String.format("%s", pathAndScript);
		} else {
			return String.format("%s %s", pathAndScript, service.serviceName());
		}
	}

	private String getPathAndScript(String scriptsHome, String scriptName)
			throws PathOperationException, ResourceNotFoundException {
		String path = pathService.findByPathName(scriptsHome).get().pathName();
		Optional<ScriptResponseDto> scriptOptional = scriptService.findScriptByScriptName(scriptName);
		String scriptName1 = scriptOptional.get().scriptName();
		ScriptExtension scriptExtension = scriptExtensionConverter.toEnum(scriptOptional.get().scriptExtension());
		return String.format("$%s/%s%s", path, scriptName1, scriptExtension.getExtension());
	}

	// private String startStopCommand(Integer agentId, ServiceResponseDto service)
	// {
	// String path = pathService.findByPathName("SCRIPTS_HOME").get().pathName();
	// String scriptName = extractScriptName(service.servers());
	// return String.format("$%s/%s %s", path, scriptName, service.serviceName());
	// }

	// private String buildStartAllCommand() throws PathOperationException,
	// ResourceNotFoundException {
	// String path = pathService.findByPathName("SCRIPTS_HOME").get().pathName();
	// Optional<ScriptResponseDto> scriptOptional =
	// scriptService.findScriptByScriptName("startAll");
	// String scriptName = scriptOptional.get().scriptName();
	// ScriptExtension scriptExtension =
	// scriptExtensionConverter.toEnum(scriptOptional.get().scriptExtension());
	// return String.format("$%s/%s%s", path, scriptName,
	// scriptExtension.getExtension());
	// }
	//
	// private String buildStopAllCommand() throws PathOperationException,
	// ResourceNotFoundException {
	// String path = pathService.findByPathName("SCRIPTS_HOME").get().pathName();
	// Optional<ScriptResponseDto> scriptOptional =
	// scriptService.findScriptByScriptName("stopAll");
	// String scriptName = scriptOptional.get().scriptName();
	// ScriptExtension scriptExtension =
	// scriptExtensionConverter.toEnum(scriptOptional.get().scriptExtension());
	// return String.format("$%s/%s%s", path, scriptName,
	// scriptExtension.getExtension());
	// }

	// private String buildStartCommand(ServiceResponseDto service) throws
	// IOException, AgentOperationException {
	// String path = extractScriptPath(service.servers());
	// String scriptName = extractScriptName(service.servers());
	// return String.format("$%s/%s %s", path, scriptName, service.serviceName());
	// }
	//
	// private String buildStopCommand(ServiceResponseDto service, ScriptResponseDto
	// script)
	// throws IOException, AgentOperationException {
	// String path = extractScriptPath(service.servers());
	// ScriptExtension scriptExtension =
	// scriptExtensionConverter.toEnum(script.scriptExtension());
	// return String.format("$%s/%s%s %s", path, script.scriptName(),
	// scriptExtension.getExtension(),
	// service.serviceName());
	// }

	// private String extractPathOrScript(Set<ServerResponseDto> serverResponsedtos,
	// String targetName, boolean isPath)
	// throws IOException, AgentOperationException {
	// for (ServerResponseDto serverResponseDto : serverResponsedtos) {
	// Set<PathResponseDto> pathResponseDtos = serverResponseDto.paths();
	// for (PathResponseDto pathResponseDto : pathResponseDtos) {
	// if (isPath) {
	// // Check for path
	// if (pathResponseDto.pathName().equals(targetName)) {
	// return "$" + pathResponseDto.pathName(); // Return formatted path
	// }
	// } else {
	// // Check for script
	// Set<ScriptResponseDto> scriptResponseDtos = pathResponseDto.scripts();
	// for (ScriptResponseDto scriptResponseDto : scriptResponseDtos) {
	// if (scriptResponseDto.scriptName().equals(targetName)
	// && scriptResponseDto.scriptExtension().equals("SH")) {
	// return scriptResponseDto.scriptName() + scriptResponseDto.scriptExtension();
	// // Return
	// // script
	// // name with
	// // extension
	// }
	// }
	// }
	// }
	// }
	// return null; // Return null if not found
	// }
	//
	// private String extractScriptPath(Set<ServerResponseDto> serverResponsedtos)
	// throws IOException, AgentOperationException {
	// return extractPathOrScript(serverResponsedtos, "SCRIPTS_HOME", true);
	// }
	//
	// private String extractScriptName(Set<ServerResponseDto> serverResponsedtos)
	// throws IOException, AgentOperationException {
	// return extractPathOrScript(serverResponsedtos, "start", false);
	// }

	private void updateServiceHealthStatus(ServiceHealthStatusResponseDto serviceHealthStatus, String status,
			String lastUpdateSource) throws ServiceHealthStatusOperationException {
		ServiceHealthStatus serviceHealthStatusEntity = serviceHealthStatusMapper.toEntity(serviceHealthStatus);
		ServiceRequestDto serviceRequestDto = serviceMapper.reqToDto(serviceHealthStatusEntity.getService());

		ServiceHealthStatusRequestDto serviceHealthStatusRequestDto = new ServiceHealthStatusRequestDto(
				serviceHealthStatus.id(), serviceRequestDto, status, LocalDateTime.now(), null, null,
				serviceHealthStatus.createdDttm(), LocalDateTime.now(), serviceHealthStatus.createdSource(),
				lastUpdateSource);

		serviceHealthStatusService.updateServiceHealthStatus(serviceHealthStatus.id(), serviceHealthStatusRequestDto);
	}

	@Override
	public String startAllServices(Integer agentId) throws AgentOperationException, IOException, PathOperationException,
			ResourceNotFoundException, ServiceHealthStatusOperationException, InterruptedException, ExecutionException {
		AgentResponseDto agent = agentService.findAgentById(agentId)
				.orElseThrow(() -> new AgentOperationException("Agent does not exist"));

		log.info("Agent: {}", agent);

		if (!agent.status().equals(AgentStatus.ONLINE.toString())) {
			log.info("Agent {} is OFFLINE", agent.name());
			return "Agent is OFFLINE";
		}
		String command = startStopCommand(null, "SCRIPTS_HOME", "startAll");// buildStartAllCommand();
		log.info("Command: {}", command);
		String response = webSocketAgentService.sendCommand(agentId, command);
		String responseMessage = manageServicesController.waitForResponse().getPayload();
		log.info("responseMessage : {}", responseMessage);

		CommandResult commandResult = mapCommandResult(responseMessage);

		if (commandResult == null && response == null) {
			log.error("No response received from agent for command: {}", command);
			return "Failed to start service: No response from agent.";
		} else if (commandResult != null && responseMessage.contains("Service started")) {
			updateAllServiceHealthStatus("UP", "startAllServices");
			log.info("All Services started successfully , Response: {}", response);
			return response;
		} else if (commandResult != null
				&& responseMessage.contains("WebSocket session is closed. Cannot send message.")) {
			log.info("Failed to start the services : {}", response);
			return response;
		}

		return response;
	}

	@Override
	public String stopAllServices(Integer agentId) throws AgentOperationException, IOException, PathOperationException,
			ResourceNotFoundException, ServiceHealthStatusOperationException, InterruptedException, ExecutionException {
		AgentResponseDto agent = agentService.findAgentById(agentId)
				.orElseThrow(() -> new AgentOperationException("Agent does not exist"));

		log.info("Agent: {}", agent);

		if (!agent.status().equals(AgentStatus.ONLINE.toString())) {
			log.info("Agent {} is OFFLINE", agent.name());
			return "Agent is OFFLINE";
		}
		String command = startStopCommand(null, "SCRIPTS_HOME", "stopAll");
		;
		log.info("Command: {}", command);
		String response = webSocketAgentService.sendCommand(agentId, command);
		String responseMessage = manageServicesController.waitForResponse().getPayload();
		log.info("responseMessage : {}", responseMessage);

		CommandResult commandResult = mapCommandResult(responseMessage);

		if (commandResult == null && response == null) {
			log.error("No response received from agent for command: {}", command);
			return "Failed to start service: No response from agent.";
		} else if (commandResult != null && responseMessage.contains("Service started")
				&& commandResult.getStatus().equals("COMPLETED")) {
			updateAllServiceHealthStatus("DOWN", "stopAllServices");
			log.info("All Services stopped successfully , Response: {}", response);
			return response;
		} else if (commandResult != null
				&& responseMessage.contains("WebSocket session is closed. Cannot send message.")) {
			log.info("Failed to stopped the services : {}", response);
			return response;
		}

		return response;
	}

	// Method to update health status for all services
	private void updateAllServiceHealthStatus(String status, String lastUpdatedSource)
			throws ServiceHealthStatusOperationException, InterruptedException, ExecutionException {
		List<ServiceHealthStatusResponseDto> serviceHealthStatusResponseDtos = serviceHealthStatusService
				.findAllServiceHealthStatussAsync().get();
		for (ServiceHealthStatusResponseDto serviceHealthStatusResponseDto : serviceHealthStatusResponseDtos) {
			updateServiceHealthStatus(serviceHealthStatusResponseDto, status, lastUpdatedSource);
		}
		log.info("Updated health status to {} for all services", status);
	}

	@Override
	@Async
	@Scheduled(cron = "*/60 * * * * *")
	@Transactional
	public void periodicServiceHealthCheck() {
		try {
			CompletableFuture<List<ServiceResponseDto>> services = serviceService.findAllServicesAsync();
			for (ServiceResponseDto serviceResponseDto : services.get()) {
				boolean connectionStatus = false;
				com.pawar.todo.amt.model.Service existingService = serviceRepository.findById(serviceResponseDto.id())
						.orElseThrow(() -> new ResourceNotFoundException(
								"Service not found with ID: " + serviceResponseDto.id()));
				connectionStatus = isServiceRunning(existingService.getHealthCheckUrl());
				Optional<ServiceHealthStatusResponseDto> serviceHealthStatus = serviceHealthStatusService
						.findServiceHealthStatusByServiceId(serviceResponseDto.id());
				ServiceHealthStatus healthStatus = serviceHealthStatusMapper.toEntity(serviceHealthStatus.get());
				log.info("connectionStatus : {}", connectionStatus);
				if (connectionStatus) {
					healthStatus.setStatus(HealthCheckStatus.UP);

				} else {
					healthStatus.setStatus(HealthCheckStatus.DOWN);
				}
				healthStatus.setTimestamp(LocalDateTime.now());
				healthStatus.setLastUpdatedDttm(LocalDateTime.now());
				healthStatus.setLastUpdatedSource("PERIODIC_HEALTH_CHECK");

				ServiceHealthStatusResponseDto healthStatusResponseDto = serviceHealthStatusMapper.toDto(healthStatus);
				serviceHealthStatusService.updateServiceHealthStatus(serviceResponseDto.id(), healthStatusResponseDto);
			}
		} catch (Exception e) {
			log.error("Error during periodic service health status check", e);
		}

	}

	public boolean isServiceRunning(String healthCheckUrl) {
		boolean isServiceRunning = false;
		try {
			ResponseEntity<String> response = httpService.restCall(null,healthCheckUrl, HttpMethod.GET, null, null);
			log.info("Response : {}", response);
			log.info("Response Code: {}", response.getStatusCode());

			if (response.getStatusCode().equals(HttpStatus.OK)) {
				isServiceRunning = true;
			}
		} catch (Exception e) {
			log.error("Error during service health check", e);
		}

		return isServiceRunning;

	}

}