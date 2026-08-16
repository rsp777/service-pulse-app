package com.pawar.todo.amt.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceHealthStatusRequestDto;
import com.pawar.app.healthcheck.dto.ServiceHealthStatusResponseDto;
import com.pawar.app.healthcheck.dto.ServiceRequestDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.converter.HealthCheckStatusConverter;
import com.pawar.todo.amt.model.ServiceHealthStatus;

@Component
public class ServiceHealthStatusMapper {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceHealthStatusMapper.class);
    private final HealthCheckStatusConverter healthCheckStatusConverter;
    private final ServiceMapper serviceMapper;
    private final ServerMapper serverMapper;

    @Autowired
    public ServiceHealthStatusMapper(ServerMapper serverMapper, ServiceMapper serviceMapper, HealthCheckStatusConverter healthCheckStatusConverter) {
        this.serverMapper = serverMapper;
        this.serviceMapper = serviceMapper;
        this.healthCheckStatusConverter = healthCheckStatusConverter;
    }

    public ServiceHealthStatus toEntity(ServiceHealthStatusRequestDto dto) {
        logger.debug("Entering toEntity() with dto: {}", dto);
        if (dto == null) {
            logger.warn("Received null ServiceHealthStatusRequestDto, returning null ServiceHealthStatus entity.");
            return null;
        }

        ServiceHealthStatus serviceHealthStatus = new ServiceHealthStatus();
        populateServiceHealthStatusFromRequestDto(dto, serviceHealthStatus);
        logger.info("Converted to ServiceHealthStatus entity: {}", serviceHealthStatus);
        return serviceHealthStatus;
    }

    public ServiceHealthStatus toEntity(ServiceHealthStatusResponseDto dto) {
        logger.debug("Entering toEntity() with dto: {}", dto);
        if (dto == null) {
            logger.warn("Received null ServiceHealthStatusResponseDto, returning null ServiceHealthStatus entity.");
            return null;
        }

        ServiceHealthStatus serviceHealthStatus = new ServiceHealthStatus();
        populateServiceHealthStatusFromResponseDto(dto, serviceHealthStatus);
        logger.info("Converted to ServiceHealthStatus entity: {}", serviceHealthStatus);
        return serviceHealthStatus;
    }

    private void populateServiceHealthStatusFromRequestDto(ServiceHealthStatusRequestDto dto, ServiceHealthStatus serviceHealthStatus) {
        logger.info("Populating ServiceHealthStatus entity from ServiceHealthStatusRequestDto: {}", dto);
        serviceHealthStatus.setService(serviceMapper.toEntity(dto.service()));
        logger.debug("Set service: {}", dto.service());

        serviceHealthStatus.setStatus(healthCheckStatusConverter.toEnum(dto.status()));
        logger.debug("Set status: {}", dto.status());

        serviceHealthStatus.setResponseTime(dto.responseTime());
        logger.debug("Set responseTime: {}", dto.responseTime());

        serviceHealthStatus.setErrorMessage(dto.errorMessage());
        logger.debug("Set errorMessage: {}", dto.errorMessage());

        serviceHealthStatus.setCreatedDttm(dto.createdDttm());
        serviceHealthStatus.setLastUpdatedDttm(dto.lastUpdatedDttm());
        serviceHealthStatus.setCreatedSource(dto.createdSource());
        serviceHealthStatus.setLastUpdatedSource(dto.lastUpdatedSource());
        logger.debug("Set timestamps and source info");
    }

    private void populateServiceHealthStatusFromResponseDto(ServiceHealthStatusResponseDto dto, ServiceHealthStatus serviceHealthStatus) {
        logger.info("Populating ServiceHealthStatus entity from ServiceHealthStatusResponseDto: {}", dto);
        serviceHealthStatus.setService(serviceMapper.toEntity(dto.service()));
        logger.debug("Set service: {}", dto.service());

        serviceHealthStatus.setStatus(healthCheckStatusConverter.toEnum(dto.status()));
        logger.debug("Set status: {}", dto.status());

        serviceHealthStatus.setResponseTime(dto.responseTime());
        logger.debug("Set responseTime: {}", dto.responseTime());

        serviceHealthStatus.setErrorMessage(dto.errorMessage());
        logger.debug("Set errorMessage: {}", dto.errorMessage());

        serviceHealthStatus.setCreatedDttm(dto.createdDttm());
        serviceHealthStatus.setLastUpdatedDttm(dto.lastUpdatedDttm());
        serviceHealthStatus.setCreatedSource(dto.createdSource());
        serviceHealthStatus.setLastUpdatedSource(dto.lastUpdatedSource());
        logger.debug("Set timestamps and source info");
    }

    public ServiceHealthStatusResponseDto toDto(ServiceHealthStatus entity) {
        logger.debug("Entering toDto() with entity: {}", entity);
        if (entity == null) {
            logger.warn("Received null ServiceHealthStatus entity, returning null ServiceHealthStatusResponseDto.");
            return null;
        }

        ServiceResponseDto serviceResponseDto = serviceMapper.toDto(entity.getService());

        ServiceHealthStatusResponseDto dto = new ServiceHealthStatusResponseDto(
            entity.getId(),
            serviceResponseDto,
            entity.getStatus().name(),
            entity.getTimestamp(),
            entity.getResponseTime(),
            entity.getErrorMessage(),
            entity.getCreatedDttm(),
            entity.getLastUpdatedDttm(),
            entity.getCreatedSource(),
            entity.getLastUpdatedSource()
        );

        logger.debug("Created ServiceHealthStatusResponseDto with values: {}", 
            String.format("id=%s, service=%s, status=%s",
                entity.getId(), entity.getService(), entity.getStatus()));

        logger.info("Converted to ServiceHealthStatusResponseDto: {}", dto);
        return dto;
    }

    public void updateFromDto(ServiceHealthStatusRequestDto dto, ServiceHealthStatus serviceHealthStatus) {
        logger.debug("Entering updateFromDto() with dto: {}, ServiceHealthStatus: {}", dto, serviceHealthStatus);
        if (dto == null || serviceHealthStatus == null) {
            logger.warn("Received null ServiceHealthStatusRequestDto or ServiceHealthStatus, skipping update.");
            return;
        }

        logger.info("Updating ServiceHealthStatus entity from ServiceHealthStatusRequestDto: {}", dto);
        populateServiceHealthStatusFromRequestDto(dto, serviceHealthStatus);
        logger.info("Updated ServiceHealthStatus entity: {}", serviceHealthStatus);
    }
}