package com.pawar.todo.amt.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_condition_type")
public class AlertConditionType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "condition_type_id")
    private Integer id;
    
    @Column(name = "condition_type_name", nullable = false, unique = true)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_dttm", nullable = false)
    private LocalDateTime createdDttm = LocalDateTime.now();

    public AlertConditionType() {}

    public AlertConditionType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedDttm() { return createdDttm; }
    public void setCreatedDttm(LocalDateTime createdDttm) { this.createdDttm = createdDttm; }
}
