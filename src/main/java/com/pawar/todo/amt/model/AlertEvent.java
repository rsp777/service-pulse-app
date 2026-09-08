package com.pawar.todo.amt.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "alert_event")
public class AlertEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Integer id;

    @Column(name = "alert_id", nullable = false)
    private Integer alertId;

    @Column(name = "server_id")
    private Integer serverId;

    @Column(name = "service_id")
    private Integer serviceId;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "triggered_dttm", nullable = false)
    private LocalDateTime triggeredDttm;
}
