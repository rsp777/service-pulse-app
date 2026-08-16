package com.pawar.todo.amt.model;


import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pawar.todo.amt.constants.AgentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

@Data
@Entity
public class Agent implements AutoCloseable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agent_id")
    private Integer id;

    @JsonProperty("name")
    @Column(name = "name", nullable = false, unique = true)
    private String name; // Unique name for the agent

    @JsonProperty("host")
    @Column(name = "host", nullable = false)
    private String host; // Hostname or IP address of the agent

    @JsonProperty("port")
    @Column(name = "port", nullable = false)
    private Integer port; // Port on which the agent listens for commands

    @JsonProperty("status")
    @Column(name = "status", nullable = false)
    private AgentStatus status; // e.g., ONLINE, OFFLINE, ERROR

    @JsonProperty("agentUrl")
    @Column(name = "agent_url", nullable = false)
    private String agentWebSocketUrl;
    
    @ManyToOne
    @JoinColumn(name = "server_id")
    private Server server; 
    
    @JsonProperty("jarFilePath")
    @Column(name = "jar_file_path", nullable = true)
    private String jarFilePath; // Path to the JAR file on the remote server

    @JsonProperty("jarVersion")
    @Column(name = "jar_version")
    private String jarVersion; // Version of the JAR file

    @JsonProperty("lastDeployment")
    @JsonInclude(value = Include.CUSTOM)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Column(name = "last_deployment")
    private LocalDateTime lastDeployment; // Last deployment timestamp
    
    @JsonInclude(value = Include.CUSTOM)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @JsonProperty("lastHeartbeat")
    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat; // Last time the agent sent a heartbeat

    @JsonInclude(value = Include.CUSTOM)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @JsonProperty("createdDttm")
    @Column(name = "created_dttm", updatable = false)
    private LocalDateTime createdDttm; // Creation timestamp

    @JsonInclude(value = Include.CUSTOM)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @JsonProperty("lastUpdatedDttm")
    @Column(name = "last_updated_dttm")
    private LocalDateTime lastUpdatedDttm; // Last updated timestamp

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
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedDttm = LocalDateTime.now();
    }

    @Override
    public void close() throws Exception {
        
        throw new UnsupportedOperationException("Unimplemented method 'close'");
    }
}

