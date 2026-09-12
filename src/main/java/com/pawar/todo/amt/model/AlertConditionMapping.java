package com.pawar.todo.amt.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_condition_mapping")
public class AlertConditionMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Integer id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "condition_type_id", nullable = false)
    private AlertConditionType conditionType;
    
    @Column(name = "condition_key", nullable = false)
    private String conditionKey;
    
    @Column(name = "condition_value", nullable = false)
    private String conditionValue;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_dttm", nullable = false)
    private LocalDateTime createdDttm = LocalDateTime.now();

    public AlertConditionMapping() {}

    public AlertConditionMapping(AlertConditionType conditionType, String conditionKey, String conditionValue, String description) {
        this.conditionType = conditionType;
        this.conditionKey = conditionKey;
        this.conditionValue = conditionValue;
        this.description = description;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public AlertConditionType getConditionType() { return conditionType; }
    public void setConditionType(AlertConditionType conditionType) { this.conditionType = conditionType; }
    
    public String getConditionKey() { return conditionKey; }
    public void setConditionKey(String conditionKey) { this.conditionKey = conditionKey; }
    
    public String getConditionValue() { return conditionValue; }
    public void setConditionValue(String conditionValue) { this.conditionValue = conditionValue; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedDttm() { return createdDttm; }
    public void setCreatedDttm(LocalDateTime createdDttm) { this.createdDttm = createdDttm; }
}
