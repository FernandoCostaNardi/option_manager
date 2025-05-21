package com.olisystem.optionsmanager.service.operation.strategy;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.exception.ResourceNotFoundException;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroupStatus;
import com.olisystem.optionsmanager.model.operation.AverageOperationItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.ExitRecord;
import com.olisystem.optionsmanager.model.position.ExitStrategy;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.position.PositionOperation;
import com.olisystem.optionsmanager.model.position.PositionOperationType;
import com.olisystem.optionsmanager.model.position.PositionStatus;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.record.operation.ExistingOperationContext;
import com.olisystem.optionsmanager.record.operation.OperationContext;
import com.olisystem.optionsmanager.record.operation.OperationExitContext;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.repository.AverageOperationGroupRepository;
import com.olisystem.optionsmanager.repository.AverageOperationItemRepository;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.position.EntryLotRepository;
import com.olisystem.optionsmanager.repository.position.ExitRecordRepository;
import com.olisystem.optionsmanager.repository.position.PositionOperationRepository;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationService;
import com.olisystem.optionsmanager.service.operation.consolidate.OperationConsolidateService;
import com.olisystem.optionsmanager.service.operation.creation.OperationCreationService;
import com.olisystem.optionsmanager.service.position.PositionCalculator;
import com.olisystem.optionsmanager.service.position.PositionExitService;
import com.olisystem.optionsmanager.service.position.PositionService;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class OperationStrategyServiceImpl implements OperationStrategyService {
    private final OperationCreationService creationService;
    private final PositionService positionService;
    private final AverageOperationService averageOperationService;
    private final AverageOperationItemRepository itemRepository;
    private final PositionRepository positionRepository;
    private final OperationConsolidateService consolidateService;
    private final OperationRepository operationRepository;
    private final EntryLotRepository entryLotRepository;
    private final PositionCalculator positionCalculator;
    private final PositionOperationRepository positionOperationRepository;
    private final AverageOperationGroupRepository groupRepository;
    private final ExitRecordRepository exitRecordRepository;

    // Construtor com injeção de dependências
    public OperationStrategyServiceImpl(
            OperationCreationService creationService,
            PositionService positionService,
            AverageOperationService averageOperationService,
            AverageOperationItemRepository itemRepository,
            PositionRepository positionRepository,
            OperationConsolidateService consolidateService,
            OperationRepository operationRepository,
            EntryLotRepository entryLotRepository,
            PositionCalculator positionCalculator,
            PositionOperationRepository positionOperationRepository,
            AverageOperationGroupRepository groupRepository,
            ExitRecordRepository exitRecordRepository) {
        this.creationService = creationService;
        this.positionService = positionService;
        this.averageOperationService = averageOperationService;
        this.itemRepository = itemRepository;
        this.positionRepository = positionRepository;
        this.consolidateService = consolidateService;
        this.operationRepository = operationRepository;
        this.entryLotRepository = entryLotRepository;
        this.positionCalculator = positionCalculator;
        this.positionOperationRepository = positionOperationRepository;
        this.groupRepository = groupRepository;
        this.exitRecordRepository = exitRecordRepository;
    }

    @Override
    @Transactional
    public Operation processNewOperation(OperationContext context) {
        // 1. Criar operação ativa
        Operation savedOperation = creationService.createActiveOperation(
                context.request(), context.optionSerie(), context.currentUser());

        // 2. Criar posição associada
        Position position = positionService.createPositionFromOperation(savedOperation);
        log.info("Posição criada com sucesso: {}", position.getId());

        // 3. Criar grupo de operações
        averageOperationService.createGroupForNewPosition(savedOperation, position);

        return savedOperation;
    }

    @Override
    @Transactional
    public Operation processExistingOperation(OperationContext context, Operation activeOperation) {
        // 1. Recuperar estruturas relacionadas à operação existente
        AverageOperationGroup group = averageOperationService.getGroupByOperation(activeOperation);
        AverageOperationItem itemGroup = itemRepository.findByOperation(activeOperation);
        Position position = positionRepository.findById(group.getPositionId())
                .orElseThrow(() -> new ResourceNotFoundException("Posição não encontrada"));

        // 2. Criar contexto completo
        ExistingOperationContext existingContext = new ExistingOperationContext(
                context.request(), context.optionSerie(), context.currentUser(),
                activeOperation, group, itemGroup, position);

        // 3. Determinar estratégia baseada no tipo do item
        if (itemGroup != null && itemGroup.getRoleType() != OperationRoleType.CONSOLIDATED_ENTRY) {
            return processConsolidationStrategy(existingContext);
        } else {
            return processSimpleAdditionStrategy(existingContext);
        }
    }

    @Override
    @Transactional
    public Operation processPartialExitOperation(OperationExitContext context) {
        return null;
    }

    @Override
    @Transactional
    public Operation processExitOperation(OperationExitContext context) {
 
        // 1. Recuperar estruturas relacionadas à operação existente
         AverageOperationGroup group = averageOperationService.getGroupByOperation(context.activeOperation());
         Position position = positionRepository.findById(group.getPositionId())
                 .orElseThrow(() -> new ResourceNotFoundException("Posição não encontrada"));

        // 2. Atraves do direction de posição saber qual é o transactionType, lembrando que é de saide deve ser ao contrario
        TransactionType transactionType = context.activeOperation().getTransactionType() == TransactionType.BUY ? TransactionType.SELL : TransactionType.BUY;

         // 2. Buscar todos os entry_lot da posição
         List<EntryLot> availableLots =
             entryLotRepository.findByPositionOrderByEntryDateAsc(position).stream()
            .filter(lot -> lot.getRemainingQuantity() > 0)
            .collect(Collectors.toList());

         // 3. criar o contexto com as informações obtidas
         OperationExitPositionContext exitPositionContext = new OperationExitPositionContext(
            context, group, transactionType, position, availableLots);
            
         // 4. Verificar se o entry_lot tem mais de um lote
         if(availableLots.size() > 1){
            // 4.1 Processa saida total com multiplos lotes
            return processExitOperationWithMultipleLots(exitPositionContext);
         } else {
            // 4.1 Processa saída total com um lote
            return processExitOperationWithSingleLot(exitPositionContext);
         }
         
    }

    private Operation processExitOperationWithSingleLot(OperationExitPositionContext exitPositionContext) {
        // 1. Definir variável para o tradeType
        TradeType tradeType = null;
        BigDecimal profitLoss = BigDecimal.ZERO;
        // 2. Descobri se o unico lote tem a mesma data da requisição
        EntryLot lot = exitPositionContext.availableLots().get(0);

            // 2. Define o tradeType da nova operação será DAY
            tradeType = lot.getEntryDate().equals(exitPositionContext.context().request().getExitDate()) ? TradeType.DAY : TradeType.SWING;
            // 3. Calcula o profitLoss
            profitLoss = positionCalculator.calculateProfitLoss(exitPositionContext.context().activeOperation().getEntryUnitPrice(), 
                   exitPositionContext.context().request().getExitUnitPrice(), 
                   exitPositionContext.context().request().getQuantity());
                   //. Calcula o percentual de ganho ou perda
            BigDecimal profitLossPercentage = profitLoss.divide(
                exitPositionContext.context().activeOperation().getEntryTotalValue(), 2, RoundingMode.HALF_UP);

            // 3. Atualizar quantidade restante do lote
            lot.setRemainingQuantity(lot.getRemainingQuantity() - exitPositionContext.context().request().getQuantity());
            lot.setIsFullyConsumed(lot.getRemainingQuantity() == 0);
            entryLotRepository.save(lot);
            // 4. Atualizar a posição
            Position position = exitPositionContext.position();
            position.setTotalRealizedProfit(profitLoss);
            position.setTotalRealizedProfitPercentage(profitLossPercentage);
            position.setRemainingQuantity(position.getRemainingQuantity() - exitPositionContext.context().request().getQuantity());
            position.setStatus(PositionStatus.CLOSED);
            position.setCloseDate(exitPositionContext.context().request().getExitDate());
            log.info("Posição {} fechada. Todas as entradas consumidas.", position.getId());
            // 5. Atualizar a operação ativa para hidden
            Operation activeOperarion = exitPositionContext.context().activeOperation();
            activeOperarion.setStatus(OperationStatus.HIDDEN);
            operationRepository.save(activeOperarion);
            // 6. Criar a requisição para a nova operação
            Operation newExitOperation = creationService.createExitOperation(exitPositionContext, tradeType, profitLoss, exitPositionContext.transactionType());
            // 7. Criar o link de operação
            PositionOperation posOp = PositionOperation.builder()
                .position(position)
                .operation(newExitOperation)
                .type(PositionOperationType.FULL_EXIT)
                .timestamp(exitPositionContext.context().request().getExitDate().atStartOfDay())
                .sequenceNumber(1)
                .build();

            positionOperationRepository.save(posOp);
            // 8. Criar o Exit Record
            ExitRecord exitRecord = ExitRecord.builder()
              .entryLot(lot)
              .exitOperation(newExitOperation)
              .exitDate(exitPositionContext.context().request().getExitDate())
              .quantity(exitPositionContext.context().request().getQuantity())
              .entryUnitPrice(exitPositionContext.context().activeOperation().getEntryUnitPrice())
              .exitUnitPrice(exitPositionContext.context().request().getExitUnitPrice())
              .profitLoss(profitLoss)
              .profitLossPercentage(position.getTotalRealizedProfitPercentage())
              .appliedStrategy(ExitStrategy.LIFO)
              .build();

            exitRecordRepository.save(exitRecord);

            // 8. Criar um item do grupo com a operação newExitOperation
            AverageOperationGroup group = exitPositionContext.group();
            averageOperationService.addNewItemGroup(
                    group, newExitOperation, OperationRoleType.TOTAL_EXIT);
            // 9. Atualizar o grupo de operações
            group.setRemainingQuantity(position.getRemainingQuantity());
            group.setAvgExitPrice(newExitOperation.getExitUnitPrice());
            group.setClosedQuantity(newExitOperation.getQuantity());
            group.setTotalProfit(profitLoss);
            group.setStatus(AverageOperationGroupStatus.CLOSED);
            groupRepository.save(group);
            return newExitOperation;
    }

    private Operation processExitOperationWithMultipleLots(OperationExitPositionContext exitPositionContext) {
        return null;
    }


    @Transactional
    public Operation processConsolidationStrategy(ExistingOperationContext context) {
        // 1. Esconder a operação ativa existente
        context.activeOperation().setStatus(OperationStatus.HIDDEN);
        operationRepository.save(context.activeOperation());

        // 2. Criar nova operação oculta baseada na requisição
        Operation hiddenOperation = creationService.createHiddenOperation(
                context.request(), context.optionSerie(), context.currentUser());
        averageOperationService.addNewItemGroup(context.group(), hiddenOperation, OperationRoleType.NEW_ENTRY);

        // 3. Atualizar a posição
        positionService.addEntryToPosition(context.position(), hiddenOperation);

        // 4. Criar operação consolidada
        Operation consolidatedEntry = creationService.createConsolidatedOperation(
                context.activeOperation(), context.optionSerie(), context.currentUser());
        averageOperationService.addNewItemGroup(
                context.itemGroup().getGroup(), consolidatedEntry, OperationRoleType.CONSOLIDATED_ENTRY);

        // 5. Atualizar valores e estruturas relacionadas
        consolidateService.consolidateOperationEntryValues(consolidatedEntry, context.position());
        averageOperationService.updateAverageOperationGroup(context.group(), context.position());

        return consolidatedEntry;
    }

    @Transactional
    public Operation processSimpleAdditionStrategy(ExistingOperationContext context) {
        // 1. Criar operação oculta para a entrada adicional
        Operation hiddenOperation = creationService.createHiddenOperation(
                context.request(), context.optionSerie(), context.currentUser());
        averageOperationService.addNewItemGroup(context.group(), hiddenOperation, OperationRoleType.NEW_ENTRY);

        // 2. Atualizar posição e estruturas relacionadas
        positionService.addEntryToPosition(context.position(), hiddenOperation);
        consolidateService.consolidateOperationEntryValues(context.activeOperation(), context.position());
        averageOperationService.updateAverageOperationGroup(context.group(), context.position());

        return context.activeOperation();
    }
}
