package com.olisystem.optionsmanager.service.invoice.processing.orchestration;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.operation.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serviço para rastrear progresso de processamento em tempo real
 * Fornece métricas detalhadas e estimativas de conclusão
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@Slf4j
public class ProcessingProgressTracker {

    private final Map<UUID, ProcessingSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Inicia nova sessão de processamento
     * 
     * @param invoice Invoice a ser processada
     * @param totalItems Total de itens para processar
     * @return ID da sessão criada
     */
    public UUID startProcessingSession(Invoice invoice, int totalItems) {
        UUID sessionId = UUID.randomUUID();
        
        ProcessingSession session = ProcessingSession.builder()
                .sessionId(sessionId)
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .totalItems(totalItems)
                .processedItems(new AtomicInteger(0))
                .successfulItems(new AtomicInteger(0))
                .failedItems(new AtomicInteger(0))
                .skippedItems(new AtomicInteger(0))
                .currentPhase(ProcessingPhase.INITIALIZING)
                .phaseProgress(new ConcurrentHashMap<>())
                .startTime(LocalDateTime.now())
                .estimatedEndTime(null)
                .operationsCreated(new ArrayList<>())
                .operationsFinalized(new ArrayList<>())
                .build();
        
        activeSessions.put(sessionId, session);
        
        log.info("🚀 Sessão de processamento iniciada: {} para invoice {} ({} itens)",
                sessionId.toString().substring(0, 8), invoice.getInvoiceNumber(), totalItems);
        
        return sessionId;
    }

    /**
     * Atualiza fase atual do processamento
     * 
     * @param sessionId ID da sessão
     * @param phase Nova fase
     */
    public void updatePhase(UUID sessionId, ProcessingPhase phase) {
        ProcessingSession session = getSession(sessionId);
        if (session == null) return;
        
        ProcessingPhase previousPhase = session.getCurrentPhase();
        session.setCurrentPhase(phase);
        session.setPhaseStartTime(LocalDateTime.now());
        
        // Marcar fase anterior como concluída
        if (previousPhase != null) {
            session.getPhaseProgress().put(previousPhase, 100);
        }
        
        // Inicializar progresso da nova fase
        session.getPhaseProgress().put(phase, 0);
        
        // Recalcular estimativa
        updateEstimatedEndTime(session);
        
        log.debug("🔄 Fase atualizada para sessão {}: {} → {}",
                 sessionId.toString().substring(0, 8), previousPhase, phase);
    }

    /**
     * Atualiza progresso de uma fase específica
     * 
     * @param sessionId ID da sessão
     * @param phase Fase a ser atualizada
     * @param progressPercentage Percentual de progresso (0-100)
     */
    public void updatePhaseProgress(UUID sessionId, ProcessingPhase phase, int progressPercentage) {
        ProcessingSession session = getSession(sessionId);
        if (session == null) return;
        
        progressPercentage = Math.max(0, Math.min(100, progressPercentage));
        session.getPhaseProgress().put(phase, progressPercentage);
        
        // Recalcular estimativa se for a fase atual
        if (session.getCurrentPhase() == phase) {
            updateEstimatedEndTime(session);
        }
        
        log.debug("📊 Progresso da fase {} atualizado: {}%", phase, progressPercentage);
    }

    /**
     * Registra processamento de um item
     * 
     * @param sessionId ID da sessão
     * @param success Se o item foi processado com sucesso
     * @param skipped Se o item foi pulado
     */
    public void recordItemProcessed(UUID sessionId, boolean success, boolean skipped) {
        ProcessingSession session = getSession(sessionId);
        if (session == null) return;
        
        session.getProcessedItems().incrementAndGet();
        
        if (skipped) {
            session.getSkippedItems().incrementAndGet();
        } else if (success) {
            session.getSuccessfulItems().incrementAndGet();
        } else {
            session.getFailedItems().incrementAndGet();
        }
        
        // Atualizar progresso geral
        updateOverallProgress(session);
        
        log.debug("📈 Item processado: sucesso={}, pulado={} (Total: {}/{})",
                 success, skipped, session.getProcessedItems().get(), session.getTotalItems());
    }

    /**
     * Registra operação criada
     * 
     * @param sessionId ID da sessão
     * @param operation Operação criada
     */
    public void recordOperationCreated(UUID sessionId, Operation operation) {
        ProcessingSession session = getSession(sessionId);
        if (session == null) return;
        
        session.getOperationsCreated().add(operation.getId());
        
        log.debug("🆕 Operação criada registrada: {} (Total: {})",
                 operation.getId().toString().substring(0, 8),
                 session.getOperationsCreated().size());
    }

    /**
     * Registra operação finalizada
     * 
     * @param sessionId ID da sessão
     * @param operation Operação finalizada
     */
    public void recordOperationFinalized(UUID sessionId, Operation operation) {
        ProcessingSession session = getSession(sessionId);
        if (session == null) return;
        
        session.getOperationsFinalized().add(operation.getId());
        
        log.debug("🎯 Operação finalizada registrada: {} (Total: {})",
                 operation.getId().toString().substring(0, 8),
                 session.getOperationsFinalized().size());
    }

    /**
     * Finaliza sessão de processamento
     * 
     * @param sessionId ID da sessão
     * @param successful Se o processamento foi bem-sucedido
     * @return Resumo final da sessão
     */
    public ProcessingSummary finishSession(UUID sessionId, boolean successful) {
        ProcessingSession session = getSession(sessionId);
        if (session == null) {
            return ProcessingSummary.builder().build();
        }
        
        session.setEndTime(LocalDateTime.now());
        session.setSuccessful(successful);
        session.setCurrentPhase(successful ? ProcessingPhase.COMPLETED : ProcessingPhase.FAILED);
        
        // Calcular duração total
        Duration totalDuration = Duration.between(session.getStartTime(), session.getEndTime());
        session.setTotalDuration(totalDuration);
        
        ProcessingSummary summary = ProcessingSummary.builder()
                .sessionId(sessionId)
                .invoiceId(session.getInvoiceId())
                .invoiceNumber(session.getInvoiceNumber())
                .successful(successful)
                .totalItems(session.getTotalItems())
                .processedItems(session.getProcessedItems().get())
                .successfulItems(session.getSuccessfulItems().get())
                .failedItems(session.getFailedItems().get())
                .skippedItems(session.getSkippedItems().get())
                .operationsCreated(session.getOperationsCreated().size())
                .operationsFinalized(session.getOperationsFinalized().size())
                .totalDuration(totalDuration)
                .averageItemProcessingTime(calculateAverageItemTime(session))
                .phaseBreakdown(createPhaseBreakdown(session))
                .build();
        
        // Remover sessão ativa
        activeSessions.remove(sessionId);
        
        log.info("✅ Sessão {} finalizada: {} ({} sucessos, {} falhas, {} ignorados) em {}",
                sessionId.toString().substring(0, 8),
                successful ? "SUCESSO" : "FALHA",
                summary.getSuccessfulItems(),
                summary.getFailedItems(),
                summary.getSkippedItems(),
                formatDuration(totalDuration));
        
        return summary;
    }

    /**
     * Obtém progresso atual de uma sessão
     * 
     * @param sessionId ID da sessão
     * @return Progresso atual ou null se não encontrada
     */
    public ProcessingProgress getCurrentProgress(UUID sessionId) {
        ProcessingSession session = getSession(sessionId);
        if (session == null) return null;
        
        Duration elapsedTime = Duration.between(session.getStartTime(), LocalDateTime.now());
        
        return ProcessingProgress.builder()
                .sessionId(sessionId)
                .currentPhase(session.getCurrentPhase())
                .overallProgressPercentage(calculateOverallProgress(session))
                .phaseProgressPercentage(session.getPhaseProgress()
                                               .getOrDefault(session.getCurrentPhase(), 0))
                .processedItems(session.getProcessedItems().get())
                .totalItems(session.getTotalItems())
                .successfulItems(session.getSuccessfulItems().get())
                .failedItems(session.getFailedItems().get())
                .skippedItems(session.getSkippedItems().get())
                .elapsedTime(elapsedTime)
                .estimatedRemainingTime(calculateRemainingTime(session))
                .operationsCreated(session.getOperationsCreated().size())
                .operationsFinalized(session.getOperationsFinalized().size())
                .build();
    }

    /**
     * Lista todas as sessões ativas
     * 
     * @return Lista de progressos de todas as sessões ativas
     */
    public List<ProcessingProgress> getAllActiveSessions() {
        return activeSessions.keySet().stream()
                .map(this::getCurrentProgress)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProcessingProgress::getElapsedTime).reversed())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Obtém sessão por ID
     */
    private ProcessingSession getSession(UUID sessionId) {
        ProcessingSession session = activeSessions.get(sessionId);
        if (session == null) {
            log.warn("⚠️ Sessão não encontrada: {}", sessionId);
        }
        return session;
    }

    /**
     * Atualiza progresso geral da sessão
     */
    private void updateOverallProgress(ProcessingSession session) {
        if (session.getTotalItems() > 0) {
            int progressPercentage = (session.getProcessedItems().get() * 100) / session.getTotalItems();
            session.setOverallProgress(progressPercentage);
        }
    }

    /**
     * Calcula progresso geral
     */
    private int calculateOverallProgress(ProcessingSession session) {
        if (session.getTotalItems() == 0) return 100;
        
        // Combinar progresso de itens com progresso de fases
        int itemProgress = (session.getProcessedItems().get() * 80) / session.getTotalItems();
        int phaseProgress = session.getPhaseProgress()
                                  .getOrDefault(session.getCurrentPhase(), 0) * 20 / 100;
        
        return Math.min(100, itemProgress + phaseProgress);
    }

    /**
     * Atualiza estimativa de tempo de conclusão
     */
    private void updateEstimatedEndTime(ProcessingSession session) {
        Duration elapsed = Duration.between(session.getStartTime(), LocalDateTime.now());
        int progress = calculateOverallProgress(session);
        
        if (progress > 0) {
            long totalEstimatedMs = (elapsed.toMillis() * 100) / progress;
            session.setEstimatedEndTime(session.getStartTime().plus(
                Duration.ofMillis(totalEstimatedMs)
            ));
        }
    }

    /**
     * Calcula tempo restante estimado
     */
    private Duration calculateRemainingTime(ProcessingSession session) {
        if (session.getEstimatedEndTime() == null) return Duration.ZERO;
        
        Duration remaining = Duration.between(LocalDateTime.now(), session.getEstimatedEndTime());
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /**
     * Calcula tempo médio por item
     */
    private Duration calculateAverageItemTime(ProcessingSession session) {
        int processedItems = session.getProcessedItems().get();
        if (processedItems == 0) return Duration.ZERO;
        
        return session.getTotalDuration().dividedBy(processedItems);
    }

    /**
     * Cria breakdown detalhado das fases
     */
    private Map<ProcessingPhase, Integer> createPhaseBreakdown(ProcessingSession session) {
        return new HashMap<>(session.getPhaseProgress());
    }

    /**
     * Formata duração para exibição
     */
    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
    }

    /**
     * Sessão de processamento ativa
     */
    @lombok.Builder
    @lombok.Data
    private static class ProcessingSession {
        private UUID sessionId;
        private UUID invoiceId;
        private String invoiceNumber;
        private int totalItems;
        private AtomicInteger processedItems;
        private AtomicInteger successfulItems;
        private AtomicInteger failedItems;
        private AtomicInteger skippedItems;
        private ProcessingPhase currentPhase;
        private Map<ProcessingPhase, Integer> phaseProgress;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime phaseStartTime;
        private LocalDateTime estimatedEndTime;
        private Duration totalDuration;
        private boolean successful;
        private int overallProgress;
        private List<UUID> operationsCreated;
        private List<UUID> operationsFinalized;
    }

    /**
     * Progresso atual de processamento
     */
    @lombok.Builder
    @lombok.Data
    public static class ProcessingProgress {
        private UUID sessionId;
        private ProcessingPhase currentPhase;
        private int overallProgressPercentage;
        private int phaseProgressPercentage;
        private int processedItems;
        private int totalItems;
        private int successfulItems;
        private int failedItems;
        private int skippedItems;
        private Duration elapsedTime;
        private Duration estimatedRemainingTime;
        private int operationsCreated;
        private int operationsFinalized;
        
        public double getItemSuccessRate() {
            return processedItems > 0 ? (double) successfulItems / processedItems * 100 : 0;
        }
        
        public boolean isCompleted() {
            return overallProgressPercentage >= 100;
        }
        
        public String getProgressSummary() {
            return String.format("%d/%d itens (%d%%)", 
                                processedItems, totalItems, overallProgressPercentage);
        }
    }

    /**
     * Resumo final de processamento
     */
    @lombok.Builder
    @lombok.Data
    public static class ProcessingSummary {
        private UUID sessionId;
        private UUID invoiceId;
        private String invoiceNumber;
        private boolean successful;
        private int totalItems;
        private int processedItems;
        private int successfulItems;
        private int failedItems;
        private int skippedItems;
        private int operationsCreated;
        private int operationsFinalized;
        private Duration totalDuration;
        private Duration averageItemProcessingTime;
        private Map<ProcessingPhase, Integer> phaseBreakdown;
        
        public double getSuccessRate() {
            return totalItems > 0 ? (double) successfulItems / totalItems * 100 : 0;
        }
        
        public String getSummaryText() {
            return String.format("Processamento %s: %d/%d itens (%.1f%% sucesso) em %s",
                                successful ? "concluído" : "falhado",
                                successfulItems, totalItems,
                                getSuccessRate(),
                                totalDuration);
        }
    }

    /**
     * Fases do processamento
     */
    public enum ProcessingPhase {
        INITIALIZING("Inicializando"),
        VALIDATING("Validando"),
        DETECTING("Detectando operações"),
        MAPPING("Mapeando dados"),
        PROCESSING_OPERATIONS("Processando operações"),
        FINALIZING("Finalizando"),
        COMPLETED("Concluído"),
        FAILED("Falhado");
        
        private final String description;
        
        ProcessingPhase(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}