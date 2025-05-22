package com.olisystem.optionsmanager.service.operation.strategy;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.record.operation.OperationExitContext;

public interface ExitOperationStrategy {
    Operation process(OperationExitContext context);
}
