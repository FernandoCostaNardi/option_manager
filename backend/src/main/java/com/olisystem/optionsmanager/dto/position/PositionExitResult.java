package com.olisystem.optionsmanager.dto.position;

import com.olisystem.optionsmanager.model.position.PositionStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionExitResult {
  private UUID positionId;
  private PositionStatus newPositionStatus;
  private Integer exitQuantity;
  private BigDecimal totalExitValue;
  private BigDecimal totalProfitLoss;
  private BigDecimal profitLossPercentage;
  private Integer remainingQuantity;
  private List<ExitRecordDto> exitRecords;
  private UUID resultOperationId; // ID da operação consolidada
  private Boolean isFullExit;
  private String message;
}
