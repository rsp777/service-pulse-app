package com.pawar.todo.amt.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pawar.todo.amt.constants.CommandStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

@Data
@Entity
public class Command {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "command_id")
	private Integer id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "description")
	private String description;

	@Column(name = "parameters", columnDefinition = "TEXT")
	private String parameters; // JSON representation of parameters

	@Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CommandStatus status; // e.g., PENDING, IN_PROGRESS, COMPLETED, FAILED

	@Column(name = "result")
	private String result; // Result of the command execution

	@Column(name = "execution_time")
	private Long executionTime; // Time taken to execute the command

	@Column(name = "error_message")
	private String errorMessage; // Optional error message if execution fails

	@JsonInclude(value = Include.CUSTOM)
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	@JsonProperty("createdDttm")
	@Column(name = "created_dttm", updatable = false)
	private LocalDateTime createdDttm;

	@JsonInclude(value = Include.CUSTOM)
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	@JsonProperty("lastUpdatedDttm")
	@Column(name = "last_updated_dttm", updatable = false)
	private LocalDateTime lastUpdatedDttm;

	@JsonInclude(value = Include.CUSTOM)
	@Column(name = "created_source")
	private String createdSource;

	@JsonInclude(value = Include.CUSTOM)
	@Column(name = "last_updated_source")
	private String lastUpdatedSource;

	@PrePersist
	protected void onCreate() {
		createdDttm = LocalDateTime.now();
		lastUpdatedDttm = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		lastUpdatedDttm = LocalDateTime.now();
	}

}
