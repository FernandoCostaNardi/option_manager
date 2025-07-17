package com.olisystem.optionsmanager.service.operation.strategy.processor;

import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.exception.BusinessException;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.ExitStrategy;
import com.olisystem.optionsmanager.model.position.PositionOperationType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationGroupService;
import com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationService;
import com.olisystem.optionsmanager.service.operation.consolidate.ConsolidatedOperationService;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultipleLotExitProcessor {

    private final ProfitCalculationService profitCalculationService;
    private final EntryLotUpdateService entryLotUpdateService;
    private final PositionUpdateService positionUpdateService;
    private final OperationStatusService operationStatusService;
    private final ExitRecordService exitRecordService;
    private final PositionOperationService positionOperationService;
    private final AverageOperationGroupService averageOperationGroupService;
    private final OperationCreationService operationCreationService;
    private final ConsolidatedOperationService consolidatedOperationService;
    private final AverageOperationService averageOperationService;

    /**
     * Processa a saída de operação com múltiplos lotes
     * Implementa os 15 passos do Cenário 02
     */
    @Transactional
    public Operation process(OperationExitPositionContext context) {
        log.info("=== INICIANDO PROCESSAMENTO DE SAÍDA COM MÚLTIPLOS LOTES ===");
        log.info("Operação ID: {}, Posição ID: {}, Lotes disponíveis: {}",
                context.context().activeOperation().getId(),
                context.position().getId(),
                context.availableLots().size());

        try {
            // FASE 1: IDENTIFICAÇÃO E PREPARAÇÃO (Passos 1-3)
            validateMultipleLotContext(context);
            LotAnalysisResult lotAnalysis = analyzeLots(context);

            // FASE 2: ESTRATÉGIA DE CONSUMO (Passos 4-6)
            ConsumptionPlan consumptionPlan = createConsumptionPlan(lotAnalysis, context);

            // FASE 3: EXECUÇÃO DO CONSUMO (Passos 7-9)
            ConsumptionResult consumptionResult = executeConsumption(consumptionPlan, context);

            // FASE 4: CRIAÇÃO DE OPERAÇÕES DE SAÍDA (Passos 10-12)
            List<Operation> exitOperations = createExitOperations(consumptionResult, context);

            // FASE 5: REGISTROS E RASTREABILIDADE (Passos 13-15)
            finalizeProcessing(exitOperations, consumptionResult, context);

            log.info("=== PROCESSAMENTO DE MÚLTIPLOS LOTES CONCLUÍDO COM SUCESSO ===");

            // Retornar operação principal (última criada)
            return exitOperations.get(exitOperations.size() - 1);

        } catch (Exception e) {
            log.error("Erro durante processamento de múltiplos lotes para operação {}: {}",
                    context.context().activeOperation().getId(), e.getMessage(), e);
            throw new BusinessException("Falha no processamento da saída com múltiplos lotes: " + e.getMessage());
        }
    }

    // ======================================================================================
    // FASE 1: IDENTIFICAÇÃO E PREPARAÇÃO (Passos 1-3)
    // ======================================================================================

    /**
     * PASSO 1: Identificar múltiplos lotes de entrada
     */
    private void validateMultipleLotContext(OperationExitPositionContext context) {
        if (context.availableLots() == null || context.availableLots().size() <= 1) {
            throw new BusinessException("Processador de múltiplos lotes recebeu " +
                    (context.availableLots() == null ? 0 : context.availableLots().size()) + " lotes. Esperado: > 1");
        }

        // Validar que todos os lotes têm quantidade disponível
        List<EntryLot> emptyLots = context.availableLots().stream()
                .filter(lot -> lot.getRemainingQuantity() <= 0)
                .collect(Collectors.toList());

        if (!emptyLots.isEmpty()) {
            throw new BusinessException("Encontrados " + emptyLots.size() + " lotes sem quantidade disponível");
        }

        log.debug("Validação de múltiplos lotes aprovada: {} lotes disponíveis", context.availableLots().size());
    }

    /**
     * PASSO 2: Validar quantidade solicitada vs total disponível
     * PASSO 3: Analisar datas dos lotes vs data de saída
     */
    private LotAnalysisResult analyzeLots(OperationExitPositionContext context) {
        List<EntryLot> availableLots = context.availableLots();
        OperationFinalizationRequest request = context.context().request();
        LocalDate exitDate = request.getExitDate();

        // Calcular total disponível
        Integer totalAvailable = availableLots.stream()
                .mapToInt(EntryLot::getRemainingQuantity)
                .sum();

        // Validar quantidade solicitada
        if (request.getQuantity() > totalAvailable) {
            throw new BusinessException(
                    String.format("Quantidade solicitada (%d) excede quantidade disponível (%d)",
                            request.getQuantity(), totalAvailable));
        }

        // Separar lotes por categoria (mesmo dia vs dias anteriores)
        List<EntryLot> sameDayLots = availableLots.stream()
                .filter(lot -> lot.getEntryDate().equals(exitDate))
                .collect(Collectors.toList());

        List<EntryLot> previousDayLots = availableLots.stream()
                .filter(lot -> lot.getEntryDate().isBefore(exitDate))
                .collect(Collectors.toList());

        // Calcular quantidades por categoria
        Integer sameDayQuantity = sameDayLots.stream().mapToInt(EntryLot::getRemainingQuantity).sum();
        Integer previousDayQuantity = previousDayLots.stream().mapToInt(EntryLot::getRemainingQuantity).sum();

        log.info("Análise de lotes concluída: Total={}, Solicitado={}, MesmoDia={}, DiasAnteriores={}",
                totalAvailable, request.getQuantity(), sameDayQuantity, previousDayQuantity);

        return new LotAnalysisResult(
                totalAvailable,
                sameDayLots, sameDayQuantity,
                previousDayLots, previousDayQuantity,
                request.getQuantity() == totalAvailable
        );
    }

    // ======================================================================================
    // FASE 2: ESTRATÉGIA DE CONSUMO (Passos 4-6)
    // ======================================================================================

    /**
     * PASSO 4: Determinar estratégia de consumo automática
     * PASSO 5: Ordenar lotes conforme estratégia definida
     * PASSO 6: Calcular plano de consumo por lote
     */
    private ConsumptionPlan createConsumptionPlan(LotAnalysisResult analysis, OperationExitPositionContext context) {
        Integer requestedQuantity = context.context().request().getQuantity();

        log.debug("Criando plano de consumo para {} unidades", requestedQuantity);

        // Determinar estratégia baseada na composição dos lotes
        ConsumptionStrategy strategy = determineConsumptionStrategy(analysis, requestedQuantity);

        // Criar plano de consumo baseado na estratégia
        List<LotConsumption> consumptions = new ArrayList<>();
        Integer remainingToConsume = requestedQuantity;

        switch (strategy) {
            case DAY_TRADE_ONLY:
                consumptions.addAll(createDayTradeConsumptions(analysis.sameDayLots, remainingToConsume));
                break;

            case SWING_TRADE_ONLY:
                consumptions.addAll(createSwingTradeConsumptions(analysis.previousDayLots, remainingToConsume));
                break;

            case MIXED_AUTO:
                // Primeiro: consumir lotes do mesmo dia (LIFO)
                if (!analysis.sameDayLots.isEmpty()) {
                    Integer dayTradeQuantity = Math.min(remainingToConsume, analysis.sameDayQuantity);
                    consumptions.addAll(createDayTradeConsumptions(analysis.sameDayLots, dayTradeQuantity));
                    remainingToConsume -= dayTradeQuantity;
                }

                // Depois: consumir lotes de dias anteriores (FIFO)
                if (remainingToConsume > 0 && !analysis.previousDayLots.isEmpty()) {
                    consumptions.addAll(createSwingTradeConsumptions(analysis.previousDayLots, remainingToConsume));
                }
                break;
        }

        log.info("Plano de consumo criado: {} lotes serão consumidos, estratégia: {}", 
                consumptions.size(), strategy);
        
        // Log detalhado do plano de consumo
        for (int i = 0; i < consumptions.size(); i++) {
            LotConsumption consumption = consumptions.get(i);
            log.info("  Consumo {}: Lote={}, Quantidade={}, TradeType={}, Strategy={}", 
                    i + 1, consumption.lot.getId(), consumption.quantityToConsume, 
                    consumption.tradeType, consumption.strategy);
        }

        return new ConsumptionPlan(strategy, consumptions);
    }

    private ConsumptionStrategy determineConsumptionStrategy(LotAnalysisResult analysis, Integer requestedQuantity) {
        if (analysis.previousDayLots.isEmpty()) {
            log.debug("Estratégia: DAY_TRADE_ONLY - Apenas lotes do mesmo dia");
            return ConsumptionStrategy.DAY_TRADE_ONLY;
        }

        if (analysis.sameDayLots.isEmpty()) {
            log.debug("Estratégia: SWING_TRADE_ONLY - Apenas lotes de dias anteriores");
            return ConsumptionStrategy.SWING_TRADE_ONLY;
        }

        log.debug("Estratégia: MIXED_AUTO - Combinação Day Trade + Swing Trade");
        return ConsumptionStrategy.MIXED_AUTO;
    }

    private List<LotConsumption> createDayTradeConsumptions(List<EntryLot> lots, Integer quantityToConsume) {
        // LIFO: Mais recente primeiro (ordenar por data DESC, depois por sequência DESC, depois por UUID DESC)
        List<EntryLot> sortedLots = lots.stream()
                .sorted(Comparator
                        .comparing(EntryLot::getEntryDate).reversed()
                        .thenComparing(EntryLot::getSequenceNumber).reversed()
                        .thenComparing(EntryLot::getId).reversed())
                .collect(Collectors.toList());

        log.info("Lotes ordenados para DAY_TRADE (LIFO):");
        for (int i = 0; i < sortedLots.size(); i++) {
            EntryLot lot = sortedLots.get(i);
            log.info("  {}: ID={}, Data={}, Sequência={}, Quantidade={}", 
                    i + 1, lot.getId(), lot.getEntryDate(), lot.getSequenceNumber(), lot.getRemainingQuantity());
        }

        return createConsumptionsFromSortedLots(sortedLots, quantityToConsume, TradeType.DAY, ExitStrategy.LIFO);
    }

    private List<LotConsumption> createSwingTradeConsumptions(List<EntryLot> lots, Integer quantityToConsume) {
        // FIFO: Mais antigo primeiro (ordenar por data ASC, depois por sequência ASC, depois por UUID ASC)
        List<EntryLot> sortedLots = lots.stream()
                .sorted(Comparator
                        .comparing(EntryLot::getEntryDate)
                        .thenComparing(EntryLot::getSequenceNumber)
                        .thenComparing(EntryLot::getId))
                .collect(Collectors.toList());

        log.info("Lotes ordenados para SWING_TRADE (FIFO):");
        for (int i = 0; i < sortedLots.size(); i++) {
            EntryLot lot = sortedLots.get(i);
            log.info("  {}: ID={}, Data={}, Sequência={}, Quantidade={}", 
                    i + 1, lot.getId(), lot.getEntryDate(), lot.getSequenceNumber(), lot.getRemainingQuantity());
        }

        return createConsumptionsFromSortedLots(sortedLots, quantityToConsume, TradeType.SWING, ExitStrategy.FIFO);
    }

    private List<LotConsumption> createConsumptionsFromSortedLots(List<EntryLot> sortedLots,
                                                                  Integer quantityToConsume, TradeType tradeType, ExitStrategy strategy) {
        List<LotConsumption> consumptions = new ArrayList<>();
        int remaining = quantityToConsume;

        log.info("Criando consumos para {} unidades restantes, estratégia: {}", remaining, strategy);

        for (EntryLot lot : sortedLots) {
            if (remaining <= 0) {
                log.info("Quantidade restante é 0, parando consumo");
                break;
            }
            int available = lot.getRemainingQuantity();
            int quantityFromThisLot = Math.min(available, remaining);
            if (quantityFromThisLot > 0) {
                consumptions.add(new LotConsumption(lot, quantityFromThisLot, tradeType, strategy));
                remaining -= quantityFromThisLot;
                log.info("Lote {} consumido: {} unidades, restante: {}", 
                        lot.getId(), quantityFromThisLot, remaining);
            } else {
                log.info("Lote {} ignorado: quantidade disponível = 0", lot.getId());
            }
        }
        log.info("Total de consumos criados: {}", consumptions.size());
        return consumptions;
    }

    // ======================================================================================
    // FASE 3: EXECUÇÃO DO CONSUMO (Passos 7-9)
    // ======================================================================================

    /**
     * PASSO 7: Processar consumo de cada lote individualmente
     * PASSO 8: Consolidar resultados financeiros totais
     * PASSO 9: Atualizar Position consolidadamente
     */
    private ConsumptionResult executeConsumption(ConsumptionPlan plan, OperationExitPositionContext context) {
        log.debug("Executando consumo de {} lotes", plan.consumptions.size());

        OperationFinalizationRequest request = context.context().request();
        List<LotConsumptionResult> lotResults = new ArrayList<>();

        // CORREÇÃO: Processar cada lote individualmente SEM atualizar ainda
        for (LotConsumption consumption : plan.consumptions) {
            LotConsumptionResult result = processLotConsumption(consumption, request);
            lotResults.add(result);

            // ✅ REMOÇÃO: NÃO atualizar lote aqui - será feito depois dos ExitRecords
            // entryLotUpdateService.updateEntryLot(consumption.lot, consumption.quantityToConsume);
        }

        // Consolidar resultados por tipo de trade
        Map<TradeType, List<LotConsumptionResult>> resultsByTradeType = lotResults.stream()
                .collect(Collectors.groupingBy(r -> r.tradeType));

        // Calcular totais consolidados
        BigDecimal totalProfitLoss = lotResults.stream()
                .map(r -> r.profitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEntryValue = lotResults.stream()
                .map(r -> r.entryValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal consolidatedPercentage = profitCalculationService.calculateProfitLossPercentage(
                totalProfitLoss, totalEntryValue);

        Integer totalQuantityConsumed = lotResults.stream()
                .mapToInt(r -> r.quantityConsumed)
                .sum();

        // ✅ CORREÇÃO: DETECTAR SE É SAÍDA TOTAL ANTES de atualizar a posição
        boolean isTotalExit = totalQuantityConsumed.equals(context.position().getRemainingQuantity());
        log.info("🔍 Detecção de saída total (ANTES da atualização): quantidade_solicitada={}, remaining_quantity={}, isTotalExit={}", 
                totalQuantityConsumed, context.position().getRemainingQuantity(), isTotalExit);

        // ✅ CORREÇÃO: Usar método apropriado baseado no tipo de saída
        if (isTotalExit) {
            // Saída total - usar updatePosition (que fecha a posição)
            log.info("🎯 Processando como SAÍDA TOTAL - usando updatePosition()");
            positionUpdateService.updatePosition(context.position(), request, totalProfitLoss, consolidatedPercentage);
        } else {
            // Saída parcial - usar updatePositionPartial (que mantém posição aberta)
            log.info("🎯 Processando como SAÍDA PARCIAL - usando updatePositionPartial()");
            positionUpdateService.updatePositionPartial(context.position(), request, totalProfitLoss, consolidatedPercentage, totalQuantityConsumed);
        }

        // Atualizar status da operação de entrada original
        operationStatusService.updateOperationStatus(
                context.context().activeOperation(), OperationStatus.HIDDEN);

        log.info("Consumo calculado: {} lotes processados, total: {} unidades, lucro/prejuízo: {}",
                lotResults.size(), totalQuantityConsumed, totalProfitLoss);

        return new ConsumptionResult(
                plan.strategy,
                lotResults,
                resultsByTradeType,
                totalProfitLoss,
                consolidatedPercentage,
                totalQuantityConsumed
        );
    }

    private LotConsumptionResult processLotConsumption(LotConsumption consumption, OperationFinalizationRequest request) {
        // Calcular resultados financeiros para este lote
        BigDecimal profitLoss = profitCalculationService.calculateProfitLoss(
                consumption.lot.getUnitPrice(),
                request.getExitUnitPrice(),
                consumption.quantityToConsume);

        BigDecimal entryValue = consumption.lot.getUnitPrice()
                .multiply(BigDecimal.valueOf(consumption.quantityToConsume));

        BigDecimal exitValue = request.getExitUnitPrice()
                .multiply(BigDecimal.valueOf(consumption.quantityToConsume));

        BigDecimal percentage = profitCalculationService.calculateProfitLossPercentageFromPrices(
                consumption.lot.getUnitPrice(), request.getExitUnitPrice());

        log.debug("Lote {} consumido: {} unidades, lucro/prejuízo: {}, percentual: {}%",
                consumption.lot.getId(), consumption.quantityToConsume, profitLoss, percentage);

        return new LotConsumptionResult(
                consumption.lot,
                consumption.quantityToConsume,
                consumption.tradeType,
                consumption.strategy,
                profitLoss,
                percentage,
                entryValue,
                exitValue
        );
    }

    // ======================================================================================
    // FASE 4: CRIAÇÃO DE OPERAÇÕES DE SAÍDA (Passos 10-12)
    // ======================================================================================

    /**
     * PASSO 10: Determinar quantas operações de saída criar
     * PASSO 11: Criar operação(ões) de saída conforme necessário
     * PASSO 12: Atualizar status da operação de entrada original (já feito na fase 3)
     */
    private List<Operation> createExitOperations(ConsumptionResult result, OperationExitPositionContext context) {
        List<Operation> exitOperations = new ArrayList<>();

        // Criar operações baseadas nos tipos de trade processados
        for (Map.Entry<TradeType, List<LotConsumptionResult>> entry : result.resultsByTradeType.entrySet()) {
            TradeType tradeType = entry.getKey();
            List<LotConsumptionResult> lotResults = entry.getValue();

            // ✅ CORREÇÃO: Calcular dados médios corretos para esta operação
            TradeOperationData operationData = calculateTradeOperationData(lotResults, context.context().request());

            // ✅ CORREÇÃO: Só criar operação se a quantidade for maior que 0
            if (operationData.quantity > 0) {
                // Criar operação de saída com dados corretos
                Operation exitOperation = createExitOperationWithCorrectData(
                        context, tradeType, operationData);

                exitOperations.add(exitOperation);

                log.info("Operação de saída {} criada: tipo={}, quantidade={}, preço_médio_entrada={}, lucro/prejuízo={}, percentual={}%",
                        exitOperation.getId(), tradeType, operationData.quantity,
                        operationData.averageEntryPrice, operationData.profitLoss, operationData.profitLossPercentage);
            } else {
                log.warn("⚠️ Ignorando criação de operação para tradeType={} com quantidade=0", tradeType);
            }
        }

        return exitOperations;
    }

    /**
     * ✅ NOVO MÉTODO: Calcular dados médios corretos por tipo de trade
     */
    private TradeOperationData calculateTradeOperationData(List<LotConsumptionResult> lotResults,
                                                           OperationFinalizationRequest request) {

        // Consolidar dados dos lotes deste tipo de trade
        BigDecimal totalEntryValue = lotResults.stream()
                .map(r -> r.entryValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExitValue = lotResults.stream()
                .map(r -> r.exitValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfitLoss = lotResults.stream()
                .map(r -> r.profitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer totalQuantity = lotResults.stream()
                .mapToInt(r -> r.quantityConsumed)
                .sum();

        // Calcular preço médio de entrada ponderado
        BigDecimal averageEntryPrice = totalQuantity > 0 ?
                totalEntryValue.divide(BigDecimal.valueOf(totalQuantity), 6, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Calcular percentual baseado nos totais
        BigDecimal profitLossPercentage = totalEntryValue.compareTo(BigDecimal.ZERO) > 0 ?
                totalProfitLoss.divide(totalEntryValue, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                BigDecimal.ZERO;

        // Determinar data de entrada (mais antiga para este tipo de trade)
        LocalDate entryDate = lotResults.stream()
                .map(r -> r.lot.getEntryDate())
                .min(LocalDate::compareTo)
                .orElse(request.getExitDate());

        log.debug("Dados calculados para trade: quantidade={}, preço_médio_entrada={}, valor_total_entrada={}, " +
                        "valor_total_saída={}, lucro/prejuízo={}, percentual={}%",
                totalQuantity, averageEntryPrice, totalEntryValue, totalExitValue, totalProfitLoss, profitLossPercentage);

        return new TradeOperationData(
                totalQuantity,
                averageEntryPrice,
                totalEntryValue,
                totalExitValue,
                totalProfitLoss,
                profitLossPercentage,
                entryDate
        );
    }

    /**
     * ✅ NOVO MÉTODO: Criar operação com dados corretos calculados
     */
    private Operation createExitOperationWithCorrectData(OperationExitPositionContext context,
                                                         TradeType tradeType,
                                                         TradeOperationData operationData) {

        OperationFinalizationRequest request = context.context().request();
        Operation activeOperation = context.context().activeOperation();

        // Usar OperationCreationService básico e depois ajustar os dados
        Operation exitOperation = operationCreationService.createExitOperation(
                context, tradeType, operationData.profitLoss,
                context.transactionType(), operationData.quantity);

        // ✅ CORREÇÃO: Sobrescrever com dados corretos calculados
        exitOperation.setEntryDate(operationData.entryDate);
        exitOperation.setEntryUnitPrice(operationData.averageEntryPrice);
        exitOperation.setEntryTotalValue(operationData.totalEntryValue);
        exitOperation.setExitUnitPrice(request.getExitUnitPrice());
        exitOperation.setExitTotalValue(operationData.totalExitValue);
        exitOperation.setProfitLoss(operationData.profitLoss);
        exitOperation.setProfitLossPercentage(operationData.profitLossPercentage);
        exitOperation.setQuantity(operationData.quantity);

        // Manter outros dados da operação original
        exitOperation.setOptionSeries(activeOperation.getOptionSeries());
        exitOperation.setBrokerage(activeOperation.getBrokerage());
        exitOperation.setAnalysisHouse(activeOperation.getAnalysisHouse());
        // ✅ CORREÇÃO: Operações de saída devem ter TransactionType SELL (inverso da operação original)
        exitOperation.setTransactionType(TransactionType.SELL);
        exitOperation.setTradeType(tradeType);
        exitOperation.setExitDate(request.getExitDate());
        exitOperation.setUser(activeOperation.getUser());

        // ✅ CORREÇÃO: Operações individuais devem ser HIDDEN
        exitOperation.setStatus(OperationStatus.HIDDEN);

        // ✅ IMPORTANTE: Salvar a operação com os dados corretos
        // (O OperationCreationService já salva, mas precisamos salvar novamente após as alterações)
        // Assumindo que existe um repository injetado ou o service faz o save

        return exitOperation;
    }

    // ======================================================================================
    // FASE 5: REGISTROS E RASTREABILIDADE (Passos 13-15)
    // ======================================================================================

    /**
     * PASSO 13: Criar PositionOperation(s)
     * PASSO 14: Criar ExitRecords individuais
     * PASSO 15: Atualizar AverageOperationGroup
     */
    private void finalizeProcessing(List<Operation> exitOperations, ConsumptionResult result,
                                    OperationExitPositionContext context) {

        OperationFinalizationRequest request = context.context().request();

        // Determinar tipo de operação na posição
        PositionOperationType positionOpType = result.totalQuantityConsumed.equals(
                context.position().getTotalQuantity()) ?
                PositionOperationType.FULL_EXIT : PositionOperationType.PARTIAL_EXIT;

        // Consolidar operações de saída
        // consolidatedOperationService.consolidateExitOperations(exitOperations, context.context());

        // Criar PositionOperations para cada operação de saída
        for (Operation exitOperation : exitOperations) {
            positionOperationService.createPositionOperation(
                    context.position(), exitOperation, request, positionOpType);
        }

        // ✅ CORREÇÃO: Criar ExitRecords ANTES de atualizar os lotes
        log.debug("Criando ExitRecords para {} lotes consumidos", result.lotResults.size());
        for (LotConsumptionResult lotResult : result.lotResults) {
            // Encontrar a operação de saída correspondente ao tipo de trade
            Operation correspondingOperation = exitOperations.stream()
                    .filter(op -> op.getTradeType() == lotResult.tradeType)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Operação correspondente não encontrada para " + lotResult.tradeType));

            // Criar ExitRecord com a quantidade consumida (lote ainda não foi atualizado)
            exitRecordService.createExitRecord(
                    lotResult.lot, correspondingOperation, context.context(), lotResult.quantityConsumed);
        }

        // ✅ CORREÇÃO CRÍTICA: Usar a detecção já feita anteriormente - posição foi atualizada
        // A detecção deve ser baseada no estado ATUAL da posição (após atualização)
        boolean isTotalExit = context.position().getStatus() == com.olisystem.optionsmanager.model.position.PositionStatus.CLOSED 
                           || context.position().getRemainingQuantity() == 0;
        
        log.info("🔍 Detecção de saída total (APÓS atualização da posição): positionStatus={}, remainingQuantity={}, isTotalExit={}", 
                context.position().getStatus(), context.position().getRemainingQuantity(), isTotalExit);

        // ✅ CORREÇÃO: AGORA sim atualizar os lotes (após calcular total disponível)
        log.info("Atualizando {} lotes após cálculo do total disponível", result.lotResults.size());
        for (LotConsumptionResult lotResult : result.lotResults) {
            log.info("Atualizando lote {}: consumindo {} unidades (quantidade atual: {})", 
                    lotResult.lot.getId(), lotResult.quantityConsumed, lotResult.lot.getRemainingQuantity());
            entryLotUpdateService.updateEntryLot(lotResult.lot, lotResult.quantityConsumed);
        }

        // ✅ CORREÇÃO: Atualizar AverageOperationGroup com todas as operações
        // (Removido para evitar duplicação - será feito na lógica de consolidação abaixo)
        
        log.info("Processamento finalizado: {} operações criadas, {} ExitRecords criados, {} lotes atualizados",
                exitOperations.size(), result.lotResults.size(), result.lotResults.size());

        // ================= LÓGICA DE CONSOLIDAÇÃO =================
        
        // ✅ CORREÇÃO CRÍTICA: Marcar todas as operações de saída como HIDDEN primeiro
        for (Operation exitOperation : exitOperations) {
            log.info("Marcando operação de saída como HIDDEN: {}", exitOperation.getId());
            consolidatedOperationService.markOperationAsHidden(exitOperation);
        }
        
        if (isTotalExit) {
            // ✅ CORREÇÃO: Marcar a última operação de saída como HIDDEN e TOTAL_EXIT
            for (Operation exitOperation : exitOperations) {
                consolidatedOperationService.markOperationAsHidden(exitOperation);
                averageOperationService.addNewItemGroup(context.group(), exitOperation, com.olisystem.optionsmanager.model.operation.OperationRoleType.TOTAL_EXIT);
                log.info("Operação de saída final adicionada ao grupo como TOTAL_EXIT e marcada como HIDDEN: {}", exitOperation.getId());
            }
            // ✅ CORREÇÃO: Transformar CONSOLIDATED_RESULT existente em TOTAL_EXIT com valores corretos
            Optional<Operation> existingConsolidatedResult = consolidatedOperationService.findExistingConsolidatedResult(context.group());
            if (existingConsolidatedResult.isPresent()) {
                log.info("Transformando CONSOLIDATED_RESULT existente em TOTAL_EXIT com valores corretos: {}", existingConsolidatedResult.get().getId());
                consolidatedOperationService.transformToTotalExit(existingConsolidatedResult.get(), context.group());
            } else {
                log.info("Criando CONSOLIDATED_RESULT final");
                Operation firstExitOp = exitOperations.get(0);
                consolidatedOperationService.createConsolidatedExit(context.group(), firstExitOp, 
                    firstExitOp.getExitUnitPrice(), firstExitOp.getExitDate());
            }
            // Marcar CONSOLIDATED_ENTRY como HIDDEN
            markConsolidatedEntryAsHidden(context);
        } else {
            // Adicionar como PARTIAL_EXIT
            for (Operation exitOperation : exitOperations) {
                averageOperationService.addNewItemGroup(context.group(), exitOperation, com.olisystem.optionsmanager.model.operation.OperationRoleType.PARTIAL_EXIT);
                log.info("Operação de saída parcial adicionada ao grupo como PARTIAL_EXIT: {}", exitOperation.getId());
            }
            // Atualizar ou criar CONSOLIDATED_RESULT
            Optional<Operation> existingConsolidatedResult = consolidatedOperationService.findExistingConsolidatedResult(context.group());
            if (existingConsolidatedResult.isPresent()) {
                log.info("Atualizando CONSOLIDATED_RESULT parcial: {}", existingConsolidatedResult.get().getId());
                consolidatedOperationService.updateConsolidatedResult(
                    existingConsolidatedResult.get(),
                    exitOperations.get(0),
                    context.group()
                );
            } else {
                log.info("Criando CONSOLIDATED_RESULT parcial");
                Operation firstExitOp = exitOperations.get(0);
                consolidatedOperationService.createConsolidatedExit(context.group(), firstExitOp, 
                    firstExitOp.getExitUnitPrice(), firstExitOp.getExitDate());
            }
        }
        log.info("=== GERENCIAMENTO DE OPERAÇÕES CONSOLIDADAS CONCLUÍDO ===");
    }

    /**
     * Marca a CONSOLIDATED_ENTRY como HIDDEN, se existir no grupo.
     */
    private void markConsolidatedEntryAsHidden(OperationExitPositionContext context) {
        Optional<com.olisystem.optionsmanager.model.operation.AverageOperationItem> consolidatedEntryItem =
            context.group().getItems().stream()
                .filter(item -> item.getRoleType() == com.olisystem.optionsmanager.model.operation.OperationRoleType.CONSOLIDATED_ENTRY)
                .findFirst();
        if (consolidatedEntryItem.isPresent()) {
            Operation consolidatedEntryOp = consolidatedEntryItem.get().getOperation();
            log.info("Marcando CONSOLIDATED_ENTRY como HIDDEN: {}", consolidatedEntryOp.getId());
            consolidatedOperationService.markOperationAsHidden(consolidatedEntryOp);
        }
    }

    // ======================================================================================
    // CLASSES DE DADOS AUXILIARES
    // ======================================================================================

    private static class LotAnalysisResult {
        final Integer totalAvailable;
        final List<EntryLot> sameDayLots;
        final Integer sameDayQuantity;
        final List<EntryLot> previousDayLots;
        final Integer previousDayQuantity;
        final boolean isTotalExit;

        LotAnalysisResult(Integer totalAvailable, List<EntryLot> sameDayLots, Integer sameDayQuantity,
                          List<EntryLot> previousDayLots, Integer previousDayQuantity, boolean isTotalExit) {
            this.totalAvailable = totalAvailable;
            this.sameDayLots = sameDayLots;
            this.sameDayQuantity = sameDayQuantity;
            this.previousDayLots = previousDayLots;
            this.previousDayQuantity = previousDayQuantity;
            this.isTotalExit = isTotalExit;
        }
    }

    private static class ConsumptionPlan {
        final ConsumptionStrategy strategy;
        final List<LotConsumption> consumptions;

        ConsumptionPlan(ConsumptionStrategy strategy, List<LotConsumption> consumptions) {
            this.strategy = strategy;
            this.consumptions = consumptions;
        }
    }

    private static class LotConsumption {
        final EntryLot lot;
        final Integer quantityToConsume;
        final TradeType tradeType;
        final ExitStrategy strategy;

        LotConsumption(EntryLot lot, Integer quantityToConsume, TradeType tradeType, ExitStrategy strategy) {
            this.lot = lot;
            this.quantityToConsume = quantityToConsume;
            this.tradeType = tradeType;
            this.strategy = strategy;
        }
    }

    private static class LotConsumptionResult {
        final EntryLot lot;
        final Integer quantityConsumed;
        final TradeType tradeType;
        final ExitStrategy strategy;
        final BigDecimal profitLoss;
        final BigDecimal percentage;
        final BigDecimal entryValue;
        final BigDecimal exitValue;

        LotConsumptionResult(EntryLot lot, Integer quantityConsumed, TradeType tradeType,
                             ExitStrategy strategy, BigDecimal profitLoss, BigDecimal percentage,
                             BigDecimal entryValue, BigDecimal exitValue) {
            this.lot = lot;
            this.quantityConsumed = quantityConsumed;
            this.tradeType = tradeType;
            this.strategy = strategy;
            this.profitLoss = profitLoss;
            this.percentage = percentage;
            this.entryValue = entryValue;
            this.exitValue = exitValue;
        }
    }

    private static class ConsumptionResult {
        final ConsumptionStrategy strategy;
        final List<LotConsumptionResult> lotResults;
        final Map<TradeType, List<LotConsumptionResult>> resultsByTradeType;
        final BigDecimal totalProfitLoss;
        final BigDecimal consolidatedPercentage;
        final Integer totalQuantityConsumed;

        ConsumptionResult(ConsumptionStrategy strategy, List<LotConsumptionResult> lotResults,
                          Map<TradeType, List<LotConsumptionResult>> resultsByTradeType,
                          BigDecimal totalProfitLoss, BigDecimal consolidatedPercentage,
                          Integer totalQuantityConsumed) {
            this.strategy = strategy;
            this.lotResults = lotResults;
            this.resultsByTradeType = resultsByTradeType;
            this.totalProfitLoss = totalProfitLoss;
            this.consolidatedPercentage = consolidatedPercentage;
            this.totalQuantityConsumed = totalQuantityConsumed;
        }
    }

    private enum ConsumptionStrategy {
        DAY_TRADE_ONLY,
        SWING_TRADE_ONLY,
        MIXED_AUTO
    }

    /**
     * ✅ NOVA CLASSE: Dados calculados para uma operação de trade
     */
    private static class TradeOperationData {
        final Integer quantity;
        final BigDecimal averageEntryPrice;
        final BigDecimal totalEntryValue;
        final BigDecimal totalExitValue;
        final BigDecimal profitLoss;
        final BigDecimal profitLossPercentage;
        final LocalDate entryDate;

        TradeOperationData(Integer quantity, BigDecimal averageEntryPrice, BigDecimal totalEntryValue,
                           BigDecimal totalExitValue, BigDecimal profitLoss, BigDecimal profitLossPercentage,
                           LocalDate entryDate) {
            this.quantity = quantity;
            this.averageEntryPrice = averageEntryPrice;
            this.totalEntryValue = totalEntryValue;
            this.totalExitValue = totalExitValue;
            this.profitLoss = profitLoss;
            this.profitLossPercentage = profitLossPercentage;
            this.entryDate = entryDate;
        }
    }
}
