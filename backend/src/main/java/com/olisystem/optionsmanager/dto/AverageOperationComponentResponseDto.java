package com.olisystem.optionsmanager.dto;

import com.olisystem.optionsmanager.model.OperationRoleType;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AverageOperationComponentResponseDto {
  private UUID id;
  private UUID groupId;
  private UUID operationId;
  private String operationCode;
  private OperationRoleType roleType;
  private Integer sequenceNumber;
  private LocalDate inclusionDate;
}
