package com.pawar.todo.amt.respository;

import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.model.Server;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Repository
public interface ServerRepository extends JpaRepository<Server, Integer> {
    // Issue #34: Update telemetry without merging stale inventory associations.
    @Modifying
    @Transactional
    @Query("update Server s set s.status = :status, s.lastHealthChecked = :checkedAt, s.lastUpdatedDttm = :checkedAt where s.id = :id")
    int recordHealth(@Param("id") Integer id, @Param("status") ServerStatus status,
            @Param("checkedAt") LocalDateTime checkedAt);

	Optional<Server> findByStatus(ServerStatus serverStatus);

	boolean existsByHostname(@NotBlank(message = "Hostname cannot be blank") String hostname);

	boolean existsByIpAddress(
			@NotBlank(message = "IP address is required") @Pattern(regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$", message = "Invalid IP address format") String ipAddress);


}
