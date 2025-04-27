package com.olisystem.optionsmanager.mapper;

import com.olisystem.optionsmanager.dto.AverageOperationComponentCreateRequestDto;
import com.olisystem.optionsmanager.dto.AverageOperationComponentResponseDto;
import com.olisystem.optionsmanager.model.AverageOperationComponent;
import com.olisystem.optionsmanager.model.AverageOperationGroup;
import com.olisystem.optionsmanager.model.Operation;

public class AverageOperationComponentMapper {

  public static AverageOperationComponent toEntity(
      AverageOperationComponentCreateRequestDto dto,
      AverageOperationGroup group,
      Operation operation) {
    return AverageOperationComponent.builder()
        .group(group)
        .operation(operation)
        .roleType(dto.getRoleType())
        .sequenceNumber(dto.getSequenceNumber())
        .inclusionDate(dto.getInclusionDate())
        .build();
  }

  public static AverageOperationComponentResponseDto toDto(AverageOperationComponent entity) {
    return AverageOperationComponentResponseDto.builder()
        .id(entity.getId())
        .groupId(entity.getGroup().getId())
        .operationId(entity.getOperation().getId())
        .operationCode(entity.getOperation().getOptionSeries().getCode())
        .roleType(entity.getRoleType())
        .sequenceNumber(entity.getSequenceNumber())
        .inclusionDate(entity.getInclusionDate())
        .build();
  }
}
