package com.olisystem.optionsmanager.service.invoice.processing.integration;

import com.olisystem.optionsmanager.service.invoice.processing.detection.ConsolidatedOperation;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Resumo de validação de múltiplas operações
 * Contém estatísticas de validação em lote
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class ValidationSummary {
    
    // === ESTATÍSTICAS ===
    private int totalOperations;
    private double successRate; // % de operações válidas
    
    // === OPERAÇÕES ===
    private List<ConsolidatedOperation> validOperations;
    private List<ConsolidatedOperation> invalidOperations;
    
    /**
     * Verifica se todas as operações são válidas
     */
    public boolean isAllValid() {
        return invalidOperations.isEmpty();
    }
    
    /**
     * Verifica se há operações inválidas
     */
    public boolean hasInvalidOperations() {
        return !invalidOperations.isEmpty();
    }
    
    /**
     * Retorna o número de operações válidas
     */
    public int getValidCount() {
        return validOperations != null ? validOperations.size() : 0;
    }
    
    /**
     * Retorna o número de operações inválidas
     */
    public int getInvalidCount() {
        return invalidOperations != null ? invalidOperations.size() : 0;
    }
    
    /**
     * Retorna descrição resumida
     */
    public String getSummary() {
        return String.format("%d válidas, %d inválidas (%.1f%% sucesso)", 
            getValidCount(), getInvalidCount(), successRate);
    }
} 