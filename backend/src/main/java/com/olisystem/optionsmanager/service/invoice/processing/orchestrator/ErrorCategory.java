package com.olisystem.optionsmanager.service.invoice.processing.orchestrator;

/**
 * Categorias de erro para processamento de invoices
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
public enum ErrorCategory {
    
    /**
     * Erros de validação de dados
     */
    VALIDATION,
    
    /**
     * Erros de duplicata
     */
    DUPLICATE,
    
    /**
     * Erros de detecção de operações
     */
    DETECTION,
    
    /**
     * Erros de integração de operações
     */
    INTEGRATION,
    
    /**
     * Erros de banco de dados
     */
    DATABASE,
    
    /**
     * Erros de rede/conexão
     */
    NETWORK,
    
    /**
     * Erros de sistema
     */
    SYSTEM,
    
    /**
     * Erros desconhecidos
     */
    UNKNOWN
} 