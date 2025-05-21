package com.olisystem.optionsmanager.record.operation;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OperationBuildExitData(
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
    public static OperationBuildExitData fromOperation(Operation operation) {
        return new OperationBuildExitData(
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
    public static OperationBuildExitData fromRequest(OperationExitPositionContext context, BigDecimal profitLoss, TradeType tradeType, TransactionType transactionType) {
        BigDecimal entryTotalValue = context.context().activeOperation().getEntryUnitPrice().multiply(BigDecimal.valueOf(context.context().request().getQuantity()));
        BigDecimal exitTotalValue = context.context().request().getExitUnitPrice().multiply(BigDecimal.valueOf(context.context().request().getQuantity()));
        BigDecimal profitLossPercentage = context.position().getTotalRealizedProfitPercentage();
        return new OperationBuildExitData(
                context.context().activeOperation().getBrokerage().getId(),
                context.context().activeOperation().getAnalysisHouse() != null ? context.context().activeOperation().getAnalysisHouse().getId() : null,
                tradeType,
                transactionType,
                context.context().activeOperation().getEntryDate(),
                context.context().request().getExitDate(),
                context.context().request().getQuantity(),
                context.context().activeOperation().getEntryUnitPrice(),
                entryTotalValue,
                context.context().request().getExitUnitPrice(),
                exitTotalValue,
                profitLoss,
                profitLossPercentage
        );
    }
}
