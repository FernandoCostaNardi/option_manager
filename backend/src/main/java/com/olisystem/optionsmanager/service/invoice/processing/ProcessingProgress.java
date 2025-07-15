package com.olisystem.optionsmanager.service.invoice.processing;

import lombok.Builder;
import lombok.Data;

/**
 * Representa o progresso do processamento
 */
@Data
@Builder
public class ProcessingProgress {
    private String status;
    private int currentInvoice;
    private int totalInvoices;
    private String currentStep;
    private int operationsCreated;
    private int operationsSkipped;
    private int operationsUpdated;
}
