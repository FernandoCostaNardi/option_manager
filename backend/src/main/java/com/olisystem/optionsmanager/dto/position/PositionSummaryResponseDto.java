package com.olisystem.optionsmanager.dto.position;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionSummaryResponseDto {
  private List<PositionDto> positions;
  private int currentPage;
  private int totalPages;
  private long totalElements;
  private int pageSize;

  // Estatísticas
  private long totalOpenPositions;
  private long totalPartialPositions;
  private long totalClosedPositions;
  private BigDecimal totalInvestedValue;
  private BigDecimal totalRealizedProfit;
  private BigDecimal totalRealizedProfitPercentage;
  private long totalBuyPositions;
  private long totalSellPositions;
  private long totalCallOptions;
  private long totalPutOptions;
}
