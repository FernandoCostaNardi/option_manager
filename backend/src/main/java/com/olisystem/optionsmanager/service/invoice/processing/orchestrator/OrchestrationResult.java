package com.olisystem.optionsmanager.service.invoice.processing.orchestrator;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.service.invoice.processing.detection.DetectionResult;
import com.olisystem.optionsmanager.service.invoice.processing.integration.IntegrationResult;
import com.olisystem.optionsmanager.service.invoice.processing.integration.ValidationSummary;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Resultado principal da orquestração de processamento de invoices
 * Contém todas as informações sobre o processamento completo
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class OrchestrationResult {
    
    // === METADADOS ===
    private boolean success;
    private String errorMessage;
    private long startTime;
    private long endTime;
    
    // === ESTATÍSTICAS GERAIS ===
    private int totalInvoices;
    private double overallSuccessRate; // % de sucesso geral
    
    // === ESTATÍSTICAS POR ETAPA ===
    private int validInvoicesCount;
    private int invalidInvoicesCount;
    private int detectedOperationsCount;
    private int consolidatedOperationsCount;
    private int createdOperationsCount;
    private int failedOperationsCount;
    
    // === RESULTADOS DETALHADOS ===
    private ValidationOrchestrationResult validationResult;
    private DetectionResult detectionResult;
    private ValidationSummary integrationValidation;
    private IntegrationResult integrationResult;
    private List<Invoice> processedInvoices;
    
    /**
     * Verifica se a orquestração foi bem-sucedida
     */
    public boolean isSuccessful() {
        return success && errorMessage == null;
    }
    
    /**
     * Calcula o tempo total de processamento
     */
    public long getProcessingTimeMs() {
        if (startTime > 0 && endTime > 0) {
            return endTime - startTime;
        }
        return 0;
    }
    
    /**
     * Retorna descrição resumida
     */
    public String getSummary() {
        return String.format("Processadas %d invoices: %d válidas, %d operações criadas (%.1f%% sucesso)", 
            totalInvoices, validInvoicesCount, createdOperationsCount, overallSuccessRate);
    }
    
    /**
     * Verifica se há operações criadas
     */
    public boolean hasCreatedOperations() {
        return createdOperationsCount > 0;
    }
    
    /**
     * Verifica se há erros
     */
    public boolean hasErrors() {
        return failedOperationsCount > 0 || invalidInvoicesCount > 0;
    }
} 