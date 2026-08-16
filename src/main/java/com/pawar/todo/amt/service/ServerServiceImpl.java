package com.pawar.todo.amt.service;

import java.time.LocalDateTime;
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

import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.cache.ServerCache;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.exceptions.ResourceAlreadyExistsException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServerOperationException;
import com.pawar.todo.amt.mapper.ServerMapper;
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.respository.ServerRepository;
import com.pawar.todo.amt.validator.ServerStatusValidator;


@Service
public class ServerServiceImpl implements ServerService{

    private final static Logger logger = LoggerFactory.getLogger(ServerServiceImpl.class.getName());

    private final ServerRepository serverRepository;
    private final ServerMapper serverMapper;
    private final ServerStatusValidator serverStatusValidator;
    private final ServerCache serverCache;

	public ServerServiceImpl(ServerRepository serverRepository,
                        ServerMapper serverMapper,
                        ServerStatusValidator serverStatusValidator,
                        ServerCache serverCache) {
        this.serverRepository = serverRepository;
        this.serverMapper = serverMapper;
        this.serverStatusValidator = serverStatusValidator;
        this.serverCache = serverCache;
    }

	@Override
    @Transactional
    public ServerResponseDto createServer(ServerRequestDto serverRequestDto) throws ServerOperationException {
        try {
            logger.info("Creating new server: {}", serverRequestDto.hostname());
            validateServerRequest(serverRequestDto);
            
            Server server = serverMapper.toEntity(serverRequestDto);
            server.setCreatedDttm(LocalDateTime.now());
            server.setLastUpdatedDttm(LocalDateTime.now());
            
            Server savedServer = serverRepository.save(server);
            logger.debug("Server created successfully: ID={}", savedServer.getId());
            
            return serverMapper.toDto(savedServer);
        } catch (Exception e) {
            logger.error("Failed to create server: ", e);
            throw new ServerOperationException("Failed to create server", e);
        }
    }

	@Override
    @Transactional(readOnly = true)
    public Optional<ServerResponseDto> findServerById(Integer id) throws ServerOperationException {
        try {
            logger.debug("Fetching server by ID: {}", id);
            
            return serverCache.get(id)
                .or(() -> {
                    try {
                        Server server = serverRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Server not found with ID: " + id));
                        ServerResponseDto dto = serverMapper.toDto(server);
                        serverCache.put(id, dto);
                        return Optional.of(dto);
                    } catch (ResourceNotFoundException e) {
                        logger.error("Error fetching server with ID: {}", id, e);
                        return Optional.empty(); // Return empty if not found
                    }
                });
        } catch (Exception e) {
            logger.error("Error fetching server with ID: {}", id, e);
            throw new ServerOperationException("Failed to fetch server", e);
        }
    }


	@Override
    @Async("serverTaskExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<List<ServerResponseDto>> findAllServersAsync() {
        try {
            logger.info("Async fetching of all servers initiated");
            
            List<Server> servers = serverRepository.findAll();
            List<ServerResponseDto> response = servers.stream()
                .peek(s -> logger.debug("Processing server: {}", s.getId()))
                .map(serverMapper::toDto)
                .toList();
            
            logger.info("Async server fetch completed successfully");
            return CompletableFuture.completedFuture(response);
        } catch (NullPointerException e) {
            logger.error("Async server fetch failed, Server not found", e);
            throw new CompletionException("Failed to fetch servers asynchronously, Server not found", e);
        } 
        catch (Exception e) {
            logger.error("Async server fetch failed", e);
            throw new CompletionException("Failed to fetch servers asynchronously", e);
        }
    }

	@Override
    @Transactional
    public ServerResponseDto updateServer(Integer id, ServerRequestDto serverRequestDto) throws ServerOperationException {
        try {
            logger.info("Updating server ID: {}", id);
            
            Server server = serverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Server not found with ID: " + id));
            
            serverMapper.updateFromDto(serverRequestDto, server);
            server.setLastUpdatedDttm(LocalDateTime.now());
            
            Server updatedServer = serverRepository.save(server);
            serverCache.evict(id);
            logger.debug("Server updated successfully: ID={}", id);
            
            return serverMapper.toDto(updatedServer);
        } catch (Exception e) {
            logger.error("Failed to update server ID: {}", id, e);
            throw new ServerOperationException("Failed to update server", e);
        }
    }

	@Override
    @Async
    @Transactional
    public ListenableFuture<Void> deleteServerAsync(Integer id) throws ServerOperationException {
        try {
            logger.info("Async deletion initiated for server ID: {}", id);
            
            serverRepository.deleteById(id);
            serverCache.evict(id);
            logger.debug("Async deletion completed for server ID: {}", id);
            
            return new AsyncResult<>(null);
        } catch (Exception e) {
            logger.error("Async deletion failed for server ID: {}", id, e);
            throw new ServerOperationException("Failed to delete server asynchronously", e);
        }
    }

	@Override
	@Transactional
    public List<ServerResponseDto> findServersByStatus(ServerStatus status) throws ServerOperationException {
        try {
            logger.debug("Fetching servers with status: {}", status.name());
            
            ServerStatus serverStatus = ServerStatus.valueOf(status.name().toUpperCase());
            
            List<ServerResponseDto> dtos = serverRepository.findByStatus(serverStatus)
            		.stream()
            	    .map(serverMapper::toDto)
            	    .collect(Collectors.toList());
            
            return  dtos;
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid server status requested: {}", status);
            throw new IllegalArgumentException("Invalid server status: " + status);
        } catch (Exception e) {
            logger.error("Error fetching servers by status: {}", status, e);
            throw new ServerOperationException("Failed to fetch servers by status", e);
        }
    }

    private void validateServerRequest(ServerRequestDto dto) throws ResourceAlreadyExistsException {
        serverStatusValidator.validate(dto.status());
        
        if (serverRepository.existsByHostname(dto.hostname())) {
            throw new ResourceAlreadyExistsException("Server with hostname '" + dto.hostname() + "' already exists");
        }
        
        if (serverRepository.existsByIpAddress(dto.ipAddress())) {
            throw new ResourceAlreadyExistsException("Server with IP address '" + dto.ipAddress() + "' already exists");
        }
    }
}
