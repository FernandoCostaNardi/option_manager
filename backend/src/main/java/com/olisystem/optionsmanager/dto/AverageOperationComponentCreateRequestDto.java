package com.olisystem.optionsmanager.dto;

import com.olisystem.optionsmanager.model.OperationRoleType;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class AverageOperationComponentCreateRequestDto {
  private UUID groupId;
  private UUID operationId;
  private OperationRoleType roleType;
  private Integer sequenceNumber;
  private LocalDate inclusionDate;
}
