package com.olisystem.optionsmanager.service.invoice.processing.validation;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.repository.InvoiceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço de validação de invoices
 * ✅ CORREÇÃO: Usa repositório em vez de acessar coleção lazy
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceValidationService {

    private final InvoiceItemRepository invoiceItemRepository;

    // Constantes de validação
    private static final int MAX_YEARS_OLD = 5;
    private static final BigDecimal MIN_UNIT_PRICE = BigDecimal.valueOf(0.01);
    private static final BigDecimal MIN_TOTAL_VALUE = BigDecimal.valueOf(0.01);
    private static final BigDecimal MAX_TOTAL_VALUE = BigDecimal.valueOf(1000000.00);
    private static final BigDecimal PRICE_TOLERANCE = BigDecimal.valueOf(0.05); // 5 centavos

    /**
     * Valida uma invoice completa
     * ✅ CORREÇÃO: Usa repositório para buscar itens
     */
    public InvoiceValidationResult validateInvoice(Invoice invoice) {
        log.debug("🔍 Validando invoice: {}", invoice.getInvoiceNumber());
        
        InvoiceValidationResult result = InvoiceValidationResult.builder()
            .invoiceNumber(invoice.getInvoiceNumber())
            .isValid(true)
            .build();
        
        try {
            // Validar dados básicos da invoice
            validateBasicInvoiceData(invoice, result);
            
            // ✅ CORREÇÃO: Buscar itens via repositório em vez de acessar coleção lazy
            List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithAllRelations(invoice.getId());
            
            // Validar itens da invoice
            validateInvoiceItems(items, result);
            
            // Validar consistência geral
            validateInvoiceConsistency(items, result);
            
            log.debug("✅ Validação concluída para invoice: {} - Válida: {}", 
                invoice.getInvoiceNumber(), result.isValid());
            
        } catch (Exception e) {
            log.error("❌ Erro durante validação da invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage());
            result.addError("Erro interno na validação: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Valida dados básicos da invoice
     */
    private void validateBasicInvoiceData(Invoice invoice, InvoiceValidationResult result) {
        // Validar número da invoice
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().trim().isEmpty()) {
            result.addError("Número da invoice é obrigatório");
        }
        
        // Validar data de negociação
        if (invoice.getTradingDate() == null) {
            result.addError("Data de negociação é obrigatória");
        } else {
            validateTradingDate(invoice.getTradingDate(), result);
        }
        
        // Validar corretora
        if (invoice.getBrokerage() == null) {
            result.addError("Corretora é obrigatória");
        }
        
        // Validar usuário
        if (invoice.getUser() == null) {
            result.addError("Usuário é obrigatório");
        }
    }

    /**
     * Valida data de negociação
     */
    private void validateTradingDate(LocalDate tradingDate, InvoiceValidationResult result) {
        LocalDate now = LocalDate.now();
        LocalDate maxOldDate = now.minusYears(MAX_YEARS_OLD);
        
        if (tradingDate.isAfter(now)) {
            result.addError("Data de negociação não pode ser futura");
        }
        
        if (tradingDate.isBefore(maxOldDate)) {
            result.addError(String.format("Data de negociação não pode ser anterior a %d anos", MAX_YEARS_OLD));
        }
    }

    /**
     * Valida itens da invoice
     * ✅ CORREÇÃO: Recebe lista de itens como parâmetro
     */
    private void validateInvoiceItems(List<InvoiceItem> items, InvoiceValidationResult result) {
        if (items == null || items.isEmpty()) {
            result.addError("Invoice deve possuir pelo menos um item");
            return;
        }
        
        // Validar cada item individualmente
        for (int i = 0; i < items.size(); i++) {
            InvoiceItem item = items.get(i);
            validateInvoiceItem(item, i + 1, result);
        }
        
        // Validar se há pelo menos um item válido
        long validItems = items.stream()
            .filter(item -> isItemValid(item))
            .count();
        
        if (validItems == 0) {
            result.addError("Invoice deve possuir pelo menos um item válido");
        }
    }

    /**
     * Valida um item individual da invoice
     */
    private void validateInvoiceItem(InvoiceItem item, int sequenceNumber, InvoiceValidationResult result) {
        String itemPrefix = String.format("Item %d: ", sequenceNumber);
        
        // Validar código do ativo
        if (item.getAssetCode() == null || item.getAssetCode().trim().isEmpty()) {
            result.addError(itemPrefix + "Código do ativo é obrigatório");
        }
        
        // Validar tipo de operação
        if (item.getOperationType() == null) {
            result.addError(itemPrefix + "Tipo de operação é obrigatório");
        } else if (!isValidOperationType(item.getOperationType())) {
            result.addError(itemPrefix + "Tipo de operação deve ser 'C' (Compra) ou 'V' (Venda)");
        }
        
        // Validar quantidade
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            result.addError(itemPrefix + "Quantidade deve ser maior que zero");
        }
        
        // Validar preço unitário
        if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(MIN_UNIT_PRICE) < 0) {
            result.addError(itemPrefix + String.format("Preço unitário deve ser maior que %s", MIN_UNIT_PRICE));
        }
        
        // Validar valor total
        if (item.getTotalValue() == null || item.getTotalValue().compareTo(MIN_TOTAL_VALUE) < 0) {
            result.addError(itemPrefix + String.format("Valor total deve ser maior que %s", MIN_TOTAL_VALUE));
        }
        
        // Validar valor total máximo
        if (item.getTotalValue() != null && item.getTotalValue().compareTo(MAX_TOTAL_VALUE) > 0) {
            result.addError(itemPrefix + String.format("Valor total não pode exceder %s", MAX_TOTAL_VALUE));
        }
        
        // Validar consistência: preço × quantidade ≈ total
        validatePriceQuantityConsistency(item, sequenceNumber, result);
    }

    /**
     * Valida se o tipo de operação é válido
     */
    private boolean isValidOperationType(String operationType) {
        return "C".equals(operationType) || "V".equals(operationType);
    }

    /**
     * Valida consistência entre preço, quantidade e valor total
     */
    private void validatePriceQuantityConsistency(InvoiceItem item, int sequenceNumber, InvoiceValidationResult result) {
        if (item.getUnitPrice() != null && item.getQuantity() != null && item.getTotalValue() != null) {
            BigDecimal expectedTotal = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
            
            BigDecimal difference = item.getTotalValue().subtract(expectedTotal).abs();
            
            if (difference.compareTo(PRICE_TOLERANCE) > 0) {
                result.addError(String.format("Item %d: Inconsistência entre preço × quantidade e valor total. " +
                    "Esperado: %s, Calculado: %s, Diferença: %s", 
                    sequenceNumber, item.getTotalValue(), expectedTotal, difference));
            }
        }
    }

    /**
     * Verifica se um item é válido
     */
    private boolean isItemValid(InvoiceItem item) {
        return item.getAssetCode() != null && !item.getAssetCode().trim().isEmpty() &&
               item.getOperationType() != null && isValidOperationType(item.getOperationType()) &&
               item.getQuantity() != null && item.getQuantity() > 0 &&
               item.getUnitPrice() != null && item.getUnitPrice().compareTo(MIN_UNIT_PRICE) >= 0 &&
               item.getTotalValue() != null && item.getTotalValue().compareTo(MIN_TOTAL_VALUE) >= 0;
    }

    /**
     * Valida consistência geral da invoice
     * ✅ CORREÇÃO: Recebe lista de itens como parâmetro
     */
    private void validateInvoiceConsistency(List<InvoiceItem> items, InvoiceValidationResult result) {
        if (items == null) return;
        
        // Validar se há valores discrepantes
        validateDiscrepantValues(items, result);
        
        // Validar se há operações suspeitas
        validateSuspiciousOperations(items, result);
    }

    /**
     * Valida valores discrepantes
     */
    private void validateDiscrepantValues(List<InvoiceItem> items, InvoiceValidationResult result) {
        // Verificar se há preços muito diferentes para o mesmo ativo
        Map<String, List<BigDecimal>> pricesByAsset = items.stream()
            .filter(item -> item.getAssetCode() != null && item.getUnitPrice() != null)
            .collect(Collectors.groupingBy(
                InvoiceItem::getAssetCode,
                Collectors.mapping(InvoiceItem::getUnitPrice, Collectors.toList())
            ));
        
        for (Map.Entry<String, List<BigDecimal>> entry : pricesByAsset.entrySet()) {
            List<BigDecimal> prices = entry.getValue();
            if (prices.size() > 1) {
                BigDecimal minPrice = prices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                BigDecimal maxPrice = prices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                BigDecimal difference = maxPrice.subtract(minPrice);
                BigDecimal percentage = difference.divide(minPrice, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
                
                if (percentage.compareTo(BigDecimal.valueOf(50)) > 0) { // 50% de diferença
                    result.addWarning(String.format("Preços muito diferentes para ativo %s: min=%s, max=%s (diferença=%s%%)", 
                        entry.getKey(), minPrice, maxPrice, percentage));
                }
            }
        }
    }

    /**
     * Valida operações suspeitas
     */
    private void validateSuspiciousOperations(List<InvoiceItem> items, InvoiceValidationResult result) {
        // Verificar se há Day Trades óbvios (mesmo ativo, compra e venda)
        Map<String, List<InvoiceItem>> itemsByAsset = items.stream()
            .filter(item -> item.getAssetCode() != null)
            .collect(Collectors.groupingBy(InvoiceItem::getAssetCode));
        
        for (Map.Entry<String, List<InvoiceItem>> entry : itemsByAsset.entrySet()) {
            String assetCode = entry.getKey();
            List<InvoiceItem> assetItems = entry.getValue();
            
            boolean hasBuy = assetItems.stream().anyMatch(item -> "C".equals(item.getOperationType()));
            boolean hasSell = assetItems.stream().anyMatch(item -> "V".equals(item.getOperationType()));
            
            if (hasBuy && hasSell) {
                result.addInfo(String.format("Detectado possível Day Trade para ativo %s", assetCode));
            }
        }
    }

    /**
     * Resultado da validação de invoice
     */
    @lombok.Builder
    @lombok.Data
    public static class InvoiceValidationResult {
        private boolean isValid;
        private String invoiceNumber;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> info = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
            isValid = false;
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public void addInfo(String info) {
            this.info.add(info);
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public boolean hasInfo() {
            return !info.isEmpty();
        }
        
        public String getSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Invoice: ").append(invoiceNumber).append(" - ");
            summary.append(isValid ? "VÁLIDA" : "INVÁLIDA");
            
            if (hasErrors()) {
                summary.append(" (").append(errors.size()).append(" erros)");
            }
            
            if (hasWarnings()) {
                summary.append(" (").append(warnings.size()).append(" avisos)");
            }
            
            return summary.toString();
        }
    }
} 