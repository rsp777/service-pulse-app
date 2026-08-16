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

import com.pawar.app.healthcheck.dto.HealthCheckRequestDto;
import com.pawar.app.healthcheck.dto.HealthCheckResponseDto;
import com.pawar.todo.amt.cache.HealthCheckCache;
import com.pawar.todo.amt.exceptions.HealthCheckOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.mapper.HealthCheckMapper;
import com.pawar.todo.amt.model.HealthCheck;
import com.pawar.todo.amt.respository.HealthCheckRepository;

@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckServiceImpl.class);

    private final HealthCheckRepository healthCheckRepository;
    private final HealthCheckMapper healthCheckMapper;
    private final HealthCheckCache healthCheckCache;

    public HealthCheckServiceImpl(HealthCheckRepository healthCheckRepository,
            HealthCheckMapper healthCheckMapper,
            HealthCheckCache healthCheckCache) {
        this.healthCheckRepository = healthCheckRepository;
        this.healthCheckMapper = healthCheckMapper;
        this.healthCheckCache = healthCheckCache;
    }

    @Override
    @Transactional
    public HealthCheckResponseDto createHealthCheck(HealthCheckRequestDto request)
            throws HealthCheckOperationException {
        LocalDateTime now = LocalDateTime.now();
        try {
            logger.info("Creating new HealthCheck: {}", request.url());
            HealthCheck healthCheck = healthCheckMapper.toEntity(request);
            healthCheck.setCreatedDttm(now);
            healthCheck.setLastUpdatedDttm(now);
            HealthCheck savedHealthCheck = healthCheckRepository.save(healthCheck);
            logger.debug("HealthCheck created successfully: ID={}", savedHealthCheck.getId());

            return healthCheckMapper.toDto(savedHealthCheck);
        } catch (Exception e) {
            logger.error("Failed to create healthCheck: ", e);
            throw new HealthCheckOperationException("Failed to create healthCheck", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HealthCheckResponseDto> findHealthCheckById(Integer id) throws HealthCheckOperationException {
        try {
            logger.debug("Fetching healthCheck by ID: {}", id);
            return healthCheckCache.get(id).or(() -> {
                try {
                    return fetchHealthCheckById(id);
                } catch (ResourceNotFoundException e) {
                    e.printStackTrace();
                }
                return Optional.empty();
            });
        } catch (Exception e) {
            logger.error("Error fetching healthCheck with ID: {}", id, e);
            throw new HealthCheckOperationException("Failed to fetch healthCheck", e);
        }
    }

    private Optional<HealthCheckResponseDto> fetchHealthCheckById(Integer id) throws ResourceNotFoundException {
        HealthCheck healthCheck = healthCheckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HealthCheck not found with ID: " + id));
        HealthCheckResponseDto dto = healthCheckMapper.toDto(healthCheck);
        healthCheckCache.put(id, dto);
        logger.info("HealthCheck found: {}", dto);
        return Optional.of(dto);
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<HealthCheckResponseDto>> findAllHealthChecksAsync() {
        try {
            logger.info("Async fetching of all healthChecks initiated");
            List<HealthCheck> healthChecks = healthCheckRepository.findAll();
            List<HealthCheckResponseDto> response = healthChecks.stream()
                    .peek(s -> logger.debug("Processing healthCheck: {}", s.getId()))
                    .map(healthCheckMapper::toDto)
                    .collect(Collectors.toList());

            logger.info("Async healthCheck fetch completed successfully");
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            logger.error("Async healthCheck fetch failed", e);
            throw new CompletionException("Failed to fetch healthChecks asynchronously", e);
        }
    }

    @Override
    @Transactional
    public HealthCheckResponseDto updateHealthCheck(Integer id, HealthCheckRequestDto healthCheckRequestDto)
            throws HealthCheckOperationException {
        LocalDateTime now = LocalDateTime.now();
        try {
            logger.info("Updating healthCheck ID: {}", id);
            HealthCheck healthCheck = healthCheckRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("HealthCheck not found with ID: " + id));

            healthCheckMapper.updateFromDto(healthCheckRequestDto, healthCheck);
            healthCheck.setLastUpdatedDttm(now);

            HealthCheck updatedHealthCheck = healthCheckRepository.save(healthCheck);
            healthCheckCache.evict(id);
            logger.debug("HealthCheck updated successfully: ID={}", id);

            return healthCheckMapper.toDto(updatedHealthCheck);
        } catch (Exception e) {
            logger.error("Failed to update HealthCheck ID: {}", id, e);
            throw new HealthCheckOperationException("Failed to update healthCheck", e);
        }
    }

    @Override
    @Async
    @Transactional
    public ListenableFuture<Void> deleteHealthCheckAsync(Integer id) throws HealthCheckOperationException {
        try {
            logger.info("Async deletion initiated for healthCheck ID: {}", id);
            healthCheckRepository.deleteById(id);
            healthCheckCache.evict(id);
            logger.debug("Async deletion completed for healthCheck ID: {}", id);
            return new AsyncResult<>(null);
        } catch (Exception e) {
            logger.error("Async deletion failed for healthCheck ID: {}", id, e);
            throw new HealthCheckOperationException("Failed to delete healthCheck asynchronously", e);
        }
    }
}
