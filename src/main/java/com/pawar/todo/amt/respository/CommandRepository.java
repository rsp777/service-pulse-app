package com.pawar.todo.amt.respository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.todo.amt.constants.CommandStatus;
import com.pawar.todo.amt.model.Command;
import com.pawar.todo.amt.model.Path;

import jakarta.validation.constraints.NotBlank;

@Repository
public interface CommandRepository extends JpaRepository<Command, Integer> {

	Optional<Command> findByStatus(CommandStatus commandStatus);
	Optional<Command> findByDescription(String description);


}