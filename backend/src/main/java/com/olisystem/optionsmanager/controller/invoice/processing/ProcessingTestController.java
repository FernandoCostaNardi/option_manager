package com.olisystem.optionsmanager.controller.invoice.processing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Controller SIMPLIFICADO para teste do frontend
 * Substitui temporariamente o sistema complexo da Fase 2
 */
@RestController
@RequestMapping("/api/processing")
@Slf4j
public class ProcessingTestController {

    @PostMapping("/estimate")
    public ResponseEntity<Map<String, Object>> estimateProcessing(@RequestBody Map<String, Object> request) {
        log.info("🧮 Estimando processamento...");
        
        @SuppressWarnings("unchecked")
        List<String> invoiceIds = (List<String>) request.get("invoiceIds");
        
        Map<String, Object> estimate = new HashMap<>();
        estimate.put("estimatedDuration", 45); // 45 segundos
        estimate.put("complexity", "medium");
        estimate.put("potentialOperations", invoiceIds.size() * 3);
        estimate.put("warnings", Arrays.asList(
            "Algumas operações podem ser duplicatas",
            "Verifique se as datas estão corretas"
        ));
        estimate.put("canProceed", true);
        
        return ResponseEntity.ok(estimate);
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processInvoices(@RequestBody Map<String, Object> request) {
        log.info("🚀 Processando invoices (modo teste)...");
        
        @SuppressWarnings("unchecked")
        List<String> invoiceIds = (List<String>) request.get("invoiceIds");
        
        // Simular processamento rápido
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("partialSuccess", false);
        response.put("operationsCreated", invoiceIds.size() * 2);
        response.put("operationsUpdated", 1);
        response.put("operationsSkipped", 0);
        response.put("summary", String.format("Processamento concluído! %d operações criadas com sucesso.", invoiceIds.size() * 2));
        response.put("sessionId", UUID.randomUUID().toString());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process/{invoiceId}")
    public ResponseEntity<Map<String, Object>> processSingleInvoice(
            @PathVariable String invoiceId,
            @RequestBody(required = false) Map<String, Object> request) {
        
        log.info("🎯 Processando invoice única: {}", invoiceId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("operationsCreated", 2);
        response.put("operationsUpdated", 0);
        response.put("operationsSkipped", 0);
        response.put("summary", "Invoice processada com sucesso! 2 operações criadas.");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getProcessingStatus(@PathVariable String sessionId) {
        log.info("📊 Consultando status da sessão: {}", sessionId);
        
        Map<String, Object> status = new HashMap<>();
        status.put("sessionId", sessionId);
        status.put("status", "COMPLETED");
        status.put("currentInvoice", 3);
        status.put("totalInvoices", 3);
        status.put("currentStep", "Processamento concluído");
        status.put("operationsCreated", 6);
        status.put("operationsUpdated", 1);
        status.put("operationsSkipped", 0);
        status.put("elapsedTime", 42);
        status.put("messages", Arrays.asList(
            "Processamento iniciado",
            "Analisando invoice 1/3...",
            "Criadas 2 operações para invoice 1",
            "Analisando invoice 2/3...",
            "Criadas 2 operações para invoice 2",
            "Analisando invoice 3/3...",
            "Criadas 2 operações para invoice 3",
            "Processamento concluído com sucesso!"
        ));
        status.put("errors", Arrays.asList());
        
        return ResponseEntity.ok(status);
    }

    @GetMapping(value = "/status/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("permitAll()")
    public SseEmitter getProcessingStream(
            @PathVariable String sessionId,
            @RequestParam(required = false) String token,
            HttpServletResponse response) {
        
        log.info("📡 Stream SSE solicitado para sessão: {} (token presente: {})", 
                 sessionId, token != null);
        
        // ✅ CONFIGURAR HEADERS CORS ESPECÍFICOS PARA SSE
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Allow-Credentials", "false"); // Para * origin
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        
        SseEmitter emitter = new SseEmitter(60000L); // 60 segundos timeout
        
        // Configurar handlers
        emitter.onCompletion(() -> log.info("✅ SSE completado para sessão: {}", sessionId));
        emitter.onTimeout(() -> log.warn("⏰ SSE timeout para sessão: {}", sessionId));
        emitter.onError((e) -> log.error("❌ SSE erro para sessão: {} - {}", sessionId, e.getMessage()));
        
        // Simular progresso assíncrono
        CompletableFuture.runAsync(() -> {
            try {
                // ✅ EVENTO INICIAL IMEDIATO
                log.info("📤 Enviando evento inicial SSE...");
                emitter.send(SseEmitter.event()
                    .data("{\"status\":\"STARTED\",\"currentInvoice\":0,\"totalInvoices\":3,\"operationsCreated\":0,\"currentStep\":\"Iniciando processamento...\"}"));
                
                Thread.sleep(1000);
                log.info("📤 Enviando progresso 1/3...");
                emitter.send(SseEmitter.event()
                    .data("{\"status\":\"PROCESSING\",\"currentInvoice\":1,\"totalInvoices\":3,\"operationsCreated\":0,\"currentStep\":\"Analisando invoice 1/3...\"}"));
                
                Thread.sleep(2000);
                log.info("📤 Enviando progresso 2/3...");
                emitter.send(SseEmitter.event()
                    .data("{\"status\":\"PROCESSING\",\"currentInvoice\":2,\"totalInvoices\":3,\"operationsCreated\":2,\"currentStep\":\"Analisando invoice 2/3...\"}"));
                
                Thread.sleep(2000);
                log.info("📤 Enviando progresso 3/3...");
                emitter.send(SseEmitter.event()
                    .data("{\"status\":\"PROCESSING\",\"currentInvoice\":3,\"totalInvoices\":3,\"operationsCreated\":4,\"currentStep\":\"Analisando invoice 3/3...\"}"));
                
                Thread.sleep(1000);
                log.info("📤 Enviando conclusão...");
                emitter.send(SseEmitter.event()
                    .data("{\"status\":\"COMPLETED\",\"currentInvoice\":3,\"totalInvoices\":3,\"operationsCreated\":6,\"currentStep\":\"Processamento concluído!\"}"));
                
                log.info("✅ Finalizando SSE para sessão: {}", sessionId);
                emitter.complete();
                
            } catch (Exception e) {
                log.error("❌ Erro no SSE para sessão {}: {}", sessionId, e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });
        
        log.info("🔄 Retornando SseEmitter para sessão: {}", sessionId);
        return emitter;
    }

    @PostMapping("/status/{sessionId}/cancel")
    public ResponseEntity<Void> cancelProcessing(@PathVariable String sessionId) {
        log.info("❌ Cancelando sessão: {}", sessionId);
        return ResponseEntity.ok().build();
    }
}