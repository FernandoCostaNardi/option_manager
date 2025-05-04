package com.olisystem.optionsmanager.dto.operation;

import java.math.BigDecimal;
import java.util.UUID;

import com.olisystem.optionsmanager.model.operation.TargetType;

import lombok.Builder;
import lombok.Data;

@Data 
@Builder
public class OperationTargetResponseDto {
  private UUID id;
  private UUID operationId;
  private TargetType type;
  private Integer sequence;
  private BigDecimal value;
}
