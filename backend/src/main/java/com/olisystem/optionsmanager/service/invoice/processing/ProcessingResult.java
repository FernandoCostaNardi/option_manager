package com.olisystem.optionsmanager.service.invoice.processing;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Resultado do processamento de múltiplas invoices
 */
@Data
@Builder
public class ProcessingResult {
    private long startTime;
    private long endTime;
    private int totalInvoices;
    
    @Builder.Default
    private List<InvoiceProcessingResult> invoiceResults = new ArrayList<>();
    
    @Builder.Default  
    private List<String> errors = new ArrayList<>();
    
    public void addInvoiceResult(InvoiceProcessingResult result) {
        if (invoiceResults == null) {
            invoiceResults = new ArrayList<>();
        }
        invoiceResults.add(result);
    }
    
    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }
    
    public int getTotalOperationsCreated() {
        return invoiceResults.stream()
            .mapToInt(InvoiceProcessingResult::getOperationsCreated)
            .sum();
    }
    
    public int getTotalOperationsSkipped() {
        return invoiceResults.stream()
            .mapToInt(InvoiceProcessingResult::getOperationsSkipped)
            .sum();
    }
    
    public long getProcessingTimeMs() {
        return endTime - startTime;
    }
    
    public boolean isSuccess() {
        return errors.isEmpty() && getTotalOperationsCreated() > 0;
    }
    
    public boolean isPartialSuccess() {
        return !errors.isEmpty() && getTotalOperationsCreated() > 0;
    }
}
