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

    // Factory method para criar a partir de um request - CORRIGIDO
    public static OperationBuildExitData fromRequest(OperationExitPositionContext context,
                                                     BigDecimal profitLoss,
                                                     TradeType tradeType,
                                                     TransactionType transactionType,
                                                     Integer totalQuantity) {

        // ✅ CORREÇÃO: Determinar preço de entrada correto baseado no status da Position
        BigDecimal entryUnitPrice;
        boolean isSubsequentOperation = context.position().getStatus() == 
                com.olisystem.optionsmanager.model.position.PositionStatus.PARTIAL;
        
        if (isSubsequentOperation) {
            // Para operações após a primeira parcial, usar preço break-even da Position
            entryUnitPrice = context.position().getAveragePrice();
        } else {
            // Para primeira operação, usar preço original do lote
            entryUnitPrice = context.availableLots().get(0).getUnitPrice();
        }

        // Calcular valores totais baseados na quantidade da operação
        BigDecimal entryTotalValue = entryUnitPrice.multiply(BigDecimal.valueOf(totalQuantity));
        BigDecimal exitTotalValue = context.context().request().getExitUnitPrice()
                .multiply(BigDecimal.valueOf(totalQuantity));

        // Calcular o percentual baseado no lucro e valor de entrada
        BigDecimal profitLossPercentage = BigDecimal.ZERO;
        if (entryTotalValue.compareTo(BigDecimal.ZERO) > 0) {
            profitLossPercentage = profitLoss
                    .divide(entryTotalValue, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return new OperationBuildExitData(
                context.context().activeOperation().getBrokerage().getId(),
                context.context().activeOperation().getAnalysisHouse() != null ?
                        context.context().activeOperation().getAnalysisHouse().getId() : null,
                tradeType,
                transactionType,
                context.context().activeOperation().getEntryDate(),
                context.context().request().getExitDate(),
                totalQuantity,
                entryUnitPrice, // Preço unitário de entrada do lote
                entryTotalValue, // Valor total de entrada calculado
                context.context().request().getExitUnitPrice(),
                exitTotalValue, // Valor total de saída calculado
                profitLoss, // ✅ Usar o valor passado como parâmetro
                profitLossPercentage // ✅ Calcular baseado no profitLoss
        );
    }
}
