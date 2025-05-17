package com.olisystem.optionsmanager.dto.position;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntryLotResponseDto {
  private String id;
  private String operationId;
  private LocalDate entryDate;
  private BigDecimal unitPrice;
  private Integer originalQuantity;
  private Integer remainingQuantity;
  private BigDecimal totalValue;
  private String dayKey;
}
