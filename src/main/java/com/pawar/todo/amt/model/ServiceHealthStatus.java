package com.pawar.todo.amt.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pawar.todo.amt.constants.HealthCheckStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "server_id", "service_id" }))
public class ServiceHealthStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "health_status_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private Service service; // Reference to the associated service

    @ManyToOne
    @JoinColumn(name = "server_id")
    private Server server;

    @Enumerated(EnumType.STRING)
    @JsonProperty("status")
    @Column(name = "status", nullable = false)
    private HealthCheckStatus status; // e.g., UP, DOWN, UNKNOWN

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp; // When the health status was recorded

    @JsonProperty("responseTime")
    @Column(name = "response_time")
    private Long responseTime; // Time taken for the health check in milliseconds

    @JsonProperty("errorMessage")
    @Column(name = "error_message")
    private String errorMessage; // Optional error message if health check fails

    @JsonInclude(value = Include.CUSTOM)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @JsonProperty("createdDttm")
    @Column(name = "created_dttm", updatable = false)
    private LocalDateTime createdDttm; // Timestamp of creation

    @JsonInclude(value = Include.CUSTOM)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @JsonProperty("lastUpdatedDttm")
    @Column(name = "last_updated_dttm")
    private LocalDateTime lastUpdatedDttm; // Timestamp of last update

    @JsonInclude(value = Include.CUSTOM)
    @Column(name = "created_source")
    private String createdSource; // Source of creation

    @JsonInclude(value = Include.CUSTOM)
    @Column(name = "last_updated_source")
    private String lastUpdatedSource; // Source of last update

    @PrePersist
    protected void onCreate() {
        createdDttm = LocalDateTime.now();
        lastUpdatedDttm = LocalDateTime.now();
        timestamp = LocalDateTime.now(); // Set timestamp on creation
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedDttm = LocalDateTime.now();
    }
}
