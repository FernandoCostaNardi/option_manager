package com.olisystem.optionsmanager.service.invoice.processing.validation;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.enums.InvoiceProcessingStatus;
import com.olisystem.optionsmanager.repository.InvoiceProcessingLogRepository;
import com.olisystem.optionsmanager.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço para validação de limites de processamento em lote
 * Controla quantidade de operações processadas simultaneamente
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchLimitValidator {

    private final InvoiceProcessingLogRepository processingLogRepository;
    
    // Configurações de limite
    private static final int MAX_OPERATIONS_PER_BATCH = 5;
    private static final int MAX_ITEMS_PER_INVOICE = 50;
    private static final int MAX_CONCURRENT_PROCESSING = 3;
    private static final int MAX_DAILY_PROCESSING = 100;

    /**
     * Valida se um lote de invoices pode ser processado
     * 
     * @param invoices Lista de invoices para processar
     * @param user Usuário solicitante
     * @throws BusinessException se limites forem excedidos
     */
    public void validateBatchLimits(List<Invoice> invoices, User user) {
        log.debug("🔍 Validando limites para lote com {} invoices do usuário {}", 
                 invoices.size(), user.getUsername());
        
        List<String> errors = new ArrayList<>();
        
        // 1. Validar limite de invoices no lote
        validateBatchSize(invoices, errors);
        
        // 2. Validar limite de itens por invoice
        validateItemLimits(invoices, errors);
        
        // 3. Validar limite de processamento simultâneo
        validateConcurrentProcessing(user, errors);
        
        // 4. Validar limite diário
        validateDailyLimit(user, errors);
        
        // 5. Validar recursos do sistema
        validateSystemResources(invoices, errors);
        
        if (!errors.isEmpty()) {
            String errorMessage = "Limites de processamento excedidos:\n" + String.join("\n", errors);
            log.warn("❌ Batch limit validation failed for user {}: {}", user.getUsername(), errorMessage);
            throw new BusinessException(errorMessage);
        }
        
        log.info("✅ Limites validados para {} invoices do usuário {}", 
                invoices.size(), user.getUsername());
    }

    /**
     * Valida tamanho do lote de invoices
     */
    private void validateBatchSize(List<Invoice> invoices, List<String> errors) {
        if (invoices.isEmpty()) {
            errors.add("- Lote vazio - pelo menos 1 invoice é necessária");
            return;
        }
        
        if (invoices.size() > MAX_OPERATIONS_PER_BATCH) {
            errors.add(String.format("- Máximo %d invoices por lote (enviadas: %d)", 
                                   MAX_OPERATIONS_PER_BATCH, invoices.size()));
        }
        
        log.debug("📊 Tamanho do lote: {} invoices (limite: {})", 
                 invoices.size(), MAX_OPERATIONS_PER_BATCH);
    }

    /**
     * Valida limite de itens por invoice
     */
    private void validateItemLimits(List<Invoice> invoices, List<String> errors) {
        int totalItems = 0;
        
        for (Invoice invoice : invoices) {
            int itemCount = invoice.getItems() != null ? invoice.getItems().size() : 0;
            totalItems += itemCount;
            
            if (itemCount > MAX_ITEMS_PER_INVOICE) {
                errors.add(String.format("- Invoice %s possui %d itens (máximo %d)", 
                                       invoice.getInvoiceNumber(), itemCount, MAX_ITEMS_PER_INVOICE));
            }
            
            if (itemCount == 0) {
                errors.add(String.format("- Invoice %s não possui itens para processar", 
                                       invoice.getInvoiceNumber()));
            }
        }
        
        log.debug("📊 Total de itens no lote: {} (média: {} por invoice)", 
                 totalItems, invoices.isEmpty() ? 0 : totalItems / invoices.size());
    }

    /**
     * Valida limite de processamento simultâneo do usuário
     */
    private void validateConcurrentProcessing(User user, List<String> errors) {
        long activeProcessing = processingLogRepository
                .findByStatusAndUserOrderByCreatedAtDesc(InvoiceProcessingStatus.PROCESSING, user)
                .size();
        
        if (activeProcessing >= MAX_CONCURRENT_PROCESSING) {
            errors.add(String.format("- Máximo %d processamentos simultâneos (ativos: %d)", 
                                   MAX_CONCURRENT_PROCESSING, activeProcessing));
        }
        
        log.debug("📊 Processamentos ativos do usuário: {} (limite: {})", 
                 activeProcessing, MAX_CONCURRENT_PROCESSING);
    }

    /**
     * Valida limite diário de processamento
     */
    private void validateDailyLimit(User user, List<String> errors) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        
        long dailyProcessing = processingLogRepository
                .findWithFilters(user.getId(), null, startOfDay, endOfDay, 
                               org.springframework.data.domain.PageRequest.of(0, 1000))
                .getTotalElements();
        
        if (dailyProcessing >= MAX_DAILY_PROCESSING) {
            errors.add(String.format("- Limite diário de %d processamentos atingido (hoje: %d)", 
                                   MAX_DAILY_PROCESSING, dailyProcessing));
        }
        
        log.debug("📊 Processamentos hoje: {} (limite: {})", dailyProcessing, MAX_DAILY_PROCESSING);
    }

    /**
     * Valida recursos do sistema
     */
    private void validateSystemResources(List<Invoice> invoices, List<String> errors) {
        // Calcular complexidade estimada do processamento
        int complexity = calculateProcessingComplexity(invoices);
        
        // Limite baseado na complexidade (peso arbitrário)
        int maxComplexity = 1000;
        
        if (complexity > maxComplexity) {
            errors.add(String.format("- Processamento muito complexo (score: %d, máximo: %d). " +
                                   "Considere reduzir o lote", complexity, maxComplexity));
        }
        
        log.debug("📊 Complexidade do lote: {} (limite: {})", complexity, maxComplexity);
    }

    /**
     * Calcula complexidade estimada do processamento
     */
    private int calculateProcessingComplexity(List<Invoice> invoices) {
        int complexity = 0;
        
        for (Invoice invoice : invoices) {
            // Base: 10 pontos por invoice
            complexity += 10;
            
            // 2 pontos por item
            if (invoice.getItems() != null) {
                complexity += invoice.getItems().size() * 2;
                
                // Pontos extras para Day Trade (mais complexo)
                long dayTradeItems = invoice.getItems().stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsDayTrade()))
                        .count();
                complexity += dayTradeItems * 3;
                
                // Pontos extras para diferentes ativos (mais validações)
                long uniqueAssets = invoice.getItems().stream()
                        .map(InvoiceItem::getAssetCode)
                        .distinct()
                        .count();
                complexity += uniqueAssets * 5;
            }
        }
        
        return complexity;
    }

    /**
     * Retorna estatísticas de uso atual do usuário
     */
    public BatchLimitStats getBatchLimitStats(User user) {
        long activeProcessing = processingLogRepository
                .findByStatusAndUserOrderByCreatedAtDesc(InvoiceProcessingStatus.PROCESSING, user)
                .size();
        
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        
        long dailyProcessing = processingLogRepository
                .findWithFilters(user.getId(), null, startOfDay, endOfDay,
                               org.springframework.data.domain.PageRequest.of(0, 1000))
                .getTotalElements();
        
        // Estatísticas por status
        List<Object[]> statusCounts = processingLogRepository.countByStatusForUser(user);
        Map<String, Long> statusMap = statusCounts.stream()
                .collect(Collectors.toMap(
                        arr -> arr[0].toString(),
                        arr -> (Long) arr[1]
                ));
        
        return BatchLimitStats.builder()
                .maxInvoicesPerBatch(MAX_OPERATIONS_PER_BATCH)
                .maxItemsPerInvoice(MAX_ITEMS_PER_INVOICE)
                .maxConcurrentProcessing(MAX_CONCURRENT_PROCESSING)
                .maxDailyProcessing(MAX_DAILY_PROCESSING)
                .currentActiveProcessing(activeProcessing)
                .currentDailyProcessing(dailyProcessing)
                .remainingConcurrentSlots(Math.max(0, MAX_CONCURRENT_PROCESSING - activeProcessing))
                .remainingDailySlots(Math.max(0, MAX_DAILY_PROCESSING - dailyProcessing))
                .statusCounts(statusMap)
                .build();
    }

    /**
     * Estatísticas de limites de processamento
     */
    @lombok.Builder
    @lombok.Data
    public static class BatchLimitStats {
        private int maxInvoicesPerBatch;
        private int maxItemsPerInvoice;
        private int maxConcurrentProcessing;
        private int maxDailyProcessing;
        private long currentActiveProcessing;
        private long currentDailyProcessing;
        private long remainingConcurrentSlots;
        private long remainingDailySlots;
        private Map<String, Long> statusCounts;
        
        public boolean canProcessMoreBatches() {
            return remainingConcurrentSlots > 0 && remainingDailySlots > 0;
        }
        
        public double getConcurrentUsagePercentage() {
            return maxConcurrentProcessing > 0 ? 
                   (double) currentActiveProcessing / maxConcurrentProcessing * 100 : 0;
        }
        
        public double getDailyUsagePercentage() {
            return maxDailyProcessing > 0 ? 
                   (double) currentDailyProcessing / maxDailyProcessing * 100 : 0;
        }
    }
}