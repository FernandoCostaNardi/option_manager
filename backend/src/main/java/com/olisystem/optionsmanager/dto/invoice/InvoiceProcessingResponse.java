package com.olisystem.optionsmanager.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceProcessingResponse {
    private boolean success;
    private String message;
    private String invoiceId;
    private Long processingTime;
    private int operationsCreated;
    private int operationsUpdated;
    private int operationsSkipped;
} 