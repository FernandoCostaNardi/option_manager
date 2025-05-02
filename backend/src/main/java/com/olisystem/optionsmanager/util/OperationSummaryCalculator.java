package com.olisystem.optionsmanager.util;

import com.olisystem.optionsmanager.dto.OperationItemDto;
import com.olisystem.optionsmanager.dto.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.model.OperationStatus;
import com.olisystem.optionsmanager.model.OptionType;
import com.olisystem.optionsmanager.model.TradeType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class OperationSummaryCalculator {

  public static OperationSummaryResponseDto calculateSummary(List<OperationItemDto> operations) {
    if (operations == null || operations.isEmpty()) {
      return OperationSummaryResponseDto.builder()
          .operations(List.of())
          .totalActiveOperations(0L)
          .totalPutOperations(0L)
          .totalCallOperations(0L)
          .totalEntryValue(BigDecimal.ZERO)
          .totalWinningOperations(0L)
          .totalLosingOperations(0L)
          .totalSwingTradeOperations(0L)
          .totalDayTradeOperations(0L)
          .totalProfitLoss(BigDecimal.ZERO)
          .totalProfitLossPercentage(BigDecimal.ZERO)
          .build();
    }

    // Contadores
    long activeOperations =
        operations.stream().filter(op -> OperationStatus.ACTIVE.equals(op.getStatus())).count();

    long putOperations =
        operations.stream().filter(op -> OptionType.PUT.equals(op.getOptionType())).count();

    long callOperations =
        operations.stream().filter(op -> OptionType.CALL.equals(op.getOptionType())).count();

    BigDecimal totalEntryValue =
        operations.stream()
            .map(OperationItemDto::getEntryTotalValue)
            .filter(value -> value != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    long winningOperations =
        operations.stream()
            .filter(
                op ->
                    op.getProfitLoss() != null && op.getProfitLoss().compareTo(BigDecimal.ZERO) > 0)
            .count();

    long losingOperations =
        operations.stream()
            .filter(
                op ->
                    op.getProfitLoss() != null && op.getProfitLoss().compareTo(BigDecimal.ZERO) < 0)
            .count();

    long swingTradeOperations =
        operations.stream().filter(op -> TradeType.SWING.equals(op.getTradeType())).count();

    long dayTradeOperations =
        operations.stream().filter(op -> TradeType.DAY.equals(op.getTradeType())).count();

    BigDecimal totalProfitLoss =
        operations.stream()
            .map(OperationItemDto::getProfitLoss)
            .filter(value -> value != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Cálculo da porcentagem total de lucro/prejuízo
    BigDecimal totalProfitLossPercentage = BigDecimal.ZERO;
    if (totalEntryValue.compareTo(BigDecimal.ZERO) > 0) {
      totalProfitLossPercentage =
          totalProfitLoss
              .multiply(new BigDecimal("100"))
              .divide(totalEntryValue, 2, RoundingMode.HALF_UP);
    }

    return OperationSummaryResponseDto.builder()
        .operations(operations)
        .totalActiveOperations(activeOperations)
        .totalPutOperations(putOperations)
        .totalCallOperations(callOperations)
        .totalEntryValue(totalEntryValue)
        .totalWinningOperations(winningOperations)
        .totalLosingOperations(losingOperations)
        .totalSwingTradeOperations(swingTradeOperations)
        .totalDayTradeOperations(dayTradeOperations)
        .totalProfitLoss(totalProfitLoss)
        .totalProfitLossPercentage(totalProfitLossPercentage)
        .build();
  }
}
