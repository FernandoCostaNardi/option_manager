package com.olisystem.optionsmanager.service.invoice.processing.detection;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Resultado da detecção de operações
 * Contém todas as informações sobre o processo de detecção
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class DetectionResult {
    
    // === METADADOS ===
    private boolean success;
    private String errorMessage;
    
    // === ESTATÍSTICAS ===
    private int totalInvoices;
    private int totalItems;
    private double detectionRate; // % de itens que geraram operações
    private double consolidationRate; // % de operações consolidadas
    
    // === OPERAÇÕES DETECTADAS ===
    private List<DetectedOperation> detectedOperations;
    private List<ClassifiedOperation> classifiedOperations;
    private List<ConsolidatedOperation> consolidatedOperations;
    
    // === DISTRIBUIÇÃO POR TIPO ===
    private Map<String, Long> typeDistribution;
    
    // === TEMPO DE PROCESSAMENTO ===
    private long processingTimeMs;
    
    /**
     * Verifica se a detecção foi bem-sucedida
     */
    public boolean isSuccessful() {
        return success && errorMessage == null;
    }
    
    /**
     * Retorna o número total de operações finais
     */
    public int getTotalFinalOperations() {
        return consolidatedOperations != null ? consolidatedOperations.size() : 0;
    }
    
    /**
     * Retorna o número de operações candidatas
     */
    public int getTotalCandidateOperations() {
        return detectedOperations != null ? detectedOperations.size() : 0;
    }
} 