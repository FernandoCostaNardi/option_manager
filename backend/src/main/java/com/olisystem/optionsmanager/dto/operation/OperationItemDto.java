package com.olisystem.optionsmanager.dto.operation;

import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.option_serie.OptionType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperationItemDto {
  private UUID id;
  private UUID brokerageId;
  private String brokerageName;
  private UUID analysisHouseId;
  private String analysisHouseName;
  private UUID optionSeriesId;
  private String optionSeriesCode; // Alterado para String
  private TransactionType transactionType;
  private TradeType tradeType;
  private OptionType optionType;
  private LocalDate entryDate;
  private LocalDate exitDate;
  private OperationStatus status;
  private Integer quantity;
  private BigDecimal entryUnitPrice;
  private BigDecimal entryTotalValue;
  private BigDecimal exitUnitPrice;
  private BigDecimal exitTotalValue;
  private BigDecimal profitLoss;
  private BigDecimal profitLossPercentage;
  private String baseAssetLogoUrl;
  private String baseAssetName;
  private BigDecimal result;

  // Campos para relacionamento com grupos de operações
  private String roleType; // Tipo de papel no grupo (Original, Parcial, Resultado, etc.)
  private Integer sequenceNumber; // Número de sequência no grupo
  private UUID groupId; // ID do grupo de operações médias
  private Boolean isPartialExit; // Indica se é uma saída parcial
  private Boolean hasPartialExits; // Indica se tem saídas parciais
  private UUID originalOperationId; // ID da operação original (para operações relacionadas)
  private UUID positionId; // ID da posição associada
}
