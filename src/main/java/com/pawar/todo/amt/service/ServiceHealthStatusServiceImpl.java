package com.pawar.todo.amt.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.ServiceHealthStatusRequestDto;
import com.pawar.app.healthcheck.dto.ServiceHealthStatusResponseDto;
import com.pawar.app.healthcheck.dto.ServiceRequestDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.cache.ServerCache;
import com.pawar.todo.amt.cache.ServiceCache;
import com.pawar.todo.amt.cache.ServiceHealthStatusCache;
import com.pawar.todo.amt.constants.CommandStatus;
import com.pawar.todo.amt.constants.HealthCheckStatus;
import com.pawar.todo.amt.converter.HealthCheckStatusConverter;
import com.pawar.todo.amt.converter.ScriptExtensionConverter;
import com.pawar.todo.amt.exceptions.CommandOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServiceHealthStatusOperationException;
import com.pawar.todo.amt.exceptions.ServiceOperationException;
import com.pawar.todo.amt.mapper.PathMapper;
import com.pawar.todo.amt.mapper.ScriptMapper;
import com.pawar.todo.amt.mapper.ServerMapper;
import com.pawar.todo.amt.mapper.ServiceHealthStatusMapper;
import com.pawar.todo.amt.mapper.ServiceMapper;

import com.pawar.todo.amt.model.Path;
import com.pawar.todo.amt.model.Script;
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.model.ServiceHealthStatus;
import com.pawar.todo.amt.respository.PathRepository;
import com.pawar.todo.amt.respository.ScriptRepository;
import com.pawar.todo.amt.respository.ServerRepository;
import com.pawar.todo.amt.respository.ServiceHealthStatusRepository;
import com.pawar.todo.amt.respository.ServiceRepository;

@Service
public class ServiceHealthStatusServiceImpl implements ServiceHealthStatusService {

	private final static Logger logger = LoggerFactory.getLogger(ServiceHealthStatusServiceImpl.class.getName());

	private final ServiceRepository serviceRepository;
	private final ServiceHealthStatusRepository serviceHealthStatusRepository;
	private final ServerMapper serverMapper;
	private final ServiceMapper serviceMapper;
	private final ServiceHealthStatusMapper serviceHealthStatusMapper;
	private final ServiceCache serviceCache;
	private final ServiceHealthStatusCache serviceHealthStatusCache;
	private final HealthCheckStatusConverter healthCheckStatusConverter;
//    private final HealthCheckStatusConverter healthCheckStatusConverter;
//    private final ScriptExtensionConverter scriptExtensionConverter;

	public ServiceHealthStatusServiceImpl(ServiceRepository serviceRepository,
			ServiceHealthStatusRepository serviceHealthStatusRepository, PathRepository pathRepository,
			ScriptRepository scriptRepository, ServerMapper serverMapper, ServiceMapper serviceMapper,
			ServiceHealthStatusMapper serviceHealthStatusMapper, PathMapper pathMapper, ScriptMapper scriptMapper,
			ServerCache serverCache, ServiceCache serviceCache, ServiceHealthStatusCache serviceHealthStatusCache,
			HealthCheckStatusConverter healthCheckStatusConverter) {
		this.serviceRepository = serviceRepository;
		this.serviceHealthStatusRepository = serviceHealthStatusRepository;
		this.serverMapper = serverMapper;
		this.serviceMapper = serviceMapper;
		this.serviceHealthStatusMapper = serviceHealthStatusMapper;
		this.serviceCache = serviceCache;
		this.serviceHealthStatusCache = serviceHealthStatusCache;
		this.healthCheckStatusConverter = healthCheckStatusConverter;
	}

	@Override
	@Transactional
	public ServiceHealthStatusResponseDto createServiceHealthStatus(
			ServiceHealthStatusRequestDto serviceHealthStatusRequestDto) throws ServiceHealthStatusOperationException {
		try {
			logger.info("Creating new ServiceHealthStatus: {}", serviceHealthStatusRequestDto);

			ServiceHealthStatus serviceHealthStatus = serviceHealthStatusMapper.toEntity(serviceHealthStatusRequestDto);

			Optional<com.pawar.todo.amt.model.Service> service = serviceRepository
					.findById(serviceHealthStatusRequestDto.service().id());

			serviceHealthStatus.setService(service.get());
			serviceHealthStatus.setCreatedDttm(LocalDateTime.now());
			serviceHealthStatus.setLastUpdatedDttm(LocalDateTime.now());

			ServiceHealthStatus savedServiceHealthStatus = serviceHealthStatusRepository.save(serviceHealthStatus);
			logger.debug("ServiceHealthStatus created successfully: ID={}", savedServiceHealthStatus.getId());

			return serviceHealthStatusMapper.toDto(savedServiceHealthStatus);
		} catch (Exception e) {
			logger.error("Failed to create serviceHealthStatus: ", e);
			throw new ServiceHealthStatusOperationException("Failed to create serviceHealthStatus", e);
		}
	}

	@Override
	@Async
	@Transactional(readOnly = true)
	public CompletableFuture<List<ServiceHealthStatusResponseDto>> findAllServiceHealthStatussAsync() {
		try {
				logger.debug("Fetching all service health statuses asynchronously");
			List<ServiceHealthStatus> serviceHealthStatuss = serviceHealthStatusRepository.findAll();
			List<ServiceHealthStatusResponseDto> response = serviceHealthStatuss.stream()
					.peek(s -> logger.debug("Processing serviceHealthStatu: {}", s.getId()))
					.map(serviceHealthStatusMapper::toDto).collect(Collectors.toList());

				logger.debug("Completed asynchronous service health status fetch");
			return CompletableFuture.completedFuture(response);
		} catch (Exception e) {
			logger.error("Async serviceHealthStatus fetch failed", e);
			throw new CompletionException("Failed to fetch serviceHealthStatuss asynchronously", e);
		}
	}

	@Override
    @Transactional
    public ServiceHealthStatusResponseDto updateServiceHealthStatus(Integer id, ServiceHealthStatusRequestDto request) throws ServiceHealthStatusOperationException {
        try {
            logger.info("Updating serviceHealthStatus ID: {}", id);
            ServiceHealthStatus serviceHealthStatus = serviceHealthStatusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("serviceHealthStatus not found with ID: " + id));

            // Update Path and Script if necessary
            if (request.service() != null ) {
            	logger.info("request.service().serviceName() != serviceHealthStatus.getService().getServiceName() : {}",request.service().serviceName() != serviceHealthStatus.getService().getServiceName());
                if (!request.service().serviceName().equals(serviceHealthStatus.getService().getServiceName())) {
                	serviceHealthStatus.setService(serviceMapper.toEntity(request.service()));
				}
            }
            serviceHealthStatus.setStatus(healthCheckStatusConverter.toEnum(request.status()));
            serviceHealthStatus.setTimestamp(request.timestamp());
            serviceHealthStatus.setResponseTime(request.responseTime());
            serviceHealthStatus.setErrorMessage(request.errorMessage());
            serviceHealthStatus.setLastUpdatedSource(request.lastUpdatedSource());
            serviceHealthStatus.setLastUpdatedDttm(LocalDateTime.now());

            ServiceHealthStatus updatedServiceHealthStatus = serviceHealthStatusRepository.save(serviceHealthStatus);
            serviceCache.evict(id);
            logger.debug("ServiceHealthStatus updated successfully: ID={}", id);

            return serviceHealthStatusMapper.toDto(updatedServiceHealthStatus);
        } catch (Exception e) {
            logger.error("Failed to update serviceHealthStatus ID: {}", id, e);
            throw new ServiceHealthStatusOperationException("Failed to update serviceHealthStatus", e);
        }
    }

	@Override
	@Async
	@Transactional
	public ListenableFuture<Void> deleteServiceHealthStatusAsync(Integer id)
			throws ServiceHealthStatusOperationException {
		try {
			logger.info("Async deletion initiated for serviceHealthStatus ID: {}", id);
			serviceHealthStatusRepository.deleteById(id);
			serviceHealthStatusCache.evict(id);
			logger.debug("Async deletion completed for serviceHealthStatus ID: {}", id);
			return new AsyncResult<>(null);
		} catch (Exception e) {
			logger.error("Async deletion failed for serviceHealthStatus ID: {}", id, e);
			throw new ServiceHealthStatusOperationException("Failed to delete serviceHealthStatus asynchronously", e);
		}
	}

	@Override
	public List<ServiceHealthStatusResponseDto> findServiceHealthStatusByStatus(HealthCheckStatus status)
			throws ServiceHealthStatusOperationException {
		try {
			logger.debug("Fetching commands with status: {}", status.name());

			HealthCheckStatus healthCheckStatus = HealthCheckStatus.valueOf(status.name().toUpperCase());

			Optional<List<ServiceHealthStatus>> serviceHealthStatuss = serviceHealthStatusRepository
					.findByStatus(healthCheckStatus);
			// .stream()
			// .map(serviceHealthStatusMapper::toDto)
			// .collect(Collectors.toList());
			List<ServiceHealthStatusResponseDto> dtos = new ArrayList<>();

			for (ServiceHealthStatus serviceHealthStatus : serviceHealthStatuss.get()) {
				ServiceHealthStatusResponseDto dto = serviceHealthStatusMapper.toDto(serviceHealthStatus);
				dtos.add(dto);
			}
			return dtos;
		} catch (IllegalArgumentException e) {
			logger.warn("Invalid serviceHealthStatus requested: {}", status);
			throw new IllegalArgumentException("Invalid serviceHealthStatus: " + status);
		} catch (Exception e) {
			logger.error("Error fetching commands by status: {}", status, e);
			throw new ServiceHealthStatusOperationException("Failed to fetch serviceHealthStatus by status", e);
		}
	}

	@Override
	@Transactional
	public Optional<List<ServiceHealthStatusResponseDto>> findServiceHealthStatusByServerId(Integer id)
			throws ServiceHealthStatusOperationException, ResourceNotFoundException {
		try {
			logger.debug("Fetching ServiceHealthStatus by server ID: {}", id);
				logger.debug("Service health status cache lookup serverId={}", id);

			try {

				Optional<List<ServiceHealthStatus>> serviceHealthStatuss = Optional
						.ofNullable(serviceHealthStatusRepository.findByServerId(id).orElseThrow(
								() -> new ResourceNotFoundException("ServiceHealthStatus not found with ID: " + id)));
								logger.debug("Fetched service health statuses for serverId={}, count={}", id,
												serviceHealthStatuss.map(List::size).orElse(0));
				List<ServiceHealthStatusResponseDto> dtos = new ArrayList<>();

				for (ServiceHealthStatus serviceHealthStatus : serviceHealthStatuss.get()) {
					ServiceHealthStatusResponseDto dto = serviceHealthStatusMapper.toDto(serviceHealthStatus);
					dtos.add(dto);
				}

								logger.debug("Mapped service health statuses for serverId={}, count={}", id, dtos.size());
//                    	serviceHealthStatusCache.put(id, dto);
				return Optional.of(dtos);
			} catch (ResourceNotFoundException e) {
				logger.error("Error fetching serviceHealthStatus with server ID: {}", id, e);
				return Optional.empty(); // Return empty if not found
			} catch (Exception e) {
				logger.error("Error fetching serviceHealthStatus with server ID: {}", id, e);
				return Optional.empty(); // Return empty if not found
			}

		} catch (Exception e) {
			logger.error("Error fetching serviceHealthStatus with server ID: {}", id, e);
			throw new ServiceHealthStatusOperationException("Failed to fetch serviceHealthStatus", e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ServiceHealthStatusResponseDto> findServiceHealthStatusById(Integer id)
			throws ServiceHealthStatusOperationException {
		try {
			logger.debug("Fetching ServiceHealthStatus by ID: {}", id);
			return serviceHealthStatusCache.get(id).or(() -> {
				try {
					Optional<ServiceHealthStatus> serviceHealthStatus = Optional.ofNullable(
							serviceHealthStatusRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(
									"ServiceHealthStatus not found with ID: " + id)));
					ServiceHealthStatusResponseDto dto = serviceHealthStatusMapper.toDto(serviceHealthStatus.get());
					serviceHealthStatusCache.put(id, dto);
					return Optional.of(dto);
				} catch (ResourceNotFoundException e) {
					logger.error("Error fetching serviceHealthStatus with ID: {}", id, e);
					return Optional.empty(); // Return empty if not found
				}
			});
		} catch (Exception e) {
			logger.error("Error fetching serviceHealthStatus with ID: {}", id, e);
			throw new ServiceHealthStatusOperationException("Failed to fetch serviceHealthStatus", e);
		}
	}

	@Override
	public Optional<ServiceHealthStatusResponseDto> findServiceHealthStatusByServiceId(Integer serviceId)
			throws ServiceHealthStatusOperationException {
		try {
			logger.debug("Fetching ServiceHealthStatus by service ID: {}", serviceId);
			return serviceHealthStatusCache.get(serviceId).or(() -> {
				try {
					Optional<ServiceHealthStatus> serviceHealthStatus = Optional
							.ofNullable(serviceHealthStatusRepository.findServiceHealthStatusByServiceId(serviceId)
									.orElseThrow(() -> new ResourceNotFoundException(
											"ServiceHealthStatus not found with ID: " + serviceId)));
					ServiceHealthStatusResponseDto dto = serviceHealthStatusMapper.toDto(serviceHealthStatus.get());
										logger.debug("Mapped service health status for serviceId={}", serviceId);
					serviceHealthStatusCache.put(serviceId, dto);
					return Optional.of(dto);
				} catch (ResourceNotFoundException e) {
					logger.error("Error fetching serviceHealthStatus with server ID: {}", serviceId, e);
					return Optional.empty(); // Return empty if not found
				}
			});
		} catch (Exception e) {
			logger.error("Error fetching serviceHealthStatus with service ID: {}", serviceId, e);
			throw new ServiceHealthStatusOperationException("Failed to fetch serviceHealthStatus", e);
		}
	}

	@Override
	public ServiceHealthStatusResponseDto updateServiceHealthStatus(Integer id, ServiceHealthStatusResponseDto response)
			throws ServiceHealthStatusOperationException {
		try {
			logger.info("Updating serviceHealthStatus ID: {}", id);
			ServiceHealthStatus serviceHealthStatus = serviceHealthStatusRepository.findServiceHealthStatusByServiceId(id)
					.orElseThrow(() -> new ResourceNotFoundException("serviceHealthStatus not found with ID: " + id));

			// Update Path and Script if necessary
            if (response.service() != null ) {
            	logger.info("request.service().serviceName() != serviceHealthStatus.getService().getServiceName() : {}",response.service().serviceName() != serviceHealthStatus.getService().getServiceName());
                if (!response.service().serviceName().equals(serviceHealthStatus.getService().getServiceName())) {
                	serviceHealthStatus.setService(serviceMapper.toEntity(response.service()));
				}
            }
			serviceHealthStatus.setStatus(healthCheckStatusConverter.toEnum(response.status()));
			serviceHealthStatus.setTimestamp(response.timeStamp());
			serviceHealthStatus.setResponseTime(response.responseTime());
			serviceHealthStatus.setErrorMessage(response.errorMessage());
			serviceHealthStatus.setLastUpdatedSource(response.lastUpdatedSource());
			serviceHealthStatus.setLastUpdatedDttm(LocalDateTime.now());

						logger.debug("Persisting service health status id={}", serviceHealthStatus.getId());

			ServiceHealthStatus updatedServiceHealthStatus = serviceHealthStatusRepository.save(serviceHealthStatus);
			serviceCache.evict(id);
			logger.debug("ServiceHealthStatus updated successfully: ID={}", id);

			return serviceHealthStatusMapper.toDto(updatedServiceHealthStatus);
		} catch (Exception e) {
			logger.error("Failed to update serviceHealthStatus ID: {}", id, e);
			throw new ServiceHealthStatusOperationException("Failed to update serviceHealthStatus", e);
		}
	}
}
