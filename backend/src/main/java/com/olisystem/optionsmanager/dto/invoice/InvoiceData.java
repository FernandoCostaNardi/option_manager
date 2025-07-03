package com.olisystem.optionsmanager.dto.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO que representa os dados básicos de uma nota de corretagem
 */
public record InvoiceData(
    UUID id,
    String invoiceNumber,
    LocalDate tradingDate,
    LocalDate settlementDate,
    String clientName,
    String brokerageName,
    BigDecimal grossOperationsValue,
    BigDecimal netOperationsValue,
    BigDecimal totalCosts,
    BigDecimal netSettlementValue,
    int itemsCount,
    LocalDateTime importedAt,
    List<InvoiceItemData> items
) {
    
    /**
     * DTO que representa um item da nota de corretagem
     */
    public record InvoiceItemData(
        UUID id,
        Integer sequenceNumber,
        String operationType,
        String marketType,
        String assetCode,
        String assetSpecification,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalValue,
        Boolean isDayTrade,
        LocalDate expirationDate,
        BigDecimal strikePrice,
        String observations
    ) {
        
        public boolean isPurchase() {
            return "C".equalsIgnoreCase(operationType);
        }
        
        public boolean isSale() {
            return "V".equalsIgnoreCase(operationType);
        }
        
        public boolean isOption() {
            return marketType != null && 
                   (marketType.contains("OPCAO") || marketType.contains("OPTION"));
        }
    }
}