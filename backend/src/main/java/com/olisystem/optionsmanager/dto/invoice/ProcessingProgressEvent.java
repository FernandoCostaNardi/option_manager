package com.olisystem.optionsmanager.dto.invoice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Evento de progresso do processamento de invoices
 * ✅ INTEGRAÇÃO: Sistema de progresso em tempo real
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-21
 */
@Data
@Builder
public class ProcessingProgressEvent {
    
    /**
     * Tipo do evento
     */
    private ProgressEventType type;
    
    /**
     * Mensagem descritiva
     */
    private String message;
    
    /**
     * Operação atual (1-based)
     */
    private Integer current;
    
    /**
     * Total de operações
     */
    private Integer total;
    
    /**
     * Status do processamento
     */
    private ProgressStatus status;
    
    /**
     * ID da invoice sendo processada
     */
    private String invoiceId;
    
    /**
     * Número da invoice
     */
    private String invoiceNumber;
    
    /**
     * Timestamp do evento
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * Porcentagem de progresso (0-100)
     */
    private Integer percentage;
    
    /**
     * Detalhes adicionais (opcional)
     */
    private String details;
    
    /**
     * Tipos de eventos de progresso
     */
    public enum ProgressEventType {
        STARTED,           // Processamento iniciado
        PROCESSING,        // Processando operação
        COMPLETED,         // Operação concluída
        ERROR,            // Erro no processamento
        FINISHED          // Processamento finalizado
    }
    
    /**
     * Status do progresso
     */
    public enum ProgressStatus {
        PENDING,          // Aguardando
        PROCESSING,       // Processando
        COMPLETED,        // Concluído
        ERROR,           // Erro
        FINISHED         // Finalizado
    }
    
    /**
     * Calcula a porcentagem de progresso
     */
    public Integer calculatePercentage() {
        if (total == null || total == 0) {
            return 0;
        }
        if (current == null) {
            return 0;
        }
        return Math.min(100, (current * 100) / total);
    }
    
    /**
     * Cria evento de início
     */
    public static ProcessingProgressEvent started(String invoiceId, String invoiceNumber, int total) {
        return ProcessingProgressEvent.builder()
            .type(ProgressEventType.STARTED)
                                    .message("Iniciando processamento...")
            .current(0)
            .total(total)
            .status(ProgressStatus.PENDING)
            .invoiceId(invoiceId)
            .invoiceNumber(invoiceNumber)
            .timestamp(LocalDateTime.now())
            .percentage(0)
            .build();
    }
    
                    /**
                 * Cria evento de processamento
                 */
                public static ProcessingProgressEvent processing(String invoiceId, String invoiceNumber, int current, int total) {
                    int percentage = total > 0 ? (current * 100) / total : 0;
                    String message = "Processando operacao " + current + " de " + total + "...";
                    return ProcessingProgressEvent.builder()
                        .type(ProgressEventType.PROCESSING)
                        .message(message)
                        .current(current)
                        .total(total)
                        .status(ProgressStatus.PROCESSING)
                        .invoiceId(invoiceId)
                        .invoiceNumber(invoiceNumber)
                        .timestamp(LocalDateTime.now())
                        .percentage(percentage)
                        .build();
                }
    
                    /**
                 * Cria evento de conclusão de operação
                 */
                public static ProcessingProgressEvent completed(String invoiceId, String invoiceNumber, int current, int total) {
                    int percentage = total > 0 ? (current * 100) / total : 0;
                    String message = "Operacao " + current + " concluida";
                    return ProcessingProgressEvent.builder()
                        .type(ProgressEventType.COMPLETED)
                        .message(message)
                        .current(current)
                        .total(total)
                        .status(ProgressStatus.COMPLETED)
                        .invoiceId(invoiceId)
                        .invoiceNumber(invoiceNumber)
                        .timestamp(LocalDateTime.now())
                        .percentage(percentage)
                        .build();
                }
    
    /**
     * Cria evento de erro
     */
    public static ProcessingProgressEvent error(String invoiceId, String invoiceNumber, String errorMessage) {
        return ProcessingProgressEvent.builder()
            .type(ProgressEventType.ERROR)
                                    .message("Erro no processamento: " + errorMessage)
            .status(ProgressStatus.ERROR)
            .invoiceId(invoiceId)
            .invoiceNumber(invoiceNumber)
            .timestamp(LocalDateTime.now())
            .details(errorMessage)
            .build();
    }
    
                    /**
                 * Cria evento de finalização
                 */
                public static ProcessingProgressEvent finished(String invoiceId, String invoiceNumber, int totalOperations) {
                    String message = "Processamento concluido! " + totalOperations + " operacoes criadas";
                    return ProcessingProgressEvent.builder()
                        .type(ProgressEventType.FINISHED)
                        .message(message)
                        .current(totalOperations)
                        .total(totalOperations)
                        .status(ProgressStatus.FINISHED)
                        .invoiceId(invoiceId)
                        .invoiceNumber(invoiceNumber)
                        .timestamp(LocalDateTime.now())
                        .percentage(100)
                        .build();
                }
} 