package com.olisystem.optionsmanager.dto.invoice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response da importação de notas de corretagem
 */
public record InvoiceImportResponse(
    String message,
    int totalFilesProcessed,
    int successfulImports,
    int duplicateFiles,
    int failedImports,
    LocalDateTime processedAt,
    List<ImportResult> results
) {
    
    /**
     * Resultado individual de cada arquivo importado
     */
    public record ImportResult(
        String fileName,
        ImportStatus status,
        String message,
        UUID invoiceId,
        InvoiceData invoiceData
    ) {
        
        public boolean isSuccess() {
            return status == ImportStatus.SUCCESS;
        }
        
        public boolean isDuplicate() {
            return status == ImportStatus.DUPLICATE;
        }
        
        public boolean isError() {
            return status == ImportStatus.ERROR;
        }
    }
    
    /**
     * Status da importação de cada arquivo
     */
    public enum ImportStatus {
        SUCCESS("Importado com sucesso"),
        DUPLICATE("Arquivo já importado anteriormente"),
        ERROR("Erro na importação");
        
        private final String description;
        
        ImportStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
