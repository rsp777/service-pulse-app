package com.pawar.todo.amt.mapper;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.AgentRequestDto;
import com.pawar.app.healthcheck.dto.AgentResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.converter.AgentStatusConverter;
import com.pawar.todo.amt.exceptions.ServerOperationException;
import com.pawar.todo.amt.model.Agent;
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.service.ServerService;

/**
 * Mapper class for converting between Server entity and DTOs
 */
@Component
public class AgentMapper {

	private static final Logger logger = LoggerFactory.getLogger(AgentMapper.class);
	private final AgentStatusConverter agentStatusConverter;
	private ServerMapper serverMapper;
	
	@Autowired
	private ServerService serverService;

	public AgentMapper(AgentStatusConverter agentStatusConverter) {
		this.agentStatusConverter = agentStatusConverter;
	}
	
	public void setServerMapper(ServerMapper serverMapper) {
		this.serverMapper = serverMapper;
	}

	public Agent toEntity(AgentRequestDto dto) {
		logger.debug("Entering toEntity() with dto: {}", dto);
		if (dto == null) {
			logger.warn("Received null AgentRequestDto, returning null Agent entity.");
			return null;
		}

		Agent agent = new Agent();
		populateAgentFromDto(dto, agent);
		logger.info("Converted to Agent entity: {}", agent);
		return agent;
	}

	public Agent toEntity(AgentResponseDto dto) {
		logger.debug("Entering toEntity() with dto: {}", dto);
		if (dto == null) {
			logger.warn("Received null AgentResponseDto, returning null Agent entity.");
			return null;
		}

		Agent agent = new Agent();
		populateAgentFromDto(dto, agent);
		agent.setCreatedDttm(LocalDateTime.now());
		agent.setLastUpdatedDttm(LocalDateTime.now());
		logger.info("Converted to Agent entity: {}", agent);
		return agent;
	}

	private void populateAgentFromDto(AgentRequestDto dto, Agent agent) {
		logger.info("Populating Agent entity from AgentRequestDto: {}", dto);
		agent.setName(dto.name());
		agent.setHost(dto.host());
		agent.setPort(dto.port());
		agent.setStatus(agentStatusConverter.toEnum(dto.status()));
		agent.setJarFilePath(dto.jarFilePath());
		agent.setJarVersion(dto.jarVersion());
		agent.setAgentWebSocketUrl(dto.agentWebSocketUrl());
		agent.setCreatedSource(dto.createdSource());
		agent.setLastUpdatedSource(dto.lastUpdatedSource());
		logger.debug("Populated Agent entity: {}", agent);
	}

	private void populateAgentFromDto(AgentResponseDto dto, Agent agent) {
		logger.info("Populating Agent entity from AgentResponseDto: {}", dto);
		agent.setName(dto.name());
		agent.setHost(dto.host());
		agent.setPort(dto.port());
		agent.setStatus(agentStatusConverter.toEnum(dto.status()));
		agent.setJarFilePath(dto.jarFilePath());
		agent.setJarVersion(dto.jarVersion());
		agent.setLastDeployment(dto.lastDeployment());
		agent.setCreatedSource(dto.createdSource());
		agent.setLastUpdatedSource(dto.lastUpdatedSource());
		logger.debug("Populated Agent entity: {}", agent);
	}

	public AgentResponseDto toDto(Agent entity) throws ServerOperationException {
		logger.debug("Entering toDto() with entity: {}", entity);
		if (entity == null) {
			logger.warn("Received null Agent entity, returning null AgentResponseDto.");
			return null;
		}
	 Optional<ServerResponseDto> serverResponseDto = serverService.findServerById(entity.getServer().getId());
		if (serverResponseDto !=null && !serverResponseDto.isEmpty()) {
			AgentResponseDto dto = new AgentResponseDto(
					entity.getId(),
					entity.getName(),
					entity.getHost(),
					entity.getPort(),
					entity.getStatus().name(),
					entity.getAgentWebSocketUrl(),
					serverResponseDto.get(),
					entity.getJarFilePath(),
					entity.getJarVersion(),
					entity.getLastDeployment(),
					entity.getLastHeartbeat(),
					entity.getCreatedDttm(),
					entity.getLastUpdatedDttm(),
					entity.getCreatedSource(),
					entity.getLastUpdatedSource());
			logger.info("Converted to AgentResponseDto: {}", dto);
			return dto;

		}
		return null;
		
	}

	public void updateFromDto(AgentRequestDto dto, Agent agent) {
		logger.debug("Entering updateFromDto() with dto: {}, agent: {}", dto, agent);
		if (dto == null || agent == null) {
			logger.warn("Received null AgentRequestDto or Agent, skipping update.");
			return;
		}

		populateAgentFromDto(dto, agent);
		agent.setLastUpdatedDttm(LocalDateTime.now());
		logger.info("Updated Agent entity: {}", agent);
	}

	public void updateFromDto(AgentResponseDto dto, Agent agent) {
		logger.debug("Entering updateFromDto() with dto: {}, agent: {}", dto, agent);
		if (dto == null || agent == null) {
			logger.warn("Received null AgentResponseDto or Agent, skipping update.");
			return;
		}

		populateAgentFromDto(dto, agent);
		agent.setLastUpdatedDttm(LocalDateTime.now());
		logger.info("Updated Agent entity: {}", agent);
	}
}
