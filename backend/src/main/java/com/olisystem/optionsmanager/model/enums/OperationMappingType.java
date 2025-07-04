package com.olisystem.optionsmanager.model.enums;

/**
 * Tipos de mapeamento entre InvoiceItems e Operations
 * 
 * @author Sistema de Gestão de Opções  
 * @since 2025-07-03
 */
public enum OperationMappingType {
    
    /**
     * Nova operação criada a partir do invoice item
     */
    NEW_OPERATION("Nova Operação"),
    
    /**
     * Item usado para finalizar operação existente
     */
    EXISTING_OPERATION_EXIT("Finalização de Operação Existente"),
    
    /**
     * Entrada de Day Trade (compra no mesmo dia)
     */
    DAY_TRADE_ENTRY("Entrada Day Trade"),
    
    /**
     * Saída de Day Trade (venda no mesmo dia)
     */
    DAY_TRADE_EXIT("Saída Day Trade"),
    
    /**
     * Item ignorado (duplicata ou inválido)
     */
    SKIPPED("Ignorado"),
    
    /**
     * Entrada de Swing Trade (operação que permanece aberta)
     */
    SWING_TRADE_ENTRY("Entrada Swing Trade"),
    
    /**
     * Saída de Swing Trade (finaliza operação de outro dia)
     */
    SWING_TRADE_EXIT("Saída Swing Trade");
    
    private final String description;
    
    OperationMappingType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Verifica se o tipo representa entrada de operação
     */
    public boolean isEntry() {
        return this == NEW_OPERATION || this == DAY_TRADE_ENTRY || this == SWING_TRADE_ENTRY;
    }
    
    /**
     * Verifica se o tipo representa saída de operação
     */
    public boolean isExit() {
        return this == EXISTING_OPERATION_EXIT || this == DAY_TRADE_EXIT || this == SWING_TRADE_EXIT;
    }
    
    /**
     * Verifica se o tipo representa Day Trade
     */
    public boolean isDayTrade() {
        return this == DAY_TRADE_ENTRY || this == DAY_TRADE_EXIT;
    }
}