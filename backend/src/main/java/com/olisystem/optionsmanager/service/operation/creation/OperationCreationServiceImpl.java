package com.olisystem.optionsmanager.service.operation.creation;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.record.operation.OperationBuildData;
import com.olisystem.optionsmanager.record.operation.OperationBuildExitData;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.service.analysis_house.AnalysisHouseService;
import com.olisystem.optionsmanager.service.brokerage.BrokerageService;
import com.olisystem.optionsmanager.service.operation.target.OperationTargetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class OperationCreationServiceImpl implements OperationCreationService {
    private final BrokerageService brokerageService;
    private final AnalysisHouseService analysisHouseService;
    private final OperationRepository operationRepository;
    private final OperationTargetService targetService;

    // Construtor com injeção de dependências
    public OperationCreationServiceImpl(
            BrokerageService brokerageService,
            AnalysisHouseService analysisHouseService,
            OperationRepository operationRepository,
            OperationTargetService targetService) {
        this.brokerageService = brokerageService;
        this.analysisHouseService = analysisHouseService;
        this.operationRepository = operationRepository;
        this.targetService = targetService;
    }

    @Override
    @Transactional
    public Operation createActiveOperation(OperationDataRequest request, OptionSerie optionSerie, User currentUser) {
        Operation operation = buildOperation(
                OperationBuildData.fromRequest(request),
                optionSerie,
                OperationStatus.ACTIVE,
                currentUser
        );

        Operation savedOperation = operationRepository.save(operation);
        logOperationCreation(OperationStatus.ACTIVE, savedOperation);

        targetService.processOperationTargets(request, savedOperation);

        return savedOperation;
    }

    @Override
    @Transactional
    public Operation createHiddenOperation(OperationDataRequest request, OptionSerie optionSerie, User currentUser) {
        Operation operation = buildOperation(
                OperationBuildData.fromRequest(request),
                optionSerie,
                OperationStatus.HIDDEN,
                currentUser
        );

        Operation savedOperation = operationRepository.save(operation);
        logOperationCreation(OperationStatus.HIDDEN, savedOperation);

        targetService.processOperationTargets(request, savedOperation);

        return savedOperation;
    }

    @Override
    @Transactional
    public Operation createConsolidatedOperation(Operation originalOperation, OptionSerie optionSerie, User currentUser) {
        Operation operation = buildOperation(
                OperationBuildData.fromOperation(originalOperation),
                optionSerie,
                OperationStatus.ACTIVE,
                currentUser
        );

        Operation savedOperation = operationRepository.save(operation);
        logOperationCreation(OperationStatus.ACTIVE, savedOperation);

        return savedOperation;
    }

    @Override
    public Operation createExitOperation(OperationExitPositionContext context, TradeType tradeType, BigDecimal profitLoss, TransactionType type) {
        Operation operation = buildExitOperation(
                OperationBuildExitData.fromRequest(context, profitLoss, tradeType, type),
                context.context().activeOperation().getOptionSeries(),
                profitLoss.compareTo(BigDecimal.ZERO) > 0 ? OperationStatus.WINNER : OperationStatus.LOSER,
                context.context().currentUser()
        );

        Operation savedOperation = operationRepository.save(operation);
        logOperationCreation(savedOperation.getStatus(), savedOperation);
        return savedOperation;
    }

    private Operation buildExitOperation(OperationBuildExitData data, OptionSerie optionSeries, OperationStatus status, User currentUser) {
        var brokerage = brokerageService.getBrokerageById(data.brokerageId());
        var analysisHouse = data.analysisHouseId() != null ?
                analysisHouseService.findById(data.analysisHouseId()).orElse(null) :
                null;

        // 2. Calcular valor total
        BigDecimal entryTotalValue = data.entryUnitPrice()
                .multiply(BigDecimal.valueOf(data.quantity()));

        // 3. Criar a entidade usando Builder
        return Operation.builder()
                .optionSeries(optionSeries)
                .brokerage(brokerage)
                .analysisHouse(analysisHouse)
                .transactionType(data.transactionType())
                .tradeType(data.tradeType())
                .entryDate(data.entryDate())
                .exitDate(data.exitDate())
                .quantity(data.quantity())
                .entryUnitPrice(data.entryUnitPrice())
                .entryTotalValue(entryTotalValue)
                .exitUnitPrice(data.exitUnitPrice())
                .exitTotalValue(data.exitTotalValue())
                .profitLoss(data.profitLoss())
                .profitLossPercentage(data.profitLossPercentage())
                .status(status)
                .user(currentUser)
                .build();
    }
    

    /**
     * Método unificado para construir uma operação, independentemente da fonte de dados
     */
    private Operation buildOperation(OperationBuildData data, OptionSerie optionSerie,
                                     OperationStatus status, User currentUser) {
        // 1. Buscar entidades relacionadas
        var brokerage = brokerageService.getBrokerageById(data.brokerageId());
        var analysisHouse = data.analysisHouseId() != null ?
                analysisHouseService.findById(data.analysisHouseId()).orElse(null) :
                null;

        // 2. Calcular valor total
        BigDecimal entryTotalValue = data.entryUnitPrice()
                .multiply(BigDecimal.valueOf(data.quantity()));

        // 3. Criar a entidade usando Builder
        return Operation.builder()
                .optionSeries(optionSerie)
                .brokerage(brokerage)
                .analysisHouse(analysisHouse)
                .transactionType(TransactionType.BUY)
                .entryDate(data.entryDate())
                .quantity(data.quantity())
                .entryUnitPrice(data.entryUnitPrice())
                .entryTotalValue(entryTotalValue)
                .status(status)
                .user(currentUser)
                .build();
    }

    private void logOperationCreation(OperationStatus status, Operation operation) {
        log.info("Nova operação {} criada: id={}, quantidade={}, preço={}",
                status, operation.getId(), operation.getQuantity(), operation.getEntryUnitPrice());
    }
}

