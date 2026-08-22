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

import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.app.healthcheck.dto.ScriptResponseDto;
import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceHealthStatusRequestDto;
import com.pawar.app.healthcheck.dto.ServiceHealthStatusResponseDto;
import com.pawar.app.healthcheck.dto.ServiceRequestDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.sop.http.service.HttpService;
import com.pawar.todo.amt.constants.HealthCheckStatus;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.constants.ScriptExtension;
import com.pawar.todo.amt.converter.ScriptExtensionConverter;
import com.pawar.todo.amt.exceptions.AgentOperationException;
import com.pawar.todo.amt.exceptions.PathOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServiceHealthStatusOperationException;
import com.pawar.todo.amt.exceptions.ServiceOperationException;
import com.pawar.todo.amt.mapper.ServerMapper;
import com.pawar.todo.amt.mapper.ServiceHealthStatusMapper;
import com.pawar.todo.amt.mapper.ServiceMapper;
import com.pawar.todo.amt.model.Command;
import com.pawar.todo.amt.model.ServiceHealthStatus;
import com.pawar.todo.amt.respository.ServiceRepository;
import com.pawar.todo.amt.ssh.SshCommandService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ManageServicesImpl implements ManageServices {

	private final SshCommandService sshCommandService;
	private ScriptService scriptService;
	private ServiceService serviceService;
	private ServiceHealthStatusService serviceHealthStatusService;
	private ServerService serverService;
	private CommandService commandService;
	private final ScriptExtensionConverter scriptExtensionConverter;
	private ServiceHealthStatusMapper serviceHealthStatusMapper;
	private ServiceMapper serviceMapper;
	private final HttpService httpService;

	private PathService pathService;

	@Autowired
	private ServiceRepository serviceRepository;

	public ManageServicesImpl(SshCommandService sshCommandService,
			ScriptExtensionConverter scriptExtensionConverter,
			HttpService httpService) {
		this.sshCommandService = sshCommandService;
		this.scriptExtensionConverter = scriptExtensionConverter;
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
	public void setServerService(ServerService serverService) {
		this.serverService = serverService;
	}

	@Autowired
	public void setCommandService(CommandService commandService) {
		this.commandService = commandService;
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
	// this.serverMapper = serverMapper;

	// }

	@Override
	public String startService(Integer serverId, Integer serviceId) throws AgentOperationException,
			ServiceOperationException, IOException, ServiceHealthStatusOperationException, PathOperationException,
			ResourceNotFoundException {
		ServerResponseDto server = getServer(serverId);
		ServiceResponseDto service = serviceService.findServiceById(serviceId)
				.orElseThrow(() -> new ServiceOperationException("Service not found"));
		ServiceHealthStatusResponseDto serviceHealthStatus = serviceHealthStatusService
				.findServiceHealthStatusByServiceId(serviceId)
				.orElseThrow(() -> new ServiceHealthStatusOperationException("Service health status not found"));

		log.info("Server: {}", server);

		if (!server.status().equals(ServerStatus.ONLINE.toString())) {
			log.info("Server {} is OFFLINE", server.hostname());
			return "Server is OFFLINE";
		}
		if (isServiceRunning(serverId, service.serviceName())) {
			log.info("Service {} is already running on server {}", service.serviceName(), server.hostname());
			updateServiceHealthStatus(serviceHealthStatus, "UP", "startServiceCheck");
			return "Service is already running";
		}
		log.info("Service : {}", service);
		String command = startStopCommand(service, "SCRIPTS_HOME", "start");// buildStartCommand(agentId, service);
		log.info("Command: {}", command);
		String response = sshCommandService.execute(server, command);
		if (isServiceRunning(serverId, service.serviceName())) {
			updateServiceHealthStatus(serviceHealthStatus, "UP", "startService");
		}

		log.info("Service {} started successfully for Server ID {}. Response: {}", service.serviceName(), serverId,
				response);
		return response;
	}

	@Override
	public String stopService(Integer serverId, Integer serviceId) throws AgentOperationException,
			ServiceOperationException, IOException, ResourceNotFoundException, ServiceHealthStatusOperationException,
			PathOperationException {

		ServerResponseDto server = getServer(serverId);
		ServiceResponseDto service = serviceService.findServiceById(serviceId)
				.orElseThrow(() -> new ServiceOperationException("Service not found"));

		ServiceHealthStatusResponseDto serviceHealthStatus = serviceHealthStatusService
				.findServiceHealthStatusByServiceId(serviceId)
				.orElseThrow(() -> new ServiceHealthStatusOperationException("Service health status not found"));

		if (!server.status().equals(ServerStatus.ONLINE.toString())) {
			log.info("Server {} is OFFLINE", server.hostname());
			return "Server is OFFLINE";
		}

		String command = startStopCommand(service, "SCRIPTS_HOME", "stop");// buildStopCommand(service, script);
		log.info("Command: {}", command);
		String response = sshCommandService.execute(server, command);
		boolean serviceRunning = isServiceRunning(serverId, service.serviceName());
		updateServiceHealthStatus(serviceHealthStatus, serviceRunning ? "UP" : "DOWN", "stopServiceCheck");
		if (serviceRunning) {
			log.warn("Service {} is still running on server {} after stop command", service.serviceName(),
					server.hostname());
		}
		return response;
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

	private ServerResponseDto getServer(Integer serverId) throws AgentOperationException {
		try {
			return serverService.findServerById(serverId)
					.orElseThrow(() -> new AgentOperationException("Server does not exist"));
		} catch (com.pawar.todo.amt.exceptions.ServerOperationException exception) {
			throw new AgentOperationException("Failed to load server", exception);
		}
	}

	@Override
	public String startAllServices(Integer serverId)
			throws AgentOperationException, IOException, PathOperationException,
			ResourceNotFoundException, ServiceHealthStatusOperationException, InterruptedException, ExecutionException {
		ServerResponseDto server = getServer(serverId);

		log.info("Server: {}", server);

		if (!server.status().equals(ServerStatus.ONLINE.toString())) {
			log.info("Server {} is OFFLINE", server.hostname());
			return "Server is OFFLINE";
		}
		String command = startStopCommand(null, "SCRIPTS_HOME", "startAll");// buildStartAllCommand();
		log.info("Command: {}", command);
		String response = sshCommandService.execute(server, command);
		if (response.contains("Service started")) {
			updateAllServiceHealthStatus("UP", "startAllServices");
		}

		return response;
	}

	@Override
	public String stopAllServices(Integer serverId) throws AgentOperationException, IOException, PathOperationException,
			ResourceNotFoundException, ServiceHealthStatusOperationException, InterruptedException, ExecutionException {
		ServerResponseDto server = getServer(serverId);

		log.info("Server: {}", server);

		if (!server.status().equals(ServerStatus.ONLINE.toString())) {
			log.info("Server {} is OFFLINE", server.hostname());
			return "Server is OFFLINE";
		}
		String command = startStopCommand(null, "SCRIPTS_HOME", "stopAll");
		;
		log.info("Command: {}", command);
		String response = sshCommandService.execute(server, command);
		if (response.contains("Service stopped")) {
			updateAllServiceHealthStatus("DOWN", "stopAllServices");
		}

		return response;
	}

	@Override
	public String restartAllServices(Integer serverId)
			throws AgentOperationException, IOException, PathOperationException,
			ResourceNotFoundException, ServiceHealthStatusOperationException, InterruptedException, ExecutionException {
		stopAllServices(serverId);
		return startAllServices(serverId);
	}

	public boolean isServiceRunning(Integer serverId, String serviceName) {
		try {

			CommandResponseDto commandResponseDto = commandService.findCommand("CheckService")
					.orElseThrow(() -> new ServiceOperationException("Command does not exist"));

			String isServiceRunningCmd = commandResponseDto.name() + " " + commandResponseDto.parameters() + " "
					+ serviceName;
			ServerResponseDto server = getServer(serverId);
			String response = sshCommandService.execute(server, isServiceRunningCmd).trim().toLowerCase();
			log.info("Service: {}, Response: {}", serviceName, response);
			return response.equals("active") || response.equals("running") || response.equals("up")
					|| response.contains(" active ") || response.startsWith("active ");
		} catch (Exception e) {
			log.error("Error during service health check", e);
		}
		return false;

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
	@Scheduled(cron = "${healthcheck.cron}")
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
			ResponseEntity<String> response = httpService.restCall(null, healthCheckUrl, HttpMethod.GET, null, null);
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