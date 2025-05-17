package com.olisystem.optionsmanager.dto.operation;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperationSummaryResponseDto {
  private List<OperationItemDto> operations;

  // Totalizadores
  private Long totalActiveOperations;
  private Long totalPutOperations;
  private Long totalCallOperations;
  private BigDecimal totalEntryValue;
  private Long totalWinningOperations;
  private Long totalLosingOperations;
  private Long totalSwingTradeOperations;
  private Long totalDayTradeOperations;
  private BigDecimal totalProfitLoss;
  private BigDecimal totalProfitLossPercentage;

  // Paginação
  private int currentPage;
  private int totalPages;
  private long totalElements;
  private int pageSize;
}
