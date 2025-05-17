package com.olisystem.optionsmanager.service.operation.strategy;

import com.olisystem.optionsmanager.record.operation.OperationContext;
import com.olisystem.optionsmanager.model.operation.Operation;

public interface OperationStrategyService {
    /**
     * Processa uma nova operação
     */
    Operation processNewOperation(OperationContext context);

    /**
     * Processa uma operação em uma série onde já existe operação ativa
     */
    Operation processExistingOperation(OperationContext context, Operation activeOperation);
}
