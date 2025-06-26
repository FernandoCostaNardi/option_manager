package com.olisystem.optionsmanager.service.position;

import com.olisystem.optionsmanager.dto.position.EntryLotDto;
import com.olisystem.optionsmanager.dto.position.ExitRecordDto;
import com.olisystem.optionsmanager.dto.position.PositionDto;
import com.olisystem.optionsmanager.dto.position.PositionSummaryResponseDto;
import com.olisystem.optionsmanager.model.option_serie.OptionType;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.ExitStrategy;
import com.olisystem.optionsmanager.model.position.PositionStatus;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PositionCalculator {

  private static final int PRECISION = 6;

  /** Calcula o preço médio de entrada com base nos lotes restantes */
  public BigDecimal calculateRemainingAveragePrice(List<EntryLot> entryLots) {
    BigDecimal totalRemainingQuantity = BigDecimal.ZERO;
    BigDecimal weightedSum = BigDecimal.ZERO;

    for (EntryLot lot : entryLots) {
      if (lot.getRemainingQuantity() > 0) {
        BigDecimal lotQuantity = BigDecimal.valueOf(lot.getRemainingQuantity());
        totalRemainingQuantity = totalRemainingQuantity.add(lotQuantity);
        weightedSum = weightedSum.add(lot.getUnitPrice().multiply(lotQuantity));
      }
    }

    if (totalRemainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
      return weightedSum.divide(totalRemainingQuantity, PRECISION, RoundingMode.HALF_UP);
    }

    return BigDecimal.ZERO;
  }

  /** Processa lotes para saída usando a estratégia FIFO */
  public List<ExitRecordDto> processExitFIFO(
      List<EntryLotDto> availableLots,
      Integer totalExitQuantity,
      BigDecimal exitUnitPrice,
      LocalDate exitDate) {
    List<ExitRecordDto> exitRecords = new ArrayList<>();

    // Ordenar por data de entrada (mais antiga primeiro)
    List<EntryLotDto> sortedLots = new ArrayList<>(availableLots);
    sortedLots.sort(
        Comparator.comparing(EntryLotDto::getEntryDate)
            .thenComparing(EntryLotDto::getSequenceNumber));

    int remainingToExit = totalExitQuantity;

    for (EntryLotDto lot : sortedLots) {
      if (remainingToExit <= 0) break;

      // Quantidade a ser consumida deste lote
      int quantityFromThisLot = Math.min(lot.getRemainingQuantity(), remainingToExit);

      if (quantityFromThisLot > 0) {
        // Calcular lucro/prejuízo
        BigDecimal profitLoss =
            calculateProfitLoss(lot.getUnitPrice(), exitUnitPrice, quantityFromThisLot);
        BigDecimal profitLossPercentage =
            calculateProfitLossPercentage(lot.getUnitPrice(), exitUnitPrice);

        ExitRecordDto exitRecord =
            ExitRecordDto.builder()
                .entryLotId(lot.getId())
                .exitDate(exitDate)
                .quantity(quantityFromThisLot)
                .entryUnitPrice(lot.getUnitPrice())
                .exitUnitPrice(exitUnitPrice)
                .profitLoss(profitLoss)
                .profitLossPercentage(profitLossPercentage)
                .appliedStrategy(ExitStrategy.FIFO)
                .entryDate(lot.getEntryDate())
                .sequenceNumber(lot.getSequenceNumber())
                .build();

        exitRecords.add(exitRecord);

        // Atualizar quantidade restante do lote
        lot.setRemainingQuantity(lot.getRemainingQuantity() - quantityFromThisLot);
        lot.setIsFullyConsumed(lot.getRemainingQuantity() == 0);

        // Atualizar quantidade restante a sair
        remainingToExit -= quantityFromThisLot;
      }
    }

    return exitRecords;
  }

  /** Processa lotes para saída usando a estratégia LIFO */
  public List<ExitRecordDto> processExitLIFO(
      List<EntryLotDto> availableLots,
      Integer totalExitQuantity,
      BigDecimal exitUnitPrice,
      LocalDate exitDate) {
    List<ExitRecordDto> exitRecords = new ArrayList<>();

    // Ordenar por data de entrada (mais recente primeiro)
    List<EntryLotDto> sortedLots = new ArrayList<>(availableLots);
    sortedLots.sort(
        Comparator.comparing(EntryLotDto::getEntryDate)
            .reversed()
            .thenComparing(Comparator.comparing(EntryLotDto::getSequenceNumber).reversed()));

    int remainingToExit = totalExitQuantity;

    for (EntryLotDto lot : sortedLots) {
      if (remainingToExit <= 0) break;

      // Quantidade a ser consumida deste lote
      int quantityFromThisLot = Math.min(lot.getRemainingQuantity(), remainingToExit);

      if (quantityFromThisLot > 0) {
        // Calcular lucro/prejuízo
        BigDecimal profitLoss =
            calculateProfitLoss(lot.getUnitPrice(), exitUnitPrice, quantityFromThisLot);
        BigDecimal profitLossPercentage =
            calculateProfitLossPercentage(lot.getUnitPrice(), exitUnitPrice);

        ExitRecordDto exitRecord =
            ExitRecordDto.builder()
                .entryLotId(lot.getId())
                .exitDate(exitDate)
                .quantity(quantityFromThisLot)
                .entryUnitPrice(lot.getUnitPrice())
                .exitUnitPrice(exitUnitPrice)
                .profitLoss(profitLoss)
                .profitLossPercentage(profitLossPercentage)
                .appliedStrategy(ExitStrategy.LIFO)
                .entryDate(lot.getEntryDate())
                .sequenceNumber(lot.getSequenceNumber())
                .build();

        exitRecords.add(exitRecord);

        // Atualizar quantidade restante do lote
        lot.setRemainingQuantity(lot.getRemainingQuantity() - quantityFromThisLot);
        lot.setIsFullyConsumed(lot.getRemainingQuantity() == 0);

        // Atualizar quantidade restante a sair
        remainingToExit -= quantityFromThisLot;
      }
    }

    return exitRecords;
  }

  /**
   * Processa lotes para saída usando a estratégia AUTO (LIFO para mesmo dia, FIFO para dias
   * anteriores)
   */
  public List<ExitRecordDto> processExitAuto(
      List<EntryLotDto> availableLots,
      Integer totalExitQuantity,
      BigDecimal exitUnitPrice,
      LocalDate exitDate) {
    List<ExitRecordDto> exitRecords = new ArrayList<>();

    // Separar lotes do mesmo dia e de dias anteriores
    List<EntryLotDto> sameDayLots =
        availableLots.stream()
            .filter(lot -> lot.getEntryDate().equals(exitDate))
            .collect(Collectors.toList());

    List<EntryLotDto> previousDaysLots =
        availableLots.stream()
            .filter(lot -> lot.getEntryDate().isBefore(exitDate))
            .collect(Collectors.toList());

    int remainingToExit = totalExitQuantity;

    // Se houver lote do mesmo dia suficiente, só consome ele (day trade puro)
    int totalSameDayQty = sameDayLots.stream().mapToInt(EntryLotDto::getRemainingQuantity).sum();
    if (totalSameDayQty >= remainingToExit) {
      List<ExitRecordDto> lifoRecords =
          processExitLIFO(sameDayLots, remainingToExit, exitUnitPrice, exitDate);
      exitRecords.addAll(lifoRecords);
      remainingToExit = 0;
    } else {
      // Primeiro consome o que tem do mesmo dia (se houver)
      if (!sameDayLots.isEmpty()) {
        List<ExitRecordDto> lifoRecords =
            processExitLIFO(sameDayLots, totalSameDayQty, exitUnitPrice, exitDate);
        exitRecords.addAll(lifoRecords);
        remainingToExit -= totalSameDayQty;
      }
      // Depois consome lotes de dias anteriores (FIFO)
      if (remainingToExit > 0 && !previousDaysLots.isEmpty()) {
        List<ExitRecordDto> fifoRecords =
            processExitFIFO(previousDaysLots, remainingToExit, exitUnitPrice, exitDate);
        exitRecords.addAll(fifoRecords);
      }
    }
    return exitRecords;
  }

  /** Calcula o lucro/prejuízo para uma saída */
  public BigDecimal calculateProfitLoss(BigDecimal entryPrice, BigDecimal exitPrice, int quantity) {
    BigDecimal entryValue = entryPrice.multiply(BigDecimal.valueOf(quantity));
    BigDecimal exitValue = exitPrice.multiply(BigDecimal.valueOf(quantity));

    return exitValue.subtract(entryValue);
  }

  /** Calcula o percentual de lucro/prejuízo */
  public BigDecimal calculateProfitLossPercentage(BigDecimal entryPrice, BigDecimal exitPrice) {
    if (entryPrice.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }

    return exitPrice
        .subtract(entryPrice)
        .divide(entryPrice, PRECISION, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100));
  }

  /** Calcula totais para o resumo de posições */
  public PositionSummaryResponseDto calculateSummary(
      List<PositionDto> positions,
      int currentPage,
      int totalPages,
      long totalElements,
      int pageSize) {
    // Estatísticas da página atual
    long openPositions =
        positions.stream().filter(p -> p.getStatus() == PositionStatus.OPEN).count();
    long partialPositions =
        positions.stream().filter(p -> p.getStatus() == PositionStatus.PARTIAL).count();
    long closedPositions =
        positions.stream().filter(p -> p.getStatus() == PositionStatus.CLOSED).count();

    BigDecimal totalInvestedValue =
        positions.stream()
            .filter(p -> p.getStatus() != PositionStatus.CLOSED)
            .map(p -> p.getAveragePrice().multiply(BigDecimal.valueOf(p.getRemainingQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalRealizedProfit =
        positions.stream()
            .map(
                p ->
                    p.getTotalRealizedProfit() != null
                        ? p.getTotalRealizedProfit()
                        : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Calcular percentual médio ponderado pelo valor
    BigDecimal totalValue = BigDecimal.ZERO;
    BigDecimal weightedPercentage = BigDecimal.ZERO;

    for (PositionDto p : positions) {
      if (p.getTotalRealizedProfit() != null
          && p.getTotalRealizedProfit().compareTo(BigDecimal.ZERO) != 0) {
        BigDecimal absProfit = p.getTotalRealizedProfit().abs();
        totalValue = totalValue.add(absProfit);

        if (p.getTotalRealizedProfitPercentage() != null) {
          weightedPercentage =
              weightedPercentage.add(absProfit.multiply(p.getTotalRealizedProfitPercentage()));
        }
      }
    }

    BigDecimal avgProfitPercentage = BigDecimal.ZERO;
    if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
      avgProfitPercentage = weightedPercentage.divide(totalValue, PRECISION, RoundingMode.HALF_UP);
    }

    // Contadores por tipo
    long buyPositions =
        positions.stream().filter(p -> p.getDirection() == TransactionType.BUY).count();
    long sellPositions =
        positions.stream().filter(p -> p.getDirection() == TransactionType.SELL).count();

    long callOptions = positions.stream().filter(p -> p.getOptionType() == OptionType.CALL).count();
    long putOptions = positions.stream().filter(p -> p.getOptionType() == OptionType.PUT).count();

    return PositionSummaryResponseDto.builder()
        .positions(positions)
        .currentPage(currentPage)
        .totalPages(totalPages)
        .totalElements(totalElements)
        .pageSize(pageSize)
        .totalOpenPositions(openPositions)
        .totalPartialPositions(partialPositions)
        .totalClosedPositions(closedPositions)
        .totalInvestedValue(totalInvestedValue)
        .totalRealizedProfit(totalRealizedProfit)
        .totalRealizedProfitPercentage(avgProfitPercentage)
        .totalBuyPositions(buyPositions)
        .totalSellPositions(sellPositions)
        .totalCallOptions(callOptions)
        .totalPutOptions(putOptions)
        .build();
  }
}
