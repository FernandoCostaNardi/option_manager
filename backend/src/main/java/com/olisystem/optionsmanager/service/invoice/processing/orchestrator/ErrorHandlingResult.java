package com.olisystem.optionsmanager.service.invoice.processing.orchestrator;

import lombok.Builder;
import lombok.Data;

/**
 * Resultado do tratamento de erro
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class ErrorHandlingResult {
    
    // === CATEGORIZAÇÃO ===
    private ErrorCategory category;
    private Exception originalError;
    private String userMessage;
    private boolean isRecoverable;
    
    // === CONTEXTO ===
    private String context;
    private long timestamp;
    
    /**
     * Verifica se é um erro crítico
     */
    public boolean isCritical() {
        return !isRecoverable;
    }
    
    /**
     * Retorna descrição do erro
     */
    public String getDescription() {
        return String.format("[%s] %s (recuperável: %s)", 
            category, userMessage, isRecoverable ? "sim" : "não");
    }
} 