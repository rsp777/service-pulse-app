// Issue #28: Comprehensive unit tests for ServiceMapper and ServiceResponseDto serviceType mapping [Author: Ravindra Singh Pawar / Antigravity]
package com.pawar.todo.amt.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.pawar.app.healthcheck.dto.ServiceRequestDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.model.Service;

class ServiceMapperTest {

    private ServiceMapper serviceMapper;

    @BeforeEach
    void setUp() {
        serviceMapper = new ServiceMapper();
    }

    @Test
    @DisplayName("+ve: toDto maps serviceType correctly from Service entity")
    void toDto_mapsServiceType_success() {
        Service entity = new Service();
        entity.setId(101);
        entity.setServiceName("inventory-service");
        entity.setServiceType("SPRING_BOOT");

        ServiceResponseDto dto = serviceMapper.toDto(entity);

        assertNotNull(dto);
        assertEquals(101, dto.id());
        assertEquals("inventory-service", dto.serviceName());
        assertEquals("SPRING_BOOT", dto.serviceType());
    }

    @Test
    @DisplayName("+ve: toEntity maps serviceType correctly from ServiceResponseDto")
    void toEntity_fromResponseDto_mapsServiceType() {
        ServiceResponseDto dto = new ServiceResponseDto(202, null, "worker-node", "SYSTEMD", null, null, null, null);

        Service entity = serviceMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals("worker-node", entity.getServiceName());
        assertEquals("SYSTEMD", entity.getServiceType());
    }

    @Test
    @DisplayName("-ve: toDto with null serviceType preserves null without crashing")
    void toDto_nullServiceType_handledCleanly() {
        Service entity = new Service();
        entity.setId(303);
        entity.setServiceName("legacy-app");
        entity.setServiceType(null);

        ServiceResponseDto dto = serviceMapper.toDto(entity);

        assertNotNull(dto);
        assertNull(dto.serviceType());
    }

    @Test
    @DisplayName("-ve: toDto returns null when entity is null")
    void toDto_nullEntity_returnsNull() {
        assertNull(serviceMapper.toDto((Service) null));
    }

    @Test
    @DisplayName("-ve: toEntity returns null when response dto is null")
    void toEntity_nullDto_returnsNull() {
        assertNull(serviceMapper.toEntity((ServiceResponseDto) null));
    }

    @Test
    @DisplayName("+ve: reqToDto maps serviceType correctly from Service entity")
    void reqToDto_mapsServiceType() {
        Service entity = new Service();
        entity.setId(404);
        entity.setServiceName("api-gateway");
        entity.setServiceType("OTHER");

        ServiceRequestDto reqDto = serviceMapper.reqToDto(entity);

        assertNotNull(reqDto);
        assertEquals(404, reqDto.id());
        assertEquals("api-gateway", reqDto.serviceName());
        assertEquals("OTHER", reqDto.serviceType());
    }

    @Test
    @DisplayName("+ve: backwards-compatible ServiceResponseDto constructor defaults serviceType to null")
    void serviceResponseDto_backwardCompatibility() {
        ServiceResponseDto dto = new ServiceResponseDto(505, null, "compat-svc", null, null, "source", "source");
        assertEquals(505, dto.id());
        assertEquals("compat-svc", dto.serviceName());
        assertNull(dto.serviceType());
    }
}
