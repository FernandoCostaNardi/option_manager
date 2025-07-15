package com.olisystem.optionsmanager.dto.invoice;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Resposta para status de upload
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class InvoiceUploadStatusResponse {
    
    /**
     * ID do upload
     */
    private UUID uploadId;
    
    /**
     * Status atual do upload
     */
    private String status; // UPLOADED, PROCESSING, PROCESSED, ERROR
    
    /**
     * Mensagem descritiva do status
     */
    private String message;
    
    /**
     * Progresso atual (0-100)
     */
    private Integer progress;
    
    /**
     * Tempo decorrido em milissegundos
     */
    private Long elapsedTime;
    
    /**
     * Tempo estimado restante em milissegundos
     */
    private Long estimatedRemainingTime;
} 