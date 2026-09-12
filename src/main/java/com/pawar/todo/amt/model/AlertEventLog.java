package com.pawar.todo.amt.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_event_log")
public class AlertEventLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer id;
    
    @Column(name = "alert_id", nullable = false)
    private Integer alertId;
    
    @Column(name = "server_id")
    private Integer serverId;
    
    @Column(name = "service_id")
    private Integer serviceId;
    
    @Column(name = "trigger_condition", nullable = false)
    private String triggerCondition;
    
    @Column(name = "alert_status", nullable = false)
    private String alertStatus;
    
    @Column(name = "alert_message")
    private String alertMessage;
    
    @Column(name = "triggered_dttm", nullable = false)
    private LocalDateTime triggeredDttm = LocalDateTime.now();

    public AlertEventLog() {}

    public AlertEventLog(Integer alertId, Integer serverId, Integer serviceId, String triggerCondition, String alertStatus, String alertMessage) {
        this.alertId = alertId;
        this.serverId = serverId;
        this.serviceId = serviceId;
        this.triggerCondition = triggerCondition;
        this.alertStatus = alertStatus;
        this.alertMessage = alertMessage;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getAlertId() { return alertId; }
    public void setAlertId(Integer alertId) { this.alertId = alertId; }
    
    public Integer getServerId() { return serverId; }
    public void setServerId(Integer serverId) { this.serverId = serverId; }
    
    public Integer getServiceId() { return serviceId; }
    public void setServiceId(Integer serviceId) { this.serviceId = serviceId; }
    
    public String getTriggerCondition() { return triggerCondition; }
    public void setTriggerCondition(String triggerCondition) { this.triggerCondition = triggerCondition; }
    
    public String getAlertStatus() { return alertStatus; }
    public void setAlertStatus(String alertStatus) { this.alertStatus = alertStatus; }
    
    public String getAlertMessage() { return alertMessage; }
    public void setAlertMessage(String alertMessage) { this.alertMessage = alertMessage; }
    
    public LocalDateTime getTriggeredDttm() { return triggeredDttm; }
    public void setTriggeredDttm(LocalDateTime triggeredDttm) { this.triggeredDttm = triggeredDttm; }
}
