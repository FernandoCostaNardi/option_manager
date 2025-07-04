package com.olisystem.optionsmanager.dto.invoice.processing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para status e progresso de processamento
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingStatusResponse {

    private UUID sessionId;
    private String status;
    private boolean isCompleted;
    private boolean isSuccessful;
    
    // Progresso atual
    private ProgressInfo currentProgress;
    
    // Fase atual
    private PhaseInfo currentPhase;
    
    // Estatísticas
    private ProcessingStats stats;
    
    // Timing
    private TimingInfo timing;
    
    // Logs recentes
    private List<ProcessingLogEntry> recentLogs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressInfo {
        private int overallProgressPercentage;
        private int phaseProgressPercentage;
        private int processedItems;
        private int totalItems;
        private int successfulItems;
        private int failedItems;
        private int skippedItems;
        private double itemSuccessRate;
        
        public String getProgressText() {
            return String.format("%d/%d itens (%d%%)", processedItems, totalItems, overallProgressPercentage);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhaseInfo {
        private String currentPhase;
        private String phaseDescription;
        private int phaseProgressPercentage;
        private LocalDateTime phaseStartTime;
        private List<String> completedPhases;
        private List<String> remainingPhases;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingStats {
        private int operationsCreated;
        private int operationsFinalized;
        private int totalOperations;
        private int invoicesProcessed;
        private int totalInvoices;
        private double processingRate; // itens por segundo
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimingInfo {
        private LocalDateTime startTime;
        private LocalDateTime estimatedEndTime;
        private Duration elapsedTime;
        private Duration estimatedRemainingTime;
        private Duration averageItemTime;
        
        public String getElapsedTimeFormatted() {
            if (elapsedTime == null) return "0s";
            
            long seconds = elapsedTime.getSeconds();
            if (seconds < 60) {
                return seconds + "s";
            } else if (seconds < 3600) {
                return (seconds / 60) + "m " + (seconds % 60) + "s";
            } else {
                return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
            }
        }
        
        public String getRemainingTimeFormatted() {
            if (estimatedRemainingTime == null) return "Calculando...";
            return getFormattedDuration(estimatedRemainingTime);
        }
        
        private String getFormattedDuration(Duration duration) {
            long seconds = duration.getSeconds();
            if (seconds < 60) {
                return seconds + "s";
            } else if (seconds < 3600) {
                return (seconds / 60) + "m " + (seconds % 60) + "s";
            } else {
                return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingLogEntry {
        private LocalDateTime timestamp;
        private String level; // INFO, WARN, ERROR
        private String message;
        private String phase;
        private String category;
    }
    
    public boolean canBeCancelled() {
        return !isCompleted && !"FAILED".equals(status);
    }
    
    public String getStatusDescription() {
        if (isCompleted) {
            return isSuccessful ? "Processamento concluído com sucesso" : "Processamento falhou";
        }
        return "Processamento em andamento";
    }
}