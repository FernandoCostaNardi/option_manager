package com.olisystem.optionsmanager.mapper;

import com.olisystem.optionsmanager.dto.OperationCreateRequestDto;
import com.olisystem.optionsmanager.dto.OperationResponseDto;
import com.olisystem.optionsmanager.model.AnalysisHouse;
import com.olisystem.optionsmanager.model.Brokerage;
import com.olisystem.optionsmanager.model.Operation;
import com.olisystem.optionsmanager.model.OptionSerie;

public class OperationMapper {

  public static Operation toEntity(
      OperationCreateRequestDto dto,
      Brokerage brokerage,
      AnalysisHouse analysisHouse,
      OptionSerie optionSeries) {
    return Operation.builder()
        .brokerage(brokerage)
        .analysisHouse(analysisHouse)
        .optionSeries(optionSeries)
        .transactionType(dto.getTransactionType())
        .entryDate(dto.getEntryDate())
        .exitDate(dto.getExitDate())
        .status(dto.getStatus())
        .quantity(dto.getQuantity())
        .entryUnitPrice(dto.getEntryUnitPrice())
        .entryTotalValue(dto.getEntryTotalValue())
        .exitUnitPrice(dto.getExitUnitPrice())
        .exitTotalValue(dto.getExitTotalValue())
        .adjustedAveragePrice(dto.getAdjustedAveragePrice())
        .averagePriceAdjustmentValue(dto.getAveragePriceAdjustmentValue())
        .build();
  }

  public static OperationResponseDto toDto(Operation entity) {
    return OperationResponseDto.builder()
        .id(entity.getId())
        .brokerageId(entity.getBrokerage().getId())
        .brokerageName(entity.getBrokerage().getName())
        .analysisHouseId(
            entity.getAnalysisHouse() != null ? entity.getAnalysisHouse().getId() : null)
        .analysisHouseName(
            entity.getAnalysisHouse() != null ? entity.getAnalysisHouse().getName() : null)
        .optionSeriesId(entity.getOptionSeries().getId())
        .optionSeriesCode(entity.getOptionSeries().getCode())
        .transactionType(entity.getTransactionType())
        .entryDate(entity.getEntryDate())
        .exitDate(entity.getExitDate())
        .status(entity.getStatus())
        .quantity(entity.getQuantity())
        .entryUnitPrice(entity.getEntryUnitPrice())
        .entryTotalValue(entity.getEntryTotalValue())
        .exitUnitPrice(entity.getExitUnitPrice())
        .exitTotalValue(entity.getExitTotalValue())
        .adjustedAveragePrice(entity.getAdjustedAveragePrice())
        .averagePriceAdjustmentValue(entity.getAveragePriceAdjustmentValue())
        .build();
  }
}
