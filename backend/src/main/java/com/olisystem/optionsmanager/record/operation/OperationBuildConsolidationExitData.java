package com.olisystem.optionsmanager.record.operation;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OperationBuildConsolidationExitData(
        UUID brokerageId,
        UUID analysisHouseId,
        TradeType tradeType,
        TransactionType transactionType,
        LocalDate entryDate,
        LocalDate exitDate,
        Integer quantity,
        BigDecimal entryUnitPrice,
        BigDecimal entryTotalValue,
        BigDecimal exitUnitPrice,
        BigDecimal exitTotalValue,
        BigDecimal profitLoss,
        BigDecimal profitLossPercentage
) {
    // Factory method para criar a partir de uma operação existente
    public static OperationBuildConsolidationExitData fromOperation(Operation operation) {
        return new OperationBuildConsolidationExitData(
                operation.getBrokerage().getId(),
                operation.getAnalysisHouse() != null ? operation.getAnalysisHouse().getId() : null,
                operation.getTradeType(),
                operation.getTransactionType(),
                operation.getEntryDate(),
                operation.getExitDate(),
                operation.getQuantity(),
                operation.getEntryUnitPrice(),
                operation.getEntryTotalValue(),
                operation.getExitUnitPrice(),
                operation.getExitTotalValue(),
                operation.getProfitLoss(),
                operation.getProfitLossPercentage()
        );
    }

    // Factory method para criar a partir de um request
    public static OperationBuildConsolidationExitData fromRequest(OperationExitPositionContext context, Operation operation) {
        Integer qtyExit = context.group().getClosedQuantity();
        BigDecimal entryTotalValue = context.context().activeOperation().getEntryTotalValue();
        BigDecimal exitTotalValue = context.group().getAvgExitPrice().multiply(BigDecimal.valueOf(qtyExit));
        return new OperationBuildConsolidationExitData(
                operation.getBrokerage().getId(),
                operation.getAnalysisHouse() != null ? context.context().activeOperation().getAnalysisHouse().getId() : null,
                operation.getTradeType(),
                operation.getTransactionType(),
                context.context().activeOperation().getEntryDate(),
                operation.getExitDate(),
                qtyExit,
                context.position().getAveragePrice(),
                entryTotalValue,
                context.group().getAvgExitPrice(),
                exitTotalValue,
                context.position().getTotalRealizedProfit(),
                context.position().getTotalRealizedProfitPercentage()
        );
    }


}
