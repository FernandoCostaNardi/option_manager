package com.olisystem.optionsmanager.service.operation.strategy.processor;

import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.exception.BusinessException;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroupStatus;
import com.olisystem.optionsmanager.model.operation.AverageOperationItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.PositionOperationType;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.repository.AverageOperationGroupRepository;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import com.olisystem.optionsmanager.resolver.tradeType.TradeTypeResolver;
import com.olisystem.optionsmanager.service.operation.average.AveragePriceCalculator;
import com.olisystem.optionsmanager.service.operation.consolidate.ConsolidatedOperationService;
import com.olisystem.optionsmanager.service.operation.creation.OperationCreationService;
import com.olisystem.optionsmanager.service.operation.detector.PartialExitDetector;
import com.olisystem.optionsmanager.service.operation.exitRecord.ExitRecordService;
import com.olisystem.optionsmanager.service.operation.profit.ProfitCalculationService;
import com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationService;
import com.olisystem.optionsmanager.service.position.entrylots.EntryLotUpdateService;
import com.olisystem.optionsmanager.service.position.positionOperation.PositionOperationService;
import com.olisystem.optionsmanager.service.position.update.PositionUpdateService;
import com.olisystem.optionsmanager.record.consumption.LotConsumption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartialExitProcessor {

    private final PartialExitDetector partialExitDetector;
    private final TradeTypeResolver tradeTypeResolver;
    private final ProfitCalculationService profitCalculationService;
    private final AveragePriceCalculator averagePriceCalculator;
    private final ConsolidatedOperationService consolidatedOperationService;
    private final OperationCreationService operationCreationService;
    private final EntryLotUpdateService entryLotUpdateService;
    private final PositionOperationService positionOperationService;
    private final ExitRecordService exitRecordService;
    private final PositionUpdateService positionUpdateService;
    private final AverageOperationGroupRepository groupRepository;
    private final AverageOperationService averageOperationService;
    private final PositionRepository positionRepository;
    private final OperationRepository operationRepository;

    /**
     * Processa saída parcial com lote único - Implementa os 20 passos do Cenário 3.1
     */
    @Transactional
    public Operation process(OperationExitPositionContext context) {

        log.info("=== INICIANDO PROCESSAMENTO DE SAÍDA PARCIAL - CENÁRIO 3.1 ===");
        log.info("Operação ID: {}, Posição ID: {}, Quantidade solicitada: {}",
                context.context().activeOperation().getId(),
                context.position().getId(),
                context.context().request().getQuantity());

        try {
            // Validações iniciais
            validatePartialExitContext(context);

            // Determinar tipo de saída
            PartialExitDetector.ExitType exitType = partialExitDetector.determineExitType(
                    context.position(), context.context().request().getQuantity());

            partialExitDetector.logExitTypeDetails(exitType, context.position(),
                    context.context().request().getQuantity());

            // Processar baseado no tipo detectado
            switch (exitType) {
                case FIRST_PARTIAL_EXIT:
                    return processFirstPartialExit(context);

                case SUBSEQUENT_PARTIAL_EXIT:
                    return processSubsequentPartialExit(context);

                case FINAL_PARTIAL_EXIT:
                    return processFinalPartialExit(context);

                case SINGLE_TOTAL_EXIT:
                    // Este caso deveria ser tratado pelo SingleLotExitProcessor
                    log.warn("Saída total única detectada no PartialExitProcessor - delegando para SingleLotExitProcessor");
                    throw new BusinessException("Saída total única deve ser processada pelo SingleLotExitProcessor");

                default:
                    throw new BusinessException("Tipo de saída não suportado: " + exitType);
            }

        } catch (Exception e) {
            log.error("Erro durante processamento de saída parcial para operação {}: {}",
                    context.context().activeOperation().getId(), e.getMessage(), e);
            throw new BusinessException("Falha no processamento da saída parcial: " + e.getMessage());
        }
    }

    // ======================================================================================
    // FASE 1: PRIMEIRA SAÍDA PARCIAL (Passos 1-8)
    // ======================================================================================

    /**
     * PASSOS 1-8: Processa a primeira saída parcial
     */
    @Transactional
    public Operation processFirstPartialExit(OperationExitPositionContext context) {

        log.info("=== PROCESSANDO PRIMEIRA SAÍDA PARCIAL ===");

        // PASSO 2: Processar saída normalmente
        ExitOperationResult exitResult = processNormalExit(context);

        // ✅ CORREÇÃO: Garantir que a operação de saída tenha os valores corretos antes de criar a consolidada
        if (exitResult.exitOperation.getProfitLoss() == null ||
                exitResult.exitOperation.getProfitLoss().compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Corrigindo valores zerados na operação de saída antes de criar consolidada");
            exitResult.exitOperation.setProfitLoss(exitResult.profitLoss);
            exitResult.exitOperation.setProfitLossPercentage(exitResult.profitLossPercentage);
            // ✅ CORREÇÃO: Usar ConsolidatedOperationService para atualizar operação
            consolidatedOperationService.updateOperationValues(exitResult.exitOperation, 
                    exitResult.profitLoss, exitResult.profitLossPercentage);
        }

        // ✅ CORREÇÃO CRÍTICA: Verificar se já existe CONSOLIDATED_ENTRY antes de criar
        // PASSO 4: Criar ou buscar operação consolidadora de entrada
        log.info("🔧 Verificando se CONSOLIDATED_ENTRY já existe");
        Optional<Operation> existingConsolidatedEntry = consolidatedOperationService.findExistingConsolidatedEntry(context.group());
        
        Operation consolidatedEntry;
        if (existingConsolidatedEntry.isPresent()) {
            log.info("🔧 CONSOLIDATED_ENTRY já existe, usando: {}", existingConsolidatedEntry.get().getId());
            consolidatedEntry = existingConsolidatedEntry.get();
        } else {
            log.info("🔧 Criando nova CONSOLIDATED_ENTRY");
            consolidatedEntry = consolidatedOperationService.createConsolidatedEntry(
                    context.context().activeOperation(), context.group());
        }

        // PASSO 3: Gerenciar operação consolidadora de saída
        Operation consolidatedExit;
        Optional<Operation> existingConsolidatedResult = consolidatedOperationService.findExistingConsolidatedResult(context.group());
        
        if (existingConsolidatedResult.isPresent()) {
            // Atualizar CONSOLIDATED_RESULT existente (passos 6, 10)
            log.info("Atualizando CONSOLIDATED_RESULT existente");
            consolidatedExit = consolidatedOperationService.updateConsolidatedResult(
                    existingConsolidatedResult.get(), exitResult.exitOperation, context.group());
        } else {
            // Criar primeira CONSOLIDATED_RESULT (passo 4)
            log.info("Criando primeira CONSOLIDATED_RESULT");
            consolidatedExit = consolidatedOperationService.createConsolidatedExit(
                    exitResult.exitOperation, context.group());
        }

        // PASSO NOVO: Adicionar operação de saída como PARTIAL_EXIT no grupo (passos 3, 5)
        averageOperationService.addNewItemGroup(context.group(), exitResult.exitOperation, 
                com.olisystem.optionsmanager.model.operation.OperationRoleType.PARTIAL_EXIT);
        log.info("Operação de saída adicionada ao grupo como PARTIAL_EXIT: {}", exitResult.exitOperation.getId());

        // PASSO 5 & 6: Calcular e atualizar novo preço médio
        updateConsolidatedEntryAfterExit(consolidatedEntry, exitResult, context);

        // PASSO 7: Marcar operações originais como HIDDEN (mas NÃO a CONSOLIDATED_ENTRY)
        consolidatedOperationService.markOperationAsHidden(context.context().activeOperation());
        consolidatedOperationService.markOperationAsHidden(exitResult.exitOperation);
        
        // ✅ CORREÇÃO: CONSOLIDATED_ENTRY deve permanecer ACTIVE até a saída final
        log.info("✅ CONSOLIDATED_ENTRY mantida como ACTIVE para futuras saídas: {}", consolidatedEntry.getId());
        
        // 🔧 DEBUG: Verificar se a operação realmente está ACTIVE
        Operation refreshedEntry = operationRepository.findById(consolidatedEntry.getId()).orElse(null);
        if (refreshedEntry != null) {
            log.info("🔧 DEBUG: Status atual da CONSOLIDATED_ENTRY: {}", refreshedEntry.getStatus());
        }

        // PASSO 8: Atualizar entidades relacionadas
        updateEntitiesAfterFirstExit(context, exitResult);

        log.info("=== PRIMEIRA SAÍDA PARCIAL PROCESSADA COM SUCESSO ===");

        return consolidatedExit;
    }

    // ======================================================================================
    // FASE 2: SAÍDAS PARCIAIS SUBSEQUENTES (Passos 9-14)
    // ======================================================================================

    /**
     * PASSOS 9-14: Processa saídas parciais subsequentes
     */
    @Transactional
    public Operation processSubsequentPartialExit(OperationExitPositionContext context) {

        log.info("=== PROCESSANDO SAÍDA PARCIAL SUBSEQUENTE ===");

        // PASSO 10: Processar saída baseada no custo original
        ExitOperationResult exitResult = processNormalExit(context);

        // ✅ CORREÇÃO: Garantir que a operação de saída tenha os valores corretos
        if (exitResult.exitOperation.getProfitLoss() == null ||
                exitResult.exitOperation.getProfitLoss().compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Corrigindo valores zerados na operação de saída subsequente");
            exitResult.exitOperation.setProfitLoss(exitResult.profitLoss);
            exitResult.exitOperation.setProfitLossPercentage(exitResult.profitLossPercentage);
            // ✅ CORREÇÃO: Usar ConsolidatedOperationService para atualizar operação
            consolidatedOperationService.updateOperationValues(exitResult.exitOperation, 
                    exitResult.profitLoss, exitResult.profitLossPercentage);
        }

        // ✅ CORREÇÃO: Buscar operações consolidadoras usando métodos corretos
        Optional<Operation> consolidatedEntryOpt = consolidatedOperationService.findExistingConsolidatedEntry(context.group());
        Optional<Operation> consolidatedResultOpt = consolidatedOperationService.findExistingConsolidatedResult(context.group());

        // ✅ CORREÇÃO: CONSOLIDATED_ENTRY é obrigatória, CONSOLIDATED_RESULT pode ser criada se não existir
        if (consolidatedEntryOpt.isEmpty()) {
            log.error("❌ CONSOLIDATED_ENTRY não encontrada para saída subsequente");
            throw new BusinessException("Operação consolidadora de entrada não encontrada para saída subsequente");
        }

        Operation consolidatedEntry = consolidatedEntryOpt.get();
        Operation consolidatedExit;
        
        if (consolidatedResultOpt.isPresent()) {
            // ✅ CONSOLIDATED_RESULT existe - usar ela
            consolidatedExit = consolidatedResultOpt.get();
            log.info("✅ CONSOLIDATED_RESULT encontrada: {}", consolidatedExit.getId());
        } else {
            // ✅ CONSOLIDATED_RESULT não existe - criar nova
            log.info("🔧 CONSOLIDATED_RESULT não encontrada - criando nova");
            consolidatedExit = consolidatedOperationService.createConsolidatedExit(
                    exitResult.exitOperation, context.group());
            log.info("✅ Nova CONSOLIDATED_RESULT criada: {}", consolidatedExit.getId());
        }
        
        log.info("✅ Operações consolidadoras configuradas:");
        log.info("✅ CONSOLIDATED_ENTRY: {}", consolidatedEntry.getId());
        log.info("✅ CONSOLIDATED_RESULT: {}", consolidatedExit.getId());

        // ✅ CORREÇÃO CRÍTICA: Adicionar operação de saída como PARTIAL_EXIT no grupo
        averageOperationService.addNewItemGroup(context.group(), exitResult.exitOperation, 
                com.olisystem.optionsmanager.model.operation.OperationRoleType.PARTIAL_EXIT);
        log.info("✅ Operação de saída SUBSEQUENTE adicionada ao grupo como PARTIAL_EXIT: {}", exitResult.exitOperation.getId());

        // PASSO 11: Atualizar operação consolidadora de saída
        consolidatedOperationService.updateConsolidatedExit(
                consolidatedExit,
                exitResult.profitLoss,
                exitResult.quantity,
                context.context().request().getExitDate(),
                context.context().request().getExitUnitPrice(),
                exitResult.exitOperation.getEntryTotalValue());

        // PASSO 12 & 13: Calcular e atualizar novo preço médio
        updateConsolidatedEntryAfterExit(consolidatedEntry, exitResult, context);

        // Marcar operação de saída como HIDDEN
        consolidatedOperationService.markOperationAsHidden(exitResult.exitOperation);

        // PASSO 14: Atualizar entidades relacionadas
        updateEntitiesAfterSubsequentExit(context, exitResult);

        log.info("=== SAÍDA PARCIAL SUBSEQUENTE PROCESSADA COM SUCESSO ===");

        return consolidatedExit;
    }

    // ======================================================================================
    // FASE 3: SAÍDA FINAL (Passos 15-18)
    // ======================================================================================

    /**
     * PASSOS 15-18: Processa a saída final
     */
    @Transactional
    public Operation processFinalPartialExit(OperationExitPositionContext context) {

        log.info("=== PROCESSANDO SAÍDA FINAL ===");

        // PASSO 16: Processar saída final baseada no custo original
        ExitOperationResult exitResult = processNormalExit(context);

        // ✅ CORREÇÃO: Garantir que a operação de saída tenha os valores corretos
        if (exitResult.exitOperation.getProfitLoss() == null ||
                exitResult.exitOperation.getProfitLoss().compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Corrigindo valores zerados na operação de saída final");
            exitResult.exitOperation.setProfitLoss(exitResult.profitLoss);
            exitResult.exitOperation.setProfitLossPercentage(exitResult.profitLossPercentage);
            // ✅ CORREÇÃO: Usar ConsolidatedOperationService para atualizar operação
            consolidatedOperationService.updateOperationValues(exitResult.exitOperation, 
                    exitResult.profitLoss, exitResult.profitLossPercentage);
        }

        // 🔧 CORREÇÃO: Adicionar lógica de atualização da operação consolidada (igual aos outros métodos)
        Operation consolidatedExit;
        Optional<Operation> existingConsolidatedResult = consolidatedOperationService.findExistingConsolidatedResult(context.group());
        
        if (existingConsolidatedResult.isPresent()) {
            // Atualizar CONSOLIDATED_RESULT existente
            log.info("🔧 FINAL_PARTIAL_EXIT: Atualizando CONSOLIDATED_RESULT existente");
            consolidatedExit = consolidatedOperationService.updateConsolidatedResult(
                    existingConsolidatedResult.get(), exitResult.exitOperation, context.group());
        } else {
            // Não deveria acontecer no cenário 3.3, mas tratando como segurança
            log.warn("🔧 FINAL_PARTIAL_EXIT: CONSOLIDATED_RESULT não encontrada - criando nova");
            consolidatedExit = consolidatedOperationService.createConsolidatedExit(
                    exitResult.exitOperation, context.group());
        }

        // PASSO NOVO: Adicionar operação de saída como TOTAL_EXIT no grupo
        averageOperationService.addNewItemGroup(context.group(), exitResult.exitOperation, 
                com.olisystem.optionsmanager.model.operation.OperationRoleType.TOTAL_EXIT);
        log.info("✅ Operação de saída FINAL adicionada ao grupo como TOTAL_EXIT: {}", exitResult.exitOperation.getId());

        // ✅ CORREÇÃO: Buscar operação consolidadora de entrada com logs detalhados
        log.info("🔍 Buscando CONSOLIDATED_ENTRY no grupo: {}", context.group().getId());
        
        // ✅ NOVA CORREÇÃO: Usar método específico do repositório
        Optional<Operation> consolidatedEntryOpt = consolidatedOperationService.findExistingConsolidatedEntry(context.group());
        
        Operation consolidatedEntry;
        if (consolidatedEntryOpt.isPresent()) {
            consolidatedEntry = consolidatedEntryOpt.get();
            log.info("✅ CONSOLIDATED_ENTRY encontrada via repositório: {}", consolidatedEntry.getId());
        } else {
            log.error("❌ CONSOLIDATED_ENTRY não encontrada no grupo: {}", context.group().getId());
            log.error("❌ Items do grupo: {}", context.group().getItems());
            
            // ✅ CORREÇÃO: Tentar buscar diretamente no repositório
            List<AverageOperationItem> allItems = groupRepository.findById(context.group().getId())
                    .map(group -> group.getItems())
                    .orElse(new ArrayList<>());
            
            log.info("🔍 Items encontrados diretamente: {}", allItems.size());
            
            // ✅ NOVA CORREÇÃO: Log detalhado de todos os items
            for (AverageOperationItem item : allItems) {
                log.info("🔍 Item: RoleType={}, OperationId={}, Status={}", 
                    item.getRoleType(), item.getOperation().getId(), item.getOperation().getStatus());
            }
            
            Optional<AverageOperationItem> entryItem = allItems.stream()
                    .filter(item -> item.getRoleType() == OperationRoleType.CONSOLIDATED_ENTRY)
                    .findFirst();
            
            if (entryItem.isPresent()) {
                consolidatedEntry = entryItem.get().getOperation();
                log.info("✅ CONSOLIDATED_ENTRY encontrada via busca direta: {}", consolidatedEntry.getId());
            } else {
                log.error("❌ CONSOLIDATED_ENTRY não encontrada mesmo via busca direta");
                throw new BusinessException("Operação consolidadora de entrada não encontrada para saída final");
            }
        }

        if (consolidatedExit == null) {
            log.error("❌ CONSOLIDATED_EXIT é null");
            throw new BusinessException("Operação consolidadora de saída não encontrada para saída final");
        }

        // PASSO 18: Finalizar operação consolidadora de entrada (quantidade = 0)
        consolidatedOperationService.updateConsolidatedEntry(
                consolidatedEntry,
                BigDecimal.ZERO, // Preço médio irrelevante quando quantidade = 0
                0, // Quantidade final = 0
                BigDecimal.ZERO); // Valor total final = 0

        // ✅ CORREÇÃO: Para saída final, a operação TOTAL_EXIT deve ficar HIDDEN
        // mas a CONSOLIDATED_RESULT deve ficar WINNER/LOSER
        consolidatedOperationService.markOperationAsHidden(exitResult.exitOperation);
        
        // ✅ CORREÇÃO: Marcar CONSOLIDATED_RESULT com status apropriado baseado no profitLoss consolidado
        log.info("🔧 DEBUG: ProfitLoss consolidado: {}, ProfitLoss da saída: {}", 
                consolidatedExit.getProfitLoss(), exitResult.profitLoss);
        
        if (consolidatedExit.getProfitLoss().compareTo(BigDecimal.ZERO) > 0) {
            consolidatedExit.setStatus(com.olisystem.optionsmanager.model.operation.OperationStatus.WINNER);
        } else if (consolidatedExit.getProfitLoss().compareTo(BigDecimal.ZERO) < 0) {
            consolidatedExit.setStatus(com.olisystem.optionsmanager.model.operation.OperationStatus.LOSER);
        } else {
            consolidatedExit.setStatus(com.olisystem.optionsmanager.model.operation.OperationStatus.NEUTRAl);
        }
        log.info("✅ CONSOLIDATED_RESULT marcada como {} (P&L consolidado: {})", 
                consolidatedExit.getStatus(), consolidatedExit.getProfitLoss());

        // Marcar CONSOLIDATED_ENTRY como HIDDEN (passo 11)
        Optional<AverageOperationItem> consolidatedEntryItem = context.group().getItems().stream()
                .filter(item -> item.getRoleType() == OperationRoleType.CONSOLIDATED_ENTRY)
                .findFirst();
                
        if (consolidatedEntryItem.isPresent()) {
            Operation consolidatedEntryOp = consolidatedEntryItem.get().getOperation();
            log.info("Marcando CONSOLIDATED_ENTRY como HIDDEN: {}", consolidatedEntryOp.getId());
            consolidatedOperationService.markOperationAsHidden(consolidatedEntryOp);
        }

        // PASSO 19: Atualizar entidades finais
        updateEntitiesAfterFinalExit(context, exitResult);

        log.info("=== SAÍDA FINAL PROCESSADA COM SUCESSO ===");

        return consolidatedExit;
    }

    // ======================================================================================
    // MÉTODOS AUXILIARES
    // ======================================================================================

    /**
     * Processa a saída normalmente (como SingleLotExitProcessor)
     */
    private ExitOperationResult processNormalExit(OperationExitPositionContext context) {

        List<EntryLot> availableLots = context.availableLots();
        OperationFinalizationRequest request = context.context().request();
        Operation activeOperation = context.context().activeOperation();

        log.info("🔄 Processando saída com {} lotes disponíveis, quantidade solicitada: {}", 
                availableLots.size(), request.getQuantity());

        // ✅ NOVO: Distribuir quantidade entre múltiplos lotes
        List<LotConsumption> consumptions = distributeQuantityAcrossLots(availableLots, request.getQuantity());
        
        BigDecimal totalProfitLoss = BigDecimal.ZERO;
        BigDecimal totalEntryValue = BigDecimal.ZERO;
        int totalQuantityConsumed = 0;

        // Processar cada lote individualmente
        for (LotConsumption consumption : consumptions) {
            EntryLot lot = consumption.lot();
            int quantityFromLot = consumption.quantityToConsume();
            
            log.info("📦 Processando lote {}: {} unidades", lot.getSequenceNumber(), quantityFromLot);

            // Calcular resultados financeiros para este lote
            BigDecimal lotProfitLoss = profitCalculationService.calculateProfitLoss(
                    lot.getUnitPrice(),
                    request.getExitUnitPrice(),
                    quantityFromLot);

            BigDecimal lotEntryValue = lot.getUnitPrice().multiply(BigDecimal.valueOf(quantityFromLot));

            totalProfitLoss = totalProfitLoss.add(lotProfitLoss);
            totalEntryValue = totalEntryValue.add(lotEntryValue);
            totalQuantityConsumed += quantityFromLot;

            log.info("💰 Lote {}: P&L={}, EntryValue={}", lot.getSequenceNumber(), lotProfitLoss, lotEntryValue);
        }

        // Calcular percentual consolidado
        BigDecimal profitLossPercentage = profitCalculationService.calculateProfitLossPercentage(
                totalProfitLoss, totalEntryValue);

        // Determinar tipo de operação (usar o primeiro lote como referência)
        TradeType tradeType = tradeTypeResolver.determineTradeType(
                availableLots.get(0).getEntryDate(), request.getExitDate());

        // Criar operação de saída consolidada
        Operation exitOperation = operationCreationService.createExitOperation(
                context, tradeType, totalProfitLoss, context.transactionType(), totalQuantityConsumed);

        // CORREÇÃO: Garantir que os valores foram definidos corretamente
        if (exitOperation.getProfitLoss() == null || exitOperation.getProfitLoss().compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Operação de saída criada com profitLoss zerado! Corrigindo...");
            exitOperation.setProfitLoss(totalProfitLoss);
            exitOperation.setProfitLossPercentage(profitLossPercentage);
        }

        // Criar registros relacionados
        positionOperationService.createPositionOperation(
                context.position(), exitOperation, request,
                partialExitDetector.isFinalExit(context.position(), request.getQuantity()) ?
                        PositionOperationType.FULL_EXIT : PositionOperationType.PARTIAL_EXIT);

        // ✅ NOVO: Criar ExitRecord para cada lote consumido
        for (LotConsumption consumption : consumptions) {
            exitRecordService.createExitRecord(
                    consumption.lot(), 
                    exitOperation, 
                    context.context(), 
                    consumption.quantityToConsume()
            );
        }

        // ✅ NOVO: Atualizar todos os lotes consumidos
        for (LotConsumption consumption : consumptions) {
            entryLotUpdateService.updateEntryLot(consumption.lot(), consumption.quantityToConsume());
        }

        log.info("✅ Saída processada com múltiplos lotes: {} lotes, {} unidades, P&L={}, Percentual={}%",
                consumptions.size(), totalQuantityConsumed, totalProfitLoss, profitLossPercentage);

        return new ExitOperationResult(exitOperation, totalProfitLoss, profitLossPercentage, totalQuantityConsumed);
    }

    /**
     * ✅ NOVO MÉTODO: Distribui quantidade entre múltiplos lotes
     */
    private List<LotConsumption> distributeQuantityAcrossLots(List<EntryLot> lots, int totalQuantityNeeded) {
        List<LotConsumption> consumptions = new ArrayList<>();
        int remainingToConsume = totalQuantityNeeded;

        // Ordenar lotes por data de entrada (FIFO)
        List<EntryLot> sortedLots = lots.stream()
                .sorted((a, b) -> a.getEntryDate().compareTo(b.getEntryDate()))
                .collect(Collectors.toList());

        for (EntryLot lot : sortedLots) {
            if (remainingToConsume <= 0) break;

            int quantityFromThisLot = Math.min(lot.getRemainingQuantity(), remainingToConsume);
            
            if (quantityFromThisLot > 0) {
                consumptions.add(new LotConsumption(lot, quantityFromThisLot, TradeType.SWING));
                remainingToConsume -= quantityFromThisLot;
                
                log.info("📋 Planejado: Lote {} - {} unidades", lot.getSequenceNumber(), quantityFromThisLot);
            }
        }

        if (remainingToConsume > 0) {
            throw new BusinessException(String.format(
                "Quantidade insuficiente nos lotes. Necessário: %d, Disponível: %d", 
                totalQuantityNeeded, totalQuantityNeeded - remainingToConsume));
        }

        return consumptions;
    }

    /**
     * Atualiza operação consolidadora de entrada com novo preço médio
     */
    private void updateConsolidatedEntryAfterExit(Operation consolidatedEntry,
                                                  ExitOperationResult exitResult,
                                                  OperationExitPositionContext context) {

        OperationFinalizationRequest request = context.context().request();

        // Calcular valor total recebido na saída
        BigDecimal exitTotalValue = request.getExitUnitPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        // Calcular novo preço médio usando a fórmula CORRETA
        BigDecimal newAveragePrice = averagePriceCalculator.calculateNewAveragePrice(
                consolidatedEntry.getEntryTotalValue(),
                exitTotalValue,
                context.position().getRemainingQuantity() - request.getQuantity());

        BigDecimal newTotalValue = averagePriceCalculator.calculateRemainingValue(
                consolidatedEntry.getEntryTotalValue(), exitTotalValue);

        // Log detalhado do cálculo
        averagePriceCalculator.logCalculationDetails(
                "Saída Parcial",
                consolidatedEntry.getEntryTotalValue(),
                exitTotalValue,
                newTotalValue,
                context.position().getRemainingQuantity() - request.getQuantity(),
                newAveragePrice);

        // Atualizar operação consolidadora de entrada
        consolidatedOperationService.updateConsolidatedEntry(
                consolidatedEntry,
                newAveragePrice,
                context.position().getRemainingQuantity() - request.getQuantity(),
                newTotalValue);
    }

    /**
     * Atualiza entidades relacionadas após primeira saída parcial
     */
    private void updateEntitiesAfterFirstExit(OperationExitPositionContext context,
                                              ExitOperationResult exitResult) {

        // Atualizar Position com validações rigorosas
        positionUpdateService.updatePositionPartial(
                context.position(),
                context.context().request(),
                exitResult.profitLoss,
                exitResult.profitLossPercentage,
                exitResult.quantity
        );

        // ✅ CORREÇÃO CRÍTICA: Forçar reload da posição para garantir estado atualizado
        Position updatedPosition = positionRepository.findById(context.position().getId())
                .orElseThrow(() -> new BusinessException("Posição não encontrada após atualização"));

        // ✅ VALIDAÇÃO CRÍTICA: Verificar se posição foi corretamente atualizada
        validatePositionStateAfterPartialExit(updatedPosition, exitResult.quantity, context.context().request().getQuantity());

        // Atualizar AverageOperationGroup
        AverageOperationGroup group = context.group();
        group.setRemainingQuantity(updatedPosition.getRemainingQuantity()); // ✅ Usar quantidade da posição atualizada
        group.setClosedQuantity(group.getTotalQuantity() - updatedPosition.getRemainingQuantity());
        group.setStatus(AverageOperationGroupStatus.PARTIALLY_CLOSED);

        // CORREÇÃO: Acumular lucro
        BigDecimal currentProfit = group.getTotalProfit() != null ? group.getTotalProfit() : BigDecimal.ZERO;
        group.setTotalProfit(currentProfit.add(exitResult.profitLoss));

        groupRepository.save(group);

        log.info("=== ESTADO APÓS PRIMEIRA SAÍDA PARCIAL ===");
        log.info("Position {}: status={}, remaining={}, total={}", 
                updatedPosition.getId(), updatedPosition.getStatus(), 
                updatedPosition.getRemainingQuantity(), updatedPosition.getTotalQuantity());
        log.info("Group {}: status={}, remaining={}, closed={}", 
                group.getId(), group.getStatus(), 
                group.getRemainingQuantity(), group.getClosedQuantity());
        log.info("=== VALIDAÇÃO: SEGUNDA SAÍDA DEVE SER POSSÍVEL ===");
    }

    /**
     * Atualiza entidades relacionadas após saída parcial subsequente
     */
    private void updateEntitiesAfterSubsequentExit(OperationExitPositionContext context,
                                                   ExitOperationResult exitResult) {

        // Atualizar Position
        positionUpdateService.updatePositionPartial(
                context.position(),
                context.context().request(),
                exitResult.profitLoss,
                exitResult.profitLossPercentage,
                exitResult.quantity
        );

        // ✅ CORREÇÃO CRÍTICA: Forçar reload da posição
        Position updatedPosition = positionRepository.findById(context.position().getId())
                .orElseThrow(() -> new BusinessException("Posição não encontrada após atualização"));

        // ✅ VALIDAÇÃO CRÍTICA: Verificar se posição foi corretamente atualizada
        validatePositionStateAfterPartialExit(updatedPosition, exitResult.quantity, context.context().request().getQuantity());

        // Atualizar AverageOperationGroup
        AverageOperationGroup group = context.group();
        group.setRemainingQuantity(updatedPosition.getRemainingQuantity()); // ✅ Usar quantidade da posição atualizada
        group.setClosedQuantity(group.getTotalQuantity() - updatedPosition.getRemainingQuantity());

        // Determinar status do grupo baseado na posição atualizada
        if (updatedPosition.getStatus() == com.olisystem.optionsmanager.model.position.PositionStatus.CLOSED) {
            group.setStatus(AverageOperationGroupStatus.CLOSED);
        } else {
            group.setStatus(AverageOperationGroupStatus.PARTIALLY_CLOSED);
        }

        // CORREÇÃO: Acumular lucro
        BigDecimal currentProfit = group.getTotalProfit() != null ? group.getTotalProfit() : BigDecimal.ZERO;
        group.setTotalProfit(currentProfit.add(exitResult.profitLoss));

        groupRepository.save(group);

        log.info("=== ESTADO APÓS SAÍDA SUBSEQUENTE ===");
        log.info("Position {}: status={}, remaining={}, total={}", 
                updatedPosition.getId(), updatedPosition.getStatus(), 
                updatedPosition.getRemainingQuantity(), updatedPosition.getTotalQuantity());
        log.info("Group {}: status={}, remaining={}, closed={}", 
                group.getId(), group.getStatus(), 
                group.getRemainingQuantity(), group.getClosedQuantity());
    }

    /**
     * Atualiza entidades após saída final
     */
    private void updateEntitiesAfterFinalExit(OperationExitPositionContext context,
                                              ExitOperationResult exitResult) {

        // Atualizar Position para fechamento total
        positionUpdateService.updatePosition(
                context.position(),
                context.context().request(),
                // Para saída final, o lucro total é o acumulado da Position + este último
                context.position().getTotalRealizedProfit().add(exitResult.profitLoss),
                exitResult.profitLossPercentage
        );

        // Atualizar AverageOperationGroup para CLOSED
        AverageOperationGroup group = context.group();
        group.setStatus(AverageOperationGroupStatus.CLOSED);
        group.setClosedQuantity(group.getTotalQuantity());
        group.setRemainingQuantity(0);

        // CORREÇÃO: Finalizar o lucro total do grupo
        BigDecimal currentProfit = group.getTotalProfit() != null ? group.getTotalProfit() : BigDecimal.ZERO;
        group.setTotalProfit(currentProfit.add(exitResult.profitLoss));

        groupRepository.save(group);

        log.info("Grupo finalizado: Lucro total = {}", group.getTotalProfit());
    }

    /**
     * Validações iniciais para saída parcial
     */
    private void validatePartialExitContext(OperationExitPositionContext context) {

        if (context.availableLots() == null || context.availableLots().isEmpty()) {
            throw new BusinessException("PartialExitProcessor requer pelo menos 1 lote. Recebido: " +
                    (context.availableLots() == null ? 0 : context.availableLots().size()));
        }

        log.info("✅ Validação de lotes: {} lotes disponíveis para saída parcial", context.availableLots().size());

        if (!partialExitDetector.validateExitQuantity(context.position(), context.context().request().getQuantity())) {
            throw new BusinessException("Quantidade de saída inválida");
        }
    }

    /**
     * ✅ NOVA VALIDAÇÃO: Verifica se o estado da posição está correto após saída parcial
     */
    private void validatePositionStateAfterPartialExit(Position position, int quantityExited, int requestedQuantity) {
        log.info("=== VALIDANDO ESTADO DA POSIÇÃO APÓS SAÍDA PARCIAL ===");
        
        // Validação 1: Quantidade saída deve corresponder à solicitada
        if (quantityExited != requestedQuantity) {
            throw new BusinessException(String.format(
                "Inconsistência: Quantidade saída (%d) diferente da solicitada (%d)", 
                quantityExited, requestedQuantity));
        }

        // Validação 2: Posição deve ter status correto
        boolean shouldBeClosed = position.getRemainingQuantity() == 0;
        boolean shouldBePartial = position.getRemainingQuantity() > 0 && position.getRemainingQuantity() < position.getTotalQuantity();
        
        if (shouldBeClosed && position.getStatus() != com.olisystem.optionsmanager.model.position.PositionStatus.CLOSED) {
            throw new BusinessException(String.format(
                "ERRO CRÍTICO: Posição deveria estar CLOSED mas está %s (remaining=%d)", 
                position.getStatus(), position.getRemainingQuantity()));
        }
        
        if (shouldBePartial && position.getStatus() != com.olisystem.optionsmanager.model.position.PositionStatus.PARTIAL) {
            log.warn("AVISO: Posição deveria estar PARTIAL mas está {} (remaining={})", 
                    position.getStatus(), position.getRemainingQuantity());
            // ✅ AUTO-CORREÇÃO: Forçar status correto
            position.setStatus(com.olisystem.optionsmanager.model.position.PositionStatus.PARTIAL);
            positionRepository.save(position);
            log.info("Status da posição corrigido para PARTIAL");
        }

        // Validação 3: Quantidade restante deve ser lógica
        if (position.getRemainingQuantity() < 0) {
            throw new BusinessException(String.format(
                "ERRO CRÍTICO: Quantidade restante não pode ser negativa: %d", 
                position.getRemainingQuantity()));
        }

        if (position.getRemainingQuantity() > position.getTotalQuantity()) {
            throw new BusinessException(String.format(
                "ERRO CRÍTICO: Quantidade restante (%d) maior que total (%d)", 
                position.getRemainingQuantity(), position.getTotalQuantity()));
        }

        log.info("✅ VALIDAÇÃO PASSOU: Position está em estado consistente");
        log.info("Position ID: {}, Status: {}, Remaining: {}/{}", 
                position.getId(), position.getStatus(), 
                position.getRemainingQuantity(), position.getTotalQuantity());
    }

    /**
     * Classe auxiliar para resultado da saída
     */
    private static class ExitOperationResult {
        final Operation exitOperation;
        final BigDecimal profitLoss;
        final BigDecimal profitLossPercentage;
        final Integer quantity;

        ExitOperationResult(Operation exitOperation, BigDecimal profitLoss,
                            BigDecimal profitLossPercentage, Integer quantity) {
            this.exitOperation = exitOperation;
            this.profitLoss = profitLoss;
            this.profitLossPercentage = profitLossPercentage;
            this.quantity = quantity;
        }
    }
}
