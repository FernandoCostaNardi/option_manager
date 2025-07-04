package com.olisystem.optionsmanager.dto.invoice.processing;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request para processamento de invoices
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceProcessingRequest {

    @NotEmpty(message = "Lista de invoices não pode estar vazia")
    @Size(max = 5, message = "Máximo 5 invoices por processamento")
    private List<UUID> invoiceIds;

    private boolean forceReprocessing;

    private ProcessingOptions options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingOptions {
        
        @Builder.Default
        private boolean skipValidation = false;
        
        @Builder.Default
        private boolean skipDuplicateCheck = false;
        
        @Builder.Default
        private boolean continueOnError = true;
        
        @Builder.Default
        private int maxRetries = 3;
        
        private String notes;
    }
}