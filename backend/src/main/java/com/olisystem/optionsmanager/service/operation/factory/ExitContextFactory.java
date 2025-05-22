package com.olisystem.optionsmanager.service.operation.factory;

import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.record.operation.OperationExitContext;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.resolver.transactionType.TransactionTypeResolver;
import com.olisystem.optionsmanager.service.operation.averageOperation.finder.OperationGroupFinder;
import com.olisystem.optionsmanager.service.position.entrylots.EntryLotService;
import com.olisystem.optionsmanager.service.position.finder.PositionFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExitContextFactory {

    private final OperationGroupFinder operationGroupFinder;
    private final PositionFinder positionFinder;
    private final TransactionTypeResolver transactionTypeResolver;
    private final EntryLotService entryLotService;

    /**
     * Cria o contexto de posição para operação de saída
     */
    public OperationExitPositionContext createPositionContext(OperationExitContext context) {
        log.debug("Criando contexto de posição para operação: {}", context.activeOperation().getId());

        // Buscar grupo de operações
        AverageOperationGroup group = operationGroupFinder.findGroupByOperation(context.activeOperation());

        // Buscar posição
        Position position = positionFinder.findPositionById(group.getPositionId());

        // Obter tipo de transação inverso
        TransactionType transactionType = transactionTypeResolver
                .resolveInverseTransactionType(context.activeOperation().getTransactionType());

        // Buscar lotes disponíveis
        List<EntryLot> availableLots = entryLotService.findAvailableLotsByPosition(position);

        return new OperationExitPositionContext(context, group, transactionType, position, availableLots);
    }
}
