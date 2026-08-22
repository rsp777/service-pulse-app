package com.pawar.todo.amt.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;

import com.pawar.app.healthcheck.dto.AgentRequestDto;
import com.pawar.app.healthcheck.dto.AgentResponseDto;
import com.pawar.todo.amt.cache.AgentCache;
import com.pawar.todo.amt.constants.AgentStatus;
import com.pawar.todo.amt.exceptions.AgentOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServerOperationException;
import com.pawar.todo.amt.mapper.AgentMapper;
import com.pawar.todo.amt.model.Agent;
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.respository.AgentRepository;
import com.pawar.todo.amt.respository.ServerRepository;
import com.pawar.todo.amt.ssh.SshCommandService;

@Service
public class AgentServiceImpl implements AgentService {

	private static final Logger logger = LoggerFactory.getLogger(AgentServiceImpl.class);

	private final AgentRepository agentRepository;
	private final ServerRepository serverRepository;
	private AgentMapper agentMapper;
	private final AgentCache agentCache;
	private final SshCommandService sshCommandService;

	public AgentServiceImpl(AgentRepository agentRepository, ServerRepository serverRepository,
			AgentMapper agentMapper, AgentCache agentCache, SshCommandService sshCommandService) {
		this.agentRepository = agentRepository;
		this.serverRepository = serverRepository;
		this.agentMapper = agentMapper;
		this.agentCache = agentCache;
		this.sshCommandService = sshCommandService;
	}

	@Autowired
	public void setAgentMapper(AgentMapper agentMapper) {
		this.agentMapper = agentMapper;

	}
	
	@Override
	@Transactional
	public AgentResponseDto createAgent(AgentRequestDto request) throws AgentOperationException {
		LocalDateTime now = LocalDateTime.now();
		try {
			logger.info("Creating new Agent: {}", request.name());
			Agent agent = agentMapper.toEntity(request);
			setServerForAgent(request, agent);
			agent.setCreatedDttm(now);
			agent.setLastUpdatedDttm(now);

			Agent savedAgent = agentRepository.save(agent);
			logger.debug("Agent created successfully: ID={}", savedAgent.getId());

			return agentMapper.toDto(savedAgent);
		} catch (Exception e) {
			logger.error("Failed to create agent: ", e);
			throw new AgentOperationException("Failed to create agent", e);
		}
	}

	private void setServerForAgent(AgentRequestDto request, Agent agent) throws ResourceNotFoundException {
		if (request.server() != null) {
			Server server = serverRepository.findById(request.server().id())
					.orElseThrow(
							() -> new ResourceNotFoundException("Server not found with ID: " + request.server().id()));
			agent.setServer(server);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AgentResponseDto> findAgentById(Integer id) throws AgentOperationException {

		logger.debug("Fetching agent by ID: {}", id);
		return agentCache.get(id).or(() -> {
			try {
				return fetchAgentById(id);
			} catch (ResourceNotFoundException | ServerOperationException e) {

				e.printStackTrace();
				return Optional.empty();

			}
		});

	}

	private Optional<AgentResponseDto> fetchAgentById(Integer id) throws ResourceNotFoundException, ServerOperationException {
		Agent agent = agentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Agent not found with ID: " + id));
		AgentResponseDto dto = agentMapper.toDto(agent);
		logger.info("Agent found: {}", dto);
		return Optional.of(dto);
	}

	@Override
	public Optional<AgentResponseDto> findAgentByServerId(Integer serverId) throws AgentOperationException {
		try {
			logger.debug("Fetching agent by server ID: {}", serverId);
			return agentCache.get(serverId).or(() -> {
				Agent agent;
				try {
					agent = agentRepository.findAgentByServerId(serverId)
							.orElseThrow(
									() -> new ResourceNotFoundException("Agent not found for server ID: " + serverId));
					AgentResponseDto dto = agentMapper.toDto(agent);
					logger.info("Agent found: {}", dto);
					return Optional.of(dto);
				} catch (ResourceNotFoundException | ServerOperationException e) {

					e.printStackTrace();
					return Optional.empty();
				}

			});
		} catch (Exception e) {
			logger.error("Error fetching agent for server ID: {}", serverId, e);
			throw new AgentOperationException("Failed to fetch agent", e);
		}
	}

	@Override
	@Async
	@Transactional(readOnly = true)
	public CompletableFuture<List<AgentResponseDto>> findAllAgentsAsync() {
		try {
			logger.info("Async fetching of all agents initiated");
			List<Agent> agents = agentRepository.findAll();
			List<AgentResponseDto> response = (List<AgentResponseDto>) agents.stream()
				    .peek(agent -> logger.debug("Processing agent: {}", agent.getId()))
				    .map(agent -> {
				        try {
				            return Optional.of(agentMapper.toDto(agent));
				        } catch (ServerOperationException e) {
				            logger.error("Error converting agent with ID {}: {}", agent.getId(), e.getMessage());
				            return Optional.empty(); // Return an empty Optional if an exception occurs
				        }
				    })
				    .flatMap(Optional::stream) // Flatten the stream of Optionals
				    .collect(Collectors.toList());

			logger.info("Async agent fetch completed successfully");
			return CompletableFuture.completedFuture(response);
		}
		
		catch (Exception e) {
			logger.error("Async agent fetch failed", e);
			throw new CompletionException("Failed to fetch agents asynchronously", e);
		} 
	}

	@Override
	@Transactional
	public AgentResponseDto updateAgent(Integer id, AgentRequestDto request) throws AgentOperationException {
		LocalDateTime now = LocalDateTime.now();
		try {
			logger.info("Updating agent ID: {}", id);
			Agent agent = agentRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Agent not found with ID: " + id));

			agentMapper.updateFromDto(request, agent);
			agent.setLastUpdatedDttm(now);

			Agent updatedAgent = agentRepository.save(agent);
			AgentResponseDto dto = agentMapper.toDto(updatedAgent);
			agentCache.evict(id);
			agentCache.put(id, dto);
			logger.debug("Agent updated successfully: ID={}", id);

			return dto;
		} catch (Exception e) {
			logger.error("Failed to update agent ID: {}", id, e);
			throw new AgentOperationException("Failed to update agent", e);
		}
	}

	@Override
	@Async
	@Transactional
	public ListenableFuture<Void> deleteAgentAsync(Integer id) throws AgentOperationException {
		try {
			logger.info("Async deletion initiated for agent ID: {}", id);
			agentRepository.deleteById(id);
			agentCache.evict(id);
			logger.debug("Async deletion completed for agent ID: {}", id);
			return new AsyncResult<>(null);
		} catch (Exception e) {
			logger.error("Async deletion failed for agent ID: {}", id, e);
			throw new AgentOperationException("Failed to delete agent asynchronously", e);
		}
	}

	@Override
	public List<AgentResponseDto> findAgentsByStatus(AgentStatus status) throws AgentOperationException {
		try {
			logger.debug("Fetching agents with status: {}", status.name());
			List<AgentResponseDto> dtos = (List<AgentResponseDto>) agentRepository.findByStatus(status).stream()
//			Stream<Agent> stream = agentRepository.findByStatus(status).stream()
			.map(agent -> {
                try {
                    return agentMapper.toDto(agent);
                } catch (ServerOperationException e) {
                    logger.error("Error converting agent with ID {}: {}", agent.getId(), e.getMessage());
                    return null; // Return null if an exception occurs
                }
            });

			return dtos;
		} catch (Exception e) {
			logger.error("Error fetching agents by status: {}", status, e);
			throw new AgentOperationException("Failed to fetch agents by status", e);
		}
	}

	@Async
	// @Scheduled(cron = "*/60 * * * * *")
	@Transactional
	public void periodicAgentHealthStatusCheck() throws AgentOperationException {
		try {
			List<AgentResponseDto> agents = findAllAgentsAsync().get();
			for (AgentResponseDto agentDto : agents) {
				boolean connectionStatus = agentDto.server() != null && sshCommandService.isReachable(agentDto.server());
				Agent existingAgent = agentRepository.findById(agentDto.id())
						.orElseThrow(() -> new ResourceNotFoundException("Agent not found with ID: " + agentDto.id()));

				logger.info("Connection status: {} for agent ID: {}", connectionStatus, existingAgent.getId());
				agentMapper.updateFromDto(agentDto, existingAgent);

				if (connectionStatus && existingAgent.getStatus().equals(AgentStatus.OFFLINE)) {
					existingAgent.setStatus(AgentStatus.ONLINE);
					logger.info("Agent name: {}, Status: {}", agentDto.name(), AgentStatus.ONLINE);
				} else if (!connectionStatus && existingAgent.getStatus().equals(AgentStatus.ONLINE)) {
					existingAgent.setStatus(AgentStatus.OFFLINE);
					logger.warn("Agent name: {}, Status: {}", agentDto.name(), AgentStatus.OFFLINE);
				}
				agentRepository.save(existingAgent);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error during periodic agent health status check", e);
			throw new AgentOperationException("Failed to check agent health status", e);
		}
	}

	@Override
	public boolean checkAgentStatus(Integer id) throws AgentOperationException {
		AgentResponseDto agent = findAgentById(id)
				.orElseThrow(() -> new AgentOperationException("Agent not found with ID: " + id));
		return agent.server() != null && sshCommandService.isReachable(agent.server());
	}
}