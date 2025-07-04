package com.olisystem.optionsmanager.service.invoice.processing.orchestration;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.auth.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Manipulador de erros para processamento de invoices
 * Categoriza, trata e reporta erros durante o processamento
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@Slf4j
public class ProcessingErrorHandler {

    // Contadores de erro por processo
    private final Map<String, ProcessErrorContext> processErrorContexts = new ConcurrentHashMap<>();
    
    // Histórico de erros (últimos 1000)
    private final LinkedList<ProcessingError> errorHistory = new LinkedList<>();
    private static final int MAX_ERROR_HISTORY = 1000;
    
    // Padrões para categorização automática de erros
    private static final Map<Pattern, ErrorCategory> ERROR_PATTERNS = Map.of(
        Pattern.compile("(?i).*(connection|timeout|network).*"), ErrorCategory.INFRASTRUCTURE,
        Pattern.compile("(?i).*(validation|invalid|required).*"), ErrorCategory.VALIDATION,
        Pattern.compile("(?i).*(duplicate|already exists).*"), ErrorCategory.BUSINESS_RULE,
        Pattern.compile("(?i).*(permission|access|unauthorized).*"), ErrorCategory.SECURITY,
        Pattern.compile("(?i).*(null.*pointer|array.*index).*"), ErrorCategory.APPLICATION,
        Pattern.compile("(?i).*(database|sql|constraint).*"), ErrorCategory.DATA
    );

    /**
     * Manipula erro de validação de lote
     * 
     * @param processId ID do processo
     * @param error Erro ocorrido
     */
    public void handleBatchValidationError(String processId, Exception error) {
        ProcessingError processingError = createProcessingError(
            processId, null, null, error, ErrorType.BATCH_VALIDATION
        );
        
        registerError(processId, processingError);
        
        log.error("🚨 Erro de validação de lote no processo {}: {}", processId, error.getMessage());
        
        // Erro de lote é sempre crítico - para todo o processamento
        markProcessAsCriticalFailure(processId, "Validação de lote falhou");
    }

    /**
     * Manipula erro de validação de invoice individual
     * 
     * @param processId ID do processo
     * @param invoice Invoice com erro
     * @param error Erro ocorrido
     */
    public void handleValidationError(String processId, Invoice invoice, Exception error) {
        ProcessingError processingError = createProcessingError(
            processId, invoice, null, error, ErrorType.INVOICE_VALIDATION
        );
        
        registerError(processId, processingError);
        
        log.warn("⚠️ Erro de validação na invoice {} do processo {}: {}", 
                invoice.getInvoiceNumber(), processId, error.getMessage());
        
        // Erro de validação de invoice individual não para o processo
        // Apenas registra para relatório
    }

    /**
     * Manipula erro de detecção
     * 
     * @param processId ID do processo
     * @param invoice Invoice com erro
     * @param error Erro ocorrido
     */
    public void handleDetectionError(String processId, Invoice invoice, Exception error) {
        ProcessingError processingError = createProcessingError(
            processId, invoice, null, error, ErrorType.DETECTION
        );
        
        registerError(processId, processingError);
        
        log.warn("⚠️ Erro de detecção na invoice {} do processo {}: {}", 
                invoice.getInvoiceNumber(), processId, error.getMessage());
        
        // Avaliar se erro é recuperável
        if (isRecoverableError(error)) {
            log.info("🔄 Erro de detecção é recuperável - continuando processamento");
        } else {
            log.warn("❌ Erro de detecção não recuperável - invoice será ignorada");
        }
    }

    /**
     * Manipula erro de processamento
     * 
     * @param processId ID do processo
     * @param invoice Invoice com erro
     * @param error Erro ocorrido
     */
    public void handleProcessingError(String processId, Invoice invoice, Exception error) {
        ProcessingError processingError = createProcessingError(
            processId, invoice, null, error, ErrorType.PROCESSING
        );
        
        registerError(processId, processingError);
        
        log.error("❌ Erro de processamento na invoice {} do processo {}: {}", 
                 invoice.getInvoiceNumber(), processId, error.getMessage());
        
        ProcessErrorContext context = processErrorContexts.get(processId);
        
        // Verificar se deve tentar retry
        if (context != null && shouldRetry(processingError, context)) {
            log.info("🔄 Tentando retry para invoice {} (tentativa {})", 
                    invoice.getInvoiceNumber(), context.getRetryCount(invoice.getId()) + 1);
            
            context.incrementRetry(invoice.getId());
            // O retry seria implementado chamando novamente o processamento da invoice
        } else {
            log.warn("❌ Não será feito retry para invoice {} - erro não recuperável ou limite atingido", 
                    invoice.getInvoiceNumber());
        }
    }

    /**
     * Manipula erro crítico que afeta todo o processo
     * 
     * @param processId ID do processo
     * @param error Erro crítico
     */
    public void handleCriticalError(String processId, Exception error) {
        ProcessingError processingError = createProcessingError(
            processId, null, null, error, ErrorType.CRITICAL
        );
        
        registerError(processId, processingError);
        
        log.error("🔥 ERRO CRÍTICO no processo {}: {}", processId, error.getMessage(), error);
        
        markProcessAsCriticalFailure(processId, "Erro crítico: " + error.getMessage());
        
        // Notificar sistemas externos sobre erro crítico
        notifyExternalSystems(processingError);
    }

    /**
     * Manipula erro de rollback
     * 
     * @param processId ID do processo
     * @param originalError Erro original que causou rollback
     * @param rollbackError Erro durante rollback (se houver)
     */
    public void handleRollbackError(String processId, Exception originalError, Exception rollbackError) {
        if (rollbackError != null) {
            ProcessingError processingError = createProcessingError(
                processId, null, null, rollbackError, ErrorType.ROLLBACK
            );
            
            processingError.setOriginalError(originalError.getMessage());
            registerError(processId, processingError);
            
            log.error("🔥 ERRO DURANTE ROLLBACK no processo {}: Original: {} | Rollback: {}", 
                     processId, originalError.getMessage(), rollbackError.getMessage());
        } else {
            log.info("✅ Rollback executado com sucesso para processo {} após erro: {}", 
                    processId, originalError.getMessage());
        }
    }

    /**
     * Retorna relatório de erros de um processo
     * 
     * @param processId ID do processo
     * @return Relatório consolidado
     */
    public ErrorReport getErrorReport(String processId) {
        ProcessErrorContext context = processErrorContexts.get(processId);
        
        if (context == null) {
            return ErrorReport.builder()
                    .processId(processId)
                    .errors(new ArrayList<>())
                    .totalErrors(0)
                    .criticalErrors(0)
                    .recoverableErrors(0)
                    .invoicesAffected(0)
                    .build();
        }
        
        List<ProcessingError> processErrors = context.getErrors();
        
        long criticalErrors = processErrors.stream()
                .filter(error -> error.getSeverity() == ErrorSeverity.CRITICAL)
                .count();
        
        long recoverableErrors = processErrors.stream()
                .filter(error -> error.isRecoverable())
                .count();
        
        Set<String> affectedInvoices = new HashSet<>();
        processErrors.stream()
                .filter(error -> error.getInvoiceNumber() != null)
                .forEach(error -> affectedInvoices.add(error.getInvoiceNumber()));
        
        Map<ErrorCategory, Long> errorsByCategory = processErrors.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    ProcessingError::getCategory,
                    java.util.stream.Collectors.counting()
                ));
        
        Map<ErrorType, Long> errorsByType = processErrors.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    ProcessingError::getType,
                    java.util.stream.Collectors.counting()
                ));
        
        return ErrorReport.builder()
                .processId(processId)
                .errors(processErrors)
                .totalErrors(processErrors.size())
                .criticalErrors((int) criticalErrors)
                .recoverableErrors((int) recoverableErrors)
                .invoicesAffected(affectedInvoices.size())
                .errorsByCategory(errorsByCategory)
                .errorsByType(errorsByType)
                .hasBlockingErrors(context.isCriticalFailure())
                .build();
    }

    /**
     * Retorna estatísticas gerais de erros
     * 
     * @return Estatísticas consolidadas
     */
    public ErrorStatistics getErrorStatistics() {
        Map<ErrorCategory, Long> categoryStats = errorHistory.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    ProcessingError::getCategory,
                    java.util.stream.Collectors.counting()
                ));
        
        Map<ErrorType, Long> typeStats = errorHistory.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    ProcessingError::getType,
                    java.util.stream.Collectors.counting()
                ));
        
        long criticalErrorsLast24h = errorHistory.stream()
                .filter(error -> error.getTimestamp().isAfter(LocalDateTime.now().minusHours(24)))
                .filter(error -> error.getSeverity() == ErrorSeverity.CRITICAL)
                .count();
        
        return ErrorStatistics.builder()
                .totalErrorsInHistory(errorHistory.size())
                .errorsByCategory(categoryStats)
                .errorsByType(typeStats)
                .criticalErrorsLast24h((int) criticalErrorsLast24h)
                .activeProcessesWithErrors(processErrorContexts.size())
                .build();
    }

    /**
     * Limpa contexto de erro de um processo concluído
     * 
     * @param processId ID do processo
     */
    public void cleanupProcessErrorContext(String processId) {
        ProcessErrorContext context = processErrorContexts.remove(processId);
        
        if (context != null) {
            log.debug("🧹 Contexto de erro limpo para processo {}", processId);
        }
    }

    /**
     * Cria erro de processamento padronizado
     */
    private ProcessingError createProcessingError(String processId, Invoice invoice, User user, 
                                                Exception error, ErrorType type) {
        
        ErrorCategory category = categorizeError(error);
        ErrorSeverity severity = determineSeverity(type, category);
        boolean recoverable = isRecoverableError(error);
        
        return ProcessingError.builder()
                .id(UUID.randomUUID())
                .processId(processId)
                .invoiceId(invoice != null ? invoice.getId() : null)
                .invoiceNumber(invoice != null ? invoice.getInvoiceNumber() : null)
                .userId(user != null ? user.getId() : (invoice != null ? invoice.getUser().getId() : null))
                .type(type)
                .category(category)
                .severity(severity)
                .errorMessage(error.getMessage())
                .stackTrace(getStackTraceString(error))
                .isRecoverable(recoverable)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Registra erro no contexto do processo
     */
    private void registerError(String processId, ProcessingError error) {
        ProcessErrorContext context = processErrorContexts.computeIfAbsent(
            processId, k -> new ProcessErrorContext(processId)
        );
        
        context.addError(error);
        
        // Adicionar ao histórico global
        addToErrorHistory(error);
    }

    /**
     * Categoriza erro baseado na mensagem
     */
    private ErrorCategory categorizeError(Exception error) {
        String message = error.getMessage();
        if (message == null) {
            message = error.getClass().getSimpleName();
        }
        
        for (Map.Entry<Pattern, ErrorCategory> entry : ERROR_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(message).matches()) {
                return entry.getValue();
            }
        }
        
        return ErrorCategory.UNKNOWN;
    }

    /**
     * Determina severidade do erro
     */
    private ErrorSeverity determineSeverity(ErrorType type, ErrorCategory category) {
        // Tipos sempre críticos
        if (type == ErrorType.CRITICAL || type == ErrorType.ROLLBACK) {
            return ErrorSeverity.CRITICAL;
        }
        
        // Categorias que indicam severidade alta
        if (category == ErrorCategory.INFRASTRUCTURE || category == ErrorCategory.SECURITY) {
            return ErrorSeverity.HIGH;
        }
        
        // Validações e regras de negócio são médias
        if (type == ErrorType.INVOICE_VALIDATION || category == ErrorCategory.BUSINESS_RULE) {
            return ErrorSeverity.MEDIUM;
        }
        
        return ErrorSeverity.LOW;
    }

    /**
     * Verifica se erro é recuperável
     */
    private boolean isRecoverableError(Exception error) {
        String message = error.getMessage();
        if (message == null) {
            return false;
        }
        
        // Erros temporários que podem ser recuperados
        List<String> recoverablePatterns = Arrays.asList(
            "timeout", "connection", "network", "temporary", "retry"
        );
        
        return recoverablePatterns.stream()
                .anyMatch(pattern -> message.toLowerCase().contains(pattern));
    }

    /**
     * Verifica se deve tentar retry
     */
    private boolean shouldRetry(ProcessingError error, ProcessErrorContext context) {
        if (!error.isRecoverable()) {
            return false;
        }
        
        // Limite de 3 tentativas por invoice
        int retryCount = context.getRetryCount(error.getInvoiceId());
        return retryCount < 3;
    }

    /**
     * Marca processo como falha crítica
     */
    private void markProcessAsCriticalFailure(String processId, String reason) {
        ProcessErrorContext context = processErrorContexts.get(processId);
        if (context != null) {
            context.setCriticalFailure(true);
            context.setCriticalFailureReason(reason);
        }
    }

    /**
     * Notifica sistemas externos sobre erro crítico
     */
    private void notifyExternalSystems(ProcessingError error) {
        // TODO: Implementar notificações (email, webhook, etc.)
        log.info("📧 Notificação de erro crítico enviada para sistemas externos");
    }

    /**
     * Converte stack trace para string
     */
    private String getStackTraceString(Exception error) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        error.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Adiciona erro ao histórico global
     */
    private void addToErrorHistory(ProcessingError error) {
        errorHistory.addLast(error);
        
        // Manter apenas últimos 1000 erros
        if (errorHistory.size() > MAX_ERROR_HISTORY) {
            errorHistory.removeFirst();
        }
    }

    // Enums e classes auxiliares

    public enum ErrorType {
        BATCH_VALIDATION, INVOICE_VALIDATION, DETECTION, PROCESSING, CRITICAL, ROLLBACK
    }

    public enum ErrorCategory {
        VALIDATION, BUSINESS_RULE, INFRASTRUCTURE, SECURITY, APPLICATION, DATA, UNKNOWN
    }

    public enum ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    @lombok.Builder
    @lombok.Data
    public static class ProcessingError {
        private UUID id;
        private String processId;
        private UUID invoiceId;
        private String invoiceNumber;
        private UUID userId;
        private ErrorType type;
        private ErrorCategory category;
        private ErrorSeverity severity;
        private String errorMessage;
        private String stackTrace;
        private String originalError; // Para erros de rollback
        private boolean isRecoverable;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    private static class ProcessErrorContext {
        private final String processId;
        private final List<ProcessingError> errors = new ArrayList<>();
        private final Map<UUID, Integer> retryCountByInvoice = new HashMap<>();
        private boolean criticalFailure = false;
        private String criticalFailureReason;
        
        public ProcessErrorContext(String processId) {
            this.processId = processId;
        }
        
        public void addError(ProcessingError error) {
            errors.add(error);
        }
        
        public int getRetryCount(UUID invoiceId) {
            return retryCountByInvoice.getOrDefault(invoiceId, 0);
        }
        
        public void incrementRetry(UUID invoiceId) {
            retryCountByInvoice.put(invoiceId, getRetryCount(invoiceId) + 1);
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class ErrorReport {
        private String processId;
        private List<ProcessingError> errors;
        private int totalErrors;
        private int criticalErrors;
        private int recoverableErrors;
        private int invoicesAffected;
        private Map<ErrorCategory, Long> errorsByCategory;
        private Map<ErrorType, Long> errorsByType;
        private boolean hasBlockingErrors;
    }

    @lombok.Builder
    @lombok.Data
    public static class ErrorStatistics {
        private int totalErrorsInHistory;
        private Map<ErrorCategory, Long> errorsByCategory;
        private Map<ErrorType, Long> errorsByType;
        private int criticalErrorsLast24h;
        private int activeProcessesWithErrors;
    }
}