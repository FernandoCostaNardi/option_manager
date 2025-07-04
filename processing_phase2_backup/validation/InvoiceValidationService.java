package com.olisystem.optionsmanager.service.invoice.processing.validation;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço para validação de invoices antes do processamento
 * Verifica se a invoice está pronta para criar operações
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@Slf4j
public class InvoiceValidationService {

    /**
     * Valida se uma invoice pode ser processada
     * 
     * @param invoice Invoice a ser validada
     * @throws BusinessException se a invoice não for válida
     */
    public void validateForProcessing(Invoice invoice) {
        log.debug("🔍 Validando invoice {} para processamento", invoice.getInvoiceNumber());
        
        List<String> errors = new ArrayList<>();
        
        // Validações básicas da invoice
        validateInvoiceBasicData(invoice, errors);
        
        // Validações dos itens
        validateInvoiceItems(invoice, errors);
        
        // Validações de negócio
        validateBusinessRules(invoice, errors);
        
        if (!errors.isEmpty()) {
            String errorMessage = "Invoice não pode ser processada:\n" + String.join("\n", errors);
            log.warn("❌ Validation failed for invoice {}: {}", invoice.getInvoiceNumber(), errorMessage);
            throw new BusinessException(errorMessage);
        }
        
        log.debug("✅ Invoice {} validada com sucesso", invoice.getInvoiceNumber());
    }

    /**
     * Valida dados básicos da invoice
     */
    private void validateInvoiceBasicData(Invoice invoice, List<String> errors) {
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().trim().isEmpty()) {
            errors.add("- Número da nota é obrigatório");
        }
        
        if (invoice.getTradingDate() == null) {
            errors.add("- Data de pregão é obrigatória");
        }
        
        if (invoice.getClientName() == null || invoice.getClientName().trim().isEmpty()) {
            errors.add("- Nome do cliente é obrigatório");
        }
        
        if (invoice.getBrokerage() == null) {
            errors.add("- Corretora é obrigatória");
        }
        
        if (invoice.getUser() == null) {
            errors.add("- Usuário é obrigatório");
        }
        
        // Validar data não muito antiga (> 5 anos) nem futura
        if (invoice.getTradingDate() != null) {
            LocalDate fiveYearsAgo = LocalDate.now().minusYears(5);
            LocalDate today = LocalDate.now();
            
            if (invoice.getTradingDate().isBefore(fiveYearsAgo)) {
                errors.add("- Data de pregão muito antiga (> 5 anos)");
            }
            
            if (invoice.getTradingDate().isAfter(today)) {
                errors.add("- Data de pregão não pode ser futura");
            }
        }
    }

    /**
     * Valida itens da invoice
     */
    private void validateInvoiceItems(Invoice invoice, List<String> errors) {
        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            errors.add("- Invoice deve ter pelo menos uma operação");
            return;
        }
        
        // Validar cada item
        for (int i = 0; i < invoice.getItems().size(); i++) {
            InvoiceItem item = invoice.getItems().get(i);
            validateInvoiceItem(item, i + 1, errors);
        }
        
        // Validar consistência entre itens
        validateItemsConsistency(invoice, errors);
    }

    /**
     * Valida um item individual da invoice
     */
    private void validateInvoiceItem(InvoiceItem item, int position, List<String> errors) {
        String prefix = "- Item " + position + ": ";
        
        if (item.getAssetCode() == null || item.getAssetCode().trim().isEmpty()) {
            errors.add(prefix + "Código do ativo é obrigatório");
        }
        
        if (item.getOperationType() == null || 
            (!item.getOperationType().equals("C") && !item.getOperationType().equals("V"))) {
            errors.add(prefix + "Tipo de operação deve ser 'C' ou 'V'");
        }
        
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            errors.add(prefix + "Quantidade deve ser maior que zero");
        }
        
        if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(prefix + "Preço unitário deve ser maior que zero");
        }
        
        if (item.getTotalValue() == null || item.getTotalValue().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(prefix + "Valor total deve ser maior que zero");
        }
        
        // Validar consistência preço x quantidade x total
        if (item.getUnitPrice() != null && item.getQuantity() != null && item.getTotalValue() != null) {
            BigDecimal calculatedTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal tolerance = BigDecimal.valueOf(0.05); // 5 centavos de tolerância
            
            if (calculatedTotal.subtract(item.getTotalValue()).abs().compareTo(tolerance) > 0) {
                errors.add(prefix + "Inconsistência: preço × quantidade ≠ valor total");
            }
        }
    }

    /**
     * Valida consistência entre itens da invoice
     */
    private void validateItemsConsistency(Invoice invoice, List<String> errors) {
        // Verificar se há operações balanceadas para Day Trade
        long compras = invoice.getItems().stream()
                .filter(item -> "C".equals(item.getOperationType()))
                .count();
        
        long vendas = invoice.getItems().stream()
                .filter(item -> "V".equals(item.getOperationType()))
                .count();
        
        // Validar que todas as operações são da mesma data de pregão
        boolean sameTradingDate = invoice.getItems().stream()
                .allMatch(item -> invoice.getTradingDate().equals(invoice.getTradingDate()));
        
        if (!sameTradingDate) {
            errors.add("- Todos os itens devem ter a mesma data de pregão da invoice");
        }
        
        log.debug("📊 Invoice {}: {} compras, {} vendas", invoice.getInvoiceNumber(), compras, vendas);
    }

    /**
     * Valida regras de negócio específicas
     */
    private void validateBusinessRules(Invoice invoice, List<String> errors) {
        // Limite de operações para processamento em lote (máximo 50 por invoice)
        if (invoice.getItems().size() > 50) {
            errors.add("- Invoice possui muitas operações (máx. 50). Considere dividir em múltiplas notas");
        }
        
        // Verificar se há itens com valores muito discrepantes (possível erro)
        BigDecimal maxValue = invoice.getItems().stream()
                .map(InvoiceItem::getTotalValue)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal minValue = invoice.getItems().stream()
                .map(InvoiceItem::getTotalValue)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        // Se a diferença for muito grande, alertar
        if (maxValue.compareTo(BigDecimal.ZERO) > 0 && minValue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = maxValue.divide(minValue, 2, BigDecimal.ROUND_HALF_UP);
            if (ratio.compareTo(BigDecimal.valueOf(1000)) > 0) {
                errors.add("- Valores muito discrepantes entre operações. Verifique os dados");
            }
        }
    }

    /**
     * Valida se uma invoice pode ser reprocessada
     */
    public void validateForReprocessing(Invoice invoice) {
        log.debug("🔄 Validando invoice {} para reprocessamento", invoice.getInvoiceNumber());
        
        // Validações básicas primeiro
        validateForProcessing(invoice);
        
        // Validações específicas de reprocessamento serão implementadas
        // em ReprocessingValidationService
        
        log.debug("✅ Invoice {} validada para reprocessamento", invoice.getInvoiceNumber());
    }
}