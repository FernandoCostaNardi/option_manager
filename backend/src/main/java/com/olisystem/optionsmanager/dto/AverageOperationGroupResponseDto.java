package com.olisystem.optionsmanager.dto;

import com.olisystem.optionsmanager.model.AverageOperationGroupStatus;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AverageOperationGroupResponseDto {
  private UUID id;
  private LocalDate creationDate;
  private AverageOperationGroupStatus status;
  private String notes;
}
