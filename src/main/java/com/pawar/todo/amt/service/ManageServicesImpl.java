package com.pawar.todo.amt.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.model.ServiceHealthStatus;
import com.pawar.todo.amt.respository.ServiceRepository;
import com.pawar.todo.amt.respository.ServiceHealthStatusRepository;
import com.pawar.todo.amt.respository.ServerServiceConfigurationRepository;
import com.pawar.todo.amt.respository.ApplicationConfigurationRepository;
import com.pawar.todo.amt.model.ApplicationConfiguration;
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
	private final AlertConfigurationService alertConfigurationService;

	private PathService pathService;

	@Autowired
	private ServiceRepository serviceRepository;

	@Value("${healthcheck.enabled:true}")
	private boolean healthCheckEnabled = true;

	@Value("${service-management.enabled:true}")
	private boolean serviceManagementEnabled = true;

	@Autowired
	private ApplicationConfigurationRepository applicationConfigurationRepository;

	@Autowired
	private ServiceHealthStatusRepository serviceHealthStatusRepository;

	@Autowired
	private ServerServiceConfigurationRepository serverServiceConfigurationRepository;

	public ManageServicesImpl(SshCommandService sshCommandService,
			ScriptExtensionConverter scriptExtensionConverter,
			HttpService httpService, AlertConfigurationService alertConfigurationService) {
		this.sshCommandService = sshCommandService;
		this.scriptExtensionConverter = scriptExtensionConverter;
		this.httpService = httpService;
		this.alertConfigurationService = alertConfigurationService;
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

	@Override
	public String startService(Integer serverId, Integer serviceId) throws AgentOperationException,
			ServiceOperationException, IOException, ServiceHealthStatusOperationException, PathOperationException,
			ResourceNotFoundException {
		if (!runtimeFlag("service-management.enabled", serviceManagementEnabled)) {
			return "Service management is disabled";
		}
		ServerResponseDto server = getServer(serverId);
		ServiceResponseDto service = serviceService.findServiceById(serviceId)
				.orElseThrow(() -> new ServiceOperationException("Service not found"));
		ServiceHealthStatusResponseDto serviceHealthStatus = getOrCreateHealthStatus(service);

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

		ServiceHealthStatusResponseDto serviceHealthStatus = getOrCreateHealthStatus(service);

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
		String path = pathService.findByPathName(scriptsHome)
				.orElseThrow(() -> new PathOperationException("Path not found: " + scriptsHome)).pathName();
		Optional<ScriptResponseDto> scriptOptional = scriptService.findScriptByScriptName(scriptName);
		ScriptResponseDto script = scriptOptional
				.orElseThrow(() -> new ResourceNotFoundException("Script not found: " + scriptName));
		String scriptName1 = script.scriptName();
		ScriptExtension scriptExtension = scriptExtensionConverter.toEnum(script.scriptExtension());
		return String.format("%s/%s%s", "$" + path, scriptName1, scriptExtension.getExtension());
	}

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
		updateAllServiceHealthStatus(serverId, "UP", "startAllServices");

		return response;
	}

	@Override
	public String stopAllServices(Integer serverId) throws AgentOperationException, IOException, PathOperationException,
			ResourceNotFoundException, ServiceHealthStatusOperationException, InterruptedException, ExecutionException {
		if (!runtimeFlag("service-management.enabled", serviceManagementEnabled)) {
			return "Service management is disabled";
		}
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
		updateAllServiceHealthStatus(serverId, "DOWN", "stopAllServices");

		return response;
	}

	@Override
	public String restartAllServices(Integer serverId)
			throws AgentOperationException, IOException, PathOperationException,
			ResourceNotFoundException, ServiceHealthStatusOperationException, InterruptedException, ExecutionException {
		stopAllServices(serverId);
		return startAllServices(serverId);
	}

	@Override
	public void streamAllServices(Integer serverId, String action, Consumer<String> lineConsumer, AtomicBoolean stopped)
			throws AgentOperationException, IOException, PathOperationException, ResourceNotFoundException,
			ServiceHealthStatusOperationException, InterruptedException, ExecutionException {
		if (!runtimeFlag("service-management.enabled", serviceManagementEnabled)) {
			lineConsumer.accept("Service management is disabled");
			return;
		}
		ServerResponseDto server = getServer(serverId);
		if (!ServerStatus.ONLINE.toString().equals(server.status())) {
			lineConsumer.accept("Server is OFFLINE");
			return;
		}
		if ("restart".equals(action)) {
			streamBulkCommand(server, "stopAll", lineConsumer, stopped);
			if (!stopped.get()) {
				streamBulkCommand(server, "startAll", lineConsumer, stopped);
			}
			return;
		}
		if (!"start".equals(action) && !"stop".equals(action)) {
			throw new IllegalArgumentException("Unsupported service action: " + action);
		}
		streamBulkCommand(server, action + "All", lineConsumer, stopped);
	}

	private void streamBulkCommand(ServerResponseDto server, String scriptName, Consumer<String> lineConsumer,
			AtomicBoolean stopped)
			throws IOException, PathOperationException, ResourceNotFoundException, AgentOperationException,
			ServiceHealthStatusOperationException, InterruptedException, ExecutionException {
		String status = scriptName.startsWith("start") ? "UP" : "DOWN";
		String command = startStopCommand(null, "SCRIPTS_HOME", scriptName);
		log.info("Streaming command: {}", command);
		sshCommandService.stream(server, command, lineConsumer, stopped);
		if (!stopped.get()) {
			updateAllServiceHealthStatus(server.id(), status, scriptName + "Services");
		}
	}

	public boolean isServiceRunning(Integer serverId, String serviceName) {
		try {

			CommandResponseDto commandResponseDto = commandService.findCommand("CheckService")
					.orElseThrow(() -> new ServiceOperationException("Command does not exist"));

			String isServiceRunningCmd = commandResponseDto.name() + " " + commandResponseDto.parameters() + " "
					+ serviceName;
			log.info("isServiceRunningCmd : {}", isServiceRunningCmd);
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
	private void updateAllServiceHealthStatus(Integer serverId, String status, String lastUpdatedSource)
			throws AgentOperationException, ServiceHealthStatusOperationException, InterruptedException,
			ExecutionException {
		ServerResponseDto server = getServer(serverId);
		for (ServiceResponseDto service : Optional.ofNullable(server.services()).orElse(Set.of())) {
			ServiceHealthStatusResponseDto healthStatus = getOrCreateHealthStatus(service);
			updateServiceHealthStatus(healthStatus, status, lastUpdatedSource);
		}
		log.info("Updated health status to {} for services on server {}", status, serverId);
	}

	private ServiceHealthStatusResponseDto getOrCreateHealthStatus(ServiceResponseDto service)
			throws ServiceHealthStatusOperationException {
		Optional<ServiceHealthStatusResponseDto> existing = serviceHealthStatusService
				.findServiceHealthStatusByServiceId(service.id());
		if (existing.isPresent()) {
			return existing.get();
		}
		ServiceRequestDto serviceRequest = serviceMapper.reqToDto(serviceMapper.toEntity(service));
		return serviceHealthStatusService.createServiceHealthStatus(new ServiceHealthStatusRequestDto(
				null, serviceRequest, "UNKNOWN", LocalDateTime.now(), null, null, null, LocalDateTime.now(),
				"SERVICE_MANAGEMENT", "SERVICE_MANAGEMENT"));
	}

	@Override
	@Async
	@Scheduled(cron = "${healthcheck.cron}")
	@Transactional
	public void periodicServiceHealthCheck() {
		if (!runtimeFlag("healthcheck.enabled", healthCheckEnabled)) {
			log.debug("Scheduled service health checks are disabled");
			return;
		}
		try {
			CompletableFuture<List<ServiceResponseDto>> services = serviceService.findAllServicesAsync();
			for (ServiceResponseDto serviceResponseDto : services.get()) {
				com.pawar.todo.amt.model.Service existingService = serviceRepository.findById(serviceResponseDto.id())
						.orElseThrow(() -> new ResourceNotFoundException(
								"Service not found with ID: " + serviceResponseDto.id()));
				for (Server server : Optional.ofNullable(existingService.getServers()).orElse(Set.of())) {
					Optional<String> configuredHealthCheckUrl = serverServiceConfigurationRepository
							.findByServerIdAndServiceId(server.getId(), existingService.getId())
							.map(configuration -> configuration.getHealthCheckUrl());
					if (configuredHealthCheckUrl.isEmpty() || configuredHealthCheckUrl.get().isBlank()) {
						log.warn(
								"Skipping health check: no server-specific URL configured for serverId={}, serviceId={}",
								server.getId(), existingService.getId());
						continue;
					}
					ServiceHealthStatus healthStatus = serviceHealthStatusRepository
							.findByServiceIdAndServerId(existingService.getId(), server.getId())
							.orElseGet(ServiceHealthStatus::new);
					healthStatus.setService(existingService);
					healthStatus.setServer(server);
					long healthCheckStarted = System.nanoTime();
					boolean serviceUp = isServiceRunning(configuredHealthCheckUrl.get());
					healthStatus.setResponseTime((System.nanoTime() - healthCheckStarted) / 1_000_000);
					log.info("Health check completed: serverId={}, serviceId={}, status={}", server.getId(),
							existingService.getId(), serviceUp ? HealthCheckStatus.UP : HealthCheckStatus.DOWN);
					healthStatus.setStatus(serviceUp ? HealthCheckStatus.UP : HealthCheckStatus.DOWN);
					healthStatus.setTimestamp(LocalDateTime.now());
					healthStatus.setLastUpdatedDttm(LocalDateTime.now());
					healthStatus.setLastUpdatedSource("PERIODIC_HEALTH_CHECK");
					serviceHealthStatusRepository.save(healthStatus);
					if (runtimeFlag("alert-management.enabled", true)) {
						alertConfigurationService.evaluate(server.getId(), existingService.getId(),
								healthStatus.getStatus().name(), healthStatus.getResponseTime());
					}
				}
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

	private boolean runtimeFlag(String key, boolean fallback) {
		if (applicationConfigurationRepository == null) {
			return fallback;
		}
		return applicationConfigurationRepository.findById(key).map(configuration -> configuration.getValue())
				.map(Boolean::parseBoolean).orElse(fallback);
	}

}