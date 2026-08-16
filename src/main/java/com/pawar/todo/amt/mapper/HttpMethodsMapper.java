package com.pawar.todo.amt.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.HttpMethodsRequestDto;
import com.pawar.app.healthcheck.dto.HttpMethodsResponseDto;
import com.pawar.todo.amt.converter.HttpMethodNameConverter;
import com.pawar.todo.amt.model.HttpMethods;

/**
 * Mapper class for converting between HttpMethods entity and DTOs
 */
@Component
public class HttpMethodsMapper {

    private static final Logger logger = LoggerFactory.getLogger(HttpMethodsMapper.class);
    private final HttpMethodNameConverter httpMethodNameConverter;

    @Autowired
    public HttpMethodsMapper(HttpMethodNameConverter httpMethodNameConverter) {
        this.httpMethodNameConverter = httpMethodNameConverter;
    }

    public HttpMethods toEntity(HttpMethodsRequestDto dto) {
        logger.debug("Entering toEntity() with dto: {}", dto);
        if (dto == null) {
            logger.warn("Received null HttpMethodsRequestDto, returning null HttpMethods entity.");
            return null;
        }

        HttpMethods httpMethods = new HttpMethods();
        populateHttpMethodsFromDto(dto, httpMethods);
        logger.info("Converted to HttpMethods entity: {}", httpMethods);
        return httpMethods;
    }

    public HttpMethods toEntity(HttpMethodsResponseDto dto) {
        logger.debug("Entering toEntity() with dto: {}", dto);
        if (dto == null) {
            logger.warn("Received null HttpMethodsResponseDto, returning null HttpMethods entity.");
            return null;
        }

        HttpMethods httpMethods = new HttpMethods();
        populateHttpMethodsFromDto(dto, httpMethods);
        logger.info("Converted to HttpMethods entity: {}", httpMethods);
        return httpMethods;
    }

    private void populateHttpMethodsFromDto(HttpMethodsRequestDto dto, HttpMethods httpMethods) {
        logger.info("Populating HttpMethods entity from HttpMethodsRequestDto: {}", dto);
        httpMethods.setMethodName(httpMethodNameConverter.toEnum(dto.methodName()));
        logger.debug("Set methodName: {}", dto.methodName());

        httpMethods.setAllowedInHealthCheck(dto.allowedInHealthCheck());
        logger.debug("Set allowedInHealthCheck: {}", dto.allowedInHealthCheck());

        httpMethods.setCreatedDttm(dto.createdDttm());
        httpMethods.setLastUpdatedDttm(dto.lastUpdatedDttm());
        httpMethods.setCreatedSource(dto.createdSource());
        httpMethods.setLastUpdatedSource(dto.lastUpdatedSource());
        logger.debug("Set timestamps and source info");
    }

    private void populateHttpMethodsFromDto(HttpMethodsResponseDto dto, HttpMethods httpMethods) {
        logger.info("Populating HttpMethods entity from HttpMethodsResponseDto: {}", dto);
        httpMethods.setMethodName(httpMethodNameConverter.toEnum(dto.methodName()));
        logger.debug("Set methodName: {}", dto.methodName());

        httpMethods.setAllowedInHealthCheck(dto.allowedInHealthCheck());
        logger.debug("Set allowedInHealthCheck: {}", dto.allowedInHealthCheck());

        httpMethods.setCreatedDttm(dto.createdDttm());
        httpMethods.setLastUpdatedDttm(dto.lastUpdatedDttm());
        httpMethods.setCreatedSource(dto.createdSource());
        httpMethods.setLastUpdatedSource(dto.lastUpdatedSource());
        logger.debug("Set timestamps and source info");
    }

    public HttpMethodsResponseDto toDto(HttpMethods entity) {
        logger.debug("Entering toDto() with entity: {}", entity);
        if (entity == null) {
            logger.warn("Received null HttpMethods entity, returning null HttpMethodsResponseDto.");
            return null;
        }

        logger.info("Converting HttpMethods entity to HttpMethodsResponseDto: {}", entity);

        HttpMethodsResponseDto dto = new HttpMethodsResponseDto(
                entity.getId(),
                entity.getMethodName().name(),
                entity.isAllowedInHealthCheck(),
                entity.getCreatedDttm(),
                entity.getLastUpdatedDttm(),
                entity.getCreatedSource(),
                entity.getLastUpdatedSource());

        logger.debug("Created HttpMethodsResponseDto with values: {}",
                String.format(
                        "id=%s, methodName=%s, isAllowedInHealthCheck=%s",
                        entity.getId(),
                        entity.getMethodName().name(),
                        entity.isAllowedInHealthCheck()));

        logger.info("Converted to HttpMethodsResponseDto: {}", dto);
        return dto;
    }

    public void updateFromDto(HttpMethodsRequestDto dto, HttpMethods httpMethods) {
        logger.debug("Entering updateFromDto() with dto: {}, httpMethods: {}", dto, httpMethods);
        if (dto == null || httpMethods == null) {
            logger.warn("Received null HttpMethodsRequestDto or HttpMethods, skipping update.");
            return;
        }

        logger.info("Updating HttpMethods entity from HttpMethodsRequestDto: {}", dto);
        populateHttpMethodsFromDto(dto, httpMethods);
        logger.info("Updated HttpMethods entity: {}", httpMethods);
    }
}