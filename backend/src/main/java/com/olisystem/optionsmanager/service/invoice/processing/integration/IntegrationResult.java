package com.olisystem.optionsmanager.service.invoice.processing.integration;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Resultado da integração de operações
 * Contém todas as informações sobre o processo de integração
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class IntegrationResult {
    
    // === METADADOS ===
    private boolean success;
    private String errorMessage;
    
    // === ESTATÍSTICAS ===
    private int totalOperations;
    private double successRate; // % de operações processadas com sucesso
    private int totalMappings; // Total de mapeamentos invoice → operation
    
    // === OPERAÇÕES PROCESSADAS ===
    private List<ProcessedOperation> createdOperations;
    private List<ProcessedOperation> updatedOperations;
    private List<ProcessedOperation> failedOperations;
    
    // === TEMPO DE PROCESSAMENTO ===
    private long processingTimeMs;
    
    /**
     * Verifica se a integração foi bem-sucedida
     */
    public boolean isSuccessful() {
        return success && errorMessage == null;
    }
    
    /**
     * Retorna o número total de operações processadas com sucesso
     */
    public int getTotalSuccessfulOperations() {
        return (createdOperations != null ? createdOperations.size() : 0) + 
               (updatedOperations != null ? updatedOperations.size() : 0);
    }
    
    /**
     * Retorna o número total de operações que falharam
     */
    public int getTotalFailedOperations() {
        return failedOperations != null ? failedOperations.size() : 0;
    }
    
    /**
     * Retorna o número total de operações processadas
     */
    public int getTotalProcessedOperations() {
        return getTotalSuccessfulOperations() + getTotalFailedOperations();
    }
} 