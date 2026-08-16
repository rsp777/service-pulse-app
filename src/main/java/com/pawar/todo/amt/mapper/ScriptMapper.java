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
import com.pawar.todo.amt.converter.ScriptExtensionConverter;
import com.pawar.todo.amt.model.Path;
import com.pawar.todo.amt.model.Script;
import com.pawar.todo.amt.model.Server;

/**
 * Mapper class for converting between Script entity and DTOs
 */
@Component
public class ScriptMapper {

	private static final Logger logger = LoggerFactory.getLogger(ScriptMapper.class);
	private final ScriptExtensionConverter scriptExtensionConverter;
	private PathMapper pathMapper;

	public ScriptMapper(ScriptExtensionConverter scriptExtensionConverter) {
		this.scriptExtensionConverter = scriptExtensionConverter;
	}

	@Autowired
	public void setPathMapper(PathMapper pathMapper) {
		this.pathMapper = pathMapper;
	}

	public Script toEntity(ScriptRequestDto dto) {
		logger.debug("Entering toEntity() with dto: {}", dto);
		if (dto == null) {
			logger.warn("Received null ScriptRequestDto, returning null Script entity.");
			return null;
		}

		Script script = new Script();
		populateScriptFromRequestDto(dto, script);
		logger.info("Converted to Script entity: {}", script);
		return script;
	}

	public Script toEntity(ScriptResponseDto dto) {
		logger.debug("Entering toEntity() with dto: {}", dto);
		if (dto == null) {
			logger.warn("Received null ScriptResponseDto, returning null Script entity.");
			return null;
		}

		Script script = new Script();
		populateScriptFromResponseDto(dto, script);
		logger.info("Converted to Script entity: {}", script);
		return script;
	}

	private void populateScriptFromRequestDto(ScriptRequestDto dto, Script script) {
		logger.info("Populating Script entity from ScriptRequestDto: {}", dto);

		if (dto.id() != null) {
			script.setId(dto.id());
			logger.debug("Set id: {}", dto.id());

		}

		script.setScriptName(dto.scriptName());
		logger.debug("Set scriptName: {}", dto.scriptName());

		if (dto.scriptExtension() != null) {
			script.setScriptExtension(scriptExtensionConverter.toEnum(dto.scriptExtension()));
			logger.debug("Set scriptExtension: {}", dto.scriptExtension());
		}

		script.setCreatedDttm(dto.createdDttm());
		script.setLastUpdatedDttm(dto.lastUpdatedDttm());
		script.setCreatedSource(dto.createdSource());
		script.setLastUpdatedSource(dto.lastUpdatedSource());
		logger.debug("Set timestamps and source info");
	}

	private void populateScriptFromResponseDto(ScriptResponseDto dto, Script script) {
		logger.info("Populating Script entity from ScriptResponseDto: {}", dto);

		if (dto.id() != null) {
			script.setId(dto.id());
			logger.debug("Set id: {}", dto.id());

		}

		script.setScriptName(dto.scriptName());
		logger.debug("Set scriptName: {}", dto.scriptName());

		script.setScriptExtension(scriptExtensionConverter.toEnum(dto.scriptExtension()));
		logger.debug("Set scriptExtension: {}", dto.scriptExtension());

//		Set<PathResponseDto> pathDtos = dto.paths();
//		if (!pathDtos.isEmpty()) {
//
//			Set<Path> paths = new HashSet<>();
//			for (PathResponseDto pathResponseDto : pathDtos) {
//				Path path = pathMapper.toEntity(pathResponseDto);
//				paths.add(path);
//			}
//			script.setPaths(paths);
//			logger.debug("Set paths for script: {}", paths);
//		}

		script.setCreatedDttm(dto.createdDttm());
		script.setLastUpdatedDttm(dto.lastUpdatedDttm());
		script.setCreatedSource(dto.createdSource());
		script.setLastUpdatedSource(dto.lastUpdatedSource());
		logger.debug("Set timestamps and source info");
	}

	public ScriptResponseDto toDto(Script entity) {
		logger.info("Entering toDto() with entity: {}", entity);
		if (entity == null) {
			logger.warn("Received null Script entity, returning null ScriptResponseDto.");
			return null;
		}

//		Set<PathResponseDto> pathResponseDtos = new HashSet<>();
//		Set<Path> paths = entity.getPaths();
//
//		if (paths != null && !paths.isEmpty()) {
//			for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
//				Path path = (Path) iterator.next();
//				PathResponseDto pathResponseDto = pathMapper.toDto(path);
//				pathResponseDtos.add(pathResponseDto);
//			}
//		}

		if (entity.getScriptExtension() != null) {
			ScriptResponseDto dto = new ScriptResponseDto(entity.getId(), entity.getScriptName(),
					entity.getScriptExtension().name(), null, entity.getCreatedDttm(),
					entity.getLastUpdatedDttm(), entity.getCreatedSource(), entity.getLastUpdatedSource());
			logger.debug("Created ScriptResponseDto with values: {}",
					String.format("id=%s, scriptName=%s, scriptExtension=%s", entity.getId(), entity.getScriptName(),
							entity.getScriptExtension()));

			logger.info("Converted to ScriptResponseDto: {}", dto);
			return dto;
		}

		ScriptResponseDto dto = new ScriptResponseDto(entity.getId(), entity.getScriptName(), null, null,
				entity.getCreatedDttm(), entity.getLastUpdatedDttm(), entity.getCreatedSource(),
				entity.getLastUpdatedSource());
		logger.debug("Created ScriptResponseDto with values: {}",
				String.format("id=%s, scriptName=%s, scriptExtension=%s", entity.getId(), entity.getScriptName(),
						entity.getScriptExtension()));

		logger.info("Converted to ScriptResponseDto: {}", dto);
		return dto;

	}

	public ScriptRequestDto reqToDto(Script entity) {
		logger.debug("Entering toDto() with entity: {}", entity);
		if (entity == null) {
			logger.warn("Received null Script entity, returning null ScriptResponseDto.");
			return null;
		}

//		Set<PathRequestDto> pathRequestDtos = new HashSet<>();
//		Set<Path> paths = entity.getPaths();
//
//		if (paths != null && !paths.isEmpty()) {
//			for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
//				Path path = (Path) iterator.next();
//				PathRequestDto pathRequestDto = pathMapper.reqToDto(path);
//				pathRequestDtos.add(pathRequestDto);
//			}
//		}

		ScriptRequestDto dto = new ScriptRequestDto(entity.getId(), entity.getScriptName(),
				entity.getScriptExtension().name(), null, entity.getCreatedDttm(),
				entity.getLastUpdatedDttm(), entity.getCreatedSource(), entity.getLastUpdatedSource());

		logger.debug("Created ScriptRequestDto with values: {}",
				String.format("id=%s, scriptName=%s, scriptExtension=%s", entity.getId(), entity.getScriptName(),
						entity.getScriptExtension().name()));

		logger.info("Converted to ScriptRequestDto: {}", dto);
		return dto;
	}

	public void updateFromDto(ScriptRequestDto dto, Script script) {
		logger.debug("Entering updateFromDto() with dto: {}, script: {}", dto, script);
		if (dto == null || script == null) {
			logger.warn("Received null ScriptRequestDto or Script, skipping update.");
			return;
		}

		logger.info("Updating Script entity from ScriptRequestDto: {}", dto);
		populateScriptFromRequestDto(dto, script);
		logger.info("Updated Script entity: {}", script);
	}
}