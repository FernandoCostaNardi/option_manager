package com.olisystem.optionsmanager.service.invoice.processing.orchestrator;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Relatório de erros do processamento
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class ErrorReport {
    
    // === ESTATÍSTICAS ===
    private int totalErrors;
    private int recoverableErrors;
    private int nonRecoverableErrors;
    
    // === ERROS POR CATEGORIA ===
    private Map<ErrorCategory, List<ErrorHandlingResult>> errorsByCategory;
    
    /**
     * Adiciona erro por categoria
     */
    public void addErrorByCategory(ErrorCategory category, ErrorHandlingResult error) {
        if (errorsByCategory == null) {
            errorsByCategory = new HashMap<>();
        }
        
        errorsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(error);
    }
    
    /**
     * Obtém erros de uma categoria específica
     */
    public List<ErrorHandlingResult> getErrorsByCategory(ErrorCategory category) {
        return errorsByCategory != null ? 
            errorsByCategory.getOrDefault(category, new ArrayList<>()) : 
            new ArrayList<>();
    }
    
    /**
     * Verifica se há erros críticos
     */
    public boolean hasCriticalErrors() {
        return nonRecoverableErrors > 0;
    }
    
    /**
     * Verifica se há erros recuperáveis
     */
    public boolean hasRecoverableErrors() {
        return recoverableErrors > 0;
    }
    
    /**
     * Calcula taxa de erro
     */
    public double getErrorRate() {
        return totalErrors > 0 ? (double) nonRecoverableErrors / totalErrors * 100 : 0;
    }
    
    /**
     * Retorna descrição resumida
     */
    public String getSummary() {
        return String.format("Total: %d erros (%d críticos, %d recuperáveis, %.1f%% taxa de erro)", 
            totalErrors, nonRecoverableErrors, recoverableErrors, getErrorRate());
    }
    
    /**
     * Obtém categoria com mais erros
     */
    public ErrorCategory getMostFrequentCategory() {
        if (errorsByCategory == null || errorsByCategory.isEmpty()) {
            return null;
        }
        
        return errorsByCategory.entrySet().stream()
            .max((e1, e2) -> Integer.compare(e1.getValue().size(), e2.getValue().size()))
            .map(Map.Entry::getKey)
            .orElse(null);
    }
} 