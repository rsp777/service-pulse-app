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
import com.pawar.todo.amt.exceptions.ServerOperationException;
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
import com.pawar.todo.amt.respository.ServerRepository;
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
	private ServerMapper serverMapper;
	private final HttpService httpService;
	private final AlertConfigurationService alertConfigurationService;

	private PathService pathService;

	@Autowired
	private ServiceRepository serviceRepository;

	@Autowired
	private ServerRepository serverRepository;

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

	@Autowired
	public void setServerMapper(ServerMapper serverMapper) {
		this.serverMapper = serverMapper;
	}

	@Autowired
	public void setServerRepository(ServerRepository serverRepository) {
		this.serverRepository = serverRepository;
	}

	@Autowired
	public void setServiceRepository(ServiceRepository serviceRepository) {
		this.serviceRepository = serviceRepository;
	}

	@Autowired
	public void setServiceHealthStatusRepository(ServiceHealthStatusRepository serviceHealthStatusRepository) {
		this.serviceHealthStatusRepository = serviceHealthStatusRepository;
	}

	@Autowired
	public void setServerServiceConfigurationRepository(
			ServerServiceConfigurationRepository serverServiceConfigurationRepository) {
		this.serverServiceConfigurationRepository = serverServiceConfigurationRepository;
	}

	@Override
	public String startService(Integer serverId, Integer serviceId) throws ServerOperationException,
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
		String command = startStopCommand(server, service, "SCRIPTS_HOME", "start");// buildStartCommand(agentId, service);
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
	public String stopService(Integer serverId, Integer serviceId) throws ServerOperationException,
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

		String command = startStopCommand(server, service, "SCRIPTS_HOME", "stop");// buildStopCommand(service, script);
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

	private String startStopCommand(ServerResponseDto server, ServiceResponseDto service, String scriptsHome, String scriptName)
			throws PathOperationException, ResourceNotFoundException {
		String pathAndScript = getPathAndScript(server, scriptsHome, scriptName);
		if (service == null) {
			return String.format("%s", pathAndScript);
		} else {
			return String.format("%s %s", pathAndScript, service.serviceName());
		}
	}

	private String getPathAndScript(ServerResponseDto server, String scriptsHome, String scriptName)
			throws PathOperationException, ResourceNotFoundException {
		String scriptsDir = null;

		// 1. Check server-specific configured paths for real directory
		if (server != null && server.paths() != null) {
			scriptsDir = server.paths().stream()
					.filter(p -> scriptsHome.equalsIgnoreCase(p.pathName()))
					.map(PathResponseDto::pathDescription)
					.filter(desc -> desc != null && !desc.isBlank())
					.findFirst()
					.orElse(null);
		}

		// 2. Check global PathService for real directory
		if (scriptsDir == null || scriptsDir.isBlank()) {
			Optional<PathResponseDto> pathOpt = pathService.findByPathName(scriptsHome);
			if (pathOpt.isPresent()) {
				PathResponseDto p = pathOpt.get();
				if (p.pathDescription() != null && !p.pathDescription().isBlank()) {
					scriptsDir = p.pathDescription();
				} else {
					scriptsDir = "$" + p.pathName();
				}
			} else {
				throw new PathOperationException("Path not found: " + scriptsHome);
			}
		}

		scriptsDir = scriptsDir.trim().replaceAll("/+$", "");

		Optional<ScriptResponseDto> scriptOptional = scriptService.findScriptByScriptName(scriptName);
		ScriptResponseDto script = scriptOptional
				.orElseThrow(() -> new ResourceNotFoundException("Script not found: " + scriptName));
		String scriptName1 = script.scriptName();
		ScriptExtension scriptExtension = scriptExtensionConverter.toEnum(script.scriptExtension());
		return String.format("%s/%s%s", scriptsDir, scriptName1, scriptExtension.getExtension());
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

	private ServerResponseDto getServer(Integer serverId) throws ServerOperationException {
		try {
			return serverService.findServerById(serverId)
					.orElseThrow(() -> new ServerOperationException("Server does not exist"));
		} catch (com.pawar.todo.amt.exceptions.ServerOperationException exception) {
			throw new ServerOperationException("Failed to load server", exception);
		}
	}

	@Override
	public String startAllServices(Integer serverId)
			throws ServerOperationException, IOException, PathOperationException,
			ResourceNotFoundException, ServiceHealthStatusOperationException, InterruptedException, ExecutionException {
		ServerResponseDto server = getServer(serverId);

		log.info("Server: {}", server);

		if (!server.status().equals(ServerStatus.ONLINE.toString())) {
			log.info("Server {} is OFFLINE", server.hostname());
			return "Server is OFFLINE";
		}
		String command = startStopCommand(server, null, "SCRIPTS_HOME", "startAll");// buildStartAllCommand();
		log.info("Command: {}", command);
		String response = sshCommandService.execute(server, command);
		updateAllServiceHealthStatus(serverId, "UP", "startAllServices");

		return response;
	}

	@Override
	public String stopAllServices(Integer serverId)
			throws ServerOperationException, IOException, PathOperationException,
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
		String command = startStopCommand(server, null, "SCRIPTS_HOME", "stopAll");
		log.info("Command: {}", command);
		String response = sshCommandService.execute(server, command);
		updateAllServiceHealthStatus(serverId, "DOWN", "stopAllServices");

		return response;
	}

	@Override
	public String restartAllServices(Integer serverId)
			throws ServerOperationException, IOException, PathOperationException,
			ResourceNotFoundException, ServiceHealthStatusOperationException, InterruptedException, ExecutionException {
		stopAllServices(serverId);
		return startAllServices(serverId);
	}

	@Override
	public void streamAllServices(Integer serverId, String action, Consumer<String> lineConsumer, AtomicBoolean stopped)
			throws ServerOperationException, IOException, PathOperationException, ResourceNotFoundException,
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
			throws IOException, PathOperationException, ResourceNotFoundException, ServerOperationException,
			ServiceHealthStatusOperationException, InterruptedException, ExecutionException {
		String status = scriptName.startsWith("start") ? "UP" : "DOWN";
		String command = startStopCommand(server, null, "SCRIPTS_HOME", scriptName);
		log.info("Streaming command: {}", command);
		sshCommandService.stream(server, command, lineConsumer, stopped);
		if (!stopped.get()) {
			updateAllServiceHealthStatus(server.id(), status, scriptName + "Services");
		}
	}

	// Issue #30: Robust process/systemd check with fallback [Author: Ravindra Singh Pawar / Antigravity]
	public boolean isServiceRunning(Integer serverId, String serviceName) {
		try {
			String isServiceRunningCmd = null;
			try {
				if (commandService != null) {
					Optional<CommandResponseDto> commandResponseDto = commandService.findCommand("CheckService");
					if (commandResponseDto.isPresent()) {
						CommandResponseDto cmd = commandResponseDto.get();
						String params = cmd.parameters() != null ? cmd.parameters().trim() : "";
						isServiceRunningCmd = (cmd.name() + " " + params + " " + serviceName).trim();
					}
				}
			} catch (Exception e) {
				log.debug("CheckService command lookup failed, using fallback: {}", e.getMessage());
			}

			if (isServiceRunningCmd == null || isServiceRunningCmd.isBlank()) {
				// Issue #30: Fallback to process/systemd status verification (systemctl is-active or pgrep -f) [Author: Ravindra Singh Pawar / Antigravity]
				isServiceRunningCmd = String.format(
						"systemctl is-active %1$s 2>/dev/null || systemctl is-active %1$s.service 2>/dev/null || (pgrep -f %1$s >/dev/null && echo \"active\") || echo \"inactive\"",
						serviceName);
			}

			log.info("isServiceRunningCmd : {}", isServiceRunningCmd);
			ServerResponseDto server = getServer(serverId);
			String response = sshCommandService.execute(server, isServiceRunningCmd).trim().toLowerCase();
			log.info("Service: {}, Response: {}", serviceName, response);
			return response.equals("active") || response.equals("running") || response.equals("up")
					|| response.contains(" active ") || response.startsWith("active ") || response.endsWith(" active");
		} catch (Exception e) {
			log.error("Error during service health check for service {} on serverId {}", serviceName, serverId, e);
		}
		return false;
	}

	// Method to update health status for all services
	private void updateAllServiceHealthStatus(Integer serverId, String status, String lastUpdatedSource)
			throws ServerOperationException, ServiceHealthStatusOperationException, InterruptedException,
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

	// Issue #30: Tiered health evaluation (Tier 1: HTTP/Actuator URL, Tier 2: Host process/systemd probe) [Author: Ravindra Singh Pawar / Antigravity]
	public boolean evaluateServiceHealth(Server server, com.pawar.todo.amt.model.Service existingService) {
		Optional<String> configuredHealthCheckUrl = serverServiceConfigurationRepository != null
				? serverServiceConfigurationRepository.findByServerIdAndServiceId(server.getId(), existingService.getId())
						.map(configuration -> configuration.getHealthCheckUrl())
				: Optional.empty();

		ServiceHealthStatus healthStatus = serviceHealthStatusRepository != null
				? serviceHealthStatusRepository.findByServiceIdAndServerId(existingService.getId(), server.getId())
						.orElseGet(ServiceHealthStatus::new)
				: new ServiceHealthStatus();
		healthStatus.setService(existingService);
		healthStatus.setServer(server);
		boolean hadActiveAlert = alertConfigurationService != null
				&& alertConfigurationService.hasActiveAlert(server.getId(), existingService.getId());

		long healthCheckStarted = System.nanoTime();
		boolean serviceUp = false;

		// Tier 1: Probe HTTP endpoint if Health Check URL is configured and non-blank
		if (configuredHealthCheckUrl.isPresent() && !configuredHealthCheckUrl.get().isBlank()) {
			serviceUp = isServiceRunning(configuredHealthCheckUrl.get());
		} else {
			// Tier 2: Fallback to process/systemd status verification on target host so active services are not falsely marked DOWN
			try {
				ServerResponseDto serverDto = getServer(server.getId());
				if (ServerStatus.ONLINE.toString().equalsIgnoreCase(serverDto.status())) {
					serviceUp = isServiceRunning(server.getId(), existingService.getServiceName());
				} else {
					log.debug("Skipping host probe for service {} because server {} is OFFLINE",
							existingService.getServiceName(), server.getHostname());
					serviceUp = false;
				}
			} catch (Exception ex) {
				log.warn("Failed to probe host process status for service {} on server {}: {}",
						existingService.getServiceName(), server.getHostname(), ex.getMessage());
				serviceUp = false;
			}
		}
		healthStatus.setResponseTime((System.nanoTime() - healthCheckStarted) / 1_000_000);

		if (serviceUp) {
			if (hadActiveAlert) {
				log.info("Service recovered: serverId={}, serviceId={} is now UP (closing alert)",
						server.getId(), existingService.getId());
			} else {
				log.info("Health check completed: serverId={}, serviceId={}, status=UP",
						server.getId(), existingService.getId());
			}
		} else {
			if (!hadActiveAlert) {
				log.warn("Health check failed: serverId={}, serviceId={}, status=DOWN (triggering alert)",
						server.getId(), existingService.getId());
			} else {
				log.debug("Health check: serverId={}, serviceId={} still DOWN (alert already active)",
						server.getId(), existingService.getId());
			}
		}

		healthStatus.setStatus(serviceUp ? HealthCheckStatus.UP : HealthCheckStatus.DOWN);
		healthStatus.setTimestamp(LocalDateTime.now());
		healthStatus.setLastUpdatedDttm(LocalDateTime.now());
		healthStatus.setLastUpdatedSource("PERIODIC_HEALTH_CHECK");
		if (serviceHealthStatusRepository != null) {
			serviceHealthStatusRepository.save(healthStatus);
		}
		if (runtimeFlag("alert-management.enabled", true) && alertConfigurationService != null) {
			log.info("Triggering alert evaluation for serverId={}, serviceId={},status={}, responseTime={}",
					server.getId(), existingService.getId(), healthStatus.getStatus().name(),
					healthStatus.getResponseTime());
			alertConfigurationService.evaluate(server.getId(), existingService.getId(),
					healthStatus.getStatus().name(), healthStatus.getResponseTime());
		}
		return serviceUp;
	}

	@Override
	@Transactional
	public boolean evaluateServiceHealth(Integer serverId, Integer serviceId)
			throws ServerOperationException, ResourceNotFoundException {
		Server server = (serverRepository != null ? serverRepository.findById(serverId) : Optional.<Server>empty())
				.orElseGet(() -> {
					try {
						ServerResponseDto dto = getServer(serverId);
						if (serverMapper != null) return serverMapper.toEntity(dto);
					} catch (Exception ignored) {}
					return null;
				});
		if (server == null) {
			throw new ServerOperationException("Server does not exist: " + serverId);
		}
		com.pawar.todo.amt.model.Service service = (serviceRepository != null
				? serviceRepository.findById(serviceId)
				: Optional.<com.pawar.todo.amt.model.Service>empty())
				.orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + serviceId));
		return evaluateServiceHealth(server, service);
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
					evaluateServiceHealth(server, existingService);
				}
			}
		} catch (Exception e) {
			log.error("Error during periodic service health status check", e);
		}
	}

	// Issue #30: Robust HTTP / Spring Actuator health probe [Author: Ravindra Singh Pawar / Antigravity]
	public boolean isServiceRunning(String healthCheckUrl) {
		boolean isServiceRunning = false;
		try {
			ResponseEntity<String> response = httpService.restCall(null, healthCheckUrl, HttpMethod.GET, null, null);
			if (response != null && (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().equals(HttpStatus.OK))) {
				String body = response.getBody();
				if (body != null && body.contains("\"status\"")) {
					isServiceRunning = body.toUpperCase().matches("(?s).*\"STATUS\"\\s*:\\s*\"UP\".*");
				} else {
					isServiceRunning = true;
				}
			}
		} catch (Exception e) {
			log.debug("Service health check request failed for {}: {}", healthCheckUrl, e.getMessage());
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