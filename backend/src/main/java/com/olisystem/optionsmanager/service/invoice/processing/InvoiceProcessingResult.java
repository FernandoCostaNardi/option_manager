package com.olisystem.optionsmanager.service.invoice.processing;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Resultado do processamento de uma invoice individual
 */
@Data
@Builder
public class InvoiceProcessingResult {
    private UUID invoiceId;
    private String invoiceNumber;
    private int totalItems;
    
    @Builder.Default
    private List<UUID> createdOperations = new ArrayList<>();
    
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    
    public void addCreatedOperation(UUID operationId) {
        if (createdOperations == null) {
            createdOperations = new ArrayList<>();
        }
        createdOperations.add(operationId);
    }
    
    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }
    
    public int getOperationsCreated() {
        return createdOperations != null ? createdOperations.size() : 0;
    }
    
    public int getOperationsSkipped() {
        return totalItems - getOperationsCreated();
    }
    
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    public boolean isFullyProcessed() {
        return !hasErrors() && getOperationsCreated() == totalItems;
    }
}
