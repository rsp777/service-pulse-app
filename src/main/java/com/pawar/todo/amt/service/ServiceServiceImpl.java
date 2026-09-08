package com.pawar.todo.amt.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.app.healthcheck.dto.ScriptResponseDto;
import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceRequestDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.cache.ServiceCache;
import com.pawar.todo.amt.constants.HealthCheckStatus;
import com.pawar.todo.amt.converter.ScriptExtensionConverter;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServiceOperationException;
import com.pawar.todo.amt.mapper.PathMapper;
import com.pawar.todo.amt.mapper.ScriptMapper;
import com.pawar.todo.amt.mapper.ServiceMapper;

import com.pawar.todo.amt.model.Path;
import com.pawar.todo.amt.model.Script;
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.model.ServerServiceConfiguration;
import com.pawar.todo.amt.model.ServiceHealthStatus;
import com.pawar.todo.amt.respository.PathRepository;
import com.pawar.todo.amt.respository.ScriptRepository;
import com.pawar.todo.amt.respository.ServerRepository;
import com.pawar.todo.amt.respository.ServerServiceConfigurationRepository;
import com.pawar.todo.amt.respository.ServiceHealthStatusRepository;
import com.pawar.todo.amt.respository.ServiceRepository;

import jakarta.validation.constraints.NotNull;

@Service
public class ServiceServiceImpl implements ServiceService {

	private final static Logger logger = LoggerFactory.getLogger(ServiceServiceImpl.class.getName());

	private final ServerRepository serverRepository;
	private final ServiceRepository serviceRepository;
	private final ServiceHealthStatusRepository serviceHealthStatusRepository;
	private final PathRepository pathRepository;
	private final ScriptRepository scriptRepository;
	private final ServiceMapper serviceMapper;
	private final ScriptMapper scriptMapper;
	private final PathMapper pathMapper;
	private final ServiceCache serviceCache;
	private final ScriptExtensionConverter scriptExtensionConverter;
	private final ServerServiceConfigurationRepository serverServiceConfigurationRepository;

	public ServiceServiceImpl(ServerRepository serverRepository, ServiceRepository serviceRepository,
			ServiceHealthStatusRepository serviceHealthStatusRepository, PathRepository pathRepository,
			ScriptRepository scriptRepository, ServiceMapper serviceMapper, ScriptMapper scriptMapper,
			PathMapper pathMapper, ServiceCache serviceCache, ScriptExtensionConverter scriptExtensionConverter,
			ServerServiceConfigurationRepository serverServiceConfigurationRepository) {
		this.serverRepository = serverRepository;
		this.serviceRepository = serviceRepository;
		this.serviceHealthStatusRepository = serviceHealthStatusRepository;
		this.pathRepository = pathRepository;
		this.scriptRepository = scriptRepository;
		this.serviceMapper = serviceMapper;
		this.scriptMapper = scriptMapper;
		this.pathMapper = pathMapper;
		this.serviceCache = serviceCache;
		this.scriptExtensionConverter = scriptExtensionConverter;
		this.serverServiceConfigurationRepository = serverServiceConfigurationRepository;
	}

	@Override
	@Transactional
	public ServiceResponseDto createService(ServiceRequestDto request) throws ServiceOperationException {
		try {
			if (request == null) {
				logger.info("request is null : {}", request);
				return null;
			}

			logger.info("Creating new service: {}", request.serviceName());

			com.pawar.todo.amt.model.Service service = new com.pawar.todo.amt.model.Service();

			Set<ServerResponseDto> serverResponseDtoDtos = request.servers();
			Set<Server> servers = new HashSet<>();
			logger.info("serverResponseDtoDtos : {}",serverResponseDtoDtos);
			if (serverResponseDtoDtos != null && !serverResponseDtoDtos.isEmpty()) {
				for (Iterator iterator = serverResponseDtoDtos.iterator(); iterator.hasNext();) {
					ServerResponseDto serverResponseDto = (ServerResponseDto) iterator.next();
					logger.info("serverResponseDto.id() : {}",serverResponseDto.id());
					Optional<Server> serverOptional = serverRepository.findById(serverResponseDto.id());
					logger.info("serverOptional : {}",serverOptional);
					if (serverOptional.isPresent()) {
						Server server = serverOptional.get();
						servers.add(server);
						if (server.getServices() == null) {
							server.setServices(new HashSet<>());
						}
						server.getServices().add(service);
					}
				}
				logger.info("servers : {}",servers);
				service.setServers(servers);
			}
			service.setServiceName(request.serviceName());
			service.setCreatedSource(request.createdSource());
			service.setLastUpdatedSource(request.lastUpdatedSource());
			service.setCreatedDttm(LocalDateTime.now());
			service.setLastUpdatedDttm(LocalDateTime.now());
			logger.info("service : {}",service);
			com.pawar.todo.amt.model.Service savedService = serviceRepository.save(service);
			upsertServerConfigurations(savedService, servers, request.healthCheckUrl());
			logger.debug("Service created successfully: ID={}", savedService.getId());
			if (savedService != null) {
				for (Server server : servers) {
					ServiceHealthStatus serviceHealthStatus = new ServiceHealthStatus();
					serviceHealthStatus.setService(savedService);
					serviceHealthStatus.setServer(server);
					serviceHealthStatus.setStatus(HealthCheckStatus.UNKNOWN);
					serviceHealthStatus.setCreatedSource(service.getCreatedSource());
					serviceHealthStatus.setLastUpdatedSource(service.getLastUpdatedSource());
					serviceHealthStatusRepository.save(serviceHealthStatus);
				}
			}
			return serviceMapper.toDto(savedService);
		} catch (Exception e) {
			logger.error("Failed to create service: ", e);
			throw new ServiceOperationException("Failed to create service", e);
		}
		catch (Throwable e) {
			logger.error("Failed to create service: ", e);
			throw new ServiceOperationException("Failed to create service", e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ServiceResponseDto> findServiceById(Integer id) throws ServiceOperationException {
		try {
			logger.debug("Fetching service by ID: {}", id);
			return serviceCache.get(id).or(() -> {
				try {
					com.pawar.todo.amt.model.Service service = serviceRepository.findById(id)
							.orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + id));
					ServiceResponseDto dto = serviceMapper.toDto(service);
					serviceCache.put(id, dto);
					return Optional.of(dto);
				} catch (ResourceNotFoundException e) {
					logger.error("Error fetching service with ID: {}", id, e);
					return Optional.empty(); // Return empty if not found
				}
			});
		} catch (Exception e) {
			logger.error("Error fetching service with ID: {}", id, e);
			throw new ServiceOperationException("Failed to fetch service", e);
		}
	}

	@Override
	@Async("serviceTaskExecutor")
	@Transactional(readOnly = true)
	public CompletableFuture<List<ServiceResponseDto>> findAllServicesAsync() {
		try {
			logger.info("Async fetching of all services initiated");
			List<com.pawar.todo.amt.model.Service> services = serviceRepository.findAll();
			List<ServiceResponseDto> response = services.stream()
					.peek(s -> logger.debug("Processing service: {}", s.getId())).map(serviceMapper::toDto)
					.collect(Collectors.toList());

			logger.info("Async service fetch completed successfully");
			return CompletableFuture.completedFuture(response);
		} catch (Exception e) {
			logger.error("Async service fetch failed", e);
			throw new CompletionException("Failed to fetch services asynchronously", e);
		}
	}

	@Override
	@Transactional
	public ServiceResponseDto updateService(Integer id, ServiceRequestDto request) throws ServiceOperationException {
		try {

			if (id == null || request == null) {
				logger.info("Either id : {} or request {} is null", id, request);
				return null;
			}

			logger.info("Updating service ID: {}", id);
			com.pawar.todo.amt.model.Service service = serviceRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + id));

			Set<ServerResponseDto> serverRequestDtos = request.servers();
			Set<Server> servers = new HashSet<>();
			Set<Server> requestedServers = new HashSet<>();
			servers.addAll(Optional.ofNullable(service.getServers()).orElseGet(HashSet::new));

			if (serverRequestDtos != null && !serverRequestDtos.isEmpty()) {
				for (Iterator iterator = serverRequestDtos.iterator(); iterator.hasNext();) {
					ServerResponseDto serverResponseDto = (ServerResponseDto) iterator.next();
					Optional<Server> serverOptional = serverRepository.findById(serverResponseDto.id());
					if (serverOptional.isPresent()) {
						Server requestedServer = serverOptional.get();
						servers.add(requestedServer);
						requestedServers.add(requestedServer);
					}
				}
				service.setServers(servers);
			}
			service.setServiceName(request.serviceName());
			upsertServerConfigurations(service, requestedServers, request.healthCheckUrl());
			service.setLastUpdatedDttm(LocalDateTime.now());

			com.pawar.todo.amt.model.Service updatedService = serviceRepository.save(service);
			serviceCache.evict(id);
			logger.debug("Service updated successfully: ID={}", id);

			return serviceMapper.toDto(updatedService);
		} catch (Exception e) {
			logger.error("Failed to update service ID: {}", id, e);
			throw new ServiceOperationException("Failed to update service", e);
		}
	}

	private void upsertServerConfigurations(com.pawar.todo.amt.model.Service service, Set<Server> servers,
			String healthCheckUrl) {
		for (Server server : servers) {
			ServerServiceConfiguration configuration = serverServiceConfigurationRepository
					.findByServerIdAndServiceId(server.getId(), service.getId())
					.orElseGet(ServerServiceConfiguration::new);
			configuration.setServer(server);
			configuration.setService(service);
			configuration.setHealthCheckUrl(healthCheckUrl);
			configuration.setCreatedSource(service.getCreatedSource());
			configuration.setLastUpdatedSource(service.getLastUpdatedSource());
			serverServiceConfigurationRepository.save(configuration);
		}
	}

	@Override
	@Async
	@Transactional
	public ListenableFuture<Void> deleteServiceAsync(Integer id) throws ServiceOperationException {
		try {
			logger.info("Async deletion initiated for service ID: {}", id);
			serverServiceConfigurationRepository.deleteByServiceId(id);
			serviceRepository.deleteById(id);
			serviceCache.evict(id);
			logger.debug("Async deletion completed for service ID: {}", id);
			return new AsyncResult<>(null);
		} catch (Exception e) {
			logger.error("Async deletion failed for service ID: {}", id, e);
			throw new ServiceOperationException("Failed to delete service asynchronously", e);
		}
	}

	@Override
	public boolean isServiceExist(String serviceName) throws ServiceOperationException {
		boolean isServiceExist = serviceRepository.existsByServiceName(serviceName);
		logger.info("isServiceExist : {}",isServiceExist);
		return isServiceExist;
	}
}
