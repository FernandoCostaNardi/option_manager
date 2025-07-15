package com.olisystem.optionsmanager.service.invoice.processing.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Gerenciador de transações para processamento de invoices
 * Garante consistência e rollback em caso de erro
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionManager {

    /**
     * Executa processamento com transação
     */
    @Transactional
    public OrchestrationResult executeWithTransaction(Supplier<OrchestrationResult> processor) {
        log.info("🔄 Iniciando processamento com transação");
        
        try {
            OrchestrationResult result = processor.get();
            
            if (result.isSuccessful()) {
                log.info("✅ Processamento concluído com sucesso - commit da transação");
            } else {
                log.warn("❌ Processamento falhou - rollback da transação: {}", result.getErrorMessage());
                throw new RuntimeException("Processamento falhou: " + result.getErrorMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Erro durante processamento - rollback da transação: {}", e.getMessage(), e);
            throw new RuntimeException("Erro no processamento: " + e.getMessage(), e);
        }
    }

    /**
     * Executa processamento de múltiplas invoices com transação
     */
    @Transactional
    public OrchestrationResult processInvoicesWithTransaction(List<UUID> invoiceIds, 
                                                           InvoiceProcessingOrchestrator orchestrator,
                                                           Object user) {
        log.info("🔄 Iniciando processamento de {} invoices com transação", invoiceIds.size());
        
        try {
            // TODO: Converter user para User quando necessário
            OrchestrationResult result = orchestrator.processInvoices(invoiceIds, null, progress -> {
                log.debug("📊 Progresso: {}% - {}", progress.getPercentage(), progress.getMessage());
            });
            
            if (result.isSuccessful()) {
                log.info("✅ Processamento de {} invoices concluído - commit da transação", invoiceIds.size());
            } else {
                log.warn("❌ Processamento falhou - rollback da transação: {}", result.getErrorMessage());
                throw new RuntimeException("Processamento falhou: " + result.getErrorMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Erro durante processamento - rollback da transação: {}", e.getMessage(), e);
            throw new RuntimeException("Erro no processamento: " + e.getMessage(), e);
        }
    }

    /**
     * Executa processamento de invoice única com transação
     */
    @Transactional
    public OrchestrationResult processSingleInvoiceWithTransaction(UUID invoiceId, 
                                                                InvoiceProcessingOrchestrator orchestrator,
                                                                Object user) {
        log.info("🔄 Iniciando processamento da invoice {} com transação", invoiceId);
        
        try {
            // TODO: Converter user para User quando necessário
            OrchestrationResult result = orchestrator.processSingleInvoice(invoiceId, null);
            
            if (result.isSuccessful()) {
                log.info("✅ Processamento da invoice {} concluído - commit da transação", invoiceId);
            } else {
                log.warn("❌ Processamento falhou - rollback da transação: {}", result.getErrorMessage());
                throw new RuntimeException("Processamento falhou: " + result.getErrorMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Erro durante processamento - rollback da transação: {}", e.getMessage(), e);
            throw new RuntimeException("Erro no processamento: " + e.getMessage(), e);
        }
    }

    /**
     * Executa operação com retry em caso de erro
     */
    public OrchestrationResult executeWithRetry(Supplier<OrchestrationResult> processor, int maxRetries) {
        log.info("🔄 Iniciando processamento com retry (máximo {} tentativas)", maxRetries);
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("🔄 Tentativa {}/{}", attempt, maxRetries);
                
                OrchestrationResult result = processor.get();
                
                if (result.isSuccessful()) {
                    log.info("✅ Processamento concluído com sucesso na tentativa {}", attempt);
                    return result;
                } else {
                    log.warn("⚠️ Processamento falhou na tentativa {}: {}", attempt, result.getErrorMessage());
                    lastException = new RuntimeException("Processamento falhou: " + result.getErrorMessage());
                }
                
            } catch (Exception e) {
                log.warn("⚠️ Erro na tentativa {}: {}", attempt, e.getMessage());
                lastException = e;
                
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000 * attempt); // Backoff exponencial
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Processamento interrompido", ie);
                    }
                }
            }
        }
        
        log.error("❌ Processamento falhou após {} tentativas", maxRetries);
        throw new RuntimeException("Processamento falhou após " + maxRetries + " tentativas", lastException);
    }
} 