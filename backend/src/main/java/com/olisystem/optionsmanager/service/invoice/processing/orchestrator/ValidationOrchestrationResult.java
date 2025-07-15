package com.olisystem.optionsmanager.service.invoice.processing.orchestrator;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Resultado da validação durante a orquestração
 * Contém informações sobre a validação de invoices
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class ValidationOrchestrationResult {
    
    // === METADADOS ===
    private int totalInvoices;
    private boolean canProceed;
    private String rejectionReason;
    
    // === ESTATÍSTICAS ===
    private int validCount;
    private int invalidCount;
    private boolean hasDuplicates;
    
    // === INVOICES ===
    private List<UUID> validInvoiceIds;
    private List<UUID> invalidInvoiceIds;
    private List<String> validationErrors;
    
    /**
     * Adiciona erro de validação
     */
    public void addValidationError(String error) {
        if (validationErrors == null) {
            validationErrors = new ArrayList<>();
        }
        validationErrors.add(error);
    }
    
    /**
     * Verifica se há erros de validação
     */
    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty();
    }
    
    /**
     * Retorna descrição resumida
     */
    public String getSummary() {
        return String.format("Validadas %d invoices: %d válidas, %d inválidas, duplicatas: %s", 
            totalInvoices, validCount, invalidCount, hasDuplicates ? "sim" : "não");
    }
} 