package com.olisystem.optionsmanager.service.invoice.processing.integration;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapeamento entre Invoice e Operation
 * Representa a relação entre um item de invoice e uma operação criada
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class InvoiceOperationMapping {
    
    // === IDENTIFICAÇÃO ===
    private UUID id;
    private String mappingId; // ID único do mapeamento
    
    // === RELACIONAMENTOS ===
    private Invoice invoice;
    private InvoiceItem invoiceItem;
    private Operation operation;
    
    // === METADADOS ===
    private String mappingType; // NEW_OPERATION, EXISTING_OPERATION_EXIT, etc.
    private String notes;
    private LocalDateTime createdAt;
    
    /**
     * Verifica se é um mapeamento de nova operação
     */
    public boolean isNewOperationMapping() {
        return "NEW_OPERATION".equals(mappingType);
    }
    
    /**
     * Verifica se é um mapeamento de saída de operação existente
     */
    public boolean isExistingOperationExitMapping() {
        return "EXISTING_OPERATION_EXIT".equals(mappingType);
    }
    
    /**
     * Verifica se é um mapeamento de day trade
     */
    public boolean isDayTradeMapping() {
        return mappingType != null && mappingType.contains("DAY_TRADE");
    }
    
    /**
     * Verifica se é um mapeamento de swing trade
     */
    public boolean isSwingTradeMapping() {
        return mappingType != null && mappingType.contains("SWING_TRADE");
    }
} 