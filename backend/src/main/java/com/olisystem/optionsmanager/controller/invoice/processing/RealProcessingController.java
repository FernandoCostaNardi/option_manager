package com.olisystem.optionsmanager.controller.invoice.processing;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.service.auth.UserService;
import com.olisystem.optionsmanager.service.invoice.processing.ProcessingProgress;
import com.olisystem.optionsmanager.service.invoice.processing.ProcessingResult;
import com.olisystem.optionsmanager.service.invoice.processing.RealInvoiceProcessor;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller REAL para processamento de invoices
 * Substitui o ProcessingTestController com lógica real
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-04
 */
@RestController
@RequestMapping("/api/processing")
@RequiredArgsConstructor
@Slf4j
public class RealProcessingController {

    private final RealInvoiceProcessor invoiceProcessor;
    private final UserService userService;
    
    // Cache de sessões de processamento
    private final Map<String, ProcessingSession> activeSessions = new ConcurrentHashMap<>();

    @PostMapping("/estimate")
    public ResponseEntity<Map<String, Object>> estimateProcessing(@RequestBody Map<String, Object> request) {
        log.info("🧮 Estimando processamento real...");
        
        @SuppressWarnings("unchecked")
        List<String> invoiceIds = (List<String>) request.get("invoiceIds");
        
        Map<String, Object> estimate = new HashMap<>();
        estimate.put("estimatedDuration", invoiceIds.size() * 5); // 5 segundos por invoice
        estimate.put("complexity", "real");
        estimate.put("potentialOperations", invoiceIds.size() * 8); // Estimativa de 8 operações por invoice
        estimate.put("warnings", Arrays.asList(
            "Processamento real criará operações no banco de dados",
            "Operações duplicadas serão ignoradas",
            "Verifique se as invoices estão corretas"
        ));
        estimate.put("canProceed", true);
        
        return ResponseEntity.ok(estimate);
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processInvoices(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        log.info("🚀 Iniciando processamento REAL de invoices...");
        
        @SuppressWarnings("unchecked")
        List<String> invoiceIds = (List<String>) request.get("invoiceIds");
        
        // Converter strings para UUIDs
        List<UUID> uuidInvoiceIds = invoiceIds.stream()
            .map(UUID::fromString)
            .toList();
        
        // ✅ CORREÇÃO: Obter usuário autenticado
        final User currentUser;
        try {
            currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + authentication.getName()));
            log.info("👤 Usuário autenticado: {} ({})", currentUser.getEmail(), currentUser.getId());
        } catch (Exception e) {
            log.error("❌ Erro ao obter usuário autenticado: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Erro de autenticação: " + e.getMessage());
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        // Gerar session ID
        String sessionId = UUID.randomUUID().toString();
        
        // Criar sessão de processamento
        ProcessingSession session = new ProcessingSession(sessionId, uuidInvoiceIds.size());
        activeSessions.put(sessionId, session);
        
        // ✅ CORREÇÃO: Adicionar delay para dar tempo ao frontend conectar ao SSE
        CompletableFuture<ProcessingResult> processingFuture = CompletableFuture.supplyAsync(() -> {
            try {
                // Aguardar 2 segundos para frontend conectar ao SSE
                Thread.sleep(2000);
                log.info("🚀 Iniciando processamento após delay para conexão SSE da sessão: {}", sessionId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("⏰ Delay interrompido para sessão: {}", sessionId);
            }
            return null;
        }).thenCompose(ignored -> invoiceProcessor.processInvoicesAsync(
            uuidInvoiceIds, 
            currentUser, // ✅ CORREÇÃO: Passar usuário autenticado
            progress -> {
                session.updateProgress(progress);
                session.notifyEmitters(progress);
            }
        ));
        
        // Quando concluir, atualizar sessão
        processingFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("❌ Erro no processamento da sessão {}: {}", sessionId, throwable.getMessage(), throwable);
                session.setError(throwable.getMessage());
            } else {
                session.setResult(result);
                // ✅ CORREÇÃO: Aguardar e remover sessão finalizada para evitar reconexões
                CompletableFuture.delayedExecutor(3, java.util.concurrent.TimeUnit.SECONDS).execute(() -> {
                    activeSessions.remove(sessionId);
                    log.info("🧹 Sessão {} removida do cache após processamento", sessionId);
                });
            }
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessionId", sessionId);
        response.put("message", "Processamento iniciado com sucesso");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process/{invoiceId}")
    public ResponseEntity<Map<String, Object>> processSingleInvoice(
            @PathVariable String invoiceId,
            @RequestBody(required = false) Map<String, Object> request,
            Authentication authentication) {
        
        log.info("🎯 Processando invoice única: {}", invoiceId);
        
        // Processar como lista de um item
        Map<String, Object> batchRequest = new HashMap<>();
        batchRequest.put("invoiceIds", Arrays.asList(invoiceId));
        
        return processInvoices(batchRequest, authentication);
    }

    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getProcessingStatus(@PathVariable String sessionId) {
        log.debug("📊 Consultando status da sessão: {}", sessionId);
        
        ProcessingSession session = activeSessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        
        ProcessingProgress progress = session.getCurrentProgress();
        Map<String, Object> status = new HashMap<>();
        status.put("sessionId", sessionId);
        status.put("status", progress != null ? progress.getStatus() : "PROCESSING");
        status.put("currentInvoice", progress != null ? progress.getCurrentInvoice() : 0);
        status.put("totalInvoices", progress != null ? progress.getTotalInvoices() : 0);
        status.put("currentStep", progress != null ? progress.getCurrentStep() : "Iniciando...");
        status.put("operationsCreated", progress != null ? progress.getOperationsCreated() : 0);
        status.put("operationsUpdated", progress != null ? progress.getOperationsUpdated() : 0);
        status.put("operationsSkipped", progress != null ? progress.getOperationsSkipped() : 0);
        status.put("elapsedTime", session.getElapsedTimeSeconds());
        status.put("messages", session.getMessages());
        status.put("errors", session.getErrors());
        
        return ResponseEntity.ok(status);
    }

    @GetMapping(value = "/status/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("permitAll()")
    public SseEmitter getProcessingStream(
            @PathVariable String sessionId,
            @RequestParam(required = false) String token,
            HttpServletResponse response) {
        
        log.info("📡 Stream SSE REAL solicitado para sessão: {} (token presente: {})", 
                 sessionId, token != null);
        
        // ✅ CONFIGURAR HEADERS CORS ESPECÍFICOS PARA SSE
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Allow-Credentials", "false");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        
        SseEmitter emitter = new SseEmitter(60000L); // 60 segundos timeout
        
        // Configurar handlers
        emitter.onCompletion(() -> log.info("✅ SSE REAL completado para sessão: {}", sessionId));
        emitter.onTimeout(() -> log.warn("⏰ SSE REAL timeout para sessão: {}", sessionId));
        emitter.onError((e) -> log.error("❌ SSE REAL erro para sessão: {} - {}", sessionId, e.getMessage()));
        
        // Buscar sessão
        ProcessingSession session = activeSessions.get(sessionId);
        if (session == null) {
            log.warn("❌ Sessão não encontrada ou já finalizada: {}", sessionId);
            // ✅ CORREÇÃO: Retornar erro HTTP em vez de SSE para forçar parada no frontend
            response.setStatus(404);
            try {
                response.getWriter().write("Sessão não encontrada ou já finalizada");
                response.getWriter().flush();
            } catch (Exception e) {
                log.error("Erro ao enviar resposta 404", e);
            }
            return emitter;
        }
        
        // ✅ VERIFICAR SE SESSÃO JÁ FOI FINALIZADA
        if (session.getResult() != null) {
            log.info("📋 Sessão {} já finalizada, enviando resultado direto", sessionId);
            try {
                ProcessingProgress finalProgress = session.getCurrentProgress();
                emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(session.getObjectMapper().writeValueAsString(finalProgress)));
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data("Processamento já finalizado"));
                emitter.complete();
            } catch (Exception e) {
                log.error("Erro ao enviar resultado final da sessão já finalizada", e);
                emitter.completeWithError(e);
            }
            return emitter;
        }
        
        // Adicionar emitter à sessão
        session.addEmitter(emitter);
        
        // Enviar progresso atual se disponível
        ProcessingProgress currentProgress = session.getCurrentProgress();
        if (currentProgress != null) {
            session.sendProgressToEmitter(emitter, currentProgress);
        }
        
        log.info("🔄 Retornando SseEmitter REAL para sessão: {}", sessionId);
        return emitter;
    }

    @PostMapping("/status/{sessionId}/cancel")
    public ResponseEntity<Void> cancelProcessing(@PathVariable String sessionId) {
        log.info("❌ Cancelando sessão REAL: {}", sessionId);
        
        ProcessingSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.cancel();
            activeSessions.remove(sessionId);
        }
        
        return ResponseEntity.ok().build();
    }
}
