package com.olisystem.optionsmanager.mapper;

import com.olisystem.optionsmanager.dto.position.EntryLotDto;
import com.olisystem.optionsmanager.dto.position.ExitRecordDto;
import com.olisystem.optionsmanager.dto.position.PositionDto;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.ExitRecord;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.repository.position.ExitRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PositionMapper {

  @Autowired private ExitRecordRepository exitRecordRepository;

  public PositionDto toDto(Position position) {
    // Calcular dias abertos
    long daysOpen = 0;
    if (position.getOpenDate() != null) {
      LocalDate endDate =
          position.getCloseDate() != null ? position.getCloseDate() : LocalDate.now();
      daysOpen = ChronoUnit.DAYS.between(position.getOpenDate(), endDate);
    }

    // Calcular valor total de entrada
    BigDecimal totalEntryValue =
        position.getAveragePrice().multiply(BigDecimal.valueOf(position.getTotalQuantity()));

    // Verificar se tem múltiplas entradas
    boolean hasMultipleEntries = position.getEntryLots().size() > 1;

    // Verificar se tem saídas parciais
    long exitRecordsCount = exitRecordRepository.findByPositionId(position.getId()).size();
    boolean hasPartialExits = exitRecordsCount > 0 && position.getRemainingQuantity() > 0;

    return PositionDto.builder()
        .id(position.getId())
        .optionSeriesCode(position.getOptionSeries().getCode())
        .optionType(position.getOptionSeries().getType())
        .strikePrice(position.getOptionSeries().getStrikePrice())
        .expirationDate(position.getOptionSeries().getExpirationDate())
        .baseAssetCode(position.getOptionSeries().getAsset().getCode())
        .baseAssetName(position.getOptionSeries().getAsset().getName())
        .baseAssetLogoUrl(position.getOptionSeries().getAsset().getUrlLogo())
        .direction(position.getDirection())
        .status(position.getStatus())
        .openDate(position.getOpenDate())
        .closeDate(position.getCloseDate())
        .totalQuantity(position.getTotalQuantity())
        .averagePrice(position.getAveragePrice())
        .totalEntryValue(totalEntryValue)
        .remainingQuantity(position.getRemainingQuantity())
        .totalRealizedProfit(position.getTotalRealizedProfit())
        .totalRealizedProfitPercentage(position.getTotalRealizedProfitPercentage())
        .brokerageName(position.getBrokerage() != null ? position.getBrokerage().getName() : null)
        .brokerageId(position.getBrokerage() != null ? position.getBrokerage().getId() : null)
        .entryLotsCount(position.getEntryLots().size())
        .exitRecordsCount((int) exitRecordsCount)
        .daysOpen((int) daysOpen)
        .hasMultipleEntries(hasMultipleEntries)
        .hasPartialExits(hasPartialExits)
        .build();
  }

  public EntryLotDto toDto(EntryLot entryLot) {
    return EntryLotDto.builder()
        .id(entryLot.getId())
        .positionId(entryLot.getPosition().getId())
        .entryDate(entryLot.getEntryDate())
        .quantity(entryLot.getQuantity())
        .unitPrice(entryLot.getUnitPrice())
        .totalValue(entryLot.getTotalValue())
        .remainingQuantity(entryLot.getRemainingQuantity())
        .sequenceNumber(entryLot.getSequenceNumber())
        .isFullyConsumed(entryLot.getIsFullyConsumed())
        .build();
  }

  public ExitRecordDto toDto(ExitRecord exitRecord) {
    return ExitRecordDto.builder()
        .id(exitRecord.getId())
        .entryLotId(exitRecord.getEntryLot().getId())
        .exitOperationId(exitRecord.getExitOperation().getId())
        .exitDate(exitRecord.getExitDate())
        .quantity(exitRecord.getQuantity())
        .entryUnitPrice(exitRecord.getEntryUnitPrice())
        .exitUnitPrice(exitRecord.getExitUnitPrice())
        .profitLoss(exitRecord.getProfitLoss())
        .profitLossPercentage(exitRecord.getProfitLossPercentage())
        .appliedStrategy(exitRecord.getAppliedStrategy())
        .entryDate(exitRecord.getEntryLot().getEntryDate())
        .sequenceNumber(exitRecord.getEntryLot().getSequenceNumber())
        .build();
  }

  public List<PositionDto> toDtoList(List<Position> positions) {
    return positions.stream().map(this::toDto).collect(Collectors.toList());
  }

  public List<EntryLotDto> toEntryLotDtoList(List<EntryLot> entryLots) {
    return entryLots.stream().map(this::toDto).collect(Collectors.toList());
  }

  public List<ExitRecordDto> toExitRecordDtoList(List<ExitRecord> exitRecords) {
    return exitRecords.stream().map(this::toDto).collect(Collectors.toList());
  }
}
