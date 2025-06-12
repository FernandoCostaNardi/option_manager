package com.olisystem.optionsmanager.service.operation.strategy;

import com.olisystem.optionsmanager.exception.ResourceNotFoundException;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.record.operation.ExistingOperationContext;
import com.olisystem.optionsmanager.record.operation.OperationContext;
import com.olisystem.optionsmanager.repository.AverageOperationItemRepository;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationService;
import com.olisystem.optionsmanager.service.operation.consolidate.OperationConsolidateService;
import com.olisystem.optionsmanager.service.operation.creation.OperationCreationService;
import com.olisystem.optionsmanager.service.position.PositionService;
import lombok.extern.slf4j.Slf4j;
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

    // Construtor com injeção de dependências
    public OperationStrategyServiceImpl(
            OperationCreationService creationService,
            PositionService positionService,
            AverageOperationService averageOperationService,
            AverageOperationItemRepository itemRepository,
            PositionRepository positionRepository,
            OperationConsolidateService consolidateService,
            OperationRepository operationRepository
           ) {
        this.creationService = creationService;
        this.positionService = positionService;
        this.averageOperationService = averageOperationService;
        this.itemRepository = itemRepository;
        this.positionRepository = positionRepository;
        this.consolidateService = consolidateService;
        this.operationRepository = operationRepository;
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

    @Transactional
    public Operation processConsolidationStrategy(ExistingOperationContext context) {
        log.info("=== DEBUG CONSOLIDATION STRATEGY ===");
        log.info("Posição ANTES: Quantidade total={}, Quantidade restante={}, Preço médio={}", 
                 context.position().getTotalQuantity(), context.position().getRemainingQuantity(), context.position().getAveragePrice());
        
        // 1. Esconder a operação ativa existente
        context.activeOperation().setStatus(OperationStatus.HIDDEN);
        operationRepository.save(context.activeOperation());

        // 2. Criar nova operação oculta baseada na requisição
        Operation hiddenOperation = creationService.createHiddenOperation(
                context.request(), context.optionSerie(), context.currentUser());
        averageOperationService.addNewItemGroup(context.group(), hiddenOperation, OperationRoleType.NEW_ENTRY);

        // 3. Atualizar a posição
        log.info("Chamando addEntryToPosition com operação: quantidade={}, preço={}", 
                 hiddenOperation.getQuantity(), hiddenOperation.getEntryUnitPrice());
        positionService.addEntryToPosition(context.position(), hiddenOperation);
        log.info("Posição APÓS addEntryToPosition: Quantidade total={}, Quantidade restante={}, Preço médio={}", 
                 context.position().getTotalQuantity(), context.position().getRemainingQuantity(), context.position().getAveragePrice());

        // 4. Criar operação consolidada
        Operation consolidatedEntry = creationService.createConsolidatedOperation(
                context.activeOperation(), context.optionSerie(), context.currentUser());
        averageOperationService.addNewItemGroup(
                context.itemGroup().getGroup(), consolidatedEntry, OperationRoleType.CONSOLIDATED_ENTRY);

        // 5. Atualizar valores e estruturas relacionadas
        log.info("Chamando consolidateOperationEntryValues...");
        consolidateService.consolidateOperationEntryValues(consolidatedEntry, context.position());
        log.info("Posição APÓS consolidate: Quantidade total={}, Quantidade restante={}, Preço médio={}", 
                 context.position().getTotalQuantity(), context.position().getRemainingQuantity(), context.position().getAveragePrice());
        
        averageOperationService.updateAverageOperationGroup(context.group(), context.position());
        
        log.info("Operação consolidada final: quantidade={}, preço={}, valor total={}", 
                 consolidatedEntry.getQuantity(), consolidatedEntry.getEntryUnitPrice(), consolidatedEntry.getEntryTotalValue());
        log.info("=== FIM CONSOLIDATION STRATEGY ===");

        return consolidatedEntry;
    }

    @Transactional
    public Operation processSimpleAdditionStrategy(ExistingOperationContext context) {
        log.info("=== DEBUG SIMPLE ADDITION STRATEGY ===");
        log.info("Posição ANTES: Quantidade total={}, Quantidade restante={}, Preço médio={}", 
                 context.position().getTotalQuantity(), context.position().getRemainingQuantity(), context.position().getAveragePrice());
        
        // 1. Criar operação oculta para a entrada adicional
        Operation hiddenOperation = creationService.createHiddenOperation(
                context.request(), context.optionSerie(), context.currentUser());
        averageOperationService.addNewItemGroup(context.group(), hiddenOperation, OperationRoleType.NEW_ENTRY);

        // 2. Atualizar posição e estruturas relacionadas
        log.info("Chamando addEntryToPosition com operação: quantidade={}, preço={}", 
                 hiddenOperation.getQuantity(), hiddenOperation.getEntryUnitPrice());
        positionService.addEntryToPosition(context.position(), hiddenOperation);
        log.info("Posição APÓS addEntryToPosition: Quantidade total={}, Quantidade restante={}, Preço médio={}", 
                 context.position().getTotalQuantity(), context.position().getRemainingQuantity(), context.position().getAveragePrice());
        
        log.info("Chamando consolidateOperationEntryValues...");
        consolidateService.consolidateOperationEntryValues(context.activeOperation(), context.position());
        log.info("Posição APÓS consolidate: Quantidade total={}, Quantidade restante={}, Preço médio={}", 
                 context.position().getTotalQuantity(), context.position().getRemainingQuantity(), context.position().getAveragePrice());
        
        averageOperationService.updateAverageOperationGroup(context.group(), context.position());
        
        log.info("Operação ativa final: quantidade={}, preço={}, valor total={}", 
                 context.activeOperation().getQuantity(), context.activeOperation().getEntryUnitPrice(), context.activeOperation().getEntryTotalValue());
        log.info("=== FIM SIMPLE ADDITION STRATEGY ===");

        return context.activeOperation();
    }
}
