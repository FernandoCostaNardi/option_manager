package com.olisystem.optionsmanager.dto.invoice;

import lombok.Builder;
import lombok.Data;

/**
 * Resposta para health check do processamento
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class ProcessingHealthResponse {
    
    /**
     * Status do sistema
     */
    private String status; // HEALTHY, UNHEALTHY
    
    /**
     * Número de sessões ativas
     */
    private Integer activeSessions;
    
    /**
     * Timestamp da verificação
     */
    private Long timestamp;
} 