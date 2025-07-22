package com.olisystem.optionsmanager.dto.invoice;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO para filtros de consulta de notas de corretagem
 */
public record InvoiceFilterRequest(
    UUID brokerageId,
    LocalDate startDate,
    LocalDate endDate,
    String invoiceNumber,
    String clientName,
    LocalDate importStartDate,
    LocalDate importEndDate,
    String processingStatus, // ✅ NOVO: Status de processamento
    int page,
    int size,
    String sortBy,
    String sortDirection
) {
    
    /**
     * Construtor com valores padrão
     */
    public InvoiceFilterRequest {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
        if (sortBy == null || sortBy.trim().isEmpty()) sortBy = "tradingDate";
        if (sortDirection == null || (!sortDirection.equalsIgnoreCase("ASC") && !sortDirection.equalsIgnoreCase("DESC"))) {
            sortDirection = "DESC";
        }
    }
    
    /**
     * Construtor simplificado apenas com paginação
     */
    public static InvoiceFilterRequest withPagination(int page, int size) {
        return new InvoiceFilterRequest(
            null, null, null, null, null, null, null, null,
            page, size, null, null
        );
    }
    
    /**
     * Construtor simplificado com período
     */
    public static InvoiceFilterRequest withDateRange(LocalDate startDate, LocalDate endDate) {
        return new InvoiceFilterRequest(
            null, startDate, endDate, null, null, null, null, null,
            0, 20, null, null
        );
    }
    
    /**
     * Construtor simplificado com corretora
     */
    public static InvoiceFilterRequest withBrokerage(UUID brokerageId) {
        return new InvoiceFilterRequest(
            brokerageId, null, null, null, null, null, null, null,
            0, 20, null, null
        );
    }
    
    /**
     * ✅ NOVO: Construtor simplificado com status de processamento
     */
    public static InvoiceFilterRequest withProcessingStatus(String processingStatus) {
        return new InvoiceFilterRequest(
            null, null, null, null, null, null, null, processingStatus,
            0, 20, null, null
        );
    }
    
    /**
     * Verifica se tem filtro de período de negociação
     */
    public boolean hasDateRange() {
        return startDate != null || endDate != null;
    }
    
    /**
     * Verifica se tem filtro de período de importação
     */
    public boolean hasImportDateRange() {
        return importStartDate != null || importEndDate != null;
    }
    
    /**
     * ✅ NOVO: Verifica se tem filtro de status de processamento
     */
    public boolean hasProcessingStatusFilter() {
        return processingStatus != null;
    }
    
    /**
     * Verifica se tem filtros aplicados
     */
    public boolean hasFilters() {
        return brokerageId != null || 
               hasDateRange() || 
               hasImportDateRange() ||
               hasProcessingStatusFilter() ||
               (invoiceNumber != null && !invoiceNumber.trim().isEmpty()) ||
               (clientName != null && !clientName.trim().isEmpty());
    }
}
