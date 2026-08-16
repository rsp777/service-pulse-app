package com.pawar.todo.amt.mapper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.PathRequestDto;
import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.app.healthcheck.dto.ScriptResponseDto;
import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceRequestDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.converter.ServerStatusConverter;
import com.pawar.todo.amt.exceptions.PathOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServiceOperationException;
import com.pawar.todo.amt.model.Path;
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.model.Service;
import com.pawar.todo.amt.service.PathService;
import com.pawar.todo.amt.service.ServiceService;

/**
 * Mapper class for converting between Server entity and DTOs
 */
@Component
public class ServerMapper {

	private static final Logger logger = LoggerFactory.getLogger(ServerMapper.class);
	private final ServerStatusConverter serverStatusConverter;

	private ServiceMapper serviceMapper;
	private PathMapper pathMapper;

	@Autowired
	private PathService pathService;

	@Autowired
	private ServiceService serviceService;

	public ServerMapper(ServerStatusConverter serverStatusConverter) {
		this.serverStatusConverter = serverStatusConverter;
	}

	@Autowired
	public void setPathMapper(PathMapper pathMapper) {
		this.pathMapper = pathMapper;

	}

	@Autowired
	public void setServiceMapper(ServiceMapper serviceMapper) {
		this.serviceMapper = serviceMapper;
	}

	public Server toEntity(ServerRequestDto dto)
			throws ResourceNotFoundException, PathOperationException, ServiceOperationException {
		logger.debug("Entering toEntity() with dto: {}", dto);
		if (dto == null) {
			logger.warn("Received null ServerRequestDto, returning null Server entity.");
			return null;
		}

		Server server = new Server();
		populateServerFromRequestDto(dto, server);
		logger.info("Converted to Server entity: {}", server);
		return server;
	}

	public Server toEntity(ServerResponseDto dto) {
		logger.debug("Entering toEntity() with dto: {}", dto);
		if (dto == null) {
			logger.warn("Received null ServerResponseDto, returning null Server entity.");
			return null;
		}

		Server server = new Server();
		populateServerFromResponseDto(dto, server);
		logger.info("Converted to Server entity: {}", server);
		return server;
	}

	private void populateServerFromRequestDto(ServerRequestDto dto, Server server)
			throws ResourceNotFoundException, PathOperationException, ServiceOperationException {
		logger.info("Populating Server entity from ServerRequestDto: {}", dto);
		server.setHostname(dto.hostname());
		logger.debug("Set hostname: {}", dto.hostname());

		server.setIpAddress(dto.ipAddress());
		logger.debug("Set ipAddress: {}", dto.ipAddress());

		server.setOsType(dto.osType());
		logger.debug("Set osType: {}", dto.osType());

		Set<Path> paths = new HashSet<>();
		Set<Service> services = new HashSet<>();

		if (dto.paths() != null && !dto.paths().isEmpty()) {
			Set<PathRequestDto> pathRequestDtos = dto.paths();
			for (PathRequestDto pathRequestDto : pathRequestDtos) {
				Optional<PathRequestDto> pathRequestOptional = pathService.findPathByPathId(pathRequestDto.id());
				Path path = pathMapper.toEntity(pathRequestOptional.get());
				paths.add(path);
			}
			server.setPaths(paths);
			logger.debug("Set paths of id : {}", dto.paths());

		}
		if (dto.services() != null && !dto.services().isEmpty()) {
			Set<ServiceResponseDto> serviceResponseDtos = dto.services();
			for (ServiceResponseDto serviceResponseDto : serviceResponseDtos) {
				boolean isServiceExist = serviceService.isServiceExist(serviceResponseDto.serviceName());
				logger.info("isServiceExist : {}", isServiceExist);
				if (!isServiceExist) {
					Service service = serviceMapper.toEntity(serviceResponseDto);
					services.add(service);
				}
			}
			server.setServices(services);
		}

		if (dto.status() != null) {
			server.setStatus(serverStatusConverter.toEnum(dto.status()));
			logger.debug("Converted and set status: {}", dto.status());
		}

		server.setLastHealthChecked(dto.lastHealthChecked());
		logger.debug("Set lastHealthChecked: {}", dto.lastHealthChecked());

		server.setCreatedDttm(dto.createdDttm());
		server.setLastUpdatedDttm(dto.lastUpdatedDttm());
		server.setCreatedSource(dto.createdSource());
		server.setLastUpdatedSource(dto.lastUpdatedSource());
		logger.debug("Set timestamps and source info");
	}

	private void populateServerFromResponseDto(ServerResponseDto dto, Server server) {
		logger.info("Populating Server entity from ServerResponseDto: {}", dto);
		server.setHostname(dto.hostname());
		logger.debug("Set hostname: {}", dto.hostname());

		server.setIpAddress(dto.ipAddress());
		logger.debug("Set ipAddress: {}", dto.ipAddress());

		server.setOsType(dto.osType());
		logger.debug("Set osType: {}", dto.osType());

		server.setStatus(serverStatusConverter.toEnum(dto.status()));
		logger.debug("Converted and set status: {}", dto.status());

		server.setLastHealthChecked(dto.lastHealthChecked());
		logger.debug("Set lastHealthChecked: {}", dto.lastHealthChecked());

		Set<ServiceResponseDto> serviceDtos = dto.services();
		if (!serviceDtos.isEmpty()) {

			Set<Service> services = new HashSet<>();
			for (ServiceResponseDto serviceResponseDto : serviceDtos) {
				Service service = serviceMapper.toEntity(serviceResponseDto);
				services.add(service);
			}
			server.setServices(services);
			logger.debug("Set services for Server: {}", server);
		}

		Set<PathResponseDto> pathDtos = dto.paths();
		if (!pathDtos.isEmpty()) {

			Set<Path> paths = new HashSet<>();
			for (PathResponseDto pathResponseDto : pathDtos) {
				Path path = pathMapper.toEntity(pathResponseDto);
				paths.add(path);
			}
			server.setPaths(paths);
			logger.debug("Set paths for server: {}", server);
		}

		server.setCreatedDttm(dto.createdDttm());
		server.setLastUpdatedDttm(dto.lastUpdatedDttm());
		server.setCreatedSource(dto.createdSource());
		server.setLastUpdatedSource(dto.lastUpdatedSource());
		logger.debug("Set timestamps and source info");
	}

	public ServerRequestDto reqToDto(Server entity) {
		logger.debug("Entering toDto() with entity: {}", entity);
		if (entity == null) {
			logger.warn("Received null Server entity, returning null ServerResponseDto.");
			return null;
		}

		Set<ServiceResponseDto> serviceResponseDtos = new HashSet<>();
		Set<Service> services = entity.getServices();

		Set<PathRequestDto> pathRequestDtos = new HashSet<>();
		Set<Path> paths = entity.getPaths();

		for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
			Path path = (Path) iterator.next();
			PathRequestDto pathRequestDto = pathMapper.reqToDto(path);
			pathRequestDtos.add(pathRequestDto);
		}

		for (Iterator iterator = services.iterator(); iterator.hasNext();) {
			Service service = (Service) iterator.next();
			ServiceResponseDto serviceResponseDto = serviceMapper.toDto(service);
			serviceResponseDtos.add(serviceResponseDto);
		}

		ServerRequestDto dto = new ServerRequestDto(entity.getId(), entity.getHostname(), entity.getIpAddress(),
				entity.getOsType(), entity.getStatus().name(), entity.getLastHealthChecked(), pathRequestDtos,
				serviceResponseDtos, entity.getCreatedDttm(), entity.getLastUpdatedDttm(), entity.getCreatedSource(),
				entity.getLastUpdatedSource());

		logger.debug("Created ServerResponseDto with values: {}", String.format("id=%s, hostname=%s, ip=%s, os=%s",
				entity.getId(), entity.getHostname(), entity.getIpAddress(), entity.getOsType()));

		logger.info("Converted to ServerRequestDto: {}", dto);
		return dto;
	}

	public ServerResponseDto toDto(Server entity) {
		try {
			logger.debug("Entering toDto() with entity: {}", entity);
			if (entity == null) {
				logger.warn("Received null Server entity, returning null ServerResponseDto.");
				return null;
			}

			Set<ServiceResponseDto> serviceResponseDtos = new HashSet<>();
			Set<Service> services = entity.getServices();

			Set<PathResponseDto> pathResponseDtos = new HashSet<>();
			Set<Path> paths = entity.getPaths();
			logger.info("paths : {}",paths);
			if (paths != null && !paths.isEmpty()) {

				for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
					Path path = (Path) iterator.next();
					PathResponseDto pathResponseDto = pathMapper.toDto(path);
					pathResponseDtos.add(pathResponseDto);
				}
			}

			if (services != null && !services.isEmpty()) {
				for (Iterator iterator = services.iterator(); iterator.hasNext();) {
					Service service = (Service) iterator.next();
					ServiceResponseDto serviceResponseDto = serviceMapper.toDto(service);
					serviceResponseDtos.add(serviceResponseDto);
				}
			}

			ServerResponseDto dto = new ServerResponseDto(entity.getId(), entity.getHostname(), entity.getIpAddress(),
					entity.getOsType(), entity.getStatus().name(), entity.getLastHealthChecked(), pathResponseDtos,
					serviceResponseDtos, entity.getCreatedDttm(), entity.getLastUpdatedDttm(),
					entity.getCreatedSource(), entity.getLastUpdatedSource());

			logger.debug("Created ServerResponseDto with values: {}", String.format("id=%s, hostname=%s, ip=%s, os=%s",
					entity.getId(), entity.getHostname(), entity.getIpAddress(), entity.getOsType()));

			logger.info("Converted to ServerResponseDto: {}", dto);
			return dto;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void updateFromDto(ServerRequestDto dto, Server server)
			throws ResourceNotFoundException, PathOperationException, ServiceOperationException {
		logger.debug("Entering updateFromDto() with dto: {}, server: {}", dto, server);
		if (dto == null || server == null) {
			logger.warn("Received null ServerRequestDto or Server, skipping update.");
			return;
		}

		logger.info("Updating Server entity from ServerRequestDto: {}", dto);
		populateServerFromRequestDto(dto, server);
		logger.info("Updated Server entity: {}", server);
	}

	public ServerRequestDto toRequestDto(Server entity) {
		logger.debug("Entering toRequestDto() with entity: {}", entity);
		if (entity == null) {
			logger.warn("Received null Server entity, returning null ServerRequestDto.");
			return null;
		}

		Set<ServiceResponseDto> serviceResponseDtos = new HashSet<>();
		Set<Service> services = entity.getServices();

		Set<PathRequestDto> pathRequestDtos = new HashSet<>();
		Set<Path> paths = entity.getPaths();

		for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
			Path path = (Path) iterator.next();
			PathRequestDto pathRequestDto = pathMapper.reqToDto(path);
			pathRequestDtos.add(pathRequestDto);
		}

		for (Iterator iterator = services.iterator(); iterator.hasNext();) {
			Service service = (Service) iterator.next();
			ServiceResponseDto serviceResponseDto = serviceMapper.toDto(service);
			serviceResponseDtos.add(serviceResponseDto);
		}

		ServerRequestDto dto = new ServerRequestDto(entity.getId(), entity.getHostname(), entity.getIpAddress(),
				entity.getOsType(), entity.getStatus().name(), entity.getLastHealthChecked(), pathRequestDtos,
				serviceResponseDtos, entity.getCreatedDttm(), entity.getLastUpdatedDttm(), entity.getCreatedSource(),
				entity.getLastUpdatedSource());

		logger.debug("Created ServerRequestDto with values: {}", String.format("hostname=%s, ip=%s, os=%s",
				entity.getHostname(), entity.getIpAddress(), entity.getOsType()));
		logger.info("Converted to ServerRequestDto: {}", dto);
		return dto;
	}
}