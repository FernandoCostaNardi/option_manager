package com.olisystem.optionsmanager.service.invoice.processing.detection;

import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Operação consolidada - resultado final do processo de detecção
 * Representa uma operação pronta para ser criada no sistema
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class ConsolidatedOperation {
    
    // === IDENTIFICAÇÃO ===
    private UUID id;
    private String consolidationId; // ID único da consolidação
    
    // === DADOS DA OPERAÇÃO ===
    private String assetCode;
    private String optionCode;
    private TransactionType transactionType;
    private TradeType tradeType;
    private BigDecimal unitPrice;
    private BigDecimal totalValue;
    private Integer quantity;
    private LocalDate tradeDate;
    
    // === CONSOLIDAÇÃO ===
    private List<ClassifiedOperation> sourceOperations;
    private String consolidationReason;
    private double consolidationConfidence;
    
    // === METADADOS ===
    private String notes;
    private boolean isConfirmed;
    private boolean isReadyForCreation;
    
    /**
     * Verifica se a operação é de compra
     */
    public boolean isPurchase() {
        return TransactionType.BUY.equals(transactionType);
    }
    
    /**
     * Verifica se a operação é de venda
     */
    public boolean isSale() {
        return TransactionType.SELL.equals(transactionType);
    }
    
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
     * Verifica se a consolidação tem alta confiança
     */
    public boolean hasHighConsolidationConfidence() {
        return consolidationConfidence >= 0.8;
    }
    
    /**
     * Retorna o número de operações fonte consolidadas
     */
    public int getSourceOperationsCount() {
        return sourceOperations != null ? sourceOperations.size() : 0;
    }
} 