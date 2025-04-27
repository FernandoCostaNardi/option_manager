package com.olisystem.optionsmanager.dto;

import com.olisystem.optionsmanager.model.AverageOperationGroupStatus;
import java.time.LocalDate;
import lombok.Data;

@Data
public class AverageOperationGroupCreateRequestDto {
  private LocalDate creationDate;
  private AverageOperationGroupStatus status;
  private String notes;
}
