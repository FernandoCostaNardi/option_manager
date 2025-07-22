package com.olisystem.optionsmanager.service.invoice.processing.log;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceProcessingLog;
import com.olisystem.optionsmanager.model.enums.InvoiceProcessingStatus;
import com.olisystem.optionsmanager.repository.InvoiceProcessingLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para gerenciar logs de processamento de invoices
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceProcessingLogService {

    private final InvoiceProcessingLogRepository logRepository;

    /**
     * Cria um novo log de processamento
     */
    @Transactional
    public InvoiceProcessingLog createProcessingLog(Invoice invoice, User user) {
        log.info("📝 Criando log de processamento para invoice: {} (User: {})", 
            invoice.getInvoiceNumber(), user.getEmail());
        
        // Verificar se já existe um log para esta invoice
        Optional<InvoiceProcessingLog> existingLog = logRepository.findByInvoice(invoice);
        
        if (existingLog.isPresent()) {
            InvoiceProcessingLog existing = existingLog.get();
            log.warn("⚠️ Já existe log de processamento para invoice {} - Status: {}", 
                invoice.getInvoiceNumber(), existing.getStatus());
            return existing;
        }
        
        InvoiceProcessingLog processingLog = InvoiceProcessingLog.builder()
            .invoice(invoice)
            .user(user)
            .status(InvoiceProcessingStatus.PENDING)
            .operationsCreated(0)
            .operationsUpdated(0)
            .operationsSkipped(0)
            .reprocessedCount(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        InvoiceProcessingLog saved = logRepository.save(processingLog);
        log.info("✅ Log de processamento criado: {}", saved.getId());
        
        return saved;
    }

    /**
     * Marca o início do processamento
     */
    @Transactional
    public void markAsStarted(InvoiceProcessingLog processingLog) {
        log.info("🚀 Marcando início do processamento: {}", processingLog.getId());
        
        processingLog.markAsStarted();
        logRepository.save(processingLog);
        
        log.info("✅ Processamento iniciado: {}", processingLog.getId());
    }

    /**
     * Atualiza contadores durante o processamento
     */
    @Transactional
    public void updateCounters(InvoiceProcessingLog processingLog, 
                              int operationsCreated, 
                              int operationsUpdated, 
                              int operationsSkipped) {
        log.debug("📊 Atualizando contadores: criadas={}, atualizadas={}, ignoradas={}", 
            operationsCreated, operationsUpdated, operationsSkipped);
        
        processingLog.setOperationsCreated(operationsCreated);
        processingLog.setOperationsUpdated(operationsUpdated);
        processingLog.setOperationsSkipped(operationsSkipped);
        processingLog.setUpdatedAt(LocalDateTime.now());
        
        logRepository.save(processingLog);
    }

    /**
     * Marca a conclusão do processamento
     */
    @Transactional
    public void markAsCompleted(InvoiceProcessingLog processingLog, 
                               InvoiceProcessingStatus finalStatus,
                               String errorMessage) {
        log.info("✅ Marcando conclusão do processamento: {} - Status: {}", 
            processingLog.getId(), finalStatus);
        
        processingLog.markAsCompleted(finalStatus);
        
        if (errorMessage != null) {
            processingLog.setErrorMessage(errorMessage);
        }
        
        logRepository.save(processingLog);
        
        log.info("✅ Processamento concluído: {} - Duração: {}ms", 
            processingLog.getId(), processingLog.getProcessingDurationMs());
    }

    /**
     * Verifica se uma invoice já foi processada com sucesso
     */
    @Transactional(readOnly = true)
    public boolean isInvoiceAlreadyProcessed(Invoice invoice) {
        Optional<InvoiceProcessingLog> existingLog = logRepository.findByInvoice(invoice);
        
        if (existingLog.isPresent()) {
            InvoiceProcessingLog processingLog = existingLog.get();
            boolean isProcessed = processingLog.getStatus() == InvoiceProcessingStatus.SUCCESS || 
                                 processingLog.getStatus() == InvoiceProcessingStatus.PARTIAL_SUCCESS;
            
            log.info("🔍 Invoice {} já processada: {} (Status: {})", 
                invoice.getInvoiceNumber(), isProcessed, processingLog.getStatus());
            
            return isProcessed;
        }
        
        return false;
    }

    /**
     * Verifica se uma invoice está sendo processada atualmente
     */
    @Transactional(readOnly = true)
    public boolean isInvoiceBeingProcessed(Invoice invoice) {
        Optional<InvoiceProcessingLog> existingLog = logRepository.findByInvoice(invoice);
        
        if (existingLog.isPresent()) {
            InvoiceProcessingLog processingLog = existingLog.get();
            boolean isProcessing = processingLog.getStatus() == InvoiceProcessingStatus.PROCESSING;
            
            log.info("🔍 Invoice {} em processamento: {} (Status: {})", 
                invoice.getInvoiceNumber(), isProcessing, processingLog.getStatus());
            
            return isProcessing;
        }
        
        return false;
    }

    /**
     * Busca log de processamento de uma invoice
     */
    @Transactional(readOnly = true)
    public Optional<InvoiceProcessingLog> findProcessingLog(Invoice invoice) {
        return logRepository.findByInvoice(invoice);
    }

    /**
     * Busca log de processamento por ID
     */
    @Transactional(readOnly = true)
    public Optional<InvoiceProcessingLog> findProcessingLogById(UUID logId) {
        return logRepository.findById(logId);
    }

    /**
     * Busca logs de processamento de um usuário
     */
    @Transactional(readOnly = true)
    public List<InvoiceProcessingLog> findProcessingLogsByUser(User user) {
        return logRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Busca logs por status
     */
    @Transactional(readOnly = true)
    public List<InvoiceProcessingLog> findProcessingLogsByStatus(InvoiceProcessingStatus status) {
        return logRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * Busca logs ativos (PENDING ou PROCESSING)
     */
    @Transactional(readOnly = true)
    public List<InvoiceProcessingLog> findActiveProcessingLogs() {
        return logRepository.findActiveProcessing();
    }

    /**
     * Incrementa contador de reprocessamento
     */
    @Transactional
    public void incrementReprocessedCount(InvoiceProcessingLog processingLog) {
        log.info("🔄 Incrementando contador de reprocessamento: {}", processingLog.getId());
        
        processingLog.setReprocessedCount(processingLog.getReprocessedCount() + 1);
        processingLog.setUpdatedAt(LocalDateTime.now());
        
        logRepository.save(processingLog);
        
        log.info("✅ Contador de reprocessamento atualizado: {} -> {}", 
            processingLog.getId(), processingLog.getReprocessedCount());
    }

    /**
     * Verifica se uma invoice pode ser reprocessada (para processamento individual)
     */
    @Transactional(readOnly = true)
    public ReprocessingValidationResult validateIndividualReprocessing(Invoice invoice) {
        log.info("🔍 Validando reprocessamento individual da invoice: {}", invoice.getInvoiceNumber());
        
        Optional<InvoiceProcessingLog> existingLog = logRepository.findByInvoice(invoice);
        
        if (existingLog.isEmpty()) {
            log.info("✅ Invoice {} pode ser processada - primeiro processamento", invoice.getInvoiceNumber());
            return ReprocessingValidationResult.builder()
                .canReprocess(true)
                .reason("Primeiro processamento")
                .build();
        }
        
        InvoiceProcessingLog processingLog = existingLog.get();
        
        // Se está sendo processada, não pode reprocessar
        if (processingLog.getStatus() == InvoiceProcessingStatus.PROCESSING) {
            log.warn("❌ Invoice {} está sendo processada - não pode reprocessar", invoice.getInvoiceNumber());
            return ReprocessingValidationResult.builder()
                .canReprocess(false)
                .reason("Invoice está sendo processada atualmente")
                .build();
        }
        
        // Se já foi processada com sucesso, pode reprocessar com aviso
        if (processingLog.getStatus() == InvoiceProcessingStatus.SUCCESS || 
            processingLog.getStatus() == InvoiceProcessingStatus.PARTIAL_SUCCESS) {
            
            log.info("⚠️ Invoice {} já foi processada - pode reprocessar com aviso", invoice.getInvoiceNumber());
            return ReprocessingValidationResult.builder()
                .canReprocess(true)
                .requiresConfirmation(true)
                .reason("Invoice já foi processada anteriormente")
                .previousProcessingLog(processingLog)
                .build();
        }
        
        // Se falhou, pode reprocessar
        if (processingLog.getStatus() == InvoiceProcessingStatus.ERROR) {
            log.info("✅ Invoice {} falhou anteriormente - pode reprocessar", invoice.getInvoiceNumber());
            return ReprocessingValidationResult.builder()
                .canReprocess(true)
                .reason("Invoice falhou no processamento anterior")
                .previousProcessingLog(processingLog)
                .build();
        }
        
        // Outros casos, pode reprocessar
        log.info("✅ Invoice {} pode ser reprocessada", invoice.getInvoiceNumber());
        return ReprocessingValidationResult.builder()
            .canReprocess(true)
            .reason("Processamento anterior não concluído")
            .previousProcessingLog(processingLog)
            .build();
    }

    /**
     * Verifica se uma invoice pode ser processada em lote
     */
    @Transactional(readOnly = true)
    public boolean canProcessInBatch(Invoice invoice) {
        log.info("🔍 Verificando se invoice {} pode ser processada em lote", invoice.getInvoiceNumber());
        
        Optional<InvoiceProcessingLog> existingLog = logRepository.findByInvoice(invoice);
        
        if (existingLog.isEmpty()) {
            log.info("✅ Invoice {} pode ser processada em lote - primeiro processamento", invoice.getInvoiceNumber());
            return true;
        }
        
        InvoiceProcessingLog processingLog = existingLog.get();
        
        // Se já foi processada com sucesso, não pode processar em lote
        if (processingLog.getStatus() == InvoiceProcessingStatus.SUCCESS || 
            processingLog.getStatus() == InvoiceProcessingStatus.PARTIAL_SUCCESS) {
            
            log.warn("❌ Invoice {} já foi processada - não pode processar em lote", invoice.getInvoiceNumber());
            return false;
        }
        
        // Se está sendo processada, não pode processar em lote
        if (processingLog.getStatus() == InvoiceProcessingStatus.PROCESSING) {
            log.warn("❌ Invoice {} está sendo processada - não pode processar em lote", invoice.getInvoiceNumber());
            return false;
        }
        
        // Se falhou ou está pendente, pode processar em lote
        log.info("✅ Invoice {} pode ser processada em lote", invoice.getInvoiceNumber());
        return true;
    }

    /**
     * Resultado da validação de reprocessamento
     */
    public static class ReprocessingValidationResult {
        private final boolean canReprocess;
        private final boolean requiresConfirmation;
        private final String reason;
        private final InvoiceProcessingLog previousProcessingLog;
        
        public ReprocessingValidationResult(boolean canReprocess, boolean requiresConfirmation, 
                                         String reason, InvoiceProcessingLog previousProcessingLog) {
            this.canReprocess = canReprocess;
            this.requiresConfirmation = requiresConfirmation;
            this.reason = reason;
            this.previousProcessingLog = previousProcessingLog;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public boolean isCanReprocess() { return canReprocess; }
        public boolean isRequiresConfirmation() { return requiresConfirmation; }
        public String getReason() { return reason; }
        public InvoiceProcessingLog getPreviousProcessingLog() { return previousProcessingLog; }
        
        public static class Builder {
            private boolean canReprocess = false;
            private boolean requiresConfirmation = false;
            private String reason = "";
            private InvoiceProcessingLog previousProcessingLog = null;
            
            public Builder canReprocess(boolean canReprocess) {
                this.canReprocess = canReprocess;
                return this;
            }
            
            public Builder requiresConfirmation(boolean requiresConfirmation) {
                this.requiresConfirmation = requiresConfirmation;
                return this;
            }
            
            public Builder reason(String reason) {
                this.reason = reason;
                return this;
            }
            
            public Builder previousProcessingLog(InvoiceProcessingLog previousProcessingLog) {
                this.previousProcessingLog = previousProcessingLog;
                return this;
            }
            
            public ReprocessingValidationResult build() {
                return new ReprocessingValidationResult(canReprocess, requiresConfirmation, reason, previousProcessingLog);
            }
        }
    }
} 