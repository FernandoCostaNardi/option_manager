package com.olisystem.optionsmanager.exception.invoice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Handler global para exceções relacionadas a importação de notas de corretagem
 */
@RestControllerAdvice
@Slf4j
public class InvoiceExceptionHandler {

    /**
     * Handler para erro de parsing de nota
     */
    @ExceptionHandler(InvoiceParsingException.class)
    public ResponseEntity<Map<String, Object>> handleInvoiceParsingException(InvoiceParsingException ex) {
        log.error("Erro de parsing na nota '{}' da corretora '{}': {}", 
                  ex.getFileName(), ex.getBrokerageName(), ex.getMessage(), ex);

        Map<String, Object> errorResponse = Map.of(
            "error", "INVOICE_PARSING_ERROR",
            "message", "Erro ao processar nota de corretagem: " + ex.getMessage(),
            "fileName", ex.getFileName() != null ? ex.getFileName() : "N/A",
            "brokerage", ex.getBrokerageName() != null ? ex.getBrokerageName() : "N/A",
            "timestamp", LocalDateTime.now(),
            "status", HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handler para arquivo já importado
     */
    @ExceptionHandler(DuplicateInvoiceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateInvoiceException(DuplicateInvoiceException ex) {
        log.warn("Tentativa de importar arquivo duplicado '{}' com hash '{}'", 
                 ex.getFileName(), ex.getFileHash());

        Map<String, Object> errorResponse = Map.of(
            "error", "DUPLICATE_INVOICE",
            "message", "Arquivo já foi importado anteriormente: " + ex.getMessage(),
            "fileName", ex.getFileName() != null ? ex.getFileName() : "N/A",
            "fileHash", ex.getFileHash() != null ? ex.getFileHash() : "N/A",
            "timestamp", LocalDateTime.now(),
            "status", HttpStatus.CONFLICT.value()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handler para formato de arquivo inválido
     */
    @ExceptionHandler(InvalidFileFormatException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFileFormatException(InvalidFileFormatException ex) {
        log.error("Formato de arquivo inválido '{}': esperado '{}', encontrado '{}'", 
                  ex.getFileName(), ex.getExpectedFormat(), ex.getActualFormat());

        Map<String, Object> errorResponse = Map.of(
            "error", "INVALID_FILE_FORMAT",
            "message", "Formato de arquivo inválido: " + ex.getMessage(),
            "fileName", ex.getFileName() != null ? ex.getFileName() : "N/A",
            "expectedFormat", ex.getExpectedFormat() != null ? ex.getExpectedFormat() : "PDF",
            "actualFormat", ex.getActualFormat() != null ? ex.getActualFormat() : "DESCONHECIDO",
            "timestamp", LocalDateTime.now(),
            "status", HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handler para corretora não suportada
     */
    @ExceptionHandler(UnsupportedBrokerageException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedBrokerageException(UnsupportedBrokerageException ex) {
        log.error("Corretora não suportada '{}' (ID: {}): {}", 
                  ex.getBrokerageName(), ex.getBrokerageId(), ex.getMessage());

        Map<String, Object> errorResponse = Map.of(
            "error", "UNSUPPORTED_BROKERAGE",
            "message", "Corretora não suportada para importação: " + ex.getMessage(),
            "brokerageName", ex.getBrokerageName() != null ? ex.getBrokerageName() : "N/A",
            "brokerageId", ex.getBrokerageId() != null ? ex.getBrokerageId() : "N/A",
            "timestamp", LocalDateTime.now(),
            "status", HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handler genérico para outras exceções relacionadas a invoices
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleGenericRuntimeException(RuntimeException ex) {
        // Só trata se for de package invoice para não interferir com outros handlers
        if (ex.getStackTrace().length > 0 && 
            ex.getStackTrace()[0].getClassName().contains("invoice")) {
            
            log.error("Erro inesperado no sistema de invoices: {}", ex.getMessage(), ex);

            Map<String, Object> errorResponse = Map.of(
                "error", "INVOICE_SYSTEM_ERROR",
                "message", "Erro inesperado no sistema de importação: " + ex.getMessage(),
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
        
        // Se não for do sistema de invoices, relança para outros handlers
        throw ex;
    }
}
