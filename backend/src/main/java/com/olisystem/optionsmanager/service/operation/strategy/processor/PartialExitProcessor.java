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
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.repository.AverageOperationGroupRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

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

        // PASSO 4: Criar operação consolidadora de entrada
        Operation consolidatedEntry = consolidatedOperationService.createConsolidatedEntry(
                context.context().activeOperation(), context.group());

        // PASSO 5 & 6: Calcular e atualizar novo preço médio
        updateConsolidatedEntryAfterExit(consolidatedEntry, exitResult, context);

        // PASSO 7: Marcar operações originais como HIDDEN
        consolidatedOperationService.markOperationAsHidden(context.context().activeOperation());
        consolidatedOperationService.markOperationAsHidden(exitResult.exitOperation);

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

        // Buscar operações consolidadoras existentes
        Operation consolidatedEntry = consolidatedOperationService.findConsolidatedEntry(context.group());
        Operation consolidatedExit = consolidatedOperationService.findConsolidatedExit(context.group());

        if (consolidatedEntry == null || consolidatedExit == null) {
            throw new BusinessException("Operações consolidadoras não encontradas para saída subsequente");
        }

        // PASSO 11: Atualizar operação consolidadora de saída
        consolidatedOperationService.updateConsolidatedExit(
                consolidatedExit,
                exitResult.profitLoss,
                exitResult.quantity,
                context.context().request().getExitDate(),
                context.context().request().getExitUnitPrice());

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

        // Buscar operações consolidadoras existentes
        Operation consolidatedEntry = consolidatedOperationService.findConsolidatedEntry(context.group());
        Operation consolidatedExit = consolidatedOperationService.findConsolidatedExit(context.group());

        if (consolidatedEntry == null || consolidatedExit == null) {
            throw new BusinessException("Operações consolidadoras não encontradas para saída final");
        }

        // PASSO 17: Finalizar operação consolidadora de saída
        consolidatedOperationService.updateConsolidatedExit(
                consolidatedExit,
                exitResult.profitLoss,
                exitResult.quantity,
                context.context().request().getExitDate(),
                context.context().request().getExitUnitPrice());

        // PASSO 18: Finalizar operação consolidadora de entrada (quantidade = 0)
        consolidatedOperationService.updateConsolidatedEntry(
                consolidatedEntry,
                BigDecimal.ZERO, // Preço médio irrelevante quando quantidade = 0
                0, // Quantidade final = 0
                BigDecimal.ZERO); // Valor total final = 0

        // Marcar operação de saída como HIDDEN
        consolidatedOperationService.markOperationAsHidden(exitResult.exitOperation);

        // PASSO 18.5: Saída final - adicionar TOTAL_EXIT e atualizar CONSOLIDATED_RESULT
        Optional<Operation> existingConsolidatedResult = consolidatedOperationService.findExistingConsolidatedResult(context.group());
        if (existingConsolidatedResult.isPresent()) {
            log.info("Saída final - atualizando CONSOLIDATED_RESULT final");
            
            // Adicionar operação de saída final como TOTAL_EXIT no grupo (passo 9)
            averageOperationService.addNewItemGroup(context.group(), exitResult.exitOperation, 
                    com.olisystem.optionsmanager.model.operation.OperationRoleType.TOTAL_EXIT);
            log.info("Operação de saída final adicionada ao grupo como TOTAL_EXIT: {}", exitResult.exitOperation.getId());
            
            // Atualizar CONSOLIDATED_RESULT final (passo 10) - NÃO transformar em TOTAL_EXIT
            consolidatedOperationService.updateConsolidatedResult(
                    existingConsolidatedResult.get(), exitResult.exitOperation, context.group());
            log.info("CONSOLIDATED_RESULT atualizada para resultado final");
        }
        
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

        EntryLot lot = context.availableLots().get(0);
        OperationFinalizationRequest request = context.context().request();
        Operation activeOperation = context.context().activeOperation();

        // Determinar tipo de operação
        TradeType tradeType = tradeTypeResolver.determineTradeType(
                lot.getEntryDate(), request.getExitDate());

        // Calcular resultados financeiros baseados no CUSTO ORIGINAL
        BigDecimal profitLoss = profitCalculationService.calculateProfitLoss(
                lot.getUnitPrice(), // ✅ SEMPRE usar preço original do lote
                request.getExitUnitPrice(),
                request.getQuantity());

        BigDecimal profitLossPercentage = profitCalculationService.calculateProfitLossPercentageFromPrices(
                lot.getUnitPrice(), request.getExitUnitPrice());

        // Criar operação de saída com os valores calculados
        Operation exitOperation = operationCreationService.createExitOperation(
                context, tradeType, profitLoss, context.transactionType(), request.getQuantity());

        // CORREÇÃO: Garantir que os valores foram definidos corretamente
        if (exitOperation.getProfitLoss() == null || exitOperation.getProfitLoss().compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Operação de saída criada com profitLoss zerado! Corrigindo...");
            exitOperation.setProfitLoss(profitLoss);
            exitOperation.setProfitLossPercentage(profitLossPercentage);
        }

        // Criar registros relacionados
        positionOperationService.createPositionOperation(
                context.position(), exitOperation, request,
                partialExitDetector.isFinalExit(context.position(), request.getQuantity()) ?
                        PositionOperationType.FULL_EXIT : PositionOperationType.PARTIAL_EXIT);

        exitRecordService.createExitRecord(lot, exitOperation, context.context(), request.getQuantity());

        // Atualizar lote APÓS criar ExitRecord
        entryLotUpdateService.updateEntryLot(lot, request.getQuantity());

        log.info("Saída normal processada: P&L={}, Percentual={}%, Tipo={}",
                profitLoss, profitLossPercentage, tradeType);

        return new ExitOperationResult(exitOperation, profitLoss, profitLossPercentage, request.getQuantity());
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
     * Atualiza entidades após primeira saída
     */
    private void updateEntitiesAfterFirstExit(OperationExitPositionContext context,
                                              ExitOperationResult exitResult) {

        // Atualizar Position usando o serviço apropriado
        positionUpdateService.updatePositionPartial(
                context.position(),
                context.context().request(),
                exitResult.profitLoss,
                exitResult.profitLossPercentage,
                exitResult.quantity
        );

        // Atualizar AverageOperationGroup
        AverageOperationGroup group = context.group();
        group.setStatus(AverageOperationGroupStatus.PARTIALLY_CLOSED);
        group.setClosedQuantity(exitResult.quantity);
        group.setRemainingQuantity(context.position().getRemainingQuantity() - exitResult.quantity);

        // CORREÇÃO: Atualizar o lucro total do grupo
        BigDecimal currentProfit = group.getTotalProfit() != null ? group.getTotalProfit() : BigDecimal.ZERO;
        group.setTotalProfit(currentProfit.add(exitResult.profitLoss));

        groupRepository.save(group);

        log.info("Grupo atualizado: Lucro acumulado = {}", group.getTotalProfit());
    }

    /**
     * Atualizar entidades após saída subsequente
     */
    private void updateEntitiesAfterSubsequentExit(OperationExitPositionContext context,
                                                   ExitOperationResult exitResult) {

        // Atualizar Position usando o serviço apropriado
        positionUpdateService.updatePositionPartial(
                context.position(),
                context.context().request(),
                exitResult.profitLoss,
                exitResult.profitLossPercentage,
                exitResult.quantity
        );

        // Atualizar AverageOperationGroup
        AverageOperationGroup group = context.group();
        group.setClosedQuantity(group.getClosedQuantity() + exitResult.quantity);
        group.setRemainingQuantity(context.position().getRemainingQuantity() - exitResult.quantity);

        // CORREÇÃO: Acumular o lucro total do grupo
        BigDecimal currentProfit = group.getTotalProfit() != null ? group.getTotalProfit() : BigDecimal.ZERO;
        group.setTotalProfit(currentProfit.add(exitResult.profitLoss));

        groupRepository.save(group);

        log.info("Grupo atualizado: Lucro acumulado = {}", group.getTotalProfit());
    }

    /**
     * Atualizar entidades após saída final
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

        if (context.availableLots() == null || context.availableLots().size() != 1) {
            throw new BusinessException("PartialExitProcessor requer exatamente 1 lote. Recebido: " +
                    (context.availableLots() == null ? 0 : context.availableLots().size()));
        }

        if (!partialExitDetector.validateExitQuantity(context.position(), context.context().request().getQuantity())) {
            throw new BusinessException("Quantidade de saída inválida");
        }
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
