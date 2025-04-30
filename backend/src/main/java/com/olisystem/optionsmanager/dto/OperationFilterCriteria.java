package com.olisystem.optionsmanager.dto;

import com.olisystem.optionsmanager.model.OperationStatus;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperationFilterCriteria {
  private List<OperationStatus> status;
  private LocalDate entryDateStart;
  private LocalDate entryDateEnd;
  private LocalDate exitDateStart;
  private LocalDate exitDateEnd;
  private String analysisHouseName;
  private String brokerageName;
}
