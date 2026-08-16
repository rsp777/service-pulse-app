package com.pawar.todo.amt.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class HealthCheck {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "health_check_id")
	private Integer id;
	
	@JsonProperty("url")
	@Column(name = "url")
	private String url;
	
	@JsonProperty("operationalPort")
	@Column(name = "operationalPort")
	private Integer operationalPort;
	
	@ManyToOne
	@JoinColumn(name = "httpMethodsId")
	private HttpMethods httpMethod; 
	
	@JsonProperty("expectedResponse")
	@Column(name = "expectedResponse")
	private String expectedResponse;
	
	@ManyToOne
	@JoinColumn(name = "service_id")
	private Service service;
	
	@JsonInclude(value = Include.CUSTOM)
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	@JsonProperty("createdDttm")
	@Column(name = "createdDttm")
	private LocalDateTime createdDttm;

	@JsonInclude(value = Include.CUSTOM)
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	@JsonProperty("lastUpdatedDttm")
	@Column(name = "lastUpdatedDttm")
	private LocalDateTime lastUpdatedDttm;

	@JsonInclude(value = Include.CUSTOM)
	@Column(name = "createdSource")
	private String createdSource;

	@JsonInclude(value = Include.CUSTOM)
	@Column(name = "lastUpdatedSource")
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
