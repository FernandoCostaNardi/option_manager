package com.olisystem.optionsmanager.controller.invoice.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olisystem.optionsmanager.service.invoice.processing.ProcessingProgress;
import com.olisystem.optionsmanager.service.invoice.processing.ProcessingResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sessão de processamento com gerenciamento de SSE
 */
@Data
@Slf4j
public class ProcessingSession {
    private final String sessionId;
    private final int totalInvoices;
    private final LocalDateTime startTime;
    private final List<SseEmitter> emitters;
    private final List<String> messages;
    private final List<String> errors;
    private final ObjectMapper objectMapper;
    
    private ProcessingProgress currentProgress;
    private ProcessingResult result;
    private boolean cancelled = false;
    private String errorMessage;
    
    public ProcessingSession(String sessionId, int totalInvoices) {
        this.sessionId = sessionId;
        this.totalInvoices = totalInvoices;
        this.startTime = LocalDateTime.now();
        this.emitters = new CopyOnWriteArrayList<>();
        this.messages = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.objectMapper = new ObjectMapper();
        
        // Progresso inicial
        this.currentProgress = ProcessingProgress.builder()
            .status("STARTED")
            .currentInvoice(0)
            .totalInvoices(totalInvoices)
            .currentStep("Iniciando processamento...")
            .operationsCreated(0)
            .operationsSkipped(0)
            .operationsUpdated(0)
            .build();
    }
    
    public void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
        log.debug("📡 Emitter adicionado à sessão {}. Total: {}", sessionId, emitters.size());
        
        // Limpar emitters mortos
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(throwable -> emitters.remove(emitter));
    }
    
    public void updateProgress(ProcessingProgress progress) {
        this.currentProgress = progress;
        
        // Adicionar mensagem ao histórico
        if (progress.getCurrentStep() != null) {
            messages.add(String.format("[%s] %s", 
                LocalDateTime.now().toString().substring(11, 19), 
                progress.getCurrentStep()));
        }
        
        log.debug("📊 Progresso atualizado para sessão {}: {}/{} - {}", 
            sessionId, progress.getCurrentInvoice(), progress.getTotalInvoices(), progress.getCurrentStep());
    }
    
    public void notifyEmitters(ProcessingProgress progress) {
        if (emitters.isEmpty()) {
            log.debug("📡 Nenhum emitter conectado para sessão {}", sessionId);
            return;
        }
        
        log.info("📤 Enviando progresso para {} emitters da sessão {} - operações: {}", 
                 emitters.size(), sessionId, progress.getOperationsCreated());
        
        List<SseEmitter> deadEmitters = new ArrayList<>();
        int successfulSends = 0;
        
        for (SseEmitter emitter : emitters) {
            try {
                sendProgressToEmitter(emitter, progress);
                successfulSends++;
            } catch (Exception e) {
                // ✅ Tratamento melhorado para diferentes tipos de erro
                if (isClientDisconnectError(e)) {
                    log.debug("🔌 Cliente desconectou, removendo emitter da sessão {}", sessionId);
                } else {
                    log.warn("❌ Erro ao enviar para emitter da sessão {}: {}", sessionId, e.getMessage());
                }
                deadEmitters.add(emitter);
            }
        }
        
        // Remove emitters mortos
        emitters.removeAll(deadEmitters);
        
        log.info("📊 Progresso enviado: {} sucessos, {} falhas. Emitters ativos: {}", 
                 successfulSends, deadEmitters.size(), emitters.size());
    }
    
    public void sendProgressToEmitter(SseEmitter emitter, ProcessingProgress progress) {
        try {
            String jsonData = objectMapper.writeValueAsString(progress);
            log.info("📤 ENVIANDO VIA SSE - Sessão {}: operações criadas = {}, status = {}", 
                     sessionId, progress.getOperationsCreated(), progress.getStatus());
            log.info("📤 JSON COMPLETO: {}", jsonData);
            
            // ✅ CORREÇÃO: Tentar múltiplas estratégias de envio
            try {
                // Estratégia 1: Envio normal
                emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(jsonData)
                    .reconnectTime(1000L));
                log.info("✅ SSE enviado com sucesso via estratégia normal");
            } catch (Exception e1) {
                log.warn("⚠️ Estratégia normal falhou, tentando estratégia alternativa: {}", e1.getMessage());
                try {
                    // Estratégia 2: Envio simples
                    emitter.send(jsonData);
                    log.info("✅ SSE enviado com sucesso via estratégia alternativa");
                } catch (Exception e2) {
                    log.error("❌ Ambas estratégias falharam: normal={}, alternativa={}", e1.getMessage(), e2.getMessage());
                    throw e2; // Propagar último erro
                }
            }
            
        } catch (Exception e) {
            // ✅ Filtrar erros comuns de conexão fechada pelo cliente
            if (isClientDisconnectError(e)) {
                log.debug("🔌 Cliente desconectou do SSE para sessão {}", sessionId);
            } else {
                log.error("❌ Erro ao enviar dados SSE para sessão {}: {}", sessionId, e.getMessage(), e);
            }
            throw new RuntimeException("Erro ao enviar SSE", e);
        }
    }
    
    public void setResult(ProcessingResult result) {
        this.result = result;
        
        log.info("🎯 SETRESULT CHAMADO: operações criadas = {}, skipped = {}", 
                 result.getTotalOperationsCreated(), result.getTotalOperationsSkipped());
        
        // Criar progresso final
        ProcessingProgress finalProgress = ProcessingProgress.builder()
            .status(result.isSuccess() ? "COMPLETED" : result.isPartialSuccess() ? "PARTIAL" : "ERROR")
            .currentInvoice(result.getTotalInvoices())
            .totalInvoices(result.getTotalInvoices())
            .currentStep(String.format("Processamento concluído! %d operações criadas em %d ms.", 
                result.getTotalOperationsCreated(), result.getProcessingTimeMs()))
            .operationsCreated(result.getTotalOperationsCreated())
            .operationsSkipped(result.getTotalOperationsSkipped())
            .operationsUpdated(0)
            .build();
        
        log.info("🎯 PROGRESSO FINAL CRIADO: operações criadas = {}, status = {}", 
                 finalProgress.getOperationsCreated(), finalProgress.getStatus());
        
        updateProgress(finalProgress);
        notifyEmitters(finalProgress);
        
        // ✅ CORREÇÃO: Aguardar envio e fechar adequadamente todos os emitters
        try {
            Thread.sleep(500); // Dar tempo para envio da mensagem final
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Fechar todos os emitters com mensagem de conclusão
        for (SseEmitter emitter : emitters) {
            try {
                // Enviar evento final de conclusão
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data("Processamento finalizado"));
                emitter.complete();
                log.info("✅ Emitter fechado adequadamente para sessão {}", sessionId);
            } catch (Exception e) {
                log.warn("❌ Erro ao fechar emitter: {}", e.getMessage());
            }
        }
        emitters.clear();
        
        log.info("✅ Sessão {} finalizada com resultado: {} operações criadas", 
            sessionId, result.getTotalOperationsCreated());
    }
    
    public void setError(String errorMessage) {
        this.errorMessage = errorMessage;
        errors.add(errorMessage);
        
        ProcessingProgress errorProgress = ProcessingProgress.builder()
            .status("ERROR")
            .currentInvoice(currentProgress != null ? currentProgress.getCurrentInvoice() : 0)
            .totalInvoices(totalInvoices)
            .currentStep("Erro no processamento: " + errorMessage)
            .operationsCreated(currentProgress != null ? currentProgress.getOperationsCreated() : 0)
            .operationsSkipped(0)
            .operationsUpdated(0)
            .build();
        
        updateProgress(errorProgress);
        notifyEmitters(errorProgress);
        
        // Fechar emitters com erro
        for (SseEmitter emitter : emitters) {
            try {
                emitter.completeWithError(new RuntimeException(errorMessage));
            } catch (Exception e) {
                log.warn("❌ Erro ao fechar emitter com erro: {}", e.getMessage());
            }
        }
        emitters.clear();
    }
    
    public void cancel() {
        this.cancelled = true;
        
        ProcessingProgress cancelProgress = ProcessingProgress.builder()
            .status("CANCELLED")
            .currentInvoice(currentProgress != null ? currentProgress.getCurrentInvoice() : 0)
            .totalInvoices(totalInvoices)
            .currentStep("Processamento cancelado pelo usuário")
            .operationsCreated(currentProgress != null ? currentProgress.getOperationsCreated() : 0)
            .operationsSkipped(0)
            .operationsUpdated(0)
            .build();
        
        updateProgress(cancelProgress);
        notifyEmitters(cancelProgress);
        
        // Fechar todos os emitters
        for (SseEmitter emitter : emitters) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("❌ Erro ao fechar emitter cancelado: {}", e.getMessage());
            }
        }
        emitters.clear();
        
        log.info("❌ Sessão {} cancelada", sessionId);
    }
    
    public long getElapsedTimeSeconds() {
        return java.time.Duration.between(startTime, LocalDateTime.now()).toSeconds();
    }
    
    /**
     * Verifica se o erro é devido a desconexão do cliente
     */
    private boolean isClientDisconnectError(Exception e) {
        String message = e.getMessage();
        if (message == null) return false;
        
        return message.contains("Connection reset") ||
               message.contains("Connection aborted") ||
               message.contains("Broken pipe") ||
               message.contains("conexão estabelecida foi anulada") ||
               message.contains("ClientAbortException");
    }
}
