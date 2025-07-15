package com.olisystem.optionsmanager.service.invoice.processing.integration;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.service.invoice.processing.detection.ConsolidatedOperation;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Operação processada durante a integração
 * Representa uma operação que foi processada e integrada ao sistema
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class ProcessedOperation {
    
    // === IDENTIFICAÇÃO ===
    private UUID id;
    private String processingId; // ID único do processamento
    
    // === STATUS ===
    private boolean success;
    private boolean created; // true se foi criada, false se foi atualizada
    private String errorMessage;
    
    // === OPERAÇÃO ===
    private Operation operation; // Operação criada/atualizada no sistema
    private ConsolidatedOperation consolidatedOperation; // Operação fonte
    private OptionSerie optionSerie; // Série de opções associada
    
    // === MAPEAMENTOS ===
    private List<InvoiceOperationMapping> mappings; // Mapeamentos invoice → operation
    
    // === METADADOS ===
    private String notes;
    private long processingTimeMs;
    
    /**
     * Verifica se a operação foi processada com sucesso
     */
    public boolean isProcessedSuccessfully() {
        return success && operation != null;
    }
    
    /**
     * Verifica se a operação foi criada (não atualizada)
     */
    public boolean isNewOperation() {
        return created;
    }
    
    /**
     * Verifica se a operação foi atualizada
     */
    public boolean isUpdatedOperation() {
        return !created;
    }
    
    /**
     * Retorna o número de mapeamentos criados
     */
    public int getMappingsCount() {
        return mappings != null ? mappings.size() : 0;
    }
} 