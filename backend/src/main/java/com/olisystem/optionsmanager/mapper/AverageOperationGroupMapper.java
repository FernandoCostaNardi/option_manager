package com.olisystem.optionsmanager.mapper;

import com.olisystem.optionsmanager.dto.AverageOperationGroupCreateRequestDto;
import com.olisystem.optionsmanager.dto.AverageOperationGroupResponseDto;
import com.olisystem.optionsmanager.model.AverageOperationGroup;

public class AverageOperationGroupMapper {

  public static AverageOperationGroup toEntity(AverageOperationGroupCreateRequestDto dto) {
    return AverageOperationGroup.builder()
        .creationDate(dto.getCreationDate())
        .status(dto.getStatus())
        .notes(dto.getNotes())
        .build();
  }

  public static AverageOperationGroupResponseDto toDto(AverageOperationGroup entity) {
    return AverageOperationGroupResponseDto.builder()
        .id(entity.getId())
        .creationDate(entity.getCreationDate())
        .status(entity.getStatus())
        .notes(entity.getNotes())
        .build();
  }
}
