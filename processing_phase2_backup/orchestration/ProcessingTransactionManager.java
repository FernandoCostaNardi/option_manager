package com.olisystem.optionsmanager.service.invoice.processing.orchestration;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationSourceMapping;
import com.olisystem.optionsmanager.model.invoice.InvoiceProcessingLog;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.OperationSourceMappingRepository;
import com.olisystem.optionsmanager.repository.InvoiceProcessingLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de transações para processamento de invoices
 * Controla rollback total e isolamento de transações
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessingTransactionManager {

    private final OperationRepository operationRepository;
    private final OperationSourceMappingRepository mappingRepository;
    private final InvoiceProcessingLogRepository processingLogRepository;

    // Controle de transações ativas
    private final Map<String, ProcessTransaction> activeTransactions = new ConcurrentHashMap<>();
    private final Map<String, List<String>> processInvoiceTransactions = new ConcurrentHashMap<>();

    /**
     * Inicia transação principal do processo
     * 
     * @param processId ID do processo
     * @param invoices Lista de invoices a serem processadas
     * @return ID da transação principal
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String beginProcessTransaction(String processId, List<Invoice> invoices) {
        log.debug("🔄 Iniciando transação principal do processo {}", processId);
        
        ProcessTransaction transaction = ProcessTransaction.builder()
                .transactionId(processId)
                .processId(processId)
                .transactionType(TransactionType.PROCESS)
                .startTime(LocalDateTime.now())
                .invoices(new ArrayList<>(invoices))
                .createdOperations(new ArrayList<>())
                .createdMappings(new ArrayList<>())
                .createdLogs(new ArrayList<>())
                .status(TransactionStatus.ACTIVE)
                .build();
        
        activeTransactions.put(processId, transaction);
        processInvoiceTransactions.put(processId, new ArrayList<>());
        
        log.info("✅ Transação principal {} iniciada para {} invoices", processId, invoices.size());
        return processId;
    }

    /**
     * Inicia transação para uma invoice específica
     * 
     * @param processId ID do processo pai
     * @param invoice Invoice a ser processada
     * @return ID da transação da invoice
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String beginInvoiceTransaction(String processId, Invoice invoice) {
        String invoiceTransactionId = processId + "_" + invoice.getInvoiceNumber();
        
        log.debug("🔄 Iniciando transação da invoice {} no processo {}", 
                 invoice.getInvoiceNumber(), processId);
        
        ProcessTransaction invoiceTransaction = ProcessTransaction.builder()
                .transactionId(invoiceTransactionId)
                .processId(processId)
                .transactionType(TransactionType.INVOICE)
                .startTime(LocalDateTime.now())
                .invoices(Arrays.asList(invoice))
                .createdOperations(new ArrayList<>())
                .createdMappings(new ArrayList<>())
                .createdLogs(new ArrayList<>())
                .status(TransactionStatus.ACTIVE)
                .build();
        
        activeTransactions.put(invoiceTransactionId, invoiceTransaction);
        
        // Associar à transação principal
        processInvoiceTransactions.get(processId).add(invoiceTransactionId);
        
        log.debug("✅ Transação da invoice {} iniciada", invoice.getInvoiceNumber());
        return invoiceTransactionId;
    }

    /**
     * Registra operação criada para rollback
     * 
     * @param transactionId ID da transação
     * @param operation Operação criada
     */
    public void registerCreatedOperation(String transactionId, Operation operation) {
        ProcessTransaction transaction = activeTransactions.get(transactionId);
        if (transaction != null) {
            transaction.getCreatedOperations().add(operation);
            log.debug("📝 Operação {} registrada na transação {}", 
                     operation.getId().toString().substring(0, 8), transactionId);
        }
    }

    /**
     * Registra mapeamento criado para rollback
     * 
     * @param transactionId ID da transação
     * @param mapping Mapeamento criado
     */
    public void registerCreatedMapping(String transactionId, OperationSourceMapping mapping) {
        ProcessTransaction transaction = activeTransactions.get(transactionId);
        if (transaction != null) {
            transaction.getCreatedMappings().add(mapping);
            log.debug("📝 Mapeamento {} registrado na transação {}", 
                     mapping.getId().toString().substring(0, 8), transactionId);
        }
    }

    /**
     * Registra log criado para rollback
     * 
     * @param transactionId ID da transação
     * @param log Log criado
     */
    public void registerCreatedLog(String transactionId, InvoiceProcessingLog log) {
        ProcessTransaction transaction = activeTransactions.get(transactionId);
        if (transaction != null) {
            transaction.getCreatedLogs().add(log);
            log.debug("📝 Log {} registrado na transação {}", 
                     log.getId().toString().substring(0, 8), transactionId);
        }
    }

    /**
     * Confirma transação de uma invoice
     * 
     * @param invoiceTransactionId ID da transação da invoice
     */
    @Transactional
    public void commitInvoiceTransaction(String invoiceTransactionId) {
        ProcessTransaction transaction = activeTransactions.get(invoiceTransactionId);
        
        if (transaction != null) {
            transaction.setStatus(TransactionStatus.COMMITTED);
            transaction.setEndTime(LocalDateTime.now());
            
            log.info("✅ Transação da invoice {} confirmada - {} operações, {} mapeamentos",
                     invoiceTransactionId, 
                     transaction.getCreatedOperations().size(),
                     transaction.getCreatedMappings().size());
        }
    }

    /**
     * Executa rollback de uma transação de invoice
     * 
     * @param invoiceTransactionId ID da transação da invoice
     */
    @Transactional
    public void rollbackInvoiceTransaction(String invoiceTransactionId) {
        ProcessTransaction transaction = activeTransactions.get(invoiceTransactionId);
        
        if (transaction != null) {
            log.warn("🔄 Executando rollback da transação da invoice {}", invoiceTransactionId);
            
            executeRollbackOperations(transaction);
            
            transaction.setStatus(TransactionStatus.ROLLED_BACK);
            transaction.setEndTime(LocalDateTime.now());
            
            log.info("✅ Rollback da transação {} concluído", invoiceTransactionId);
        }
    }

    /**
     * Confirma transação principal do processo
     * 
     * @param processId ID do processo
     */
    @Transactional
    public void commitProcessTransaction(String processId) {
        ProcessTransaction transaction = activeTransactions.get(processId);
        
        if (transaction != null) {
            // Verificar se todas as transações de invoice foram confirmadas
            List<String> invoiceTransactionIds = processInvoiceTransactions.get(processId);
            boolean allCommitted = invoiceTransactionIds.stream()
                    .allMatch(id -> {
                        ProcessTransaction invoiceTransaction = activeTransactions.get(id);
                        return invoiceTransaction != null && 
                               invoiceTransaction.getStatus() == TransactionStatus.COMMITTED;
                    });
            
            if (allCommitted) {
                transaction.setStatus(TransactionStatus.COMMITTED);
                transaction.setEndTime(LocalDateTime.now());
                
                // Consolidar operações de todas as transações de invoice
                consolidateTransactionData(processId);
                
                log.info("✅ Transação principal {} confirmada - processo concluído com sucesso", processId);
            } else {
                log.warn("⚠️ Nem todas as transações de invoice foram confirmadas - processo não pode ser finalizado");
            }
        }
    }

    /**
     * Executa rollback completo do processo
     * 
     * @param processId ID do processo
     * @param cause Causa do rollback
     */
    @Transactional
    public void executeRollback(String processId, Exception cause) {
        log.error("🚨 Executando rollback COMPLETO do processo {} - Causa: {}", 
                 processId, cause.getMessage());
        
        ProcessTransaction mainTransaction = activeTransactions.get(processId);
        
        if (mainTransaction != null) {
            // Executar rollback de todas as transações de invoice primeiro
            List<String> invoiceTransactionIds = processInvoiceTransactions.get(processId);
            
            for (String invoiceTransactionId : invoiceTransactionIds) {
                ProcessTransaction invoiceTransaction = activeTransactions.get(invoiceTransactionId);
                
                if (invoiceTransaction != null && 
                    invoiceTransaction.getStatus() != TransactionStatus.ROLLED_BACK) {
                    
                    log.debug("🔄 Executando rollback da transação de invoice {}", invoiceTransactionId);
                    executeRollbackOperations(invoiceTransaction);
                    invoiceTransaction.setStatus(TransactionStatus.ROLLED_BACK);
                }
            }
            
            // Executar rollback da transação principal
            executeRollbackOperations(mainTransaction);
            
            mainTransaction.setStatus(TransactionStatus.ROLLED_BACK);
            mainTransaction.setEndTime(LocalDateTime.now());
            mainTransaction.setRollbackCause(cause.getMessage());
            
            log.error("✅ Rollback COMPLETO do processo {} concluído", processId);
        }
        
        // Limpar transações ativas
        cleanupTransactions(processId);
    }

    /**
     * Executa operações de rollback para uma transação
     * 
     * @param transaction Transação a ter rollback
     */
    private void executeRollbackOperations(ProcessTransaction transaction) {
        log.debug("🔄 Executando operações de rollback para transação {}", transaction.getTransactionId());
        
        // Rollback em ordem reversa de criação
        
        // 1. Remover logs de processamento
        for (InvoiceProcessingLog log : transaction.getCreatedLogs()) {
            try {
                processingLogRepository.delete(log);
                log.debug("🗑️ Log {} removido", log.getId());
            } catch (Exception e) {
                log.warn("⚠️ Erro ao remover log {}: {}", log.getId(), e.getMessage());
            }
        }
        
        // 2. Remover mapeamentos de origem
        for (OperationSourceMapping mapping : transaction.getCreatedMappings()) {
            try {
                mappingRepository.delete(mapping);
                log.debug("🗑️ Mapeamento {} removido", mapping.getId());
            } catch (Exception e) {
                log.warn("⚠️ Erro ao remover mapeamento {}: {}", mapping.getId(), e.getMessage());
            }
        }
        
        // 3. Remover operações criadas
        // ATENÇÃO: Isso pode ser complexo devido às dependências (Position, AverageOperationGroup, etc.)
        for (Operation operation : transaction.getCreatedOperations()) {
            try {
                // TODO: Implementar remoção cascata de Position, AverageOperationGroup, etc.
                // Por enquanto, apenas marcar como inativa
                operation.setStatus(com.olisystem.optionsmanager.model.operation.OperationStatus.HIDDEN);
                operationRepository.save(operation);
                
                log.debug("🗑️ Operação {} marcada como HIDDEN", operation.getId());
            } catch (Exception e) {
                log.warn("⚠️ Erro ao remover operação {}: {}", operation.getId(), e.getMessage());
            }
        }
        
        log.debug("✅ Rollback executado: {} operações, {} mapeamentos, {} logs processados",
                 transaction.getCreatedOperations().size(),
                 transaction.getCreatedMappings().size(),
                 transaction.getCreatedLogs().size());
    }

    /**
     * Consolida dados de todas as transações do processo
     * 
     * @param processId ID do processo
     */
    private void consolidateTransactionData(String processId) {
        ProcessTransaction mainTransaction = activeTransactions.get(processId);
        List<String> invoiceTransactionIds = processInvoiceTransactions.get(processId);
        
        // Consolidar operações, mapeamentos e logs de todas as transações
        for (String invoiceTransactionId : invoiceTransactionIds) {
            ProcessTransaction invoiceTransaction = activeTransactions.get(invoiceTransactionId);
            
            if (invoiceTransaction != null) {
                mainTransaction.getCreatedOperations().addAll(invoiceTransaction.getCreatedOperations());
                mainTransaction.getCreatedMappings().addAll(invoiceTransaction.getCreatedMappings());
                mainTransaction.getCreatedLogs().addAll(invoiceTransaction.getCreatedLogs());
            }
        }
        
        log.debug("📊 Dados consolidados: {} operações, {} mapeamentos, {} logs",
                 mainTransaction.getCreatedOperations().size(),
                 mainTransaction.getCreatedMappings().size(),
                 mainTransaction.getCreatedLogs().size());
    }

    /**
     * Limpa transações ativas após conclusão/rollback
     * 
     * @param processId ID do processo
     */
    private void cleanupTransactions(String processId) {
        List<String> invoiceTransactionIds = processInvoiceTransactions.remove(processId);
        
        if (invoiceTransactionIds != null) {
            for (String invoiceTransactionId : invoiceTransactionIds) {
                activeTransactions.remove(invoiceTransactionId);
            }
        }
        
        activeTransactions.remove(processId);
        
        log.debug("🧹 Transações limpas para processo {}", processId);
    }

    /**
     * Retorna estatísticas de uma transação
     * 
     * @param transactionId ID da transação
     * @return Estatísticas da transação
     */
    public TransactionStats getTransactionStats(String transactionId) {
        ProcessTransaction transaction = activeTransactions.get(transactionId);
        
        if (transaction == null) {
            return null;
        }
        
        return TransactionStats.builder()
                .transactionId(transactionId)
                .status(transaction.getStatus())
                .startTime(transaction.getStartTime())
                .endTime(transaction.getEndTime())
                .operationsCreated(transaction.getCreatedOperations().size())
                .mappingsCreated(transaction.getCreatedMappings().size())
                .logsCreated(transaction.getCreatedLogs().size())
                .invoicesProcessed(transaction.getInvoices().size())
                .build();
    }

    /**
     * Status de transação
     */
    public enum TransactionStatus {
        ACTIVE, COMMITTED, ROLLED_BACK
    }

    /**
     * Tipo de transação
     */
    public enum TransactionType {
        PROCESS, INVOICE
    }

    /**
     * Transação de processamento
     */
    @lombok.Builder
    @lombok.Data
    private static class ProcessTransaction {
        private String transactionId;
        private String processId;
        private TransactionType transactionType;
        private TransactionStatus status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<Invoice> invoices;
        private List<Operation> createdOperations;
        private List<OperationSourceMapping> createdMappings;
        private List<InvoiceProcessingLog> createdLogs;
        private String rollbackCause;
    }

    /**
     * Estatísticas de transação
     */
    @lombok.Builder
    @lombok.Data
    public static class TransactionStats {
        private String transactionId;
        private TransactionStatus status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int operationsCreated;
        private int mappingsCreated;
        private int logsCreated;
        private int invoicesProcessed;
        
        public long getDurationMs() {
            if (startTime != null && endTime != null) {
                return java.time.Duration.between(startTime, endTime).toMillis();
            }
            return 0;
        }
        
        public boolean isActive() {
            return status == TransactionStatus.ACTIVE;
        }
    }
}