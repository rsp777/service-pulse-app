package com.pawar.todo.amt.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pawar.app.healthcheck.dto.CommandRequestDto;
import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.todo.amt.converter.CommandStatusConverter;
import com.pawar.todo.amt.model.Command;

/**
 * Mapper class for converting between Command entity and DTOs
 */
@Component
public class CommandMapper {

	private static final Logger logger = LoggerFactory.getLogger(CommandMapper.class);
	private final CommandStatusConverter commandStatusConverter;

	@Autowired
	public CommandMapper(CommandStatusConverter commandStatusConverter) {
		this.commandStatusConverter = commandStatusConverter;
	}

	public Command toEntity(CommandRequestDto dto) {
		logger.debug("Entering toEntity() with dto: {}", dto);
		if (dto == null) {
			logger.warn("Received null CommandRequestDto, returning null Command entity.");
			return null;
		}

		Command command = new Command();
		populateCommandFromDto(dto, command);
		logger.info("Converted to Command entity: {}", command);
		return command;
	}

	public Command toEntity(CommandResponseDto dto) {
		logger.debug("Entering toEntity() with dto: {}", dto);
		if (dto == null) {
			logger.warn("Received null CommandResponseDto, returning null Command entity.");
			return null;
		}

		Command command = new Command();
		populateCommandFromDto(dto, command);
		logger.info("Converted to Command entity: {}", command);
		return command;
	}

	private void populateCommandFromDto(CommandRequestDto dto, Command command) {
		logger.info("Populating Command entity from CommandRequestDto: {}", dto);
		command.setName(dto.name());
		command.setDescription(dto.description());
		command.setParameters(dto.parameters());
		command.setStatus(commandStatusConverter.toEnum(dto.status()));
		command.setResult(dto.result());
		command.setCreatedDttm(dto.createdDttm());
		command.setLastUpdatedDttm(dto.lastUpdatedDttm());
		command.setCreatedSource(dto.createdSource());
		command.setLastUpdatedSource(dto.lastUpdatedSource());
		logger.debug("Populated Command entity: {}", command);
	}

	private void populateCommandFromDto(CommandResponseDto dto, Command command) {
		logger.info("Populating Command entity from CommandResponseDto: {}", dto);
		command.setName(dto.name());
		command.setDescription(dto.description());
		command.setStatus(commandStatusConverter.toEnum(dto.status()));
		command.setResult(dto.result());
		command.setCreatedDttm(dto.createdDttm());
		command.setLastUpdatedDttm(dto.lastUpdatedDttm());
		command.setCreatedSource(dto.createdSource());
		command.setLastUpdatedSource(dto.lastUpdatedSource());
		logger.debug("Populated Command entity: {}", command);
	}

	public CommandResponseDto toDto(Command entity) {
		logger.debug("Entering toDto() with entity: {}", entity);
		if (entity == null) {
			logger.warn("Received null Command entity, returning null CommandResponseDto.");
			return null;
		}

		CommandResponseDto dto = new CommandResponseDto(
				entity.getId(),
				entity.getName(),
				entity.getDescription(),
				entity.getParameters(),
				entity.getStatus().name(),
				entity.getResult(),
				entity.getCreatedDttm(),
				entity.getLastUpdatedDttm(),
				entity.getCreatedSource(),
				entity.getLastUpdatedSource());

		logger.info("Converted to CommandResponseDto: {}", dto);
		return dto;
	}

	public void updateFromDto(CommandRequestDto dto, Command command) {
		logger.debug("Entering updateFromDto() with dto: {}, command: {}", dto, command);
		if (dto == null || command == null) {
			logger.warn("Received null CommandRequestDto or Command, skipping update.");
			return;
		}

		logger.info("Updating Command entity from CommandRequestDto: {}", dto);
		populateCommandFromDto(dto, command);
		logger.info("Updated Command entity: {}", command);
	}
}