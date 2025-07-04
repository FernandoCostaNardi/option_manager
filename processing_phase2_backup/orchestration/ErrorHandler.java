package com.olisystem.optionsmanager.service.invoice.processing.orchestration;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para capturar, categorizar e gerenciar erros durante processamento
 * Decide estratégias de recuperação e fornece relatórios estruturados
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@Slf4j
public class ErrorHandler {

    /**
     * Captura e categoriza um erro durante o processamento
     * 
     * @param error Erro a ser processado
     * @param context Contexto onde o erro ocorreu
     * @return Erro categorizado com estratégia de recuperação
     */
    public CategorizedError handleError(Throwable error, ProcessingContext context) {
        log.debug("🚨 Categorizando erro: {} no contexto {}", 
                 error.getMessage(), context.getPhase());
        
        ErrorCategory category = categorizeError(error);
        ErrorSeverity severity = determineSeverity(category, context);
        RecoveryStrategy strategy = determineRecoveryStrategy(category, severity, context);
        
        CategorizedError categorizedError = CategorizedError.builder()
                .originalError(error)
                .category(category)
                .severity(severity)
                .recoveryStrategy(strategy)
                .context(context)
                .timestamp(LocalDateTime.now())
                .errorId(UUID.randomUUID())
                .build();
        
        logCategorizedError(categorizedError);
        
        return categorizedError;
    }

    /**
     * Processa múltiplos erros e determina ação global
     * 
     * @param errors Lista de erros categorizados
     * @return Decisão sobre como proceder
     */
    public ErrorProcessingDecision processMultipleErrors(List<CategorizedError> errors) {
        log.debug("🔄 Processando {} erros categorizados", errors.size());
        
        if (errors.isEmpty()) {
            return ErrorProcessingDecision.builder()
                    .shouldContinue(true)
                    .shouldRollback(false)
                    .overallSeverity(ErrorSeverity.INFO)
                    .errors(errors)
                    .build();
        }
        
        // Determinar severidade geral
        ErrorSeverity overallSeverity = errors.stream()
                .map(CategorizedError::getSeverity)
                .max(Comparator.comparing(ErrorSeverity::getLevel))
                .orElse(ErrorSeverity.INFO);
        
        // Contar erros críticos
        long criticalErrors = errors.stream()
                .filter(e -> e.getSeverity() == ErrorSeverity.CRITICAL)
                .count();
        
        long fatalErrors = errors.stream()
                .filter(e -> e.getSeverity() == ErrorSeverity.FATAL)
                .count();
        
        // Decisões baseadas em severidade
        boolean shouldRollback = fatalErrors > 0 || criticalErrors >= 3;
        boolean shouldContinue = !shouldRollback && overallSeverity.getLevel() < ErrorSeverity.CRITICAL.getLevel();
        
        ErrorProcessingDecision decision = ErrorProcessingDecision.builder()
                .shouldContinue(shouldContinue)
                .shouldRollback(shouldRollback)
                .overallSeverity(overallSeverity)
                .errors(errors)
                .criticalErrorCount((int) criticalErrors)
                .fatalErrorCount((int) fatalErrors)
                .recommendedAction(determineRecommendedAction(overallSeverity, criticalErrors, fatalErrors))
                .build();
        
        log.info("📊 Decisão de processamento: continuar={}, rollback={}, severidade={}", 
                decision.isShouldContinue(), decision.isShouldRollback(), overallSeverity);
        
        return decision;
    }

    /**
     * Categoriza erro baseado no tipo e mensagem
     */
    private ErrorCategory categorizeError(Throwable error) {
        String errorMessage = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
        String errorClass = error.getClass().getSimpleName().toLowerCase();
        
        // Erros de validação
        if (error instanceof IllegalArgumentException || 
            errorMessage.contains("validation") || 
            errorMessage.contains("obrigatório") ||
            errorMessage.contains("invalid")) {
            return ErrorCategory.VALIDATION;
        }
        
        // Erros de negócio
        if (error instanceof BusinessException || 
            errorMessage.contains("business") ||
            errorMessage.contains("não encontrada") ||
            errorMessage.contains("duplicata")) {
            return ErrorCategory.BUSINESS_RULE;
        }
        
        // Erros de mapeamento
        if (errorMessage.contains("mapeamento") || 
            errorMessage.contains("conversão") ||
            errorMessage.contains("mapping")) {
            return ErrorCategory.MAPPING;
        }
        
        // Erros de banco de dados
        if (errorClass.contains("sql") || 
            errorClass.contains("database") ||
            errorClass.contains("hibernate") ||
            errorMessage.contains("constraint")) {
            return ErrorCategory.DATABASE;
        }
        
        // Erros de rede/sistema
        if (errorClass.contains("timeout") || 
            errorClass.contains("connection") ||
            errorMessage.contains("network")) {
            return ErrorCategory.SYSTEM;
        }
        
        // Erros de configuração
        if (errorMessage.contains("configuration") || 
            errorMessage.contains("property") ||
            errorClass.contains("nullpointer")) {
            return ErrorCategory.CONFIGURATION;
        }
        
        // Default: erro desconhecido
        return ErrorCategory.UNKNOWN;
    }

    /**
     * Determina severidade baseada na categoria e contexto
     */
    private ErrorSeverity determineSeverity(ErrorCategory category, ProcessingContext context) {
        switch (category) {
            case VALIDATION:
                return ErrorSeverity.WARNING;
            
            case BUSINESS_RULE:
                return context.isInCriticalPhase() ? ErrorSeverity.CRITICAL : ErrorSeverity.ERROR;
            
            case MAPPING:
                return ErrorSeverity.ERROR;
            
            case DATABASE:
                return ErrorSeverity.CRITICAL;
            
            case SYSTEM:
            case CONFIGURATION:
                return ErrorSeverity.FATAL;
            
            case UNKNOWN:
            default:
                return ErrorSeverity.ERROR;
        }
    }

    /**
     * Determina estratégia de recuperação
     */
    private RecoveryStrategy determineRecoveryStrategy(ErrorCategory category, 
                                                     ErrorSeverity severity, 
                                                     ProcessingContext context) {
        
        if (severity == ErrorSeverity.FATAL) {
            return RecoveryStrategy.ABORT_ALL;
        }
        
        if (severity == ErrorSeverity.CRITICAL) {
            return RecoveryStrategy.ROLLBACK_TRANSACTION;
        }
        
        switch (category) {
            case VALIDATION:
                return RecoveryStrategy.SKIP_ITEM;
            
            case BUSINESS_RULE:
                return RecoveryStrategy.SKIP_ITEM;
            
            case MAPPING:
                return RecoveryStrategy.SKIP_ITEM;
            
            case DATABASE:
                return RecoveryStrategy.RETRY_OPERATION;
            
            default:
                return RecoveryStrategy.CONTINUE_WITH_WARNING;
        }
    }

    /**
     * Determina ação recomendada baseada nos erros
     */
    private String determineRecommendedAction(ErrorSeverity overallSeverity, 
                                            long criticalErrors, 
                                            long fatalErrors) {
        
        if (fatalErrors > 0) {
            return "Interromper processamento e verificar configuração do sistema";
        }
        
        if (criticalErrors >= 3) {
            return "Reverter transações e revisar dados de entrada";
        }
        
        if (overallSeverity == ErrorSeverity.CRITICAL) {
            return "Revisar itens com erro e tentar novamente";
        }
        
        if (overallSeverity == ErrorSeverity.ERROR) {
            return "Continuar processamento e revisar erros posteriormente";
        }
        
        return "Processamento pode continuar normalmente";
    }

    /**
     * Log estruturado do erro categorizado
     */
    private void logCategorizedError(CategorizedError error) {
        String logLevel = error.getSeverity().name();
        String message = String.format("🚨 [%s] %s: %s (Estratégia: %s)", 
                                      logLevel,
                                      error.getCategory(),
                                      error.getOriginalError().getMessage(),
                                      error.getRecoveryStrategy());
        
        switch (error.getSeverity()) {
            case FATAL:
            case CRITICAL:
                log.error(message, error.getOriginalError());
                break;
            case ERROR:
                log.error(message);
                break;
            case WARNING:
                log.warn(message);
                break;
            case INFO:
            default:
                log.info(message);
                break;
        }
    }

    /**
     * Cria relatório de erros para auditoria
     */
    public ErrorReport createErrorReport(List<CategorizedError> errors, Invoice invoice) {
        Map<ErrorCategory, Long> errorsByCategory = errors.stream()
                .collect(Collectors.groupingBy(
                    CategorizedError::getCategory,
                    Collectors.counting()
                ));
        
        Map<ErrorSeverity, Long> errorsBySeverity = errors.stream()
                .collect(Collectors.groupingBy(
                    CategorizedError::getSeverity,
                    Collectors.counting()
                ));
        
        return ErrorReport.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .totalErrors(errors.size())
                .errorsByCategory(errorsByCategory)
                .errorsBySeverity(errorsBySeverity)
                .errors(errors)
                .reportTimestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Contexto onde o erro ocorreu
     */
    @lombok.Builder
    @lombok.Data
    public static class ProcessingContext {
        private String phase; // "VALIDATION", "DETECTION", "MAPPING", "PROCESSING"
        private String subPhase;
        private Invoice invoice;
        private InvoiceItem currentItem;
        private int itemIndex;
        private boolean inCriticalPhase;
        
        public String getFullPhase() {
            return subPhase != null ? phase + "." + subPhase : phase;
        }
    }

    /**
     * Erro categorizado com estratégia
     */
    @lombok.Builder
    @lombok.Data
    public static class CategorizedError {
        private UUID errorId;
        private Throwable originalError;
        private ErrorCategory category;
        private ErrorSeverity severity;
        private RecoveryStrategy recoveryStrategy;
        private ProcessingContext context;
        private LocalDateTime timestamp;
        
        public String getSummary() {
            return String.format("[%s] %s: %s", 
                                severity, category, originalError.getMessage());
        }
    }

    /**
     * Decisão sobre processamento baseada em erros
     */
    @lombok.Builder
    @lombok.Data
    public static class ErrorProcessingDecision {
        private boolean shouldContinue;
        private boolean shouldRollback;
        private ErrorSeverity overallSeverity;
        private List<CategorizedError> errors;
        private int criticalErrorCount;
        private int fatalErrorCount;
        private String recommendedAction;
        
        public boolean canProceed() {
            return shouldContinue && !shouldRollback;
        }
        
        public boolean requiresIntervention() {
            return fatalErrorCount > 0 || criticalErrorCount >= 3;
        }
    }

    /**
     * Relatório de erros para auditoria
     */
    @lombok.Builder
    @lombok.Data
    public static class ErrorReport {
        private UUID invoiceId;
        private String invoiceNumber;
        private int totalErrors;
        private Map<ErrorCategory, Long> errorsByCategory;
        private Map<ErrorSeverity, Long> errorsBySeverity;
        private List<CategorizedError> errors;
        private LocalDateTime reportTimestamp;
        
        public boolean hasErrors() {
            return totalErrors > 0;
        }
        
        public boolean hasCriticalErrors() {
            return errorsBySeverity.getOrDefault(ErrorSeverity.CRITICAL, 0L) > 0 ||
                   errorsBySeverity.getOrDefault(ErrorSeverity.FATAL, 0L) > 0;
        }
    }

    /**
     * Categorias de erro
     */
    public enum ErrorCategory {
        VALIDATION("Validação"),
        BUSINESS_RULE("Regra de Negócio"), 
        MAPPING("Mapeamento"),
        DATABASE("Banco de Dados"),
        SYSTEM("Sistema"),
        CONFIGURATION("Configuração"),
        UNKNOWN("Desconhecido");
        
        private final String description;
        
        ErrorCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }

    /**
     * Severidade do erro
     */
    public enum ErrorSeverity {
        INFO(1),
        WARNING(2),
        ERROR(3),
        CRITICAL(4),
        FATAL(5);
        
        private final int level;
        
        ErrorSeverity(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }

    /**
     * Estratégias de recuperação
     */
    public enum RecoveryStrategy {
        CONTINUE_WITH_WARNING("Continuar com aviso"),
        SKIP_ITEM("Pular item"),
        RETRY_OPERATION("Tentar novamente"),
        ROLLBACK_TRANSACTION("Reverter transação"),
        ABORT_ALL("Abortar tudo");
        
        private final String description;
        
        RecoveryStrategy(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}