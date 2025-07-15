package com.olisystem.optionsmanager.service.invoice.processing.validation;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.repository.InvoiceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validador de limites de lote para processamento de invoices
 * ✅ CORREÇÃO: Usa repositório em vez de acessar coleção lazy
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchLimitValidator {

    private final InvoiceItemRepository invoiceItemRepository;

    // Limites de processamento
    private static final int MAX_INVOICES_PER_BATCH = 10;
    private static final int MAX_ITEMS_PER_BATCH = 100;
    private static final int MAX_ITEMS_PER_INVOICE = 50;
    private static final long MAX_TOTAL_VALUE_PER_BATCH = 1000000L; // 1 milhão

    /**
     * Valida limites de lote para processamento
     * ✅ CORREÇÃO: Usa repositório para buscar itens
     */
    public BatchLimitValidationResult validateBatchLimits(List<Invoice> invoices) {
        log.info("🔍 Validando limites de lote para {} invoices", invoices.size());
        
        BatchLimitValidationResult result = new BatchLimitValidationResult();
        
        try {
            // Validar número de invoices
            validateInvoiceCount(invoices, result);
            
            // ✅ CORREÇÃO: Buscar todos os itens via repositório
            List<InvoiceItem> allItems = new ArrayList<>();
            for (Invoice invoice : invoices) {
                List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithAllRelations(invoice.getId());
                allItems.addAll(items);
            }
            
            // Validar número total de itens
            validateTotalItemCount(allItems, result);
            
            // Validar itens por invoice
            validateItemsPerInvoice(invoices, result);
            
            // Validar valor total do lote
            validateTotalBatchValue(allItems, result);
            
            log.info("✅ Validação de limites concluída: {} itens, {} invoices", 
                allItems.size(), invoices.size());
            
        } catch (Exception e) {
            log.error("❌ Erro durante validação de limites: {}", e.getMessage(), e);
            result.addError("Erro na validação: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Valida número de invoices no lote
     */
    private void validateInvoiceCount(List<Invoice> invoices, BatchLimitValidationResult result) {
        if (invoices.size() > MAX_INVOICES_PER_BATCH) {
            result.addError(String.format("Lote excede limite de %d invoices (atual: %d)", 
                MAX_INVOICES_PER_BATCH, invoices.size()));
        }
        
        if (invoices.isEmpty()) {
            result.addError("Lote deve conter pelo menos uma invoice");
        }
    }

    /**
     * Valida número total de itens no lote
     * ✅ CORREÇÃO: Recebe lista de itens como parâmetro
     */
    private void validateTotalItemCount(List<InvoiceItem> allItems, BatchLimitValidationResult result) {
        if (allItems.size() > MAX_ITEMS_PER_BATCH) {
            result.addError(String.format("Lote excede limite de %d itens (atual: %d)", 
                MAX_ITEMS_PER_BATCH, allItems.size()));
        }
        
        if (allItems.isEmpty()) {
            result.addError("Lote deve conter pelo menos um item");
        }
    }

    /**
     * Valida número de itens por invoice
     * ✅ CORREÇÃO: Usa repositório para buscar itens
     */
    private void validateItemsPerInvoice(List<Invoice> invoices, BatchLimitValidationResult result) {
        for (Invoice invoice : invoices) {
            List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithAllRelations(invoice.getId());
            
            if (items.size() > MAX_ITEMS_PER_INVOICE) {
                result.addError(String.format("Invoice %s excede limite de %d itens (atual: %d)", 
                    invoice.getInvoiceNumber(), MAX_ITEMS_PER_INVOICE, items.size()));
            }
        }
    }

    /**
     * Valida valor total do lote
     * ✅ CORREÇÃO: Recebe lista de itens como parâmetro
     */
    private void validateTotalBatchValue(List<InvoiceItem> allItems, BatchLimitValidationResult result) {
        long totalValue = allItems.stream()
            .filter(item -> item.getTotalValue() != null)
            .mapToLong(item -> item.getTotalValue().longValue())
            .sum();
        
        if (totalValue > MAX_TOTAL_VALUE_PER_BATCH) {
            result.addError(String.format("Valor total do lote excede limite de %d (atual: %d)", 
                MAX_TOTAL_VALUE_PER_BATCH, totalValue));
        }
    }

    /**
     * Verifica se um lote pode ser processado
     */
    public boolean canProcessBatch(List<Invoice> invoices) {
        BatchLimitValidationResult result = validateBatchLimits(invoices);
        return result.isValid();
    }

    /**
     * Resultado da validação de limites de lote
     */
    public static class BatchLimitValidationResult {
        private boolean isValid = true;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
            isValid = false;
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public String getSummary() {
            return String.format("Limites: %s (%d erros, %d avisos)", 
                isValid ? "VÁLIDO" : "INVÁLIDO", errors.size(), warnings.size());
        }
    }
} 