package com.olisystem.optionsmanager.service.invoice.processing.detection;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Operação detectada durante o processo de detecção
 * Representa uma operação candidata identificada nos itens das invoices
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
@Builder
public class DetectedOperation {
    
    // === IDENTIFICAÇÃO ===
    private UUID id;
    private String detectionId; // ID único da detecção
    
    // === DADOS DA OPERAÇÃO ===
    private String assetCode;
    private String optionCode;
    private TransactionType transactionType;
    private BigDecimal unitPrice;
    private BigDecimal totalValue;
    private Integer quantity;
    private LocalDate tradeDate;
    
    // === ORIGEM ===
    private Invoice sourceInvoice;
    private List<InvoiceItem> sourceItems;
    private User user;
    
    // === CONFIANÇA ===
    private double confidenceScore; // 0.0 a 1.0
    private String confidenceReason;
    
    // === METADADOS ===
    private String notes;
    private boolean isConfirmed;
    
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
     * Verifica se a operação tem confiança alta
     */
    public boolean hasHighConfidence() {
        return confidenceScore >= 0.8;
    }
    
    /**
     * Verifica se a operação tem confiança média
     */
    public boolean hasMediumConfidence() {
        return confidenceScore >= 0.5 && confidenceScore < 0.8;
    }
    
    /**
     * Verifica se a operação tem confiança baixa
     */
    public boolean hasLowConfidence() {
        return confidenceScore < 0.5;
    }
} 