package com.pawar.todo.amt.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pawar.todo.amt.constants.ScriptExtension;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(exclude = {"paths"})
@Data
@Entity
public class Script {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "script_id")
	private Integer id;

	@JsonProperty("scriptName")
	@Column(name = "scriptName")
	private String scriptName;

	@JsonProperty("scriptExtension")
	@Column(name = "scriptExtension")
	@Enumerated(EnumType.STRING)
	private ScriptExtension scriptExtension;

	@JsonIgnore
	@EqualsAndHashCode.Exclude
	@ManyToMany(mappedBy = "scripts")
	private Set<Path> paths = new HashSet<>();

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
