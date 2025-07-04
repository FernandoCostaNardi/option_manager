package com.olisystem.optionsmanager.controller.invoice.processing;

import com.olisystem.optionsmanager.dto.invoice.processing.InvoiceProcessingResponse;
import com.olisystem.optionsmanager.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exception handler para controllers de processamento de invoices
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@RestControllerAdvice(basePackages = "com.olisystem.optionsmanager.controller.invoice.processing")
@Slf4j
public class ProcessingExceptionHandler {

    /**
     * Trata erros de validação de entrada
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("🚨 Erro de validação: {}", ex.getMessage());
        
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("Dados de entrada inválidos")
                .details(errors)
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Trata erros de negócio
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("🚨 Erro de negócio: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("BUSINESS_ERROR")
                .message(ex.getMessage())
                .details(List.of())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Trata erros de segurança (acesso negado)
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex) {
        log.warn("🚨 Erro de segurança: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("SECURITY_ERROR")
                .message("Acesso negado: " + ex.getMessage())
                .details(List.of())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Trata argumentos ilegais
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("🚨 Argumento inválido: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("INVALID_ARGUMENT")
                .message("Parâmetro inválido: " + ex.getMessage())
                .details(List.of())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Trata exceções de processamento específicas
     */
    @ExceptionHandler(ProcessingException.class)
    public ResponseEntity<InvoiceProcessingResponse> handleProcessingException(ProcessingException ex) {
        log.error("🚨 Erro de processamento: {}", ex.getMessage(), ex);
        
        InvoiceProcessingResponse.ProcessingError error = 
                InvoiceProcessingResponse.ProcessingError.builder()
                        .errorCode(ex.getErrorCode())
                        .category(ex.getCategory())
                        .severity(ex.getSeverity())
                        .message(ex.getMessage())
                        .phase(ex.getPhase())
                        .timestamp(LocalDateTime.now())
                        .build();
        
        InvoiceProcessingResponse response = InvoiceProcessingResponse.builder()
                .successful(false)
                .status(com.olisystem.optionsmanager.model.enums.InvoiceProcessingStatus.ERROR)
                .summary("Falha no processamento: " + ex.getMessage())
                .errors(List.of(error))
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .processingDuration(java.time.Duration.ZERO)
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Trata erros gerais do sistema
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("🚨 Erro interno do sistema: {}", ex.getMessage(), ex);
        
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("INTERNAL_ERROR")
                .message("Erro interno do sistema. Tente novamente mais tarde.")
                .details(List.of())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Resposta padrão de erro
     */
    @lombok.Builder
    @lombok.Data
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private List<String> details;
        private LocalDateTime timestamp;
    }

    /**
     * Exceção específica de processamento
     */
    public static class ProcessingException extends RuntimeException {
        private final String errorCode;
        private final String category;
        private final String severity;
        private final String phase;
        
        public ProcessingException(String message, String errorCode, String category, String severity, String phase) {
            super(message);
            this.errorCode = errorCode;
            this.category = category;
            this.severity = severity;
            this.phase = phase;
        }
        
        public ProcessingException(String message, Throwable cause, String errorCode, String category, String severity, String phase) {
            super(message, cause);
            this.errorCode = errorCode;
            this.category = category;
            this.severity = severity;
            this.phase = phase;
        }
        
        public String getErrorCode() { return errorCode; }
        public String getCategory() { return category; }
        public String getSeverity() { return severity; }
        public String getPhase() { return phase; }
    }

    /**
     * Cria exceção de processamento com contexto
     */
    public static ProcessingException createProcessingException(
            String message, 
            String phase,
            Throwable cause) {
        
        return new ProcessingException(
                message, 
                cause, 
                "PROCESSING_ERROR", 
                "SYSTEM", 
                "ERROR", 
                phase
        );
    }

    /**
     * Cria exceção de validação de processamento
     */
    public static ProcessingException createValidationException(
            String message, 
            String phase) {
        
        return new ProcessingException(
                message, 
                "VALIDATION_ERROR", 
                "VALIDATION", 
                "WARNING", 
                phase
        );
    }

    /**
     * Cria exceção de negócio de processamento
     */
    public static ProcessingException createBusinessException(
            String message, 
            String phase) {
        
        return new ProcessingException(
                message, 
                "BUSINESS_ERROR", 
                "BUSINESS_RULE", 
                "ERROR", 
                phase
        );
    }
}