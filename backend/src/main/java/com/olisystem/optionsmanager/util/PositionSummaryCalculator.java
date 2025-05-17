package com.olisystem.optionsmanager.util;



/** Utilitário para calcular estatísticas resumidas de posições */
public class PositionSummaryCalculator {

  /** Calcula estatísticas resumidas para uma lista de posições */
//  public static PositionSummaryResponseDto calculateSummary(
//      List<PositionResponseDto> paginatedPositions,
//      List<PositionResponseDto> allPositions,
//      int currentPage,
//      int totalPages,
//      long totalElements,
//      int pageSize) {
//
//    // Contar posições por direção
//    long totalLongPositions =
//        allPositions.stream().filter(p -> p.getDirection() == PositionDirection.LONG).count();
//
//    long totalShortPositions =
//        allPositions.stream().filter(p -> p.getDirection() == PositionDirection.SHORT).count();
//
//    // Calcular totais de investimento
//    BigDecimal totalInvested =
//        allPositions.stream()
//            .map(PositionResponseDto::getCurrentInvested)
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//    // Calcular lucro/prejuízo
//    BigDecimal totalRealizedProfitLoss =
//        allPositions.stream()
//            .map(PositionResponseDto::getRealizedProfitLoss)
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//    BigDecimal totalUnrealizedProfitLoss =
//        allPositions.stream()
//            .map(PositionResponseDto::getUnrealizedProfitLoss)
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//    // Calcular percentuais
//    BigDecimal totalUnrealizedProfitLossPercentage = BigDecimal.ZERO;
//    if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
//      totalUnrealizedProfitLossPercentage =
//          totalUnrealizedProfitLoss
//              .multiply(new BigDecimal(100))
//              .divide(totalInvested, 2, RoundingMode.HALF_UP);
//    }
//
//    return PositionSummaryResponseDto.builder()
//        .currentPage(currentPage)
//        .totalPages(totalPages)
//        .totalElements(totalElements)
//        .pageSize(pageSize)
//        .build();
//  }
}
