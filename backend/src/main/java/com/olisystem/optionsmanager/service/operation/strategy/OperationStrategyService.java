package com.olisystem.optionsmanager.service.operation.strategy;

import com.olisystem.optionsmanager.record.operation.OperationContext;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.record.operation.OperationExitContext;

public interface OperationStrategyService {
    /**
     * Processa uma nova operação
     */
    Operation processNewOperation(OperationContext context);

    /**
     * Processa uma operação em uma série onde já existe operação ativa
     */
    Operation processExistingOperation(OperationContext context, Operation activeOperation);

    /**
     * Processa uma saida parcial de uma operação
     */
    Operation processPartialExitOperation(OperationExitContext context);

    /**
     * Processa uma saida total de uma operação
     */
    Operation processExitOperation(OperationExitContext context);
}
