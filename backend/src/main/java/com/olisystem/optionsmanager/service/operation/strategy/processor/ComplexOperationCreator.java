package com.olisystem.optionsmanager.service.operation.strategy.processor;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.position.PositionOperationType;
import com.olisystem.optionsmanager.record.consumption.ComplexConsumptionResult;
import com.olisystem.optionsmanager.record.consumption.TradeOperationData;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationService;
import com.olisystem.optionsmanager.service.operation.creation.OperationCreationService;
import com.olisystem.optionsmanager.service.operation.engine.TradeDataCalculator;
import com.olisystem.optionsmanager.service.operation.exitRecord.ExitRecordService;
import com.olisystem.optionsmanager.service.operation.status.OperationStatusService;
import com.olisystem.optionsmanager.service.position.entrylots.EntryLotUpdateService;
import com.olisystem.optionsmanager.service.position.positionOperation.PositionOperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplexOperationCreator {

    private final OperationCreationService operationCreationService;
    private final TradeDataCalculator tradeDataCalculator;
    private final OperationStatusService operationStatusService;
    private final AverageOperationService averageOperationService;
    private final PositionOperationService positionOperationService;
    private final ExitRecordService exitRecordService;
    private final EntryLotUpdateService entryLotUpdateService;

    @Transactional
    public Operation createComplexExitOperations(OperationExitPositionContext context, 
                                               ComplexConsumptionResult result) {
        
        log.info("=== CRIANDO OPERAÇÕES DE SAÍDA COMPLEXAS ===");
        List<Operation> exitOperations = new ArrayList<>();
        hideOriginalOperation(context);

        if (result.hasMixedTradeTypes()) {
            exitOperations.addAll(createMixedTradeOperations(context, result));
        } else {
            exitOperations.add(createSingleTradeTypeOperation(context, result));
        }

        createTraceabilityRecords(context, result, exitOperations);
        updateConsumedLots(result);
        
        log.info("Operações de saída criadas: {}", exitOperations.size());
        return exitOperations.get(0);
    }

    private void hideOriginalOperation(OperationExitPositionContext context) {
        operationStatusService.updateOperationStatus(
            context.context().activeOperation(), 
            OperationStatus.HIDDEN
        );
        log.debug("Operação original marcada como HIDDEN");
    }

    private List<Operation> createMixedTradeOperations(OperationExitPositionContext context, 
                                                     ComplexConsumptionResult result) {
        
        List<Operation> operations = new ArrayList<>();
        
        // Criar operação Day Trade se houver
        TradeOperationData dayTradeData = tradeDataCalculator.calculateDayTradeData(result);
        if (dayTradeData.hasValidData()) {
            Operation dayTradeOp = createOperationFromTradeData(context, dayTradeData);
            operations.add(dayTradeOp);
            
            averageOperationService.addNewItemGroup(
                context.group(), 
                dayTradeOp, 
                OperationRoleType.TOTAL_EXIT
            );
        }
        
        // Criar operação Swing Trade se houver
        TradeOperationData swingTradeData = tradeDataCalculator.calculateSwingTradeData(result);
        if (swingTradeData.hasValidData()) {
            Operation swingTradeOp = createOperationFromTradeData(context, swingTradeData);
            operations.add(swingTradeOp);
            
            averageOperationService.addNewItemGroup(
                context.group(), 
                swingTradeOp, 
                OperationRoleType.TOTAL_EXIT
            );
        }
        
        log.info("Criadas {} operações para trades mistos", operations.size());
        return operations;
    }

    private Operation createSingleTradeTypeOperation(OperationExitPositionContext context, 
                                                   ComplexConsumptionResult result) {
        
        TradeType tradeType = result.dayTradeQuantity() > 0 ? TradeType.DAY : TradeType.SWING;
        TradeOperationData tradeData = tradeType == TradeType.DAY ? 
            tradeDataCalculator.calculateDayTradeData(result) :
            tradeDataCalculator.calculateSwingTradeData(result);
            
        Operation operation = createOperationFromTradeData(context, tradeData);
        
        averageOperationService.addNewItemGroup(
            context.group(), 
            operation, 
            OperationRoleType.TOTAL_EXIT
        );
        
        log.info("Criada operação única do tipo {}", tradeType);
        return operation;
    }

    private Operation createOperationFromTradeData(OperationExitPositionContext context, 
                                                 TradeOperationData tradeData) {
        
        return operationCreationService.createExitOperationWithSpecificData(
            context.context().activeOperation(),
            context.context().activeOperation().getOptionSeries(),
            context.context().currentUser(),
            tradeData.quantity(),
            tradeData.weightedAverageEntryPrice(),
            context.context().request().getExitUnitPrice(),
            tradeData.totalProfitLoss(),
            tradeData.totalProfitLossPercentage(),
            tradeData.tradeType(),
            context.context().request().getExitDate()
        );
    }

    private void createTraceabilityRecords(OperationExitPositionContext context, 
                                         ComplexConsumptionResult result,
                                         List<Operation> exitOperations) {
        
        log.debug("Criando registros de rastreabilidade");
        
        // Criar ExitRecords para cada lote consumido
        result.results().forEach(lotResult -> {
            exitRecordService.createExitRecord(
                lotResult.lot(), 
                exitOperations.get(0), // Primeira operação como referência
                context.context(),
                lotResult.quantityConsumed()
            );
        });
        
       // Criar PositionOperations para cada operação de saída
      exitOperations.forEach(operation -> {
        positionOperationService.createPositionOperation(
        context.position(),
        operation,
        context.context().request(),  // Adicionando o request
        PositionOperationType.PARTIAL_EXIT  // Adicionando o tipo de operação
    );
});
        
        log.debug("Registros de rastreabilidade criados: {} ExitRecords, {} PositionOperations", 
                result.results().size(), exitOperations.size());
    }

    private void updateConsumedLots(ComplexConsumptionResult result) {
        log.debug("Atualizando lotes consumidos");
        
        result.results().forEach(lotResult -> {
            entryLotUpdateService.updateEntryLot(
                lotResult.lot(), 
                lotResult.quantityConsumed()
            );
        });
        
        log.debug("Atualizados {} lotes", result.results().size());
    }
}
