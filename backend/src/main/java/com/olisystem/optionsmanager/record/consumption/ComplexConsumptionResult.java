package com.olisystem.optionsmanager.record.consumption;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Representa o resultado consolidado de um consumo complexo com múltiplos lotes.
 * Agrega todos os resultados individuais e fornece métricas consolidadas.
 */
public record ComplexConsumptionResult(
    List<LotConsumptionResult> results,      // Resultados individuais de cada lote
    BigDecimal totalProfitLoss,              // Lucro/prejuízo total consolidado
    BigDecimal totalDayTradeProfitLoss,      // P&L específico de Day Trade
    BigDecimal totalSwingTradeProfitLoss,    // P&L específico de Swing Trade
    int totalQuantity,                       // Quantidade total consumida
    int dayTradeQuantity,                    // Quantidade de Day Trade
    int swingTradeQuantity,                  // Quantidade de Swing Trade
    LocalDate exitDate,                      // Data da saída
    BigDecimal averageEntryPrice,            // Preço médio ponderado de entrada
    BigDecimal averageExitPrice              // Preço médio de saída
) {
    
    /**
     * Verifica se o resultado tem múltiplos tipos de trade
     */
    public boolean hasMixedTradeTypes() {
        return dayTradeQuantity > 0 && swingTradeQuantity > 0;
    }
    
    /**
     * Verifica se é uma saída parcial
     */
    public boolean isPartialExit() {
        return results.stream()
            .anyMatch(r -> r.quantityConsumed() < r.lot().getQuantity());
    }
    
    /**
     * Calcula percentual total de lucro/prejuízo
     */
    public BigDecimal getTotalProfitLossPercentage() {
        BigDecimal totalEntryValue = results.stream()
            .map(LotConsumptionResult::entryTotalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        if (totalEntryValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalProfitLoss.divide(totalEntryValue, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    public Integer totalQuantityConsumed() {
        return results.stream()
            .map(LotConsumptionResult::quantityConsumed)
            .reduce(0, Integer::sum);   
    }

}
