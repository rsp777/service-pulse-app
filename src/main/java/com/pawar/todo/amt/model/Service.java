package com.pawar.todo.amt.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString(exclude = {"servers"})
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "service_id")
	private Integer id;

//	@JsonInclude(value = Include.CUSTOM)
	@ManyToMany(mappedBy = "services")
	private Set<Server> servers;
	
	@JsonProperty("serviceName")
	@Column(name = "serviceName")
	private String serviceName;

	@JsonProperty("healthCheckUrl")
	@Column(name = "healthCheckUrl")
	private String healthCheckUrl;

	@JsonInclude(value = Include.CUSTOM)
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	@JsonProperty("lastHealthChecked")
	@Column(name = "lastHealthChecked")
	private LocalDateTime lastHealthChecked;

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
		Service other = (Service) obj;
		return Objects.equals(createdDttm, other.createdDttm) && Objects.equals(createdSource, other.createdSource)
				&& Objects.equals(healthCheckUrl, other.healthCheckUrl) && Objects.equals(id, other.id)
				&& Objects.equals(lastHealthChecked, other.lastHealthChecked)
				&& Objects.equals(lastUpdatedDttm, other.lastUpdatedDttm)
				&& Objects.equals(lastUpdatedSource, other.lastUpdatedSource)
				&& Objects.equals(serviceName, other.serviceName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdDttm, createdSource, healthCheckUrl, id, lastHealthChecked, lastUpdatedDttm,
				lastUpdatedSource, serviceName);
	}
	
	
}
