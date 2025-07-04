package com.olisystem.optionsmanager.model.enums;

/**
 * Status do processamento de invoices para criação de operações
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
public enum InvoiceProcessingStatus {
    
    /**
     * Invoice foi importada mas ainda não processada para criar operações
     */
    PENDING("Pendente"),
    
    /**
     * Processamento em andamento
     */
    PROCESSING("Processando"),
    
    /**
     * Todos os itens da invoice foram processados com sucesso
     */
    SUCCESS("Sucesso"),
    
    /**
     * Alguns itens foram processados, outros ignorados (duplicatas, etc)
     */
    PARTIAL_SUCCESS("Sucesso Parcial"),
    
    /**
     * Erro durante o processamento, nenhuma operação foi criada
     */
    ERROR("Erro"),
    
    /**
     * Processamento foi cancelado pelo usuário ou sistema
     */
    CANCELLED("Cancelado");
    
    private final String description;
    
    InvoiceProcessingStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Verifica se o status indica processamento finalizado
     */
    public boolean isFinished() {
        return this == SUCCESS || this == PARTIAL_SUCCESS || this == ERROR || this == CANCELLED;
    }
    
    /**
     * Verifica se o status indica sucesso (total ou parcial)
     */
    public boolean isSuccessful() {
        return this == SUCCESS || this == PARTIAL_SUCCESS;
    }
}