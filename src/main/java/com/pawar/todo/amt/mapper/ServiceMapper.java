package com.pawar.todo.amt.mapper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.app.healthcheck.dto.ScriptResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceRequestDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.converter.ScriptExtensionConverter;
import com.pawar.todo.amt.model.Path;
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.model.Service;

@Component
public class ServiceMapper {
	private static final Logger logger = LoggerFactory.getLogger(ServiceMapper.class);
	private final ScriptExtensionConverter scriptExtensionConverter;

	private ServerMapper serverMapper;

	public ServiceMapper() {
		this.scriptExtensionConverter = new ScriptExtensionConverter();
	}

	@Autowired
	public void setServerMapper(ServerMapper serverMapper) {
		this.serverMapper = serverMapper;
	}

	public Service toEntity(ServiceRequestDto dto) {
		logger.debug("Entering toEntity() with dto: {}", dto);
		if (dto == null) {
			logger.warn("Received null ServiceRequestDto, returning null Service entity.");
			return null;
		}

		Service service = new Service();
		populateServiceFromRequestDto(dto, service);
		logger.info("Converted to Service entity: {}", service);
		return service;
	}

	public Service toEntity(ServiceResponseDto dto) {
		logger.debug("Entering toEntity() with dto: {}", dto);
		if (dto == null) {
			logger.warn("Received null ServiceResponseDto, returning null Service entity.");
			return null;
		}

		Service service = new Service();
		populateServiceFromResponseDto(dto, service);
		logger.info("Converted to Service entity: {}", service);
		return service;
	}

	private void populateServiceFromRequestDto(ServiceRequestDto dto, Service service) {
		logger.info("Populating Service entity from ServiceRequestDto: {}", dto);
		service.setServiceName(dto.serviceName());
		logger.debug("Set serviceName: {}", dto.serviceName());

		Set<ServerResponseDto> serverResponseDtos = dto.servers();
		if (serverResponseDtos != null) {

			if (!serverResponseDtos.isEmpty() && !serverResponseDtos.isEmpty()) {
				Set<Server> servers = new HashSet<>();
				for (ServerResponseDto serverResponseDto : serverResponseDtos) {
					Server server = serverMapper.toEntity(serverResponseDto);
					servers.add(server);
				}
				service.setServers(servers);
				logger.debug("Set servers: {}", servers);
			}
		}

		service.setHealthCheckUrl(dto.healthCheckUrl());
		logger.debug("Set healthCheckUrl: {}", dto.healthCheckUrl());

		service.setLastHealthChecked(dto.lastHealthChecked());
		logger.debug("Updated lastHealthChecked to: {}", dto.lastHealthChecked());

		service.setCreatedDttm(dto.createdDttm());
		service.setLastUpdatedDttm(dto.lastUpdatedDttm());
		service.setCreatedSource(dto.createdSource());
		service.setLastUpdatedSource(dto.lastUpdatedSource());
		logger.debug("Set timestamps and source info");
	}

	private void populateServiceFromResponseDto(ServiceResponseDto dto, Service service) {
		logger.info("Populating Service entity from ServiceResponseDto: {}", dto);
		service.setServiceName(dto.serviceName());
		logger.debug("Set serviceName: {}", dto.serviceName());

		Set<ServerResponseDto> serverResponseDtos = dto.servers();
		if (serverResponseDtos !=null && !serverResponseDtos.isEmpty()) {

			Set<Server> servers = new HashSet<>();
			for (ServerResponseDto serverResponseDto : serverResponseDtos) {
				Server server = serverMapper.toEntity(serverResponseDto);
				servers.add(server);
			}
			service.setServers(servers);
			logger.debug("Set servers: {}", servers);
		}

		service.setHealthCheckUrl(dto.healthCheckUrl());
		logger.debug("Set healthCheckUrl: {}", dto.healthCheckUrl());

		service.setLastHealthChecked(dto.lastHealthChecked());
		logger.debug("Updated lastHealthChecked to: {}", dto.lastHealthChecked());

		service.setCreatedDttm(dto.createdDttm());
		service.setLastUpdatedDttm(dto.lastUpdatedDttm());
		service.setCreatedSource(dto.createdSource());
		service.setLastUpdatedSource(dto.lastUpdatedSource());
		logger.debug("Set timestamps and source info");
	}

	public ServiceResponseDto toDto(Service entity) {
		logger.debug("Entering toDto() with entity: {}", entity);
		if (entity == null) {
			logger.warn("Received null Service entity, returning null ServiceResponseDto.");
			return null;
		}

//		Set<Server> servers = entity.getServers();
//		Set<ServerResponseDto> serverResponseDtos = new HashSet<>();
//		if (servers != null) {
//
//			if (!servers.isEmpty()) {
//				for (Iterator iterator = servers.iterator(); iterator.hasNext();) {
//					Server server = (Server) iterator.next();
//
//					ServerResponseDto serverResponseDto = serverMapper.toDto(server);
//					serverResponseDtos.add(serverResponseDto);
//
//				}
//			}
//		}

		ServiceResponseDto dto = new ServiceResponseDto(entity.getId(), null, entity.getServiceName(),
				entity.getHealthCheckUrl(), entity.getLastHealthChecked(), entity.getCreatedDttm(),
				entity.getLastUpdatedDttm(), entity.getCreatedSource(), entity.getLastUpdatedSource());

		logger.debug("Created ServiceResponseDto with values: {}", String.format("id=%s, ServiceName=%s, Servers=%s",
				entity.getId(), entity.getServiceName(), entity.getServers()));

		logger.info("Converted to ServiceResponseDto: {}", dto);
		return dto;
	}

	public void updateFromDto(ServiceRequestDto dto, Service service) {
		logger.debug("Entering updateFromDto() with dto: {}, service: {}", dto, service);
		if (dto == null || service == null) {
			logger.warn("Received null ServiceRequestDto or Service, skipping update.");
			return;
		}

		logger.info("Updating Service entity from ServiceRequestDto: {}", dto);
		populateServiceFromRequestDto(dto, service);
		logger.info("Updated Service entity: {}", service);
	}

	public ServiceRequestDto reqToDto(Service entity) {
		logger.debug("Entering reqToDto() with entity: {}", entity);
		if (entity == null) {
			logger.warn("Received null Service entity, returning null ServiceRequestDto.");
			return null;
		}

		Set<Server> servers = entity.getServers();
		Set<ServerResponseDto> serverResponseDtos = new HashSet<>();

		if (servers != null) {

			if (!servers.isEmpty()) {
				for (Iterator iterator = servers.iterator(); iterator.hasNext();) {
					Server server = (Server) iterator.next();

					ServerResponseDto serverResponseDto = serverMapper.toDto(server);
					serverResponseDtos.add(serverResponseDto);

				}
			}
		}
		ServiceRequestDto dto = new ServiceRequestDto(entity.getId(), serverResponseDtos, entity.getServiceName(),
				entity.getHealthCheckUrl(), entity.getLastHealthChecked(), entity.getCreatedDttm(),
				entity.getLastUpdatedDttm(), entity.getCreatedSource(), entity.getLastUpdatedSource());

		logger.debug("Created ServiceResponseDto with values: {}", String.format("id=%s, ServiceName=%s, Servers=%s",
				entity.getId(), entity.getServiceName(), entity.getServers()));

		logger.info("Converted to ServiceRequestDto: {}", dto);
		return dto;
	}
}