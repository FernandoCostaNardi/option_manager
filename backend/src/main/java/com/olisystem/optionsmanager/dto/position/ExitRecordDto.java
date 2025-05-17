package com.olisystem.optionsmanager.dto.position;

import com.olisystem.optionsmanager.model.position.ExitStrategy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExitRecordDto {
  private UUID id;
  private UUID entryLotId;
  private UUID exitOperationId;
  private LocalDate exitDate;
  private Integer quantity;
  private BigDecimal entryUnitPrice;
  private BigDecimal exitUnitPrice;
  private BigDecimal profitLoss;
  private BigDecimal profitLossPercentage;
  private ExitStrategy appliedStrategy;
  private LocalDate entryDate; // Da entrada original
  private Integer sequenceNumber; // Sequência do lote
}
