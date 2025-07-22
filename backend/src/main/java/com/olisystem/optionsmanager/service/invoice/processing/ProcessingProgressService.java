package com.olisystem.optionsmanager.service.invoice.processing;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olisystem.optionsmanager.dto.invoice.ProcessingProgressEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Serviço para gerenciar progresso do processamento de invoices
 * ✅ INTEGRAÇÃO: Sistema de progresso em tempo real
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-21
 */
@Service
@Slf4j
public class ProcessingProgressService {
    
    private final ObjectMapper objectMapper;
    
    public ProcessingProgressService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // Configurar UTF-8 corretamente
        this.objectMapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
        
        // Log da configuração
        log.info("🔧 ProcessingProgressService configurado com UTF-8");
    }
    
    /**
     * Callbacks de progresso por session ID
     */
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Consumer<ProcessingProgressEvent>>> progressCallbacks = 
        new ConcurrentHashMap<>();
    
    /**
     * Registra um callback de progresso para uma sessão
     */
    public void registerProgressCallback(String sessionId, Consumer<ProcessingProgressEvent> callback) {
        progressCallbacks.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(callback);
        log.debug("📡 Callback de progresso registrado para sessão: {}", sessionId);
    }
    
    /**
     * Remove callbacks de progresso de uma sessão
     */
    public void unregisterProgressCallbacks(String sessionId) {
        progressCallbacks.remove(sessionId);
        log.debug("📡 Callbacks de progresso removidos para sessão: {}", sessionId);
    }
    
    /**
     * Emite evento de progresso para uma sessão
     */
    public void emitProgressEvent(String sessionId, ProcessingProgressEvent event) {
        CopyOnWriteArrayList<Consumer<ProcessingProgressEvent>> callbacks = progressCallbacks.get(sessionId);
        if (callbacks != null) {
            callbacks.forEach(callback -> {
                try {
                    callback.accept(event);
                    log.debug("📡 Evento de progresso emitido: {} - {}", sessionId, event.getMessage());
                } catch (Exception e) {
                    log.error("❌ Erro ao emitir evento de progresso: {}", e.getMessage(), e);
                }
            });
        } else {
            log.warn("⚠️ Nenhum callback registrado para sessão: {}", sessionId);
        }
    }
    
    /**
     * Emite evento de início
     */
    public void emitStarted(String sessionId, String invoiceId, String invoiceNumber, int total) {
        ProcessingProgressEvent event = ProcessingProgressEvent.started(invoiceId, invoiceNumber, total);
        emitProgressEvent(sessionId, event);
    }
    
    /**
     * Emite evento de processamento
     */
    public void emitProcessing(String sessionId, String invoiceId, String invoiceNumber, int current, int total) {
        ProcessingProgressEvent event = ProcessingProgressEvent.processing(invoiceId, invoiceNumber, current, total);
        emitProgressEvent(sessionId, event);
    }
    
    /**
     * Emite evento de conclusão
     */
    public void emitCompleted(String sessionId, String invoiceId, String invoiceNumber, int current, int total) {
        ProcessingProgressEvent event = ProcessingProgressEvent.completed(invoiceId, invoiceNumber, current, total);
        emitProgressEvent(sessionId, event);
    }
    
    /**
     * Emite evento de erro
     */
    public void emitError(String sessionId, String invoiceId, String invoiceNumber, String errorMessage) {
        ProcessingProgressEvent event = ProcessingProgressEvent.error(invoiceId, invoiceNumber, errorMessage);
        emitProgressEvent(sessionId, event);
    }
    
    /**
     * Emite evento de finalização
     */
    public void emitFinished(String sessionId, String invoiceId, String invoiceNumber, int totalOperations) {
        ProcessingProgressEvent event = ProcessingProgressEvent.finished(invoiceId, invoiceNumber, totalOperations);
        emitProgressEvent(sessionId, event);
    }
    
    /**
     * Serializa evento para JSON
     */
    public String serializeEvent(ProcessingProgressEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.debug("🔧 Evento serializado: {}", json);
            
            // Verificar se há caracteres problemáticos
            if (json.contains("\\u")) {
                log.warn("⚠️ Caracteres Unicode detectados na serialização: {}", json);
            }
            
            return json;
        } catch (JsonProcessingException e) {
            log.error("❌ Erro ao serializar evento de progresso: {}", e.getMessage(), e);
            return "{\"error\":\"Erro ao serializar evento\"}";
        }
    }
    
    /**
     * Deserializa evento de JSON
     */
    public ProcessingProgressEvent deserializeEvent(String json) {
        try {
            return objectMapper.readValue(json, ProcessingProgressEvent.class);
        } catch (IOException e) {
            log.error("❌ Erro ao deserializar evento de progresso: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Verifica se há callbacks registrados para uma sessão
     */
    public boolean hasCallbacks(String sessionId) {
        return progressCallbacks.containsKey(sessionId) && !progressCallbacks.get(sessionId).isEmpty();
    }
    
    /**
     * Limpa todas as sessões (útil para testes)
     */
    public void clearAllSessions() {
        progressCallbacks.clear();
        log.info("🧹 Todas as sessões de progresso foram limpas");
    }
} 