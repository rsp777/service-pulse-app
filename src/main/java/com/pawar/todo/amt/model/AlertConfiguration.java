package com.pawar.todo.amt.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "alert_configuration")
public class AlertConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Integer id;

    @Column(name = "alert_name", nullable = false)
    private String name;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "server_id")
    private Integer serverId;

    @Column(name = "service_id")
    private Integer serviceId;

    @Column(name = "condition_type", nullable = false)
    private String conditionType;

    @Column(name = "operator", nullable = false)
    private String operator;

    @Column(name = "condition_value", nullable = false)
    private String conditionValue;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_dttm", updatable = false)
    private LocalDateTime createdDttm;

    @Column(name = "last_updated_dttm")
    private LocalDateTime lastUpdatedDttm;

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
