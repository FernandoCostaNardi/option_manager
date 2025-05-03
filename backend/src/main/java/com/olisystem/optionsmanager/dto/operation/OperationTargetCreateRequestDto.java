package com.olisystem.optionsmanager.dto;

import com.olisystem.optionsmanager.model.TargetType;
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
