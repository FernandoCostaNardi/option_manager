package com.olisystem.optionsmanager.dto;

import com.olisystem.optionsmanager.model.OperationStatus;
import com.olisystem.optionsmanager.model.OptionType;
import com.olisystem.optionsmanager.model.TradeType;
import com.olisystem.optionsmanager.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperationSummaryResponseDto {
  private UUID id;
  private String optionSerieCode;
  private OptionType optionType;
  private TransactionType transactionType;
  private TradeType tradeType;
  private LocalDate entryDate;
  private LocalDate exitDate;
  private OperationStatus status;
  private String analysisHouseName;
  private String brokerageName;
  private Integer quantity;
  private BigDecimal entryUnitPrice;
  private BigDecimal entryTotalValue;
  private BigDecimal exitUnitPrice;
  private BigDecimal exitTotalValue;
  private BigDecimal profitLoss;
  private BigDecimal profitLossPercentage;
  private String baseAssetLogoUrl;
}
