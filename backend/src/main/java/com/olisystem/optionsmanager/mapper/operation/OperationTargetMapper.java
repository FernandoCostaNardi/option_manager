package com.olisystem.optionsmanager.mapper.operation;

import com.olisystem.optionsmanager.dto.operation.OperationTargetCreateRequestDto;
import com.olisystem.optionsmanager.dto.operation.OperationTargetResponseDto;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationTarget;

public class OperationTargetMapper {

  public static OperationTarget toEntity(OperationTargetCreateRequestDto dto, Operation operation) {
    return OperationTarget.builder()
        .operation(operation)
        .type(dto.getType())
        .sequence(dto.getSequence())
        .value(dto.getValue())
        .build();
  }

  public static OperationTargetResponseDto toDto(OperationTarget entity) {
    return OperationTargetResponseDto.builder()
        .id(entity.getId())
        .operationId(entity.getOperation().getId())
        .type(entity.getType())
        .sequence(entity.getSequence())
        .value(entity.getValue())
        .build();
  }
}
