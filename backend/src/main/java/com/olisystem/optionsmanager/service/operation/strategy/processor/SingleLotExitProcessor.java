package com.olisystem.optionsmanager.service.operation.strategy.processor;

import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.PositionOperationType;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.resolver.tradeType.TradeTypeResolver;
import com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationGroupService;
import com.olisystem.optionsmanager.service.operation.creation.OperationCreationService;
import com.olisystem.optionsmanager.service.operation.exitRecord.ExitRecordService;
import com.olisystem.optionsmanager.service.operation.profit.ProfitCalculationService;
import com.olisystem.optionsmanager.service.operation.status.OperationStatusService;
import com.olisystem.optionsmanager.service.position.entrylots.EntryLotUpdateService;
import com.olisystem.optionsmanager.service.position.positionOperation.PositionOperationService;
import com.olisystem.optionsmanager.service.position.update.PositionUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class SingleLotExitProcessor {

    private final TradeTypeResolver tradeTypeResolver;
    private final ProfitCalculationService profitCalculationService;
    private final EntryLotUpdateService entryLotUpdateService;
    private final PositionUpdateService positionUpdateService;
    private final OperationStatusService operationStatusService;
    private final ExitRecordService exitRecordService;
    private final PositionOperationService positionOperationService;
    private final AverageOperationGroupService averageOperationGroupService;
    private final OperationCreationService operationCreationService;

    /**
     * Processa a saída de operação com lote único
     */
    @Transactional
    public Operation process(OperationExitPositionContext context) {
        log.info("Processando saída com lote único para operação: {}",
                context.context().activeOperation().getId());

        // Obter lote de entrada
        EntryLot lot = context.availableLots().get(0);
        OperationFinalizationRequest request = context.context().request();
        Operation activeOperation = context.context().activeOperation();

        // Determinar tipo de operação
        TradeType tradeType = tradeTypeResolver.determineTradeType(
                lot.getEntryDate(), request.getExitDate());

        // Calcular resultados financeiros
        BigDecimal profitLoss = profitCalculationService.calculateProfitLoss(
                activeOperation.getEntryUnitPrice(),
                request.getExitUnitPrice(),
                request.getQuantity());

        BigDecimal profitLossPercentage = profitCalculationService.calculateProfitLossPercentage(
                profitLoss, activeOperation.getEntryTotalValue());

        // Atualizar entidades
        entryLotUpdateService.updateEntryLot(lot, request.getQuantity());
        positionUpdateService.updatePosition(context.position(), request, profitLoss, profitLossPercentage);
        operationStatusService.updateOperationStatus(activeOperation, OperationStatus.HIDDEN);

        // Criar nova operação de saída
        Operation exitOperation = operationCreationService.createExitOperation(
                context, tradeType, profitLoss, context.transactionType());

        // Criar registros relacionados
        positionOperationService.createPositionOperation(
                context.position(), exitOperation, request, PositionOperationType.FULL_EXIT);

        exitRecordService.createExitRecord(
                lot, exitOperation, context.context(), profitLoss, profitLossPercentage);

        averageOperationGroupService.updateOperationGroup(
                context.group(), context.position(), exitOperation, profitLoss);

        return exitOperation;
    }
}
