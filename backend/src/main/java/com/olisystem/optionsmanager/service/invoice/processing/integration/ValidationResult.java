package com.olisystem.optionsmanager.service.invoice.processing.integration;

import lombok.Builder;
import lombok.Data;

/**
 * Resultado da validação de uma operação
 * Contém as informações sobre a validação realizada
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class ValidationResult {
    
    // === STATUS ===
    private boolean valid;
    private String errorMessage;
    private String warningMessage;
    
    // === ESTATÍSTICAS ===
    private int errorCount;
    private int warningCount;
    
    /**
     * Verifica se a validação foi bem-sucedida
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Verifica se há erros
     */
    public boolean hasErrors() {
        return errorCount > 0;
    }
    
    /**
     * Verifica se há avisos
     */
    public boolean hasWarnings() {
        return warningCount > 0;
    }
    
    /**
     * Retorna descrição resumida
     */
    public String getSummary() {
        if (valid) {
            if (warningCount > 0) {
                return String.format("Válida com %d aviso(s)", warningCount);
            } else {
                return "Válida";
            }
        } else {
            return String.format("Inválida com %d erro(s)", errorCount);
        }
    }
} 