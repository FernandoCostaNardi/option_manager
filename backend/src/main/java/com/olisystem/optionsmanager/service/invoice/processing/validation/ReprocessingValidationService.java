package com.olisystem.optionsmanager.service.invoice.processing.validation;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.repository.InvoiceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de validação para reprocessamento de invoices
 * ✅ CORREÇÃO: Usa repositório em vez de acessar coleção lazy
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReprocessingValidationService {

    private final InvoiceItemRepository invoiceItemRepository;

    // Configurações de reprocessamento
    private static final int MAX_REPROCESSING_ATTEMPTS = 3;
    private static final int MIN_REPROCESSING_INTERVAL_HOURS = 1;

    /**
     * Valida se uma invoice pode ser reprocessada
     * ✅ CORREÇÃO: Usa repositório para buscar itens
     */
    public ReprocessingValidationResult validateReprocessing(Invoice invoice, User user) {
        log.info("🔍 Validando reprocessamento da invoice: {} (User: {})", 
            invoice.getInvoiceNumber(), user.getEmail());
        
        ReprocessingValidationResult result = ReprocessingValidationResult.builder()
            .canReprocess(true)
            .build();
        
        try {
            // ✅ CORREÇÃO: Buscar itens via repositório
            List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithAllRelations(invoice.getId());
            
            // Validar se a invoice tem itens válidos
            validateInvoiceItems(items, result);
            
            // Validar se o usuário tem permissão
            validateUserPermission(invoice, user, result);
            
            // Validar histórico de reprocessamento
            validateReprocessingHistory(invoice, result);
            
            // Validar intervalo mínimo
            validateReprocessingInterval(invoice, result);
            
            log.info("✅ Validação de reprocessamento concluída: {}", 
                result.isCanReprocess() ? "APROVADO" : "REJEITADO");
            
        } catch (Exception e) {
            log.error("❌ Erro durante validação de reprocessamento: {}", e.getMessage(), e);
            result.setCanReprocess(false);
            result.setRejectionReason("Erro interno na validação: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Valida se a invoice tem itens válidos para reprocessamento
     * ✅ CORREÇÃO: Recebe lista de itens como parâmetro
     */
    private void validateInvoiceItems(List<InvoiceItem> items, ReprocessingValidationResult result) {
        if (items == null || items.isEmpty()) {
            result.setCanReprocess(false);
            result.setRejectionReason("Invoice não possui itens para reprocessar");
            return;
        }
        
        // Verificar se há pelo menos um item válido
        long validItems = items.stream()
            .filter(this::isValidItemForReprocessing)
            .count();
        
        if (validItems == 0) {
            result.setCanReprocess(false);
            result.setRejectionReason("Invoice não possui itens válidos para reprocessamento");
        }
        
        log.debug("📊 Invoice possui {} itens válidos de {} total", validItems, items.size());
    }

    /**
     * Verifica se um item é válido para reprocessamento
     */
    private boolean isValidItemForReprocessing(InvoiceItem item) {
        if (item == null) return false;
        
        // Validar campos obrigatórios
        if (item.getAssetCode() == null || item.getAssetCode().trim().isEmpty()) {
            return false;
        }
        
        if (item.getOperationType() == null) {
            return false;
        }
        
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            return false;
        }
        
        if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        return true;
    }

    /**
     * Valida se o usuário tem permissão para reprocessar
     */
    private void validateUserPermission(Invoice invoice, User user, ReprocessingValidationResult result) {
        if (user == null) {
            result.setCanReprocess(false);
            result.setRejectionReason("Usuário não informado");
            return;
        }
        
        // Verificar se o usuário é o proprietário da invoice
        if (invoice.getUser() == null || !user.getId().equals(invoice.getUser().getId())) {
            result.setCanReprocess(false);
            result.setRejectionReason("Usuário não tem permissão para reprocessar esta invoice");
        }
    }

    /**
     * Valida histórico de reprocessamento
     */
    private void validateReprocessingHistory(Invoice invoice, ReprocessingValidationResult result) {
        // TODO: Implementar validação de histórico quando o repositório estiver disponível
        // Por enquanto, assumimos que pode ser reprocessada
        log.debug("📋 Histórico de reprocessamento não implementado ainda");
    }

    /**
     * Valida intervalo mínimo entre reprocessamentos
     */
    private void validateReprocessingInterval(Invoice invoice, ReprocessingValidationResult result) {
        if (invoice.getUpdatedAt() == null) {
            return; // Primeira vez, pode reprocessar
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastUpdate = invoice.getUpdatedAt();
        
        long hoursSinceLastUpdate = java.time.Duration.between(lastUpdate, now).toHours();
        
        if (hoursSinceLastUpdate < MIN_REPROCESSING_INTERVAL_HOURS) {
            result.setCanReprocess(false);
            result.setRejectionReason(String.format(
                "Intervalo mínimo de %d horas não respeitado (última atualização: %s)", 
                MIN_REPROCESSING_INTERVAL_HOURS, lastUpdate));
        }
    }

    /**
     * Valida múltiplas invoices para reprocessamento
     */
    public List<ReprocessingValidationResult> validateBatchReprocessing(List<Invoice> invoices, User user) {
        log.info("🔍 Validando reprocessamento de {} invoices", invoices.size());
        
        List<ReprocessingValidationResult> results = new ArrayList<>();
        
        for (Invoice invoice : invoices) {
            ReprocessingValidationResult result = validateReprocessing(invoice, user);
            results.add(result);
        }
        
        long approvedCount = results.stream()
            .filter(ReprocessingValidationResult::isCanReprocess)
            .count();
        
        log.info("✅ Validação em lote concluída: {} aprovadas de {} total", approvedCount, invoices.size());
        
        return results;
    }

    /**
     * Verifica se uma invoice pode ser reprocessada
     */
    public boolean canReprocessInvoice(Invoice invoice, User user) {
        ReprocessingValidationResult result = validateReprocessing(invoice, user);
        return result.isCanReprocess();
    }

    /**
     * Resultado da validação de reprocessamento
     */
    @lombok.Builder
    @lombok.Data
    public static class ReprocessingValidationResult {
        private boolean canReprocess;
        private String rejectionReason;
        private List<String> warnings = new ArrayList<>();
        private UUID invoiceId;
        private String invoiceNumber;
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public String getSummary() {
            if (canReprocess) {
                return String.format("Reprocessamento APROVADO para invoice %s", invoiceNumber);
            } else {
                return String.format("Reprocessamento REJEITADO para invoice %s: %s", 
                    invoiceNumber, rejectionReason);
            }
        }
    }
} 