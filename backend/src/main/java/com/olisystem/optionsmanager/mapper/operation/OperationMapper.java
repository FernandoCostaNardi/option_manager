package com.olisystem.optionsmanager.mapper.operation;

import com.olisystem.optionsmanager.dto.operation.OperationItemDto;
import com.olisystem.optionsmanager.model.operation.Operation;

import org.springframework.stereotype.Component;

@Component
public class OperationMapper {

  public OperationItemDto toDto(Operation operation) {
    return OperationItemDto.builder()
        .id(operation.getId())
        .optionSeriesCode(operation.getOptionSeries().getCode())
        .baseAssetName(operation.getOptionSeries().getAsset().getName())
        .baseAssetLogoUrl(operation.getOptionSeries().getAsset().getUrlLogo())
        .analysisHouseName(operation.getAnalysisHouse() != null ? operation.getAnalysisHouse().getName() : null)
        .brokerageName(operation.getBrokerage().getName())
        .transactionType(operation.getTransactionType())
        .tradeType(operation.getTradeType())
        .entryDate(operation.getEntryDate())
        .exitDate(operation.getExitDate())
        .quantity(operation.getQuantity())
        .entryUnitPrice(operation.getEntryUnitPrice())
        .exitUnitPrice(operation.getExitUnitPrice())
        .entryTotalValue(operation.getEntryTotalValue())
        .exitTotalValue(operation.getExitTotalValue())
        .result(operation.getResult())
        .status(operation.getStatus())
        .build();
  }
} 