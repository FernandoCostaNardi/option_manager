package com.olisystem.optionsmanager.controller.invoice.processing;

import com.olisystem.optionsmanager.dto.invoice.ProcessingProgressEvent;
import com.olisystem.optionsmanager.service.invoice.processing.ProcessingProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Controller para progresso do processamento via Server-Sent Events
 * ✅ INTEGRAÇÃO: Sistema de progresso em tempo real
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-21
 */
@RestController
@RequestMapping("/api/processing/progress")
@RequiredArgsConstructor
@Slf4j
public class ProcessingProgressController {
    
    private final ProcessingProgressService progressService;
    
    /**
     * Endpoint para conectar ao SSE de progresso
     * GET /api/processing/progress/{sessionId}
     */
                    @GetMapping(value = "/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public SseEmitter subscribeToProgress(@PathVariable String sessionId) {
        log.info("📡 Cliente conectando ao SSE de progresso: {}", sessionId);
        
                            // Criar emitter com timeout de 5 minutos
                    SseEmitter emitter = new SseEmitter(TimeUnit.SECONDS.toMillis(300));
        
        // Configurar callbacks do emitter
        emitter.onCompletion(() -> {
            log.info("📡 SSE completado para sessão: {}", sessionId);
            progressService.unregisterProgressCallbacks(sessionId);
        });
        
        emitter.onTimeout(() -> {
            log.warn("⏰ SSE timeout para sessão: {}", sessionId);
            progressService.unregisterProgressCallbacks(sessionId);
        });
        
        emitter.onError((ex) -> {
            log.error("❌ Erro no SSE para sessão: {} - {}", sessionId, ex.getMessage(), ex);
            progressService.unregisterProgressCallbacks(sessionId);
        });
        
        // Registrar callback de progresso
        progressService.registerProgressCallback(sessionId, event -> {
            try {
                                                                        String eventJson = progressService.serializeEvent(event);
                            log.info("🔧 JSON antes do envio SSE: {}", eventJson);
                            
                            // Verificar encoding
                            byte[] bytes = eventJson.getBytes(StandardCharsets.UTF_8);
                            String utf8String = new String(bytes, StandardCharsets.UTF_8);
                            log.info("🔧 Verificação UTF-8: original={}, utf8={}", eventJson, utf8String);
                            
                            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                                .name("progress")
                                .data(eventJson, MediaType.APPLICATION_JSON_UTF8)
                                .id(String.valueOf(System.currentTimeMillis()))
                                .reconnectTime(3000);
                            
                            emitter.send(eventBuilder);
                            log.info("📡 Evento enviado via SSE: {} - {}", sessionId, event.getMessage());
                
            } catch (IOException e) {
                log.error("❌ Erro ao enviar evento SSE: {}", e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });
        
        // Enviar evento de conexão
        try {
            ProcessingProgressEvent connectionEvent = ProcessingProgressEvent.builder()
                .type(ProcessingProgressEvent.ProgressEventType.STARTED)
                .message("🔗 Conectado ao progresso em tempo real")
                .status(ProcessingProgressEvent.ProgressStatus.PENDING)
                .timestamp(java.time.LocalDateTime.now())
                .build();
            
                                    String connectionJson = progressService.serializeEvent(connectionEvent);
                        SseEmitter.SseEventBuilder connectionBuilder = SseEmitter.event()
                            .name("connected")
                            .data(connectionJson, MediaType.APPLICATION_JSON_UTF8)
                            .id(String.valueOf(System.currentTimeMillis()))
                            .reconnectTime(3000);
            
            emitter.send(connectionBuilder);
            log.info("✅ Cliente conectado ao SSE: {}", sessionId);
            
        } catch (IOException e) {
            log.error("❌ Erro ao enviar evento de conexão: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * Endpoint para testar emissão de eventos
     * POST /api/processing/progress/{sessionId}/test
     */
    @PostMapping("/{sessionId}/test")
    public ResponseEntity<String> testProgressEvent(@PathVariable String sessionId) {
        log.info("🧪 Testando emissão de evento para sessão: {}", sessionId);
        
        if (!progressService.hasCallbacks(sessionId)) {
            return ResponseEntity.badRequest().body("Nenhum cliente conectado para sessão: " + sessionId);
        }
        
        // Emitir evento de teste
        progressService.emitStarted(sessionId, "test-invoice", "TEST-001", 3);
        
        try {
            Thread.sleep(1000);
            progressService.emitProcessing(sessionId, "test-invoice", "TEST-001", 1, 3);
            
            Thread.sleep(1000);
            progressService.emitCompleted(sessionId, "test-invoice", "TEST-001", 1, 3);
            
            Thread.sleep(1000);
            progressService.emitProcessing(sessionId, "test-invoice", "TEST-001", 2, 3);
            
            Thread.sleep(1000);
            progressService.emitCompleted(sessionId, "test-invoice", "TEST-001", 2, 3);
            
            Thread.sleep(1000);
            progressService.emitProcessing(sessionId, "test-invoice", "TEST-001", 3, 3);
            
            Thread.sleep(1000);
            progressService.emitCompleted(sessionId, "test-invoice", "TEST-001", 3, 3);
            
            Thread.sleep(1000);
            progressService.emitFinished(sessionId, "test-invoice", "TEST-001", 3);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body("Teste interrompido");
        }
        
        return ResponseEntity.ok("Eventos de teste enviados com sucesso");
    }
    
    /**
     * Endpoint para verificar status da conexão
     * GET /api/processing/progress/{sessionId}/status
     */
    @GetMapping("/{sessionId}/status")
    public ResponseEntity<String> getConnectionStatus(@PathVariable String sessionId) {
        boolean hasCallbacks = progressService.hasCallbacks(sessionId);
        String status = hasCallbacks ? "CONNECTED" : "DISCONNECTED";
        
        return ResponseEntity.ok(String.format("{\"sessionId\":\"%s\",\"status\":\"%s\"}", sessionId, status));
    }
    
    /**
     * Endpoint para desconectar sessão
     * DELETE /api/processing/progress/{sessionId}
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<String> disconnectSession(@PathVariable String sessionId) {
        log.info("🔌 Desconectando sessão: {}", sessionId);
        progressService.unregisterProgressCallbacks(sessionId);
        return ResponseEntity.ok("Sessão desconectada: " + sessionId);
    }
} 