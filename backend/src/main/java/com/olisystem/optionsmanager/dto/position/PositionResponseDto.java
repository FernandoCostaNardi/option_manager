package com.olisystem.optionsmanager.dto.position;

import com.olisystem.optionsmanager.model.position.PositionDirection;
import com.olisystem.optionsmanager.model.position.PositionStatus;
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
public class PositionResponseDto {
  private String id;
  private String optionSeriesCode;
  private String optionType;
  private String baseAssetLogoUrl;
  private PositionDirection direction;
  private PositionStatus status;
  private LocalDate creationDate;
  private LocalDate lastUpdateDate;
  private BigDecimal averageEntryPrice;
  private BigDecimal averageExitPrice;
  private Integer totalQuantity;
  private Integer remainingQuantity;
  private BigDecimal totalInvested;
  private BigDecimal currentInvested;
  private BigDecimal realizedProfitLoss;
  private BigDecimal unrealizedProfitLoss;
  private BigDecimal realizedProfitLossPercentage;
  private BigDecimal unrealizedProfitLossPercentage;
  private String brokerageName;
  private String analysisHouseName;
}
