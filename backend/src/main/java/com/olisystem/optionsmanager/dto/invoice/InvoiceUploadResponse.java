package com.olisystem.optionsmanager.dto.invoice;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Resposta para upload de invoices
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class InvoiceUploadResponse {
    
    /**
     * ID do upload
     */
    private UUID uploadId;
    
    /**
     * Status do upload
     */
    private String status; // UPLOADED, PROCESSING, PROCESSED, ERROR
    
    /**
     * Mensagem descritiva
     */
    private String message;
    
    /**
     * Mensagem de erro (se houver)
     */
    private String errorMessage;
    
    /**
     * Nome do arquivo
     */
    private String fileName;
    
    /**
     * Tamanho do arquivo em bytes
     */
    private Long fileSize;
    
    /**
     * Tipo de conteúdo
     */
    private String contentType;
    
    /**
     * Total de arquivos (para upload múltiplo)
     */
    private Integer totalFiles;
    
    /**
     * Tamanho total em bytes (para upload múltiplo)
     */
    private Long totalSize;
} 