package com.olisystem.optionsmanager.dto.position;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompatiblePositionResponse {
  private String id;
  private String optionSeriesCode;
  private String optionType;
  private String baseAssetLogoUrl;
  private Integer totalQuantity;
  private Integer remainingQuantity;
  private BigDecimal averageEntryPrice;
  private BigDecimal currentInvested;
}
