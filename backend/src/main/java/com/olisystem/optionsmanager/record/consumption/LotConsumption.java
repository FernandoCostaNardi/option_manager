package com.olisystem.optionsmanager.record.consumption;

import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.position.EntryLot;

/**
 * Representa o consumo planejado de um lote específico.
 * Contém informações sobre qual lote consumir, quanto consumir e o tipo de trade.
 */
public record LotConsumption(
    EntryLot lot,                    // O lote que será consumido
    int quantityToConsume,           // Quantidade a ser consumida deste lote
    TradeType tradeType              // Tipo de trade (DAY ou SWING)
) {
    
    /**
     * Verifica se o consumo vai esgotar completamente o lote
     */
    public boolean willFullyConsumeLot() {
        return quantityToConsume >= lot.getRemainingQuantity();
    }
    
    /**
     * Calcula quanto restará no lote após o consumo
     */
    public int calculateRemainingAfterConsumption() {
        return lot.getRemainingQuantity() - quantityToConsume;
    }
    
    /**
     * Verifica se é um consumo parcial do lote
     */
    public boolean isPartialConsumption() {
        return quantityToConsume < lot.getRemainingQuantity();
    }
    
    /**
     * Retorna descrição formatada do consumo
     */
    public String getDescription() {
        return String.format(
            "Lote %d: %d/%d unidades (%s)", 
            lot.getSequenceNumber(), 
            quantityToConsume, 
            lot.getRemainingQuantity(), 
            tradeType
        );
    }
}
