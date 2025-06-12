package com.olisystem.optionsmanager.record.consumption;

import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.position.EntryLot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Representa um plano de consumo para múltiplos lotes em cenários complexos.
 * Contém a estratégia completa de como os lotes serão consumidos.
 */
public record ComplexConsumptionPlan(
    List<LotConsumption> consumptions,      // Lista de lotes a serem consumidos
    int totalQuantity,                      // Quantidade total a ser consumida
    LocalDate exitDate,                     // Data da saída
    String strategy                         // Estratégia aplicada (AUTO, FIFO_ONLY, LIFO_ONLY)
) {
    
    /**
     * Verifica se o plano envolve múltiplos tipos de trade
     */
    public boolean hasMixedTradeTypes() {
        return consumptions.stream()
            .map(LotConsumption::tradeType)
            .distinct()
            .count() > 1;
    }
    
    /**
     * Calcula quantidade total de Day Trade
     */
    public int getDayTradeQuantity() {
        return consumptions.stream()
            .filter(c -> c.tradeType() == TradeType.DAY)
            .mapToInt(LotConsumption::quantityToConsume)
            .sum();
    }
    
    /**
     * Calcula quantidade total de Swing Trade
     */
    public int getSwingTradeQuantity() {
        return consumptions.stream()
            .filter(c -> c.tradeType() == TradeType.SWING)
            .mapToInt(LotConsumption::quantityToConsume)
            .sum();
    }
    
    /**
     * Retorna consumos apenas de Day Trade
     */
    public List<LotConsumption> getDayTradeConsumptions() {
        return consumptions.stream()
            .filter(c -> c.tradeType() == TradeType.DAY)
            .toList();
    }
    
    /**
     * Retorna consumos apenas de Swing Trade
     */
    public List<LotConsumption> getSwingTradeConsumptions() {
        return consumptions.stream()
            .filter(c -> c.tradeType() == TradeType.SWING)
            .toList();
    }
}
