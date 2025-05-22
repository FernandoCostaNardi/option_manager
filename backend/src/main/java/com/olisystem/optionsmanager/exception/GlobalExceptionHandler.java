package com.olisystem.optionsmanager.exception;

import com.olisystem.optionsmanager.dto.error.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(
      IllegalArgumentException ex, WebRequest request) {
    log.error("Erro de validação: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponseDto("Erro de validação", ex.getMessage()));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponseDto> handleIllegalStateException(
      IllegalStateException ex, WebRequest request) {
    log.error("Erro de estado: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ErrorResponseDto("Erro de autenticação", ex.getMessage()));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(
      ResourceNotFoundException ex, WebRequest request) {
    log.error("Recurso não encontrado: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponseDto("Recurso não encontrado", ex.getMessage()));
  }

  @ExceptionHandler(InvalidIdFormatException.class)
  public ResponseEntity<ErrorResponseDto> handleInvalidIdFormatException(
      InvalidIdFormatException ex, WebRequest request) {
    log.error("Formato de ID inválido: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponseDto("Formato de ID inválido", ex.getMessage()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponseDto> handleRuntimeException(
      RuntimeException ex, WebRequest request) {
    log.error("Erro inesperado: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponseDto("Erro interno do servidor", ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDto> handleException(Exception ex, WebRequest request) {
    log.error("Erro não tratado: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponseDto("Erro interno do servidor", "Ocorreu um erro inesperado"));
  }
}
