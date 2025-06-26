package com.olisystem.optionsmanager.mapper.operation;

import com.olisystem.optionsmanager.dto.operation.OperationItemDto;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OperationItemMapper {

    private final AverageOperationService averageOperationService;

    public OperationItemDto mapToDto(Operation op) {
        // Buscar o grupo da operação
        AverageOperationGroup group = averageOperationService.getGroupByOperation(op);
        
        // Buscar o roleType da operação no grupo
        String roleType = null;
        Integer sequenceNumber = null;
        if (group != null) {
            var operationItem = group.getItems().stream()
                    .filter(item -> item.getOperation().getId().equals(op.getId()))
                    .findFirst();
            if (operationItem.isPresent()) {
                roleType = operationItem.get().getRoleType().getDescription();
                sequenceNumber = operationItem.get().getSequenceNumber();
            }
        }
        
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
                .roleType(roleType)
                .sequenceNumber(sequenceNumber)
                .groupId(group != null ? group.getId() : null)
                .build();
    }
}
