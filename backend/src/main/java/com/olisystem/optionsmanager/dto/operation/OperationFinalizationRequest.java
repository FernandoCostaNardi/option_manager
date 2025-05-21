package com.olisystem.optionsmanager.dto.operation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.olisystem.optionsmanager.model.operation.TradeType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationFinalizationRequest {
  private UUID operationId;
  private Integer quantity;
  private LocalDate exitDate;
  private BigDecimal exitUnitPrice;
}
