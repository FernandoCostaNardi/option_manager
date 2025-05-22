package com.olisystem.optionsmanager.resolver.operation;

import com.olisystem.optionsmanager.record.operation.OperationExitContext;
import com.olisystem.optionsmanager.service.operation.strategy.ExitOperationStrategy;
import com.olisystem.optionsmanager.service.operation.strategy.TotalExitOperationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExitOperationStrategyResolver {

    //private final PartialExitOperationStrategy partialExitStrategy;
    private final TotalExitOperationStrategy totalExitStrategy;

    public ExitOperationStrategy resolveStrategy(OperationExitContext context) {
        boolean isPartialExit = context.activeOperation().getQuantity() > context.request().getQuantity();

        if (isPartialExit) {
           // return partialExitStrategy;
            return null;
        } else {
            return totalExitStrategy;
        }
    }
}
