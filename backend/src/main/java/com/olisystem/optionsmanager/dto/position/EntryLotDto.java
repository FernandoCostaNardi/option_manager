package com.olisystem.optionsmanager.dto.position;

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
public class EntryLotDto {
  private UUID id;
  private UUID positionId;
  private LocalDate entryDate;
  private Integer quantity;
  private BigDecimal unitPrice;
  private BigDecimal totalValue;
  private Integer remainingQuantity;
  private Integer sequenceNumber;
  private Boolean isFullyConsumed;
  private UUID originalOperationId;
}
