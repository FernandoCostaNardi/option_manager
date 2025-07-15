package com.olisystem.optionsmanager.service.invoice.processing.detection;

import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Operação classificada com tipo específico
 * Representa uma operação detectada que foi classificada com tipo de trade e metadados
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class ClassifiedOperation {
    
    // === IDENTIFICAÇÃO ===
    private UUID id;
    private String detectionId;
    
    // === DADOS DA OPERAÇÃO ===
    private String assetCode;
    private String optionCode;
    private TransactionType transactionType;
    private TradeType tradeType;
    private BigDecimal unitPrice;
    private BigDecimal totalValue;
    private Integer quantity;
    private LocalDate tradeDate;
    
    // === CLASSIFICAÇÃO ===
    private String classificationReason;
    private double classificationConfidence;
    private String tradeTypeReason;
    
    // === METADADOS ===
    private String notes;
    private boolean isDayTrade;
    private boolean isSwingTrade;
    private boolean isPositionTrade;
    
    /**
     * Verifica se é day trade
     */
    public boolean isDayTrade() {
        return TradeType.DAY.equals(tradeType);
    }
    
    /**
     * Verifica se é swing trade
     */
    public boolean isSwingTrade() {
        return TradeType.SWING.equals(tradeType);
    }
    
    /**
     * Verifica se é position trade (alias para swing trade)
     */
    public boolean isPositionTrade() {
        return TradeType.SWING.equals(tradeType);
    }
    
    /**
     * Verifica se a classificação tem alta confiança
     */
    public boolean hasHighClassificationConfidence() {
        return classificationConfidence >= 0.8;
    }
} 