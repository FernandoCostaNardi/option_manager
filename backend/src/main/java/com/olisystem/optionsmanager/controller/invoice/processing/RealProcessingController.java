package com.olisystem.optionsmanager.controller.invoice.processing;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.service.auth.UserService;
import com.olisystem.optionsmanager.service.invoice.processing.ProcessingProgress;
import com.olisystem.optionsmanager.service.invoice.processing.ProcessingResult;
import com.olisystem.optionsmanager.service.invoice.processing.InvoiceProcessingResult;
import com.olisystem.optionsmanager.service.invoice.processing.RealInvoiceProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Controller para processamento real de invoices
 * ✅ ATUALIZADO: Integração com sistema de logs de processamento
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@RestController
@RequestMapping("/api/invoice/processing/real")
@RequiredArgsConstructor
@Slf4j
public class RealProcessingController {

    private final RealInvoiceProcessor realInvoiceProcessor;
    private final UserService userService;
    
    // ✅ NOVO: Sessões ativas de processamento
    private final Map<String, ProcessingSession> activeSessions = new ConcurrentHashMap<>();

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
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Iniciar processamento assíncrono
        CompletableFuture.runAsync(() -> {
            try {
                log.info("🔄 Iniciando processamento assíncrono para {} invoices", uuidInvoiceIds.size());
                
                // Processar invoices
                CompletableFuture<ProcessingResult> future = realInvoiceProcessor.processInvoicesAsync(
                    uuidInvoiceIds, currentUser, this::updateProgress, sessionId);
                
                ProcessingResult result = future.get();
                
                // Atualizar sessão com resultado final
                session.setResult(result);
                session.setCompleted(true);
                
                if (result.isSuccess()) {
                    session.addMessage("✅ Processamento concluído com sucesso");
                    log.info("✅ Processamento concluído: {} operações criadas", result.getTotalOperationsCreated());
                } else {
                    String errorMessage = result.getErrors().isEmpty() ? "Erro desconhecido" : String.join("; ", result.getErrors());
                    session.addError("❌ Processamento falhou: " + errorMessage);
                    log.error("❌ Processamento falhou: {}", errorMessage);
                }
                
                // Enviar evento final
                session.sendEvent("complete", Map.of(
                    "success", result.isSuccess(),
                    "operationsCreated", result.getTotalOperationsCreated(),
                    "operationsSkipped", result.getTotalOperationsSkipped(),
                    "processingTimeMs", result.getProcessingTimeMs()
                ));
                
            } catch (Exception e) {
                log.error("❌ Erro durante processamento assíncrono: {}", e.getMessage(), e);
                session.addError("❌ Erro interno: " + e.getMessage());
                session.setCompleted(true);
                session.setErrorMessage(e.getMessage());
                
                // Enviar evento de erro
                session.sendEvent("error", Map.of("error", e.getMessage()));
            }
        });
        
        // Retornar session ID para o frontend
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("message", "Processamento iniciado");
        response.put("totalInvoices", uuidInvoiceIds.size());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process/{invoiceId}")
    public ResponseEntity<Map<String, Object>> processSingleInvoice(
            @PathVariable String invoiceId,
            @RequestBody(required = false) Map<String, Object> request,
            Authentication authentication) {
        
        log.info("🎯 Processando invoice única: {}", invoiceId);
        
        try {
            // Obter usuário autenticado
            User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + authentication.getName()));
            
            // Converter para UUID
            UUID invoiceUuid = UUID.fromString(invoiceId);
            
            // ✅ NOVO: Usar método de processamento individual com validação
            InvoiceProcessingResult result = realInvoiceProcessor.processSingleInvoice(invoiceUuid, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isFullyProcessed());
            response.put("invoiceId", invoiceId);
            response.put("invoiceNumber", result.getInvoiceNumber());
            response.put("totalItems", result.getTotalItems());
            response.put("operationsCreated", result.getOperationsCreated());
            response.put("operationsSkipped", result.getOperationsSkipped());
            
            if (result.isFullyProcessed()) {
                log.info("✅ Invoice {} processada com sucesso", invoiceId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("❌ Invoice {} falhou no processamento", invoiceId);
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (IllegalArgumentException e) {
            log.error("❌ ID de invoice inválido: {}", invoiceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "ID de invoice inválido: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            log.error("❌ Erro ao processar invoice {}: {}", invoiceId, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Erro interno: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * ✅ NOVO: Processa invoice individual com confirmação de reprocessamento
     */
    @PostMapping("/process/{invoiceId}/confirm")
    public ResponseEntity<Map<String, Object>> processSingleInvoiceWithConfirmation(
            @PathVariable String invoiceId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        log.info("✅ Processando invoice {} com confirmação de reprocessamento", invoiceId);
        
        try {
            // Obter usuário autenticado
            User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + authentication.getName()));
            
            // Converter para UUID
            UUID invoiceUuid = UUID.fromString(invoiceId);
            
            // Verificar se confirmação foi fornecida
            Boolean confirmed = (Boolean) request.get("confirmed");
            if (confirmed == null || !confirmed) {
                log.warn("❌ Reprocessamento da invoice {} não foi confirmado", invoiceId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Reprocessamento não foi confirmado");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Processar invoice com confirmação
            InvoiceProcessingResult result = realInvoiceProcessor.processSingleInvoice(invoiceUuid, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isFullyProcessed());
            response.put("invoiceId", invoiceId);
            response.put("invoiceNumber", result.getInvoiceNumber());
            response.put("totalItems", result.getTotalItems());
            response.put("operationsCreated", result.getOperationsCreated());
            response.put("operationsSkipped", result.getOperationsSkipped());
            response.put("reprocessed", true);
            
            if (result.isFullyProcessed()) {
                log.info("✅ Invoice {} reprocessada com sucesso", invoiceId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("❌ Invoice {} falhou no reprocessamento", invoiceId);
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("❌ Erro ao reprocessar invoice {}: {}", invoiceId, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Erro interno: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
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
        status.put("completed", session.isCompleted());
        status.put("success", session.getResult() != null ? session.getResult().isSuccess() : false);
        
        return ResponseEntity.ok(status);
    }

    /**
     * ✅ TEMPORÁRIO: Endpoint para status sem autenticação (para teste)
     */
    @GetMapping("/status/{sessionId}/public")
    public ResponseEntity<Map<String, Object>> getProcessingStatusPublic(@PathVariable String sessionId) {
        log.info("📊 Consultando status da sessão (público): {}", sessionId);
        
        ProcessingSession session = activeSessions.get(sessionId);
        if (session == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Sessão não encontrada: " + sessionId);
            errorResponse.put("availableSessions", activeSessions.keySet());
            return ResponseEntity.status(404).body(errorResponse);
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
        status.put("completed", session.isCompleted());
        status.put("success", session.getResult() != null ? session.getResult().isSuccess() : false);
        status.put("note", "Endpoint temporário para teste");
        
        return ResponseEntity.ok(status);
    }

    /**
     * ✅ TESTE: Endpoint simples para verificar se o controller está sendo carregado
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testEndpoint() {
        log.info("🧪 Testando endpoint do RealProcessingController");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "RealProcessingController está funcionando!");
        response.put("timestamp", System.currentTimeMillis());
        response.put("activeSessions", activeSessions.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ TESTE: Endpoint simples para status público
     */
    @GetMapping("/status-test/{sessionId}")
    public ResponseEntity<Map<String, Object>> testStatusEndpoint(@PathVariable String sessionId) {
        log.info("🧪 Testando endpoint de status: {}", sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("message", "Endpoint de status funcionando!");
        response.put("activeSessions", activeSessions.size());
        response.put("availableSessions", activeSessions.keySet());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stream/{sessionId}")
    public SseEmitter streamProcessingProgress(@PathVariable String sessionId) {
        log.info("📡 Iniciando stream SSE para sessão: {}", sessionId);
        
        ProcessingSession session = activeSessions.get(sessionId);
        if (session == null) {
            log.warn("⚠️ Sessão não encontrada: {}", sessionId);
            return null;
        }
        
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutos timeout
        session.addEmitter(emitter);
        
        // Enviar evento inicial
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(Map.of("sessionId", sessionId, "message", "Conectado ao stream de progresso")));
        } catch (IOException e) {
            log.error("❌ Erro ao enviar evento inicial: {}", e.getMessage());
        }
        
        return emitter;
    }

    /**
     * ✅ NOVO: Endpoint de compatibilidade para o frontend
     * Redireciona para o novo sistema de progresso SSE
     */
    @GetMapping("/stream/{sessionId}/compat")
    public SseEmitter streamProcessingProgressCompat(@PathVariable String sessionId) {
        log.info("📡 Endpoint de compatibilidade - redirecionando para novo SSE: {}", sessionId);
        
        // Criar emitter com timeout de 30 segundos
        SseEmitter emitter = new SseEmitter(TimeUnit.SECONDS.toMillis(30));
        
        // Configurar callbacks do emitter
        emitter.onCompletion(() -> {
            log.info("📡 SSE de compatibilidade completado para sessão: {}", sessionId);
        });
        
        emitter.onTimeout(() -> {
            log.warn("⏰ SSE de compatibilidade timeout para sessão: {}", sessionId);
        });
        
        emitter.onError((ex) -> {
            log.error("❌ Erro no SSE de compatibilidade para sessão: {} - {}", sessionId, ex.getMessage(), ex);
        });
        
        // Enviar evento de conexão
        try {
            Map<String, Object> connectionEvent = Map.of(
                "sessionId", sessionId,
                "message", "Conectado ao stream de progresso (modo compatibilidade)",
                "type", "CONNECTED",
                "timestamp", System.currentTimeMillis()
            );
            
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(connectionEvent, MediaType.APPLICATION_JSON));
            
            log.info("✅ Cliente conectado ao SSE de compatibilidade: {}", sessionId);
            
        } catch (IOException e) {
            log.error("❌ Erro ao enviar evento de conexão: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    @DeleteMapping("/cancel/{sessionId}")
    public ResponseEntity<Map<String, Object>> cancelProcessing(@PathVariable String sessionId) {
        log.info("🚫 Cancelando processamento da sessão: {}", sessionId);
        
        ProcessingSession session = activeSessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        
        session.cancel();
        session.addMessage("🚫 Processamento cancelado pelo usuário");
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("message", "Processamento cancelado");
        response.put("cancelled", true);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza progresso da sessão
     */
    private void updateProgress(ProcessingProgress progress) {
        log.debug("📊 Atualizando progresso: {}", progress);
        
        // Encontrar sessão correspondente
        for (ProcessingSession session : activeSessions.values()) {
            if (session.getCurrentProgress() == null || 
                session.getCurrentProgress().getCurrentInvoice() != progress.getCurrentInvoice()) {
                session.setCurrentProgress(progress);
                session.addMessage("📊 " + progress.getCurrentStep());
                break;
            }
        }
    }

    /**
     * Sessão de processamento com gerenciamento de SSE
     */
    public static class ProcessingSession {
        private final String sessionId;
        private final int totalInvoices;
        private final long startTime;
        private final List<SseEmitter> emitters;
        private final List<String> messages;
        private final List<String> errors;
        
        private ProcessingProgress currentProgress;
        private ProcessingResult result;
        private boolean completed = false;
        private boolean cancelled = false;
        private String errorMessage;
        
        public ProcessingSession(String sessionId, int totalInvoices) {
            this.sessionId = sessionId;
            this.totalInvoices = totalInvoices;
            this.startTime = System.currentTimeMillis();
            this.emitters = new ArrayList<>();
            this.messages = new ArrayList<>();
            this.errors = new ArrayList<>();
        }
        
        public void addEmitter(SseEmitter emitter) {
            emitters.add(emitter);
        }
        
        public void setCurrentProgress(ProcessingProgress progress) {
            this.currentProgress = progress;
            sendEvent("progress", progress);
        }
        
        public void setResult(ProcessingResult result) {
            this.result = result;
        }
        
        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public void addMessage(String message) {
            messages.add(message);
            sendEvent("message", Map.of("message", message));
        }
        
        public void addError(String error) {
            errors.add(error);
            sendEvent("error", Map.of("error", error));
        }
        
        public void cancel() {
            this.cancelled = true;
        }
        
        public void sendEvent(String event, Object data) {
            emitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name(event).data(data));
                    return false;
                } catch (IOException e) {
                    return true; // Remove emitter com erro
                }
            });
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public int getTotalInvoices() { return totalInvoices; }
        public ProcessingProgress getCurrentProgress() { return currentProgress; }
        public ProcessingResult getResult() { return result; }
        public boolean isCompleted() { return completed; }
        public boolean isCancelled() { return cancelled; }
        public String getErrorMessage() { return errorMessage; }
        public List<String> getMessages() { return messages; }
        public List<String> getErrors() { return errors; }
        public long getElapsedTimeSeconds() { return (System.currentTimeMillis() - startTime) / 1000; }
    }
}
