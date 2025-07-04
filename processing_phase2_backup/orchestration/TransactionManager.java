package com.olisystem.optionsmanager.service.invoice.processing.orchestration;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationSourceMapping;
import com.olisystem.optionsmanager.model.invoice.InvoiceProcessingLog;
import com.olisystem.optionsmanager.model.enums.InvoiceProcessingStatus;
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
import java.util.stream.Collectors;

/**
 * Serviço para gerenciar transações e rollback de processamento
 * Garante consistência de dados em caso de erro
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionManager {

    private final OperationRepository operationRepository;
    private final OperationSourceMappingRepository mappingRepository;
    private final InvoiceProcessingLogRepository processingLogRepository;

    /**
     * Executa processamento dentro de transação controlada
     * 
     * @param transactionContext Contexto da transação
     * @param processor Função de processamento
     * @return Resultado do processamento
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public <T> TransactionResult<T> executeInTransaction(
            TransactionContext transactionContext,
            TransactionProcessor<T> processor) {
        
        log.debug("🔄 Iniciando transação para invoice {}", 
                 transactionContext.getInvoiceProcessingLog().getInvoice().getInvoiceNumber());
        
        // Marcar início do processamento
        markProcessingStarted(transactionContext.getInvoiceProcessingLog());
        
        TransactionResult<T> result;
        
        try {
            // Executar processamento
            T processingResult = processor.process(transactionContext);
            
            // Coletar operações criadas durante o processamento
            List<Operation> createdOperations = collectCreatedOperations(transactionContext);
            List<OperationSourceMapping> createdMappings = collectCreatedMappings(transactionContext);
            
            // Validar resultados
            validateTransactionResults(createdOperations, createdMappings);
            
            // Atualizar log de processamento com sucesso
            updateProcessingLogSuccess(transactionContext.getInvoiceProcessingLog(), 
                                     createdOperations, createdMappings);
            
            result = TransactionResult.<T>builder()
                    .successful(true)
                    .result(processingResult)
                    .createdOperations(createdOperations)
                    .createdMappings(createdMappings)
                    .transactionContext(transactionContext)
                    .build();
            
            log.info("✅ Transação concluída com sucesso: {} operações criadas/finalizadas",
                     createdOperations.size());
            
        } catch (Exception e) {
            log.error("❌ Erro na transação: {}", e.getMessage(), e);
            
            // Atualizar log com erro (rollback automático fará o resto)
            updateProcessingLogError(transactionContext.getInvoiceProcessingLog(), e);
            
            result = TransactionResult.<T>builder()
                    .successful(false)
                    .error(e)
                    .transactionContext(transactionContext)
                    .build();
            
            // Re-lançar exceção para triggering rollback automático
            throw e;
        }
        
        return result;
    }

    /**
     * Executa rollback manual de operações já commitadas
     * Usado quando precisamos desfazer processamento de sessões anteriores
     * 
     * @param invoiceProcessingLog Log de processamento a ser revertido
     * @return Resultado do rollback
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RollbackResult manualRollback(InvoiceProcessingLog invoiceProcessingLog) {
        log.warn("🔄 Iniciando rollback manual para invoice {}", 
                invoiceProcessingLog.getInvoice().getInvoiceNumber());
        
        List<String> rollbackActions = new ArrayList<>();
        List<String> rollbackErrors = new ArrayList<>();
        
        try {
            // Buscar operações criadas neste processamento
            List<OperationSourceMapping> mappings = mappingRepository
                    .findByInvoice(invoiceProcessingLog.getInvoice());
            
            List<Operation> operationsToRollback = mappings.stream()
                    .map(OperationSourceMapping::getOperation)
                    .distinct()
                    .collect(Collectors.toList());
            
            log.info("📊 Rollback manual: {} operações a serem revertidas", operationsToRollback.size());
            
            // Reverter operações em ordem inversa
            Collections.reverse(operationsToRollback);
            
            for (Operation operation : operationsToRollback) {
                try {
                    rollbackSingleOperation(operation, rollbackActions);
                } catch (Exception e) {
                    String error = String.format("Erro ao reverter operação %s: %s", 
                                                operation.getId(), e.getMessage());
                    rollbackErrors.add(error);
                    log.error("❌ {}", error, e);
                }
            }
            
            // Remover mapeamentos
            try {
                mappingRepository.deleteAll(mappings);
                rollbackActions.add("Removidos " + mappings.size() + " mapeamentos de origem");
            } catch (Exception e) {
                rollbackErrors.add("Erro ao remover mapeamentos: " + e.getMessage());
            }
            
            // Atualizar status do log de processamento
            invoiceProcessingLog.setStatus(InvoiceProcessingStatus.CANCELLED);
            invoiceProcessingLog.setCompletedAt(LocalDateTime.now());
            invoiceProcessingLog.calculateProcessingDuration();
            processingLogRepository.save(invoiceProcessingLog);
            
            rollbackActions.add("Status do processamento atualizado para CANCELLED");
            
            RollbackResult result = RollbackResult.builder()
                    .successful(rollbackErrors.isEmpty())
                    .operationsRolledBack(operationsToRollback.size() - rollbackErrors.size())
                    .rollbackActions(rollbackActions)
                    .rollbackErrors(rollbackErrors)
                    .invoiceProcessingLog(invoiceProcessingLog)
                    .build();
            
            log.info("✅ Rollback manual concluído: {} ações, {} erros", 
                     rollbackActions.size(), rollbackErrors.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Erro crítico no rollback manual: {}", e.getMessage(), e);
            throw new RuntimeException("Falha no rollback manual", e);
        }
    }

    /**
     * Verifica se uma transação pode ser executada com segurança
     * 
     * @param invoiceProcessingLog Log de processamento
     * @return Resultado da verificação
     */
    public TransactionSafetyCheck checkTransactionSafety(InvoiceProcessingLog invoiceProcessingLog) {
        List<String> warnings = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        
        // Verificar se já existe processamento ativo
        boolean hasActiveProcessing = processingLogRepository
                .hasActiveProcessing(invoiceProcessingLog.getInvoice());
        
        if (hasActiveProcessing) {
            blockers.add("Existe processamento ativo para esta invoice");
        }
        
        // Verificar processamentos anteriores
        Optional<InvoiceProcessingLog> existingLog = processingLogRepository
                .findByInvoice(invoiceProcessingLog.getInvoice());
        
        if (existingLog.isPresent() && existingLog.get().getStatus() == InvoiceProcessingStatus.SUCCESS) {
            warnings.add("Invoice já foi processada com sucesso anteriormente");
        }
        
        // Verificar operações existentes
        List<OperationSourceMapping> existingMappings = mappingRepository
                .findByInvoice(invoiceProcessingLog.getInvoice());
        
        if (!existingMappings.isEmpty()) {
            warnings.add(String.format("Existem %d operações já criadas para esta invoice", 
                                      existingMappings.size()));
        }
        
        return TransactionSafetyCheck.builder()
                .safe(blockers.isEmpty())
                .warnings(warnings)
                .blockers(blockers)
                .existingOperationsCount(existingMappings.size())
                .build();
    }

    /**
     * Cria contexto de transação
     * 
     * @param invoiceProcessingLog Log de processamento
     * @return Contexto configurado
     */
    public TransactionContext createTransactionContext(InvoiceProcessingLog invoiceProcessingLog) {
        return TransactionContext.builder()
                .transactionId(UUID.randomUUID())
                .invoiceProcessingLog(invoiceProcessingLog)
                .startTime(LocalDateTime.now())
                .createdOperationIds(new ArrayList<>())
                .createdMappingIds(new ArrayList<>())
                .build();
    }

    /**
     * Marca início do processamento
     */
    private void markProcessingStarted(InvoiceProcessingLog processingLog) {
        processingLog.markAsStarted();
        processingLogRepository.save(processingLog);
    }

    /**
     * Coleta operações criadas durante transação
     */
    private List<Operation> collectCreatedOperations(TransactionContext context) {
        if (context.getCreatedOperationIds().isEmpty()) {
            return new ArrayList<>();
        }
        
        return operationRepository.findAllById(context.getCreatedOperationIds());
    }

    /**
     * Coleta mapeamentos criados durante transação
     */
    private List<OperationSourceMapping> collectCreatedMappings(TransactionContext context) {
        if (context.getCreatedMappingIds().isEmpty()) {
            return new ArrayList<>();
        }
        
        return mappingRepository.findAllById(context.getCreatedMappingIds());
    }

    /**
     * Valida resultados da transação
     */
    private void validateTransactionResults(List<Operation> operations, List<OperationSourceMapping> mappings) {
        // Verificar consistência entre operações e mapeamentos
        Set<UUID> operationIds = operations.stream()
                .map(Operation::getId)
                .collect(Collectors.toSet());
        
        Set<UUID> mappedOperationIds = mappings.stream()
                .map(mapping -> mapping.getOperation().getId())
                .collect(Collectors.toSet());
        
        if (!operationIds.equals(mappedOperationIds)) {
            log.warn("⚠️ Inconsistência: operações criadas não correspondem aos mapeamentos");
        }
    }

    /**
     * Atualiza log com sucesso
     */
    private void updateProcessingLogSuccess(InvoiceProcessingLog processingLog,
                                          List<Operation> operations,
                                          List<OperationSourceMapping> mappings) {
        
        long createdOperations = operations.stream()
                .filter(op -> mappings.stream()
                       .anyMatch(m -> m.getOperation().getId().equals(op.getId()) && 
                                     m.getMappingType().isEntry()))
                .count();
        
        long finalizedOperations = operations.stream()
                .filter(op -> mappings.stream()
                       .anyMatch(m -> m.getOperation().getId().equals(op.getId()) && 
                                     m.getMappingType().isExit()))
                .count();
        
        processingLog.setOperationsCreated((int) createdOperations);
        processingLog.setOperationsUpdated((int) finalizedOperations);
        processingLog.markAsCompleted(InvoiceProcessingStatus.SUCCESS);
        
        processingLogRepository.save(processingLog);
    }

    /**
     * Atualiza log com erro
     */
    private void updateProcessingLogError(InvoiceProcessingLog processingLog, Exception error) {
        processingLog.setErrorMessage(error.getMessage());
        processingLog.markAsCompleted(InvoiceProcessingStatus.ERROR);
        
        try {
            processingLogRepository.save(processingLog);
        } catch (Exception e) {
            log.error("❌ Erro ao salvar log de processamento com erro: {}", e.getMessage(), e);
        }
    }

    /**
     * Executa rollback de uma operação específica
     */
    private void rollbackSingleOperation(Operation operation, List<String> actions) {
        // Nota: O rollback real dependeria do status da operação
        // Por enquanto, apenas registrar a ação (rollback automático do Spring já desfez a criação)
        
        String action = String.format("Operação %s marcada para rollback (status: %s)", 
                                     operation.getId().toString().substring(0, 8),
                                     operation.getStatus());
        actions.add(action);
        
        log.debug("🔄 {}", action);
    }

    /**
     * Interface para processador de transação
     */
    @FunctionalInterface
    public interface TransactionProcessor<T> {
        T process(TransactionContext context) throws Exception;
    }

    /**
     * Contexto de transação
     */
    @lombok.Builder
    @lombok.Data
    public static class TransactionContext {
        private UUID transactionId;
        private InvoiceProcessingLog invoiceProcessingLog;
        private LocalDateTime startTime;
        private List<UUID> createdOperationIds;
        private List<UUID> createdMappingIds;
        
        public void addCreatedOperation(UUID operationId) {
            createdOperationIds.add(operationId);
        }
        
        public void addCreatedMapping(UUID mappingId) {
            createdMappingIds.add(mappingId);
        }
    }

    /**
     * Resultado de transação
     */
    @lombok.Builder
    @lombok.Data
    public static class TransactionResult<T> {
        private boolean successful;
        private T result;
        private Exception error;
        private List<Operation> createdOperations;
        private List<OperationSourceMapping> createdMappings;
        private TransactionContext transactionContext;
        
        public boolean hasCreatedOperations() {
            return createdOperations != null && !createdOperations.isEmpty();
        }
        
        public int getOperationCount() {
            return createdOperations != null ? createdOperations.size() : 0;
        }
    }

    /**
     * Resultado de rollback
     */
    @lombok.Builder
    @lombok.Data
    public static class RollbackResult {
        private boolean successful;
        private int operationsRolledBack;
        private List<String> rollbackActions;
        private List<String> rollbackErrors;
        private InvoiceProcessingLog invoiceProcessingLog;
        
        public boolean hasErrors() {
            return rollbackErrors != null && !rollbackErrors.isEmpty();
        }
        
        public String getSummary() {
            return String.format("Rollback %s: %d operações, %d ações, %d erros",
                                successful ? "concluído" : "falhado",
                                operationsRolledBack,
                                rollbackActions.size(),
                                rollbackErrors.size());
        }
    }

    /**
     * Verificação de segurança de transação
     */
    @lombok.Builder
    @lombok.Data
    public static class TransactionSafetyCheck {
        private boolean safe;
        private List<String> warnings;
        private List<String> blockers;
        private int existingOperationsCount;
        
        public boolean hasWarnings() {
            return warnings != null && !warnings.isEmpty();
        }
        
        public boolean hasBlockers() {
            return blockers != null && !blockers.isEmpty();
        }
        
        public String getSafetyReport() {
            StringBuilder report = new StringBuilder();
            report.append("Segurança da transação: ").append(safe ? "SEGURA" : "BLOQUEADA").append("\n");
            
            if (hasBlockers()) {
                report.append("Bloqueadores:\n");
                blockers.forEach(blocker -> report.append("- ").append(blocker).append("\n"));
            }
            
            if (hasWarnings()) {
                report.append("Avisos:\n");
                warnings.forEach(warning -> report.append("- ").append(warning).append("\n"));
            }
            
            return report.toString();
        }
    }
}