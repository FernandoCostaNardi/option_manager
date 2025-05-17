package com.olisystem.optionsmanager.mapper.operation;

import com.olisystem.optionsmanager.dto.operation.OperationItemDto;
import com.olisystem.optionsmanager.model.operation.Operation;
import org.springframework.stereotype.Component;

@Component
public class OperationItemMapper {

    public static OperationItemDto mapToDto(Operation op) {
        return OperationItemDto.builder()
                .id(op.getId())
                .optionSeriesCode(
                        op.getOptionSeries()
                                .getCode()
                                .toUpperCase()) // Convertendo para maiúsculas para manter consistência
                .optionType(op.getOptionSeries().getType())
                .transactionType(op.getTransactionType())
                .tradeType(op.getTradeType())
                .entryDate(op.getEntryDate())
                .exitDate(op.getExitDate())
                .status(op.getStatus())
                .analysisHouseName(op.getAnalysisHouse() != null ? op.getAnalysisHouse().getName() : null)
                .analysisHouseId(op.getAnalysisHouse() != null ? op.getAnalysisHouse().getId() : null)
                .brokerageName(op.getBrokerage() != null ? op.getBrokerage().getName() : null)
                .brokerageId(op.getBrokerage() != null ? op.getBrokerage().getId() : null)
                .quantity(op.getQuantity())
                .entryUnitPrice(op.getEntryUnitPrice())
                .entryTotalValue(op.getEntryTotalValue())
                .exitUnitPrice(op.getExitUnitPrice())
                .exitTotalValue(op.getExitTotalValue())
                .profitLoss(op.getProfitLoss())
                .profitLossPercentage(op.getProfitLossPercentage())
                .baseAssetLogoUrl(op.getOptionSeries().getAsset().getUrlLogo())
                .build();
    }
}
