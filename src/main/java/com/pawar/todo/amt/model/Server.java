package com.pawar.todo.amt.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pawar.todo.amt.constants.ServerStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

@Data
@Entity
public class Server {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "server_id")
	private Integer id;

	@JsonProperty("hostname")
	@Column(name = "hostname")
	private String hostname;

	@JsonProperty("ipAddress")
	@Column(name = "ipAddress")
	private String ipAddress;

	@JsonProperty("osType")
	@Column(name = "osType")
	private String osType;

	@Enumerated(EnumType.STRING)
	@JsonProperty("status")
	@Column(name = "status")
	private ServerStatus status;

	@JsonInclude(value = Include.CUSTOM)
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	@JsonProperty("lastHealthChecked")
	@Column(name = "lastHealthChecked")
	private LocalDateTime lastHealthChecked;

//	@JsonInclude(value = Include.CUSTOM)
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "server_path", joinColumns = @JoinColumn(name = "server_id"), inverseJoinColumns = @JoinColumn(name = "path_id"))
	private Set<Path> paths = new HashSet<>();

//	@JsonInclude(value = Include.CUSTOM)
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "server_service", joinColumns = @JoinColumn(name = "server_id"), inverseJoinColumns = @JoinColumn(name = "service_id"))
	private Set<Service> services;

	@JsonInclude(value = Include.CUSTOM)
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	@JsonProperty("createdDttm")
	@Column(name = "created_dttm")
	private LocalDateTime createdDttm;

	@JsonInclude(value = Include.CUSTOM)
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	@JsonProperty("lastUpdatedDttm")
	@Column(name = "last_updated_dttm")
	private LocalDateTime lastUpdatedDttm;

	@JsonInclude(value = Include.CUSTOM)
	@Column(name = "created_source")
	private String createdSource;

	@JsonInclude(value = Include.CUSTOM)
	@Column(name = "last_updated_source")
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Server other = (Server) obj;
		return Objects.equals(createdDttm, other.createdDttm) && Objects.equals(createdSource, other.createdSource)
				&& Objects.equals(hostname, other.hostname) && Objects.equals(id, other.id)
				&& Objects.equals(ipAddress, other.ipAddress)
				&& Objects.equals(lastHealthChecked, other.lastHealthChecked)
				&& Objects.equals(lastUpdatedDttm, other.lastUpdatedDttm)
				&& Objects.equals(lastUpdatedSource, other.lastUpdatedSource) && Objects.equals(osType, other.osType)
				&& Objects.equals(paths, other.paths) && Objects.equals(services, other.services)
				&& status == other.status;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdDttm, createdSource, hostname, id, ipAddress, lastHealthChecked, lastUpdatedDttm,
				lastUpdatedSource, osType, paths, services, status);
	}
	
	
}
