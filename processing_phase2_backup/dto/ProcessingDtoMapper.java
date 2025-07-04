package com.olisystem.optionsmanager.service.invoice.processing.dto;

import com.olisystem.optionsmanager.dto.invoice.processing.InvoiceProcessingResponse;
import com.olisystem.optionsmanager.dto.invoice.processing.ProcessingStatusResponse;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.service.invoice.processing.orchestration.ProcessingProgressTracker;
import com.olisystem.optionsmanager.service.invoice.processing.orchestration.ErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço para mapeamento entre objetos de domínio e DTOs
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@Slf4j
public class ProcessingDtoMapper {

    /**
     * Mapeia Operation para OperationSummary
     */
    public InvoiceProcessingResponse.OperationSummary mapOperationToSummary(Operation operation) {
        return InvoiceProcessingResponse.OperationSummary.builder()
                .operationId(operation.getId())
                .assetCode(extractAssetCode(operation))
                .transactionType(operation.getTransactionType().name())
                .tradeType(operation.getTradeType().name())
                .quantity(operation.getQuantity())
                .unitPrice(getEffectiveUnitPrice(operation))
                .totalValue(getEffectiveTotalValue(operation))
                .profitLoss(operation.getProfitLoss())
                .status(operation.getStatus().name())
                .createdAt(operation.getEntryDate().atStartOfDay())
                .build();
    }

    /**
     * Mapeia lista de operações para summaries
     */
    public List<InvoiceProcessingResponse.OperationSummary> mapOperationsToSummaries(List<Operation> operations) {
        return operations.stream()
                .map(this::mapOperationToSummary)
                .collect(Collectors.toList());
    }

    /**
     * Mapeia progresso para status response
     */
    public ProcessingStatusResponse mapProgressToStatusResponse(ProcessingProgressTracker.ProcessingProgress progress) {
        return ProcessingStatusResponse.builder()
                .sessionId(progress.getSessionId())
                .status(progress.getCurrentPhase().name())
                .isCompleted(progress.isCompleted())
                .isSuccessful(progress.getItemSuccessRate() > 90)
                .currentProgress(mapProgressInfo(progress))
                .currentPhase(mapPhaseInfo(progress))
                .stats(mapProcessingStats(progress))
                .timing(mapTimingInfo(progress))
                .recentLogs(List.of()) // Seria implementado com sistema de logs
                .build();
    }

    /**
     * Extrai código do ativo da operação
     */
    private String extractAssetCode(Operation operation) {
        if (operation.getOptionSeries() != null && operation.getOptionSeries().getCode() != null) {
            return operation.getOptionSeries().getCode();
        }
        return "N/A";
    }

    /**
     * Obtém preço unitário efetivo (entrada ou saída)
     */
    private java.math.BigDecimal getEffectiveUnitPrice(Operation operation) {
        // Priorizar preço de saída se disponível
        if (operation.getExitUnitPrice() != null) {
            return operation.getExitUnitPrice();
        }
        return operation.getEntryUnitPrice();
    }

    /**
     * Obtém valor total efetivo (entrada ou saída)
     */
    private java.math.BigDecimal getEffectiveTotalValue(Operation operation) {
        // Priorizar valor de saída se disponível
        if (operation.getExitTotalValue() != null) {
            return operation.getExitTotalValue();
        }
        return operation.getEntryTotalValue();
    }

    /**
     * Mapeia informações de progresso
     */
    private ProcessingStatusResponse.ProgressInfo mapProgressInfo(ProcessingProgressTracker.ProcessingProgress progress) {
        return ProcessingStatusResponse.ProgressInfo.builder()
                .overallProgressPercentage(progress.getOverallProgressPercentage())
                .phaseProgressPercentage(progress.getPhaseProgressPercentage())
                .processedItems(progress.getProcessedItems())
                .totalItems(progress.getTotalItems())
                .successfulItems(progress.getSuccessfulItems())
                .failedItems(progress.getFailedItems())
                .skippedItems(progress.getSkippedItems())
                .itemSuccessRate(progress.getItemSuccessRate())
                .build();
    }

    /**
     * Mapeia informações de fase
     */
    private ProcessingStatusResponse.PhaseInfo mapPhaseInfo(ProcessingProgressTracker.ProcessingProgress progress) {
        return ProcessingStatusResponse.PhaseInfo.builder()
                .currentPhase(progress.getCurrentPhase().name())
                .phaseDescription(progress.getCurrentPhase().getDescription())
                .phaseProgressPercentage(progress.getPhaseProgressPercentage())
                .completedPhases(getCompletedPhases(progress))
                .remainingPhases(getRemainingPhases(progress))
                .build();
    }

    /**
     * Mapeia estatísticas de processamento
     */
    private ProcessingStatusResponse.ProcessingStats mapProcessingStats(ProcessingProgressTracker.ProcessingProgress progress) {
        double processingRate = calculateProcessingRate(progress);
        
        return ProcessingStatusResponse.ProcessingStats.builder()
                .operationsCreated(progress.getOperationsCreated())
                .operationsFinalized(progress.getOperationsFinalized())
                .totalOperations(progress.getOperationsCreated() + progress.getOperationsFinalized())
                .processingRate(processingRate)
                .build();
    }

    /**
     * Mapeia informações de timing
     */
    private ProcessingStatusResponse.TimingInfo mapTimingInfo(ProcessingProgressTracker.ProcessingProgress progress) {
        return ProcessingStatusResponse.TimingInfo.builder()
                .elapsedTime(progress.getElapsedTime())
                .estimatedRemainingTime(progress.getEstimatedRemainingTime())
                .averageItemTime(calculateAverageItemTime(progress))
                .build();
    }

    /**
     * Obtém fases completadas
     */
    private List<String> getCompletedPhases(ProcessingProgressTracker.ProcessingProgress progress) {
        // Simplificação: todas as fases antes da atual estão completas
        ProcessingProgressTracker.ProcessingPhase currentPhase = progress.getCurrentPhase();
        List<String> completed = new java.util.ArrayList<>();
        
        for (ProcessingProgressTracker.ProcessingPhase phase : ProcessingProgressTracker.ProcessingPhase.values()) {
            if (phase.ordinal() < currentPhase.ordinal()) {
                completed.add(phase.name());
            }
        }
        
        return completed;
    }

    /**
     * Obtém fases restantes
     */
    private List<String> getRemainingPhases(ProcessingProgressTracker.ProcessingProgress progress) {
        ProcessingProgressTracker.ProcessingPhase currentPhase = progress.getCurrentPhase();
        List<String> remaining = new java.util.ArrayList<>();
        
        for (ProcessingProgressTracker.ProcessingPhase phase : ProcessingProgressTracker.ProcessingPhase.values()) {
            if (phase.ordinal() > currentPhase.ordinal()) {
                remaining.add(phase.name());
            }
        }
        
        return remaining;
    }

    /**
     * Calcula taxa de processamento (itens por segundo)
     */
    private double calculateProcessingRate(ProcessingProgressTracker.ProcessingProgress progress) {
        if (progress.getElapsedTime() == null || progress.getElapsedTime().isZero()) {
            return 0.0;
        }
        
        long elapsedSeconds = progress.getElapsedTime().getSeconds();
        if (elapsedSeconds == 0) {
            return 0.0;
        }
        
        return (double) progress.getProcessedItems() / elapsedSeconds;
    }

    /**
     * Calcula tempo médio por item
     */
    private java.time.Duration calculateAverageItemTime(ProcessingProgressTracker.ProcessingProgress progress) {
        if (progress.getProcessedItems() == 0 || progress.getElapsedTime() == null) {
            return java.time.Duration.ZERO;
        }
        
        return progress.getElapsedTime().dividedBy(progress.getProcessedItems());
    }
}