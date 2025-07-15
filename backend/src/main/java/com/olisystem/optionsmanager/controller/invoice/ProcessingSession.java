package com.olisystem.optionsmanager.controller.invoice;

import com.olisystem.optionsmanager.service.invoice.processing.orchestrator.OrchestrationResult;
import lombok.Data;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sessão de processamento para acompanhar progresso em tempo real
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Data
public class ProcessingSession {
    
    private final UUID sessionId;
    private final List<UUID> invoiceIds;
    private final long startTime;
    
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicInteger progress = new AtomicInteger(0);
    private final AtomicLong elapsedTime = new AtomicLong(0);
    
    private String status = "PROCESSING";
    private String message = "Iniciando processamento...";
    private OrchestrationResult result;
    
    public ProcessingSession(UUID sessionId, List<UUID> invoiceIds) {
        this.sessionId = sessionId;
        this.invoiceIds = invoiceIds;
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * Atualiza o progresso da sessão
     */
    public void updateProgress(int percentage, String message) {
        this.progress.set(percentage);
        this.message = message;
        this.elapsedTime.set(System.currentTimeMillis() - startTime);
    }
    
    /**
     * Finaliza a sessão com resultado
     */
    public void complete(OrchestrationResult result) {
        this.result = result;
        this.status = result.isSuccessful() ? "SUCCESS" : "ERROR";
        this.progress.set(100);
        this.message = result.isSuccessful() ? "Processamento concluído com sucesso" : 
            "Processamento falhou: " + result.getErrorMessage();
        this.elapsedTime.set(System.currentTimeMillis() - startTime);
    }
    
    /**
     * Cancela a sessão
     */
    public void cancel() {
        this.cancelled.set(true);
        this.status = "CANCELLED";
        this.message = "Processamento cancelado pelo usuário";
    }
    
    /**
     * Verifica se a sessão foi cancelada
     */
    public boolean isCancelled() {
        return cancelled.get();
    }
    
    /**
     * Retorna o tempo decorrido em milissegundos
     */
    public long getElapsedTime() {
        return elapsedTime.get();
    }
    
    /**
     * Retorna o tempo estimado restante
     */
    public long getEstimatedRemainingTime() {
        if (progress.get() == 0) {
            return 0;
        }
        
        long elapsed = getElapsedTime();
        int currentProgress = progress.get();
        
        if (currentProgress == 0) {
            return 0;
        }
        
        long totalEstimated = (elapsed * 100) / currentProgress;
        return totalEstimated - elapsed;
    }
    
    /**
     * Retorna o total de invoices
     */
    public int getTotalInvoices() {
        return invoiceIds.size();
    }
} 