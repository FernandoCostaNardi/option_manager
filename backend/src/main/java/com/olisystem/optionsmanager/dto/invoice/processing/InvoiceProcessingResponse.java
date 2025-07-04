package com.olisystem.optionsmanager.dto.invoice.processing;

import com.olisystem.optionsmanager.model.enums.InvoiceProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response do processamento de invoices
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceProcessingResponse {

    private UUID sessionId;
    private boolean successful;
    private InvoiceProcessingStatus status;
    private String summary;
    
    // Estatísticas
    private ProcessingStatistics statistics;
    
    // Operações criadas
    private List<OperationSummary> createdOperations;
    private List<OperationSummary> finalizedOperations;
    
    // Erros e avisos
    private List<ProcessingError> errors;
    private List<String> warnings;
    
    // Timing
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Duration processingDuration;
    
    // Links para recursos relacionados
    private List<ResourceLink> relatedResources;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingStatistics {
        private int totalInvoices;
        private int processedInvoices;
        private int failedInvoices;
        private int totalItems;
        private int processedItems;
        private int successfulItems;
        private int failedItems;
        private int skippedItems;
        private int operationsCreated;
        private int operationsFinalized;
        private double successRate;
        private double itemSuccessRate;
        
        public boolean isFullySuccessful() {
            return failedInvoices == 0 && failedItems == 0;
        }
        
        public boolean hasPartialSuccess() {
            return processedInvoices > 0 && failedInvoices > 0;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationSummary {
        private UUID operationId;
        private String assetCode;
        private String transactionType;
        private String tradeType;
        private Integer quantity;
        private java.math.BigDecimal unitPrice;
        private java.math.BigDecimal totalValue;
        private java.math.BigDecimal profitLoss;
        private String status;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingError {
        private String errorCode;
        private String category;
        private String severity;
        private String message;
        private String assetCode;
        private String invoiceNumber;
        private String phase;
        private LocalDateTime timestamp;
        private String recoveryStrategy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceLink {
        private String type; // "invoice", "operation", "report"
        private UUID resourceId;
        private String description;
        private String url;
    }
    
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    public int getTotalOperations() {
        return (createdOperations != null ? createdOperations.size() : 0) +
               (finalizedOperations != null ? finalizedOperations.size() : 0);
    }
}