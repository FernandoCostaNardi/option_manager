package com.olisystem.optionsmanager.dto.operation;

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
public class OperationFinalizationResponse {
  // Mensagem e operações envolvidas
  private String message;
  private OperationItemDto originalOperation;
  private OperationItemDto resultOperation;
  private List<OperationItemDto> operations;

  // Paginação
  private int currentPage;
  private int totalPages;
  private long totalElements;
  private int pageSize;

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
}
