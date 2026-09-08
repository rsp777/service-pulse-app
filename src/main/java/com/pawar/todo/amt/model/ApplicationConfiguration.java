package com.pawar.todo.amt.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "application_configuration")
public class ApplicationConfiguration {

    @Id
    @Column(name = "configuration_key", length = 100)
    private String key;

    @Column(name = "configuration_value", nullable = false, length = 500)
    private String value;

    @Column(name = "last_updated_dttm")
    private LocalDateTime lastUpdatedDttm;

    @PrePersist
    @PreUpdate
    protected void updateTimestamp() {
        lastUpdatedDttm = LocalDateTime.now();
    }
}
