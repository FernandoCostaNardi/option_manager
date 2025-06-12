package com.olisystem.optionsmanager.record.consumption;

import com.olisystem.optionsmanager.model.operation.TradeType;

import java.math.BigDecimal;

/**
 * Representa dados calculados para criação de operações por tipo de trade.
 * Usado para separar métricas de Day Trade e Swing Trade.
 */
public record TradeOperationData(
    TradeType tradeType,                    // Tipo do trade (DAY ou SWING)
    int quantity,                          // Quantidade deste tipo de trade
    BigDecimal totalProfitLoss,            // P&L total deste tipo
    BigDecimal totalProfitLossPercentage,  // Percentual de P&L deste tipo
    BigDecimal weightedAverageEntryPrice,  // Preço médio ponderado de entrada
    BigDecimal entryTotalValue,            // Valor total de entrada
    BigDecimal exitTotalValue              // Valor total de saída
) {
    
    /**
     * Verifica se há dados válidos para criar operação
     */
    public boolean hasValidData() {
        return quantity > 0 && entryTotalValue.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Verifica se representa um lucro
     */
    public boolean isProfit() {
        return totalProfitLoss.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Verifica se representa uma perda
     */
    public boolean isLoss() {
        return totalProfitLoss.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Retorna descrição formatada
     */
    public String getFormattedSummary() {
        return String.format(
            "%s: %d unidades, P&L: %s (%.2f%%), Preço médio: %s",
            tradeType, quantity, totalProfitLoss, totalProfitLossPercentage, weightedAverageEntryPrice
        );
    }
}
