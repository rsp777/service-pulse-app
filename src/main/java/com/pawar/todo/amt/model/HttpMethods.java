package com.pawar.todo.amt.model;

import java.time.LocalDateTime;



import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pawar.todo.amt.constants.HttpMethodName;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class HttpMethods {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "httpMethodsId")
	private Integer id;
    
	@JsonProperty("methodName")
	@Column(name = "methodName")
	@Enumerated(EnumType.STRING)
    private HttpMethodName methodName;
    
	@JsonProperty("allowedInHealthCheck")
	@Column(name = "allowedInHealthCheck")
    private boolean allowedInHealthCheck;
    
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
}