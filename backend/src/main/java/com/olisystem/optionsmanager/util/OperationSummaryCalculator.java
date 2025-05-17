package com.olisystem.optionsmanager.util;

import com.olisystem.optionsmanager.dto.operation.OperationItemDto;
import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.option_serie.OptionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class OperationSummaryCalculator {

  public static OperationSummaryResponseDto calculateSummary(List<OperationItemDto> operations) {
    return calculateSummary(operations, 0, 1, operations.size(), operations.size());
  }

  public static OperationSummaryResponseDto calculateSummary(
      List<OperationItemDto> operations,
      int currentPage,
      int totalPages,
      long totalElements,
      int pageSize) {
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
          .currentPage(currentPage)
          .totalPages(totalPages)
          .totalElements(totalElements)
          .pageSize(pageSize)
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
        .currentPage(currentPage)
        .totalPages(totalPages)
        .totalElements(totalElements)
        .pageSize(pageSize)
        .build();
  }

  /**
   * Calcula o sumário com os totalizadores de todas as operações, mas retorna apenas as operações
   * da página atual
   */
  public static OperationSummaryResponseDto calculateSummaryWithTotals(
      List<OperationItemDto> pagedOperations,
      List<OperationItemDto> allOperations,
      int currentPage,
      int totalPages,
      long totalElements,
      int pageSize) {
    if (pagedOperations == null || pagedOperations.isEmpty()) {
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
          .currentPage(currentPage)
          .totalPages(totalPages)
          .totalElements(totalElements)
          .pageSize(pageSize)
          .build();
    }

    // Contadores usando todas as operações (não apenas da página)
    long activeOperations =
        allOperations.stream().filter(op -> OperationStatus.ACTIVE.equals(op.getStatus())).count();

    long putOperations =
        allOperations.stream().filter(op -> OptionType.PUT.equals(op.getOptionType())).count();

    long callOperations =
        allOperations.stream().filter(op -> OptionType.CALL.equals(op.getOptionType())).count();

    BigDecimal totalEntryValue =
        allOperations.stream()
            .map(OperationItemDto::getEntryTotalValue)
            .filter(value -> value != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    long winningOperations =
        allOperations.stream()
            .filter(
                op ->
                    op.getProfitLoss() != null && op.getProfitLoss().compareTo(BigDecimal.ZERO) > 0)
            .count();

    long losingOperations =
        allOperations.stream()
            .filter(
                op ->
                    op.getProfitLoss() != null && op.getProfitLoss().compareTo(BigDecimal.ZERO) < 0)
            .count();

    long swingTradeOperations =
        allOperations.stream().filter(op -> TradeType.SWING.equals(op.getTradeType())).count();

    long dayTradeOperations =
        allOperations.stream().filter(op -> TradeType.DAY.equals(op.getTradeType())).count();

    BigDecimal totalProfitLoss =
        allOperations.stream()
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
        .operations(pagedOperations) // Apenas as operações da página
        .totalActiveOperations(activeOperations) // Totais de todas as operações
        .totalPutOperations(putOperations)
        .totalCallOperations(callOperations)
        .totalEntryValue(totalEntryValue)
        .totalWinningOperations(winningOperations)
        .totalLosingOperations(losingOperations)
        .totalSwingTradeOperations(swingTradeOperations)
        .totalDayTradeOperations(dayTradeOperations)
        .totalProfitLoss(totalProfitLoss)
        .totalProfitLossPercentage(totalProfitLossPercentage)
        .currentPage(currentPage)
        .totalPages(totalPages)
        .totalElements(totalElements)
        .pageSize(pageSize)
        .build();
  }
}
