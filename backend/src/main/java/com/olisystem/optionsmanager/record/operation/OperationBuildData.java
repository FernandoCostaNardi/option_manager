package com.olisystem.optionsmanager.record.operation;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.operation.Operation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OperationBuildData(
        UUID brokerageId,
        UUID analysisHouseId,
        LocalDate entryDate,
        Integer quantity,
        BigDecimal entryUnitPrice
) {
    // Factory method para criar a partir de uma operação existente
    public static OperationBuildData fromOperation(Operation operation) {
        return new OperationBuildData(
                operation.getBrokerage().getId(),
                operation.getAnalysisHouse() != null ? operation.getAnalysisHouse().getId() : null,
                operation.getEntryDate(),
                operation.getQuantity(),
                operation.getEntryUnitPrice()
        );
    }

    // Factory method para criar a partir de um request
    public static OperationBuildData fromRequest(OperationDataRequest request) {
        return new OperationBuildData(
                request.getBrokerageId(),
                request.getAnalysisHouseId(),
                request.getEntryDate(),
                request.getQuantity(),
                request.getEntryUnitPrice()
        );
    }
}
