package com.pawar.todo.amt.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.HealthCheckRequestDto;
import com.pawar.app.healthcheck.dto.HealthCheckResponseDto;
import com.pawar.app.healthcheck.dto.HttpMethodsResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.model.HealthCheck;

/**
 * Mapper class for converting between HealthCheck entity and DTOs
 */
@Component
public class HealthCheckMapper {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckMapper.class);
    private final HttpMethodsMapper httpMethodsMapper;
    private final ServiceMapper serviceMapper;

    @Autowired
    public HealthCheckMapper(HttpMethodsMapper httpMethodsMapper, ServiceMapper serviceMapper) {
        this.httpMethodsMapper = httpMethodsMapper;
        this.serviceMapper = serviceMapper;
    }

    public HealthCheck toEntity(HealthCheckRequestDto dto) {
        logger.debug("Entering toEntity() with dto: {}", dto);
        if (dto == null) {
            logger.warn("Received null HealthCheckRequestDto, returning null HealthCheck entity.");
            return null;
        }

        HealthCheck healthCheck = new HealthCheck();
        populateHealthCheckFromRequestDto(dto, healthCheck);
        logger.info("Converted to HealthCheck entity: {}", healthCheck);
        return healthCheck;
    }

    public HealthCheck toEntity(HealthCheckResponseDto dto) {
        logger.debug("Entering toEntity() with dto: {}", dto);
        if (dto == null) {
            logger.warn("Received null HealthCheckResponseDto, returning null HealthCheck entity.");
            return null;
        }

        HealthCheck healthCheck = new HealthCheck();
        populateHealthCheckFromResponseDto(dto, healthCheck);
        logger.info("Converted to HealthCheck entity: {}", healthCheck);
        return healthCheck;
    }

    private void populateHealthCheckFromRequestDto(HealthCheckRequestDto dto, HealthCheck healthCheck) {
        logger.info("Populating HealthCheck entity from HealthCheckRequestDto: {}", dto);
        healthCheck.setUrl(dto.url());
        healthCheck.setOperationalPort(dto.operationalPort());
        if (dto.httpMethod() != null && dto.httpMethod().methodName() != null) {
            healthCheck.setHttpMethod(httpMethodsMapper.toEntity(dto.httpMethod()));
        }
        healthCheck.setExpectedResponse(dto.expectedResponse());
        healthCheck.setService(serviceMapper.toEntity(dto.service()));
        healthCheck.setCreatedDttm(dto.createdDttm());
        healthCheck.setLastUpdatedDttm(dto.lastUpdatedDttm());
        healthCheck.setCreatedSource(dto.createdSource());
        healthCheck.setLastUpdatedSource(dto.lastUpdatedSource());
        logger.debug("Populated HealthCheck entity: {}", healthCheck);
    }

    private void populateHealthCheckFromResponseDto(HealthCheckResponseDto dto, HealthCheck healthCheck) {
        logger.info("Populating HealthCheck entity from HealthCheckResponseDto: {}", dto);
        healthCheck.setUrl(dto.url());
        healthCheck.setOperationalPort(dto.operationalPort());
        healthCheck.setHttpMethod(httpMethodsMapper.toEntity(dto.httpMethod()));
        healthCheck.setExpectedResponse(dto.expectedResponse());
        healthCheck.setService(serviceMapper.toEntity(dto.serviceResponseDto()));
        healthCheck.setCreatedDttm(dto.createdDttm());
        healthCheck.setLastUpdatedDttm(dto.lastUpdatedDttm());
        healthCheck.setCreatedSource(dto.createdSource());
        healthCheck.setLastUpdatedSource(dto.lastUpdatedSource());
        logger.debug("Populated HealthCheck entity: {}", healthCheck);
    }

    public HealthCheckResponseDto toDto(HealthCheck entity) {
        logger.debug("Entering toDto() with entity: {}", entity);
        if (entity == null) {
            logger.warn("Received null HealthCheck entity, returning null HealthCheckResponseDto.");
            return null;
        }

        HttpMethodsResponseDto httpMethodsResponseDto = httpMethodsMapper.toDto(entity.getHttpMethod());
        ServiceResponseDto serviceResponseDto = serviceMapper.toDto(entity.getService());

        HealthCheckResponseDto dto = new HealthCheckResponseDto(
                entity.getId(),
                entity.getUrl(),
                entity.getOperationalPort(),
                httpMethodsResponseDto,
                entity.getExpectedResponse(),
                serviceResponseDto,
                entity.getCreatedDttm(),
                entity.getLastUpdatedDttm(),
                entity.getCreatedSource(),
                entity.getLastUpdatedSource());

        logger.info("Converted to HealthCheckResponseDto: {}", dto);
        return dto;
    }

    public void updateFromDto(HealthCheckRequestDto dto, HealthCheck healthCheck) {
        logger.debug("Entering updateFromDto() with dto: {}, healthCheck: {}", dto, healthCheck);
        if (dto == null || healthCheck == null) {
            logger.warn("Received null HealthCheckRequestDto or HealthCheck, skipping update.");
            return;
        }

        logger.info("Updating HealthCheck entity from HealthCheckRequestDto: {}", dto);
        populateHealthCheckFromRequestDto(dto, healthCheck);
        logger.info("Updated HealthCheck entity: {}", healthCheck);
    }
}