package com.olisystem.optionsmanager.record.consumption;

import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.position.EntryLot;

import java.math.BigDecimal;

/**
 * Representa o resultado do consumo de um lote específico.
 * Contém os cálculos financeiros e dados processados após o consumo.
 */
public record LotConsumptionResult(
    EntryLot lot,                           // O lote que foi consumido
    int quantityConsumed,                   // Quantidade efetivamente consumida
    TradeType tradeType,                    // Tipo de trade aplicado
    BigDecimal entryUnitPrice,              // Preço unitário de entrada (do lote)
    BigDecimal exitUnitPrice,               // Preço unitário de saída
    BigDecimal profitLoss,                  // Lucro/prejuízo calculado para este consumo
    BigDecimal profitLossPercentage,        // Percentual de lucro/prejuízo
    BigDecimal entryTotalValue,             // Valor total da entrada (quantidade × preço entrada)
    BigDecimal exitTotalValue               // Valor total da saída (quantidade × preço saída)
) {
    
    /**
     * Verifica se o resultado representa um lucro
     */
    public boolean isProfit() {
        return profitLoss.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Verifica se o resultado representa uma perda
     */
    public boolean isLoss() {
        return profitLoss.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Verifica se é break even (nem lucro nem perda)
     */
    public boolean isBreakEven() {
        return profitLoss.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Retorna descrição formatada do resultado
     */
    public String getFormattedResult() {
        return String.format(
            "Lote %d: %d unidades @ %s → %s | P&L: %s (%.2f%%)",
            lot.getSequenceNumber(),
            quantityConsumed,
            entryUnitPrice,
            exitUnitPrice,
            profitLoss,
            profitLossPercentage
        );
    }
}
