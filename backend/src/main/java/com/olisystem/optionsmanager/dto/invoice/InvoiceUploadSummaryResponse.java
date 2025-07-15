package com.olisystem.optionsmanager.dto.invoice;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta para resumo de uploads
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class InvoiceUploadSummaryResponse {
    
    /**
     * ID do upload
     */
    private UUID uploadId;
    
    /**
     * Nome do arquivo
     */
    private String fileName;
    
    /**
     * Status do upload
     */
    private String status; // UPLOADED, PROCESSING, PROCESSED, ERROR
    
    /**
     * Tamanho do arquivo em bytes
     */
    private Long fileSize;
    
    /**
     * Data/hora do upload
     */
    private LocalDateTime uploadDate;
    
    /**
     * Data/hora do processamento
     */
    private LocalDateTime processedDate;
    
    /**
     * Tempo de processamento em milissegundos
     */
    private Long processingTimeMs;
    
    /**
     * Número de invoices extraídas
     */
    private Integer extractedInvoices;
    
    /**
     * Número de operações criadas
     */
    private Integer createdOperations;
} 