package com.pawar.todo.amt.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(name = "server_service_configuration", uniqueConstraints = @UniqueConstraint(columnNames = { "server_id", "service_id" }))
public class ServerServiceConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "configuration_id")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @ManyToOne(optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "health_check_url")
    private String healthCheckUrl;

    @Column(name = "created_dttm", updatable = false)
    private LocalDateTime createdDttm;

    @Column(name = "last_updated_dttm")
    private LocalDateTime lastUpdatedDttm;

    @Column(name = "created_source")
    private String createdSource;

    @Column(name = "last_updated_source")
    private String lastUpdatedSource;

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        createdDttm = LocalDateTime.now();
        lastUpdatedDttm = LocalDateTime.now();
    }

    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        lastUpdatedDttm = LocalDateTime.now();
    }
}
