package com.olisystem.optionsmanager.service.operation.strategy.processor;

import com.olisystem.optionsmanager.exception.BusinessException;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.record.consumption.ComplexConsumptionPlan;
import com.olisystem.optionsmanager.record.consumption.ComplexConsumptionResult;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.service.operation.detector.ComplexScenarioDetector;
import com.olisystem.optionsmanager.service.operation.engine.ComplexLotConsumptionEngine;
import com.olisystem.optionsmanager.service.operation.validation.MultiLotExitValidator;
import com.olisystem.optionsmanager.service.operation.consolidate.ConsolidatedOperationService;
import com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Processor responsável por processar cenários complexos 3.2.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplexScenarioProcessor {

    private final MultiLotExitValidator multiLotExitValidator;
    private final ComplexScenarioDetector complexScenarioDetector;
    private final ComplexLotConsumptionEngine consumptionEngine;
    private final ComplexOperationCreator operationCreator;
    private final ComplexAverageUpdater averageUpdater;
    private final ConsolidatedOperationService consolidatedOperationService;
    private final AverageOperationService averageOperationService;

    @Transactional
    public Operation process(OperationExitPositionContext context) {
        
        log.info("=== INICIANDO PROCESSAMENTO CENÁRIO COMPLEXO 3.2 ===");
        log.info("Operação ID: {}, Posição ID: {}, Quantidade: {}", 
                context.context().activeOperation().getId(),
                context.position().getId(),
                context.context().request().getQuantity());

        try {
            // FASE 1: Validações específicas para cenário complexo
            performComplexValidations(context);

            // FASE 2: Confirmar que é realmente um cenário complexo
            confirmComplexScenario(context);

            // FASE 3: Criar plano de consumo inteligente
            ComplexConsumptionPlan plan = createConsumptionPlan(context);

            // FASE 4: Executar consumo dos lotes
            ComplexConsumptionResult result = executeConsumption(plan, context);

            // FASE 5: Criar operação(ões) de saída (sempre criar)
            Operation primaryExitOperation = operationCreator.createComplexExitOperations(context, result);
            log.info("Operação de saída criada: {} (quantidade: {}, P&L: {})", 
                     primaryExitOperation.getId(), primaryExitOperation.getQuantity(), primaryExitOperation.getProfitLoss());

            // FASE 5.5: Gerenciar operações consolidadas (se existem)
            manageConsolidatedOperations(context, primaryExitOperation, result);

            // FASE 6: Atualizar estruturas agregadas
            averageUpdater.updateAfterComplexExit(context.position(), context.group(), result);

            log.info("=== PROCESSAMENTO COMPLEXO CONCLUÍDO COM SUCESSO ===");
            return primaryExitOperation;

        } catch (Exception e) {
            log.error("Erro durante processamento complexo para operação {}: {}", 
                    context.context().activeOperation().getId(), e.getMessage(), e);
            throw new BusinessException("Falha no processamento do cenário complexo: " + e.getMessage());
        }
    }

    // ======================================================================================
    // FASES DO PROCESSAMENTO
    // ======================================================================================

    private void performComplexValidations(OperationExitPositionContext context) {
        log.debug("FASE 1: Executando validações específicas do cenário complexo");
        
        // Validação básica de Position
        multiLotExitValidator.validatePositionForComplexExit(context.position());
        
        // Validação de quantidade disponível para múltiplos lotes
        multiLotExitValidator.validateMultiLotExit(
            context.position(), 
            context.context().request().getQuantity()
        );
        
        log.debug("Validações complexas aprovadas");
    }

    private void confirmComplexScenario(OperationExitPositionContext context) {
        log.debug("FASE 2: Confirmando que é um cenário complexo");
        
        boolean isComplex = complexScenarioDetector.isComplexScenario(
            context.position(), 
            context.context().request().getQuantity()
        );
        
        if (!isComplex) {
            throw new BusinessException(
                "Cenário não é classificado como complexo - deve ser processado por outro processor"
            );
        }
        
        ComplexScenarioDetector.ScenarioType scenario = complexScenarioDetector.detectScenario(
            context.position(), context.context().request().getQuantity()
        );
        
        log.info("Cenário confirmado: {}", scenario);
    }

    private ComplexConsumptionPlan createConsumptionPlan(OperationExitPositionContext context) {
        log.debug("FASE 3: Criando plano de consumo inteligente");
        
        ComplexConsumptionPlan plan = consumptionEngine.createConsumptionPlan(
            context.position(), 
            context.context().request()
        );
        
        log.info("Plano criado: {} lotes serão consumidos, {} Day Trade, {} Swing Trade", 
                plan.consumptions().size(), 
                plan.getDayTradeQuantity(), 
                plan.getSwingTradeQuantity());
        
        return plan;
    }

    private ComplexConsumptionResult executeConsumption(ComplexConsumptionPlan plan, 
                                                      OperationExitPositionContext context) {
        log.debug("FASE 4: Executando consumo dos lotes");
        
        ComplexConsumptionResult result = consumptionEngine.executeConsumption(
            plan, 
            context.context().request().getExitUnitPrice()
        );
        
        log.info("Consumo executado: P&L total = {}, {} Day Trade + {} Swing Trade", 
                result.totalProfitLoss(), 
                result.totalDayTradeProfitLoss(), 
                result.totalSwingTradeProfitLoss());
        
        return result;
    }

    /**
     * Gerencia APENAS operações consolidadas sem criar operação normal
     * (usado quando há operações consolidadas existentes)
     */
    private Operation manageConsolidatedOperationsOnly(OperationExitPositionContext context, 
                                                      ComplexConsumptionResult result) {
        
        log.info("=== GERENCIANDO APENAS OPERAÇÕES CONSOLIDADAS ===");
        
        // Verificar se é saída total
        boolean isTotalExit = context.position().getRemainingQuantity() == result.totalQuantityConsumed();
        
        // Buscar CONSOLIDATED_RESULT existente
        Optional<Operation> existingConsolidatedResult = 
            consolidatedOperationService.findExistingConsolidatedResult(context.group());
        
        Operation consolidatedOperation;
        
        if (existingConsolidatedResult.isPresent()) {
            log.info("Atualizando CONSOLIDATED_RESULT existente");
            
            // Criar uma operação temporária para os cálculos
            Operation tempExitOperation = createTemporaryExitOperation(context, result);
            
            // Atualizar CONSOLIDATED_RESULT com dados da saída
            consolidatedOperation = consolidatedOperationService.updateConsolidatedResult(
                existingConsolidatedResult.get(), 
                tempExitOperation, 
                context.group()
            );
            
            if (isTotalExit) {
                log.info("Saída total - transformando CONSOLIDATED_RESULT em TOTAL_EXIT");
                
                // Transformar em TOTAL_EXIT
                consolidatedOperation = consolidatedOperationService.transformToTotalExit(
                    consolidatedOperation, 
                    context.group()
                );
                
                // Marcar CONSOLIDATED_ENTRY como HIDDEN
                markConsolidatedEntryAsHidden(context);
            }
            
        } else {
            log.info("Criando primeira CONSOLIDATED_RESULT");
            
            // Criar uma operação temporária para os cálculos
            Operation tempExitOperation = createTemporaryExitOperation(context, result);
            
            // Criar CONSOLIDATED_RESULT
            consolidatedOperation = consolidatedOperationService.createConsolidatedExit(
                context.group(), tempExitOperation, 
                tempExitOperation.getExitUnitPrice(), 
                tempExitOperation.getExitDate());
            
            if (isTotalExit) {
                log.info("Saída total imediata - transformando em TOTAL_EXIT");
                consolidatedOperation = consolidatedOperationService.transformToTotalExit(
                    consolidatedOperation, context.group());
                markConsolidatedEntryAsHidden(context);
            }
        }
        
        log.info("=== GERENCIAMENTO DE CONSOLIDADAS CONCLUÍDO ===");
        return consolidatedOperation;
    }

    /**
     * Cria operação temporária com dados corretos (status e tradeType)
     */
    private Operation createTemporaryExitOperation(OperationExitPositionContext context, 
                                                  ComplexConsumptionResult result) {
        
        // Dados básicos da requisição
        int totalQuantity = context.context().request().getQuantity();
        java.math.BigDecimal exitUnitPrice = context.context().request().getExitUnitPrice();
        java.math.BigDecimal exitTotalValue = exitUnitPrice.multiply(java.math.BigDecimal.valueOf(totalQuantity));
        java.time.LocalDate exitDate = context.context().request().getExitDate();
        
        // VALIDAÇÃO 1: Determinar STATUS baseado no P&L
        com.olisystem.optionsmanager.model.operation.OperationStatus status;
        if (result.totalProfitLoss().compareTo(java.math.BigDecimal.ZERO) > 0) {
            status = com.olisystem.optionsmanager.model.operation.OperationStatus.WINNER;
        } else if (result.totalProfitLoss().compareTo(java.math.BigDecimal.ZERO) < 0) {
            status = com.olisystem.optionsmanager.model.operation.OperationStatus.LOSER;
        } else {
            status = OperationStatus.NEUTRAl; // Empate
        }
        
        // VALIDAÇÃO 2: Determinar TRADE TYPE baseado nos lotes consumidos
        com.olisystem.optionsmanager.model.operation.TradeType tradeType = determineTradeTypeFromConsumption(
            context, exitDate, result);
        
        log.debug("Operação temporária: Status={}, TradeType={}, P&L={}", 
                  status, tradeType, result.totalProfitLoss());
        
        return com.olisystem.optionsmanager.model.operation.Operation.builder()
                .optionSeries(context.position().getOptionSeries())
                .brokerage(context.position().getBrokerage())
                .transactionType(TransactionType.SELL)
                .status(status) // ✅ Status correto baseado no P&L
                .tradeType(tradeType) // ✅ TradeType correto baseado nos lotes
                .quantity(totalQuantity)
                .exitUnitPrice(exitUnitPrice)
                .exitTotalValue(exitTotalValue)
                .profitLoss(result.totalProfitLoss())
                .profitLossPercentage(result.getTotalProfitLossPercentage())
                .exitDate(exitDate)
                .user(context.position().getUser())
                .build();
    }

    /**
     * Determina o TradeType baseado nos lotes consumidos
     */
    private com.olisystem.optionsmanager.model.operation.TradeType determineTradeTypeFromConsumption(
            OperationExitPositionContext context, 
            java.time.LocalDate exitDate, 
            ComplexConsumptionResult result) {
        
        // Se há tanto Day Trade quanto Swing Trade, priorizar Day Trade
        // (regra comum em sistemas de trading)
        if (result.dayTradeQuantity() > 0) {
            log.debug("Day Trade detectado: {} unidades no mesmo dia", result.dayTradeQuantity());
            return com.olisystem.optionsmanager.model.operation.TradeType.DAY;
        } else if (result.swingTradeQuantity() > 0) {
            log.debug("Swing Trade detectado: {} unidades em dias diferentes", result.swingTradeQuantity());
            return com.olisystem.optionsmanager.model.operation.TradeType.SWING;
        } else {
            // Fallback: comparar data de saída com o lote mais antigo
            java.time.LocalDate oldestLotDate = context.availableLots().stream()
                .map(lot -> lot.getEntryDate())
                .min(java.time.LocalDate::compareTo)
                .orElse(exitDate);
                
            boolean isSameDay = oldestLotDate.equals(exitDate);
            log.debug("Fallback - comparando datas: entrada={}, saída={}, mesmo dia={}", 
                      oldestLotDate, exitDate, isSameDay);
            
            return isSameDay ? 
                com.olisystem.optionsmanager.model.operation.TradeType.DAY : 
                com.olisystem.optionsmanager.model.operation.TradeType.SWING;
        }
    }

    /**
     * Marca CONSOLIDATED_ENTRY como HIDDEN
     */
    private void markConsolidatedEntryAsHidden(OperationExitPositionContext context) {
        context.group().getItems().stream()
            .filter(item -> item.getRoleType() == 
                    com.olisystem.optionsmanager.model.operation.OperationRoleType.CONSOLIDATED_ENTRY)
            .findFirst()
            .ifPresent(item -> {
                log.info("Marcando CONSOLIDATED_ENTRY como HIDDEN: {}", item.getOperation().getId());
                consolidatedOperationService.markOperationAsHidden(item.getOperation());
            });
    }

    /**
     * Gerencia operações consolidadas quando existem
     * FLUXO CORRETO IMPLEMENTADO:
     * 1. Operação normal já foi criada
     * 2. Marcar operação normal como HIDDEN 
     * 3. Adicionar ao grupo como TOTAL_EXIT (passo 9)
     * 4. Atualizar CONSOLIDATED_RESULT (passo 10) - NÃO transformar
     * 5. Marcar CONSOLIDATED_ENTRY como HIDDEN (passo 11)
     */
    private void manageConsolidatedOperations(OperationExitPositionContext context, 
                                            Operation primaryExitOperation, 
                                            ComplexConsumptionResult result) {
        
        // Verificar se há operações consolidadas
        boolean hasConsolidated = consolidatedOperationService.hasConsolidatedOperations(
                context.position().getUser(), 
                context.position().getOptionSeries(), 
                context.position().getBrokerage()
        );
        
        if (!hasConsolidated) {
            log.debug("Nenhuma operação consolidada encontrada - pular gerenciamento");
            return;
        }
        
        log.info("=== GERENCIANDO OPERAÇÕES CONSOLIDADAS (COMPLEX SCENARIO) ===");
        
        // PASSO 1: Marcar operação normal como HIDDEN (não deve aparecer na listagem)
        log.info("Marcando operação normal como HIDDEN: {}", primaryExitOperation.getId());
        consolidatedOperationService.markOperationAsHidden(primaryExitOperation);
        
        // PASSO 2: Verificar se é saída total
        boolean isTotalExit = context.position().getRemainingQuantity() == result.totalQuantityConsumed();
        log.info("Tipo de saída: {} (quantidade restante: {}, consumida: {})", 
                 isTotalExit ? "TOTAL" : "PARCIAL", 
                 context.position().getRemainingQuantity(), 
                 result.totalQuantityConsumed());
        
        if (isTotalExit) {
            // PASSO 3: Adicionar como TOTAL_EXIT no grupo (passo 9)
            averageOperationService.addNewItemGroup(context.group(), primaryExitOperation, 
                    com.olisystem.optionsmanager.model.operation.OperationRoleType.TOTAL_EXIT);
            log.info("Operação de saída final adicionada ao grupo como TOTAL_EXIT: {}", primaryExitOperation.getId());
            
            // PASSO 4: Atualizar CONSOLIDATED_RESULT (passo 10) - NÃO transformar em TOTAL_EXIT
            Optional<Operation> existingConsolidatedResult = 
                consolidatedOperationService.findExistingConsolidatedResult(context.group());
            
            if (existingConsolidatedResult.isPresent()) {
                log.info("Atualizando CONSOLIDATED_RESULT final: {}", existingConsolidatedResult.get().getId());
                consolidatedOperationService.updateConsolidatedResult(
                    existingConsolidatedResult.get(), 
                    primaryExitOperation, 
                    context.group()
                );
            } else {
                log.info("Criando CONSOLIDATED_RESULT final");
                consolidatedOperationService.createConsolidatedExit(context.group(), primaryExitOperation, 
                    primaryExitOperation.getExitUnitPrice(), 
                    primaryExitOperation.getExitDate());
            }
            
            // PASSO 5: Marcar CONSOLIDATED_ENTRY como HIDDEN (passo 11)
            markConsolidatedEntryAsHidden(context);
            
        } else {
            log.info("Saída parcial - gerenciamento futuro (não implementado ainda)");
        }
        
        log.info("=== GERENCIAMENTO DE OPERAÇÕES CONSOLIDADAS CONCLUÍDO ===");
    }
}
