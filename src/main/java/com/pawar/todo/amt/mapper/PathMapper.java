package com.pawar.todo.amt.mapper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.PathRequestDto;
import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.app.healthcheck.dto.ScriptRequestDto;
import com.pawar.app.healthcheck.dto.ScriptResponseDto;
import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.exceptions.PathOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServiceOperationException;
import com.pawar.todo.amt.model.Path;
import com.pawar.todo.amt.model.Script;
import com.pawar.todo.amt.model.Server;

/**
 * Mapper class for converting between Path entity and DTOs
 */
@Component
public class PathMapper {

	private static final Logger logger = LoggerFactory.getLogger(PathMapper.class);

	private ScriptMapper scriptMapper;
	private ServerMapper serverMapper;

	
	@Autowired
	public void setScriptMapper(ScriptMapper scriptMapper) {
		this.scriptMapper = scriptMapper;
	}

	@Autowired
	public void setServerMapper(ServerMapper serverMapper) {
		this.serverMapper = serverMapper;
	}

	public Path toEntity(PathRequestDto dto)
			throws ResourceNotFoundException, PathOperationException, ServiceOperationException {
		logger.info("Entering toEntity() with dto: {}", dto);
		if (dto == null) {
			logger.warn("Received null PathRequestDto, returning null Path entity.");
			return null;
		}

		Path path = new Path();
		populatePathFromRequestDto(dto, path);
		logger.info("Converted to Path entity: {}", path);
		return path;
	}

	public Path toEntity(PathResponseDto dto) {
		logger.debug("Entering toEntity() with dto: {}", dto);
		if (dto == null) {
			logger.warn("Received null PathResponseDto, returning null Path entity.");
			return null;
		}

		Path path = new Path();
		populatePathFromResponseDto(dto, path);
		logger.info("Converted to Path entity: {}", path);
		return path;
	}

	private void populatePathFromRequestDto(PathRequestDto dto, Path path)
			throws ResourceNotFoundException, PathOperationException, ServiceOperationException {
		logger.info("Populating Path entity from PathRequestDto: {}", dto);
		path.setId(dto.id());
		logger.debug("Set id: {}", dto.id());

		path.setPathName(dto.pathName());
		logger.debug("Set pathName: {}", dto.pathName());

		path.setPathDescription(dto.pathDescription());
		logger.debug("Set pathDescription: {}", dto.pathDescription());

		Set<ScriptRequestDto> scriptDtos = dto.scripts();
		Set<ServerRequestDto> serverRequestDtos = dto.servers();
		logger.info("!scriptDtos.isEmpty() : {}", !scriptDtos.isEmpty());
		if (scriptDtos != null && !scriptDtos.isEmpty()) {

			Set<Script> scripts = new HashSet<>();
			for (ScriptRequestDto scriptRequestDto : scriptDtos) {
				Script script = scriptMapper.toEntity(scriptRequestDto);
				scripts.add(script);
			}
			logger.info("scriptDtos : {}", scriptDtos);

			path.setScripts(scripts);
			logger.debug("Set scripts for path: {}", scripts);
		}

		if (serverRequestDtos != null && !serverRequestDtos.isEmpty()) {

			Set<Server> servers = new HashSet<>();
			for (ServerRequestDto serverRequestDto : serverRequestDtos) {
				Server server = serverMapper.toEntity(serverRequestDto);
				servers.add(server);
			}
			path.setServers(servers);
			logger.debug("Set servers for path: {}", servers);
		}

		path.setCreatedSource(dto.createdSource());
		path.setLastUpdatedSource(dto.lastUpdatedSource());
		logger.debug("Set source info");
	}

	private void populatePathFromResponseDto(PathResponseDto dto, Path path) {
		logger.info("Populating Path entity from PathResponseDto: {}", dto);
		path.setId(dto.id());
		logger.debug("Set id: {}", dto.id());

		path.setPathName(dto.pathName());
		logger.debug("Set pathName: {}", dto.pathName());

		path.setPathDescription(dto.pathDescription());
		logger.debug("Set pathDescription: {}", dto.pathDescription());

		Set<ScriptResponseDto> scriptDtos = dto.scripts();
		Set<ServerResponseDto> serverResponseDtos = dto.servers();

		if (scriptDtos != null && !scriptDtos.isEmpty()) {

			Set<Script> scripts = new HashSet<>();
			for (ScriptResponseDto scriptResponseDto : scriptDtos) {
				Script script = scriptMapper.toEntity(scriptResponseDto);
				scripts.add(script);
			}
			path.setScripts(scripts);
			logger.debug("Set scripts for path: {}", scripts);
		}

		if (serverResponseDtos != null && !serverResponseDtos.isEmpty()) {

			Set<Server> servers = new HashSet<>();
			for (ServerResponseDto serverResponseDto : serverResponseDtos) {
				Server server = serverMapper.toEntity(serverResponseDto);
				servers.add(server);
			}
			path.setServers(servers);
			logger.debug("Set servers for path: {}", servers);
		}

		path.setCreatedDttm(dto.createdDttm());
		path.setLastUpdatedDttm(dto.lastUpdatedDttm());
		path.setCreatedSource(dto.createdSource());
		path.setLastUpdatedSource(dto.lastUpdatedSource());
		logger.debug("Set timestamps and source info");
	}

	public PathResponseDto toDto(Path entity) {
		logger.debug("Entering toDto() with entity: {}", entity);
		if (entity == null) {
			logger.warn("Received null Path entity, returning null PathResponseDto.");
			return null;
		}

//		Set<ServerResponseDto> serverResponseDtos = new HashSet<>();
		Set<ScriptResponseDto> scriptDtos = new HashSet<>();
//		Set<Server> servers = entity.getServers();
		Set<Script> scripts = entity.getScripts();
		logger.info("scripts : {}",scripts);

//		if (servers != null && !servers.isEmpty()) {
//			for (Iterator iterator = servers.iterator(); iterator.hasNext();) {
//				Server server = (Server) iterator.next();
//
//				ServerResponseDto serverResponseDto = serverMapper.toDto(server);
//				serverResponseDtos.add(serverResponseDto);
//
//			}
//		}

		if (scripts != null && !scripts.isEmpty()) {
			for (Iterator iterator = scripts.iterator(); iterator.hasNext();) {
				Script script = (Script) iterator.next();
				ScriptResponseDto scriptResponseDto = scriptMapper.toDto(script);
				scriptDtos.add(scriptResponseDto);
			}
		}

		PathResponseDto dto = new PathResponseDto(entity.getId(), entity.getPathName(), entity.getPathDescription(),
				null, scriptDtos, entity.getCreatedDttm(), entity.getLastUpdatedDttm(),
				entity.getCreatedSource(), entity.getLastUpdatedSource());

		logger.debug("Created PathResponseDto with values: {}", String.format("id=%s, pathName=%s, pathDescription=%s",
				entity.getId(), entity.getPathName(), entity.getPathDescription()));

		logger.info("Converted to PathResponseDto: {}", dto);
		return dto;
	}

	public PathRequestDto reqToDto(Path entity) {
		logger.debug("Entering toDto() with entity: {}", entity);
		if (entity == null) {
			logger.warn("Received null Path entity, returning null PathResponseDto.");
			return null;
		}

		Set<ServerRequestDto> serverRequestDtos = new HashSet<>();
		Set<ScriptRequestDto> scriptRequestDtos = new HashSet<>();
		Set<Server> servers = entity.getServers();
		Set<Script> scripts = entity.getScripts();

		if (servers != null & !servers.isEmpty()) {
			for (Iterator iterator = servers.iterator(); iterator.hasNext();) {
				Server server = (Server) iterator.next();
				ServerRequestDto serverRequestDto = serverMapper.reqToDto(server);
				serverRequestDtos.add(serverRequestDto);
			}
		}
		logger.info("scripts : {}",scripts);
		if (scripts != null & !scripts.isEmpty()) {
			for (Iterator iterator = scripts.iterator(); iterator.hasNext();) {
				Script script = (Script) iterator.next();
				ScriptRequestDto scriptRequestDto = scriptMapper.reqToDto(script);
				scriptRequestDtos.add(scriptRequestDto);
			}
		}

		PathRequestDto dto = new PathRequestDto(entity.getId(), entity.getPathName(), entity.getPathDescription(),
				serverRequestDtos, scriptRequestDtos, entity.getCreatedDttm(), entity.getLastUpdatedDttm(),
				entity.getCreatedSource(), entity.getLastUpdatedSource());

		logger.debug("Created PathResponseDto with values: {}", String.format("id=%s, pathName=%s, pathDescription=%s",
				entity.getId(), entity.getPathName(), entity.getPathDescription()));

		logger.info("Converted to PathResponseDto: {}", dto);
		return dto;
	}

	public void updateFromDto(PathRequestDto dto, Path path)
			throws ResourceNotFoundException, PathOperationException, ServiceOperationException {
		logger.debug("Entering updateFromDto() with dto: {}, path: {}", dto, path);
		if (dto == null || path == null) {
			logger.warn("Received null PathRequestDto or Path, skipping update.");
			return;
		}

		logger.info("Updating Path entity from PathRequestDto: {}", dto);
		populatePathFromRequestDto(dto, path);
		logger.info("Updated Path entity: {}", path);
	}
}