package com.olisystem.optionsmanager.dto;

import com.olisystem.optionsmanager.model.OperationStatus;
import com.olisystem.optionsmanager.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class OperationCreateRequestDto {
  private UUID brokerageId;
  private UUID analysisHouseId;
  private UUID optionSeriesId;
  private TransactionType transactionType;
  private LocalDate entryDate;
  private LocalDate exitDate;
  private OperationStatus status;
  private Integer quantity;
  private BigDecimal entryUnitPrice;
  private BigDecimal entryTotalValue;
  private BigDecimal exitUnitPrice;
  private BigDecimal exitTotalValue;
  private BigDecimal adjustedAveragePrice;
  private BigDecimal averagePriceAdjustmentValue;
}
