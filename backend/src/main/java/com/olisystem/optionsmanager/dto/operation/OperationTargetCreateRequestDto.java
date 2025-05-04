package com.olisystem.optionsmanager.dto.operation;

import com.olisystem.optionsmanager.model.operation.TargetType;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class OperationTargetCreateRequestDto {
  private UUID operationId;
  private TargetType type;
  private Integer sequence;
  private BigDecimal value;
}
