package com.olisystem.optionsmanager.service.invoice.processing.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler de erros para processamento de invoices
 * Categoriza e trata diferentes tipos de erro
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorHandler {

    /**
     * Categoriza um erro
     */
    public ErrorCategory categorizeError(Exception error) {
        String errorMessage = error.getMessage();
        
        if (errorMessage == null) {
            return ErrorCategory.UNKNOWN;
        }
        
        String lowerMessage = errorMessage.toLowerCase();
        
        // Erros de validação
        if (lowerMessage.contains("validação") || lowerMessage.contains("validation")) {
            return ErrorCategory.VALIDATION;
        }
        
        // Erros de duplicata
        if (lowerMessage.contains("duplicata") || lowerMessage.contains("duplicate")) {
            return ErrorCategory.DUPLICATE;
        }
        
        // Erros de detecção
        if (lowerMessage.contains("detecção") || lowerMessage.contains("detection")) {
            return ErrorCategory.DETECTION;
        }
        
        // Erros de integração
        if (lowerMessage.contains("integração") || lowerMessage.contains("integration")) {
            return ErrorCategory.INTEGRATION;
        }
        
        // Erros de banco de dados
        if (lowerMessage.contains("database") || lowerMessage.contains("sql") || 
            lowerMessage.contains("constraint") || lowerMessage.contains("foreign key")) {
            return ErrorCategory.DATABASE;
        }
        
        // Erros de rede/conexão
        if (lowerMessage.contains("connection") || lowerMessage.contains("timeout") || 
            lowerMessage.contains("network")) {
            return ErrorCategory.NETWORK;
        }
        
        // Erros de sistema
        if (lowerMessage.contains("system") || lowerMessage.contains("memory") || 
            lowerMessage.contains("out of memory")) {
            return ErrorCategory.SYSTEM;
        }
        
        return ErrorCategory.UNKNOWN;
    }

    /**
     * Trata um erro baseado na categoria
     */
    public ErrorHandlingResult handleError(Exception error, String context) {
        log.error("❌ Erro no contexto '{}': {}", context, error.getMessage(), error);
        
        ErrorCategory category = categorizeError(error);
        String userMessage = generateUserMessage(error, category);
        boolean isRecoverable = isRecoverableError(category);
        
        ErrorHandlingResult result = ErrorHandlingResult.builder()
            .category(category)
            .originalError(error)
            .userMessage(userMessage)
            .isRecoverable(isRecoverable)
            .context(context)
            .build();
        
        log.info("🔧 Erro categorizado como {} (recuperável: {})", category, isRecoverable);
        
        return result;
    }

    /**
     * Gera mensagem amigável para o usuário
     */
    private String generateUserMessage(Exception error, ErrorCategory category) {
        switch (category) {
            case VALIDATION:
                return "Erro de validação: " + error.getMessage();
            case DUPLICATE:
                return "Invoice duplicada detectada";
            case DETECTION:
                return "Erro na detecção de operações: " + error.getMessage();
            case INTEGRATION:
                return "Erro na integração de operações: " + error.getMessage();
            case DATABASE:
                return "Erro de banco de dados. Tente novamente.";
            case NETWORK:
                return "Erro de conexão. Verifique sua internet e tente novamente.";
            case SYSTEM:
                return "Erro do sistema. Tente novamente em alguns minutos.";
            case UNKNOWN:
            default:
                return "Erro inesperado: " + error.getMessage();
        }
    }

    /**
     * Verifica se o erro é recuperável
     */
    private boolean isRecoverableError(ErrorCategory category) {
        switch (category) {
            case VALIDATION:
            case DUPLICATE:
                return false; // Erros de validação não são recuperáveis
            case DETECTION:
            case INTEGRATION:
                return true; // Pode tentar novamente
            case DATABASE:
            case NETWORK:
                return true; // Pode tentar novamente
            case SYSTEM:
                return false; // Erro de sistema geralmente não é recuperável
            case UNKNOWN:
            default:
                return false; // Erro desconhecido não é recuperável
        }
    }

    /**
     * Processa uma lista de erros
     */
    public List<ErrorHandlingResult> handleErrors(List<Exception> errors, String context) {
        log.info("🔧 Processando {} erros no contexto '{}'", errors.size(), context);
        
        List<ErrorHandlingResult> results = new ArrayList<>();
        
        for (Exception error : errors) {
            ErrorHandlingResult result = handleError(error, context);
            results.add(result);
        }
        
        // Log de resumo
        long recoverableCount = results.stream().filter(ErrorHandlingResult::isRecoverable).count();
        long nonRecoverableCount = results.size() - recoverableCount;
        
        log.info("📊 Resumo de erros: {} recuperáveis, {} não recuperáveis", 
            recoverableCount, nonRecoverableCount);
        
        return results;
    }

    /**
     * Verifica se há erros críticos
     */
    public boolean hasCriticalErrors(List<ErrorHandlingResult> errorResults) {
        return errorResults.stream()
            .anyMatch(result -> !result.isRecoverable());
    }

    /**
     * Gera relatório de erros
     */
    public ErrorReport generateErrorReport(List<ErrorHandlingResult> errorResults) {
        ErrorReport report = ErrorReport.builder()
            .totalErrors(errorResults.size())
            .recoverableErrors((int) errorResults.stream().filter(ErrorHandlingResult::isRecoverable).count())
            .nonRecoverableErrors((int) errorResults.stream().filter(r -> !r.isRecoverable()).count())
            .build();
        
        // Agrupar por categoria
        for (ErrorHandlingResult result : errorResults) {
            report.addErrorByCategory(result.getCategory(), result);
        }
        
        return report;
    }
} 