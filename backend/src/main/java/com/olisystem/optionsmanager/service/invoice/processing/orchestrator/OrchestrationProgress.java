package com.olisystem.optionsmanager.service.invoice.processing.orchestrator;

import lombok.Builder;
import lombok.Data;

/**
 * Progresso da orquestração de processamento
 * Usado para acompanhar o progresso em tempo real
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class OrchestrationProgress {
    
    // === PROGRESSO ===
    private int percentage; // 0-100
    private String message;
    private long timestamp;
    
    /**
     * Verifica se o progresso está completo
     */
    public boolean isComplete() {
        return percentage >= 100;
    }
    
    /**
     * Verifica se o progresso está em andamento
     */
    public boolean isInProgress() {
        return percentage > 0 && percentage < 100;
    }
    
    /**
     * Retorna descrição do progresso
     */
    public String getDescription() {
        return String.format("%d%% - %s", percentage, message);
    }
} 