package com.pawar.todo.amt.respository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pawar.app.healthcheck.dto.HealthCheckResponseDto;
import com.pawar.todo.amt.constants.HealthCheckStatus;
import com.pawar.todo.amt.model.Command;
import com.pawar.todo.amt.model.HealthCheck;
import com.pawar.todo.amt.model.Path;

import jakarta.validation.constraints.NotBlank;

@Repository
public interface HealthCheckRepository extends JpaRepository<HealthCheck, Integer> {

//	Optional<HealthCheck> findByStatus(HealthCheckStatus healthCheckStatus);

}