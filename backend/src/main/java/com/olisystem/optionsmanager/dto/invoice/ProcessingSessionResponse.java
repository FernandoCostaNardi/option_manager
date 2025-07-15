package com.olisystem.optionsmanager.dto.invoice;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Resposta para sessões de processamento
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class ProcessingSessionResponse {
    
    /**
     * ID da sessão de processamento
     */
    private UUID sessionId;
    
    /**
     * Status atual do processamento
     */
    private String status; // PROCESSING, SUCCESS, ERROR, CANCELLED
    
    /**
     * Progresso atual (0-100)
     */
    private Integer progress;
    
    /**
     * Mensagem descritiva do status atual
     */
    private String message;
    
    /**
     * Tempo decorrido em milissegundos
     */
    private Long elapsedTime;
    
    /**
     * Total de invoices na sessão
     */
    private Integer totalInvoices;
} 