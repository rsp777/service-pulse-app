package com.pawar.todo.amt.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(exclude = {"servers"})
@Data
@Entity
public class Path {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "path_id")
	private Integer id;

	@JsonProperty("pathName")
	@Column(name = "pathName")
	private String pathName;

	@JsonProperty("pathDescription")
	@Column(name = "pathDescription")
	private String pathDescription;

	@JsonIgnore
	@EqualsAndHashCode.Exclude
	@ManyToMany(fetch = FetchType.EAGER,mappedBy = "paths")
	private Set<Server> servers = new HashSet<>();
	
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "path_script", joinColumns = @JoinColumn(name = "path_id"), inverseJoinColumns = @JoinColumn(name = "script_id"))
	private Set<Script> scripts = new HashSet<>();

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
