package com.olisystem.optionsmanager.service.operation.strategy;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.record.operation.OperationExitContext;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.service.operation.factory.ExitContextFactory;
import com.olisystem.optionsmanager.service.operation.strategy.processor.ExitProcessorSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TotalExitOperationStrategy implements ExitOperationStrategy {

    private final ExitContextFactory contextFactory;
    private final ExitProcessorSelector processorSelector;

    @Override
    @Transactional
    public Operation process(OperationExitContext context) {
        UUID operationId = context.activeOperation().getId();
        log.info("Executando estratégia de saída total para operação: {}", operationId);

        // Preparar contexto de posição
        OperationExitPositionContext positionContext = contextFactory.createPositionContext(context);

        // Selecionar e executar processador apropriado
        Operation result = processorSelector.selectAndProcess(positionContext);

        log.info("Saída total processada com sucesso para operação: {}", operationId);
        return result;
    }
}
