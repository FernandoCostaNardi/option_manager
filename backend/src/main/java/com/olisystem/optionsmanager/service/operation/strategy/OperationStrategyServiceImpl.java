package com.olisystem.optionsmanager.service.operation.strategy;

import com.olisystem.optionsmanager.exception.ResourceNotFoundException;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

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
        // 1. ✅ CORREÇÃO: Criar operação HIDDEN (não ACTIVE)
        Operation savedOperation = creationService.createHiddenOperation(
                context.request(), context.optionSerie(), context.currentUser());

        // 2. ✅ CORREÇÃO: Buscar posição existente antes de criar nova
        Position position = findOrCreatePosition(savedOperation);
        log.info("Posição encontrada/criada: {} (direction: {})", position.getId(), position.getDirection());

        // 3. Buscar grupo existente para a posição
        log.info("🔍 Buscando grupo existente para posição: {}", position.getId());
        AverageOperationGroup group = averageOperationService.getGroupByPosition(position);
        
        if (group == null) {
            // Se não existe, criar novo grupo
            log.info("🆕 Criando novo grupo para posição: {}", position.getId());
            group = averageOperationService.createGroupForNewPosition(savedOperation, position);
            
            // ✅ NOVO: Criar CONSOLIDATED_ENTRY logo após a primeira operação
            log.info("🆕 Criando CONSOLIDATED_ENTRY para primeira operação");
            createOrUpdateConsolidatedEntry(savedOperation, group, position);
        } else {
            // Se já existe, adicionar a operação ao grupo como NEW_ENTRY
            log.info("✅ Reutilizando grupo existente: {} (items: {})", group.getId(), group.getItems().size());
            averageOperationService.addNewItemGroup(group, savedOperation, OperationRoleType.NEW_ENTRY);
            
            // ✅ NOVO: Atualizar CONSOLIDATED_ENTRY existente com nova operação
            log.info("🔄 Atualizando CONSOLIDATED_ENTRY existente");
            createOrUpdateConsolidatedEntry(savedOperation, group, position);
        }

        // 4. ❌ REMOVIDO: Não chamar updateConsolidatedEntryAfterOperation aqui
        // Isso estava criando múltiplas CONSOLIDATED_ENTRY

        return savedOperation;
    }

    /**
     * ✅ CORREÇÃO: Busca posição existente ou cria nova
     * Garante que só existe uma posição por (user, optionSerie, brokerage)
     * A direção da posição é determinada pela primeira operação
     */
    private Position findOrCreatePosition(Operation operation) {
        log.info("🔍 Buscando posição existente para: user={}, optionSeries={}, brokerage={}", 
            operation.getUser().getUsername(), operation.getOptionSeries().getCode(), 
            operation.getBrokerage().getName());
            
        // Buscar posição existente por usuário, série de opções e corretora (sem considerar direção)
        Optional<Position> existingPosition = positionRepository.findOpenPositionByUserAndOptionSeriesAndBrokerage(
                operation.getUser(), 
                operation.getOptionSeries(),
                operation.getBrokerage()
        );

        if (existingPosition.isPresent()) {
            log.info("✅ Reutilizando posição existente: {} (direction: {}, brokerage: {})", 
                existingPosition.get().getId(), existingPosition.get().getDirection(), 
                existingPosition.get().getBrokerage().getName());
            
            // Adicionar operação à posição existente
            return positionService.addEntryToPosition(existingPosition.get(), operation);
        } else {
            log.info("🆕 Criando nova posição para operação: {} (direction: {}, brokerage: {})", 
                operation.getId(), operation.getTransactionType(), operation.getBrokerage().getName());
            
            // Criar nova posição
            return positionService.createPositionFromOperation(operation);
        }
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

        // ✅ CORREÇÃO: Verificar se é operação de venda para consumir posição
        if (context.request().getTransactionType() == TransactionType.SELL) {
            log.info("🔄 Operação de VENDA detectada - consumindo posição existente");
            // Consumir a posição em vez de adicionar
            positionService.consumePositionForExit(context.position(), hiddenOperation);
        } else {
            log.info("📥 Operação de COMPRA detectada - adicionando à posição existente");
            // Adicionar operação à posição existente
            positionService.addEntryToPosition(context.position(), hiddenOperation);
        }
        
        log.info("Posição APÓS processamento: Quantidade total={}, Quantidade restante={}, Preço médio={}", 
                 context.position().getTotalQuantity(), context.position().getRemainingQuantity(), context.position().getAveragePrice());

        // 4. ✅ CORREÇÃO: Criar operação consolidada usando a nova operação (não a ativa que foi marcada como HIDDEN)
        Operation consolidatedEntry = creationService.createConsolidatedOperation(
                hiddenOperation, context.optionSerie(), context.currentUser());
        averageOperationService.addNewItemGroup(
                context.itemGroup().getGroup(), consolidatedEntry, OperationRoleType.CONSOLIDATED_ENTRY);

        // 5. Atualizar valores e estruturas relacionadas
        log.info("Chamando consolidateOperationEntryValues...");
        consolidateService.consolidateOperationEntryValues(consolidatedEntry, context.position());
        log.info("Posição APÓS consolidate: Quantidade total={}, Quantidade restante={}, Preço médio={}", 
                 context.position().getTotalQuantity(), context.position().getRemainingQuantity(), context.position().getAveragePrice());
        
        averageOperationService.updateAverageOperationGroup(context.group(), context.position());
        
        // ❌ REMOVIDO: Não chamar updateConsolidatedEntryAfterOperation aqui
        // Isso estava criando múltiplas CONSOLIDATED_ENTRY
        
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
        
        // ✅ NOVO: Atualizar CONSOLIDATED_ENTRY com nova operação
        log.info("🔄 Atualizando CONSOLIDATED_ENTRY com nova operação");
        createOrUpdateConsolidatedEntry(hiddenOperation, context.group(), context.position());
        
        log.info("Operação ativa final: quantidade={}, preço={}, valor total={}", 
                 context.activeOperation().getQuantity(), context.activeOperation().getEntryUnitPrice(), context.activeOperation().getEntryTotalValue());
        log.info("=== FIM SIMPLE ADDITION STRATEGY ===");

        return context.activeOperation();
    }

    /**
     * ✅ CORREÇÃO: Buscar CONSOLIDATED_ENTRY existente no grupo usando repositório
     */
    private Optional<Operation> findExistingConsolidatedEntry(AverageOperationGroup group) {
        log.debug("🔍 Buscando CONSOLIDATED_ENTRY no grupo: {}", group.getId());
        
        // Busca diretamente no repositório para evitar problemas de lazy loading
        return itemRepository.findByGroupAndRoleType(group, OperationRoleType.CONSOLIDATED_ENTRY)
                .stream()
                .map(AverageOperationItem::getOperation)
                .findFirst();
    }

    /**
     * ✅ NOVO MÉTODO: Criar ou atualizar CONSOLIDATED_ENTRY (garante unicidade)
     */
    private void createOrUpdateConsolidatedEntry(Operation operation, AverageOperationGroup group, Position position) {
        log.info("🔍 Iniciando createOrUpdateConsolidatedEntry para grupo: {} e operação: {}", group.getId(), operation.getId());
        
        // Buscar CONSOLIDATED_ENTRY existente no grupo
        Optional<Operation> existingConsolidatedEntry = findExistingConsolidatedEntry(group);
        
        if (existingConsolidatedEntry.isPresent()) {
            // Atualizar a operação consolidada existente
            Operation consolidated = existingConsolidatedEntry.get();
            log.info("🔄 FOUND! Atualizando CONSOLIDATED_ENTRY existente: {} (status atual: {})", 
                consolidated.getId(), consolidated.getStatus());
            
            // Atualiza quantidade, preço médio e valor total conforme a posição consolidada
            BigDecimal oldQuantity = BigDecimal.valueOf(consolidated.getQuantity());
            BigDecimal oldPrice = consolidated.getEntryUnitPrice();
            
            consolidated.setQuantity(position.getTotalQuantity());
            consolidated.setEntryUnitPrice(position.getAveragePrice());
            consolidated.setEntryTotalValue(position.getAveragePrice().multiply(BigDecimal.valueOf(position.getTotalQuantity())));
            consolidated.setStatus(OperationStatus.ACTIVE); // Mantém ACTIVE até saída total
            operationRepository.save(consolidated);
            
            log.info("✅ CONSOLIDATED_ENTRY atualizada: {} -> {} quantity, {} -> {} price", 
                oldQuantity, consolidated.getQuantity(), oldPrice, consolidated.getEntryUnitPrice());
        } else {
            log.info("❌ NOT FOUND! Nenhuma CONSOLIDATED_ENTRY encontrada no grupo: {}", group.getId());
            log.info("🆕 Criando NOVA CONSOLIDATED_ENTRY para operação: {}", operation.getId());
            
            Operation consolidatedEntry = Operation.builder()
                    .optionSeries(operation.getOptionSeries())
                    .brokerage(operation.getBrokerage())
                    .analysisHouse(operation.getAnalysisHouse())
                    .transactionType(TransactionType.BUY)
                    .tradeType(operation.getTradeType())
                    .entryDate(operation.getEntryDate())
                    .exitDate(null)
                    .quantity(position.getTotalQuantity())
                    .entryUnitPrice(position.getAveragePrice())
                    .entryTotalValue(position.getAveragePrice().multiply(BigDecimal.valueOf(position.getTotalQuantity())))
                    .exitUnitPrice(null)
                    .exitTotalValue(null)
                    .profitLoss(java.math.BigDecimal.ZERO)
                    .profitLossPercentage(java.math.BigDecimal.ZERO)
                    .status(OperationStatus.ACTIVE)
                    .user(operation.getUser())
                    .build();
                    
            Operation saved = operationRepository.save(consolidatedEntry);
            log.info("✅ CONSOLIDATED_ENTRY criada com ID: {} (quantidade: {}, preço: {})", 
                saved.getId(), saved.getQuantity(), saved.getEntryUnitPrice());
            
            // Só adiciona ao grupo se não existe
            averageOperationService.addNewItemGroup(group, saved, OperationRoleType.CONSOLIDATED_ENTRY);
            log.info("✅ CONSOLIDATED_ENTRY adicionada ao grupo: {}", group.getId());
        }
        
        // Atualiza a quantidade do grupo
        log.info("🔄 Atualizando quantidade do grupo: {}", group.getId());
        averageOperationService.updateAverageOperationGroup(group, position);
    }
}
