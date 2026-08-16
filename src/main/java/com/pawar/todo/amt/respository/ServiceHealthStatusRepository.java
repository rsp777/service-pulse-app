package com.pawar.todo.amt.respository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pawar.app.healthcheck.dto.ServiceHealthStatusResponseDto;
import com.pawar.todo.amt.constants.HealthCheckStatus;
import com.pawar.todo.amt.model.Command;
import com.pawar.todo.amt.model.HealthCheck;
import com.pawar.todo.amt.model.HttpMethods;
import com.pawar.todo.amt.model.Path;
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.model.ServiceHealthStatus;

import jakarta.validation.constraints.NotBlank;

@Repository
public interface ServiceHealthStatusRepository extends JpaRepository<ServiceHealthStatus, Integer> {

	Optional<List<ServiceHealthStatus>> findByStatus(HealthCheckStatus healthCheckStatus);

	@Query(value = "select shs.* from service_health_status shs inner join service s on shs.service_id=s.service_id \r\n"
			+ "inner join server_service ss on shs.service_id = ss.service_id where ss.server_id = :id", nativeQuery = true)
	Optional<List<ServiceHealthStatus>> findByServerId(Integer id);

	Optional<ServiceHealthStatus> findServiceHealthStatusByServiceId(Integer serviceId);
}