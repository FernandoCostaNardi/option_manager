package com.olisystem.optionsmanager.service.invoice.processing.detection;

import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.enums.OperationMappingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para decidir como processar cada item da invoice
 * Determina se deve criar nova operação ou finalizar existente
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationMatchingService {

    private final ActiveOperationDetector activeOperationDetector;
    private final TradeTypeAnalyzer tradeTypeAnalyzer;

    /**
     * Processa matching de operações para uma lista de invoice items
     * 
     * @param invoiceItems Itens a serem processados
     * @param activeOperationResult Resultado da detecção de operações ativas
     * @param tradeAnalysisResult Resultado da análise de tipos de trade
     * @return Plano de processamento para cada item
     */
    public OperationMatchingResult processMatching(
            List<InvoiceItem> invoiceItems,
            ActiveOperationDetector.ActiveOperationDetectionResult activeOperationResult,
            TradeTypeAnalyzer.TradeTypeAnalysisResult tradeAnalysisResult) {
        
        log.debug("🔍 Processando matching para {} itens", invoiceItems.size());
        
        List<ItemProcessingPlan> processingPlans = new ArrayList<>();
        Map<String, Integer> operationCountByType = new HashMap<>();
        
        // Processar Day Trades primeiro (prioridade alta)
        processDayTrades(tradeAnalysisResult.getDayTradeGroups(), processingPlans, operationCountByType);
        
        // Processar Swing Trades (podem finalizar operações existentes)
        processSwingTrades(tradeAnalysisResult.getSwingTradeItems(), activeOperationResult, 
                          processingPlans, operationCountByType);
        
        // Processar itens órfãos (vendas sem compra correspondente)
        processOrphanItems(tradeAnalysisResult.getOrphanItems(), activeOperationResult, 
                          processingPlans, operationCountByType);
        
        OperationMatchingResult result = OperationMatchingResult.builder()
                .processingPlans(processingPlans)
                .operationCountByType(operationCountByType)
                .totalItemsToProcess(processingPlans.size())
                .newOperationsCount(operationCountByType.getOrDefault("NEW_OPERATION", 0))
                .existingOperationExitsCount(operationCountByType.getOrDefault("EXISTING_OPERATION_EXIT", 0))
                .dayTradeOperationsCount(operationCountByType.getOrDefault("DAY_TRADE_ENTRY", 0) + 
                                       operationCountByType.getOrDefault("DAY_TRADE_EXIT", 0))
                .skippedItemsCount(operationCountByType.getOrDefault("SKIPPED", 0))
                .build();
        
        log.info("✅ Matching concluído: {} itens processados, {} novas operações, {} finalizações, {} day trades",
                 result.getTotalItemsToProcess(), result.getNewOperationsCount(),
                 result.getExistingOperationExitsCount(), result.getDayTradeOperationsCount());
        
        return result;
    }

    /**
     * Processa grupos de Day Trade
     */
    private void processDayTrades(List<TradeTypeAnalyzer.DayTradeGroup> dayTradeGroups,
                                List<ItemProcessingPlan> processingPlans,
                                Map<String, Integer> operationCountByType) {
        
        for (TradeTypeAnalyzer.DayTradeGroup group : dayTradeGroups) {
            log.debug("🎯 Processando Day Trade: {}", group.getAssetCode());
            
            // Criar planos para compras (entradas)
            for (InvoiceItem buyItem : group.getBuyItems()) {
                ItemProcessingPlan plan = ItemProcessingPlan.builder()
                        .invoiceItem(buyItem)
                        .mappingType(OperationMappingType.DAY_TRADE_ENTRY)
                        .tradeType(TradeType.DAY)
                        .targetOperation(null) // Nova operação
                        .notes("Day Trade - Entrada")
                        .priority(1) // Alta prioridade
                        .requiresNewOperation(true)
                        .build();
                
                processingPlans.add(plan);
                incrementCounter(operationCountByType, "DAY_TRADE_ENTRY");
            }
            
            // Criar planos para vendas (saídas)
            for (InvoiceItem sellItem : group.getSellItems()) {
                ItemProcessingPlan plan = ItemProcessingPlan.builder()
                        .invoiceItem(sellItem)
                        .mappingType(OperationMappingType.DAY_TRADE_EXIT)
                        .tradeType(TradeType.DAY)
                        .targetOperation(null) // Será resolvido durante execução
                        .notes("Day Trade - Saída")
                        .priority(2) // Processar após entrada
                        .requiresNewOperation(false)
                        .dependsOnPreviousItem(true)
                        .build();
                
                processingPlans.add(plan);
                incrementCounter(operationCountByType, "DAY_TRADE_EXIT");
            }
        }
    }

    /**
     * Processa Swing Trades (podem ser novas operações ou finalizações)
     */
    private void processSwingTrades(List<InvoiceItem> swingTradeItems,
                                  ActiveOperationDetector.ActiveOperationDetectionResult activeOperationResult,
                                  List<ItemProcessingPlan> processingPlans,
                                  Map<String, Integer> operationCountByType) {
        
        for (InvoiceItem item : swingTradeItems) {
            ItemProcessingPlan plan = createSwingTradePlan(item, activeOperationResult);
            processingPlans.add(plan);
            incrementCounter(operationCountByType, plan.getMappingType().name());
            
            log.debug("📊 Swing Trade {}: {} -> {}", 
                     item.getAssetCode(), item.getOperationType(), plan.getMappingType());
        }
    }

    /**
     * Processa itens órfãos (vendas sem compra correspondente)
     */
    private void processOrphanItems(List<InvoiceItem> orphanItems,
                                  ActiveOperationDetector.ActiveOperationDetectionResult activeOperationResult,
                                  List<ItemProcessingPlan> processingPlans,
                                  Map<String, Integer> operationCountByType) {
        
        for (InvoiceItem item : orphanItems) {
            ItemProcessingPlan plan = createOrphanItemPlan(item, activeOperationResult);
            processingPlans.add(plan);
            incrementCounter(operationCountByType, plan.getMappingType().name());
            
            log.debug("🔍 Item órfão {}: {} -> {}", 
                     item.getAssetCode(), item.getOperationType(), plan.getMappingType());
        }
    }

    /**
     * Cria plano para Swing Trade
     */
    private ItemProcessingPlan createSwingTradePlan(InvoiceItem item,
                                                  ActiveOperationDetector.ActiveOperationDetectionResult activeOperationResult) {
        
        // Compras normalmente criam novas operações (posições abertas)
        if ("C".equals(item.getOperationType())) {
            return ItemProcessingPlan.builder()
                    .invoiceItem(item)
                    .mappingType(OperationMappingType.SWING_TRADE_ENTRY)
                    .tradeType(TradeType.SWING)
                    .targetOperation(null)
                    .notes("Swing Trade - Nova posição")
                    .priority(3)
                    .requiresNewOperation(true)
                    .build();
        }
        
        // Vendas podem finalizar operações existentes
        else if ("V".equals(item.getOperationType())) {
            List<Operation> activeOperations = activeOperationResult.getItemMatches()
                    .getOrDefault(item, new ArrayList<>());
            
            if (!activeOperations.isEmpty()) {
                // Escolher melhor operação para finalizar
                Operation bestMatch = chooseBestOperationMatch(item, activeOperations);
                
                return ItemProcessingPlan.builder()
                        .invoiceItem(item)
                        .mappingType(OperationMappingType.SWING_TRADE_EXIT)
                        .tradeType(TradeType.SWING)
                        .targetOperation(bestMatch)
                        .notes("Swing Trade - Finalização de posição existente")
                        .priority(4)
                        .requiresNewOperation(false)
                        .build();
            } else {
                // Venda sem operação ativa correspondente - item órfão
                return ItemProcessingPlan.builder()
                        .invoiceItem(item)
                        .mappingType(OperationMappingType.SKIPPED)
                        .tradeType(TradeType.SWING)
                        .targetOperation(null)
                        .notes("Venda sem operação ativa correspondente")
                        .priority(10)
                        .requiresNewOperation(false)
                        .build();
            }
        }
        
        // Tipo desconhecido
        return createSkippedPlan(item, "Tipo de operação desconhecido");
    }

    /**
     * Cria plano para item órfão
     */
    private ItemProcessingPlan createOrphanItemPlan(InvoiceItem item,
                                                  ActiveOperationDetector.ActiveOperationDetectionResult activeOperationResult) {
        
        // Vendas órfãs podem finalizar operações existentes
        if ("V".equals(item.getOperationType())) {
            List<Operation> activeOperations = activeOperationResult.getItemMatches()
                    .getOrDefault(item, new ArrayList<>());
            
            if (!activeOperations.isEmpty()) {
                Operation bestMatch = chooseBestOperationMatch(item, activeOperations);
                
                return ItemProcessingPlan.builder()
                        .invoiceItem(item)
                        .mappingType(OperationMappingType.EXISTING_OPERATION_EXIT)
                        .tradeType(TradeType.SWING)
                        .targetOperation(bestMatch)
                        .notes("Finalização de operação de outro período")
                        .priority(5)
                        .requiresNewOperation(false)
                        .build();
            }
        }
        
        // Item não pode ser processado
        return createSkippedPlan(item, "Item órfão sem operação correspondente");
    }

    /**
     * Escolhe a melhor operação para fazer match com o item
     */
    private Operation chooseBestOperationMatch(InvoiceItem item, List<Operation> operations) {
        if (operations.size() == 1) {
            return operations.get(0);
        }
        
        // Critérios de escolha (em ordem de prioridade):
        // 1. Operação mais antiga (FIFO)
        // 2. Operação com quantidade mais próxima
        // 3. Operação com melhor margem de lucro potencial
        
        return operations.stream()
                .min(Comparator
                    .comparing(Operation::getEntryDate)
                    .thenComparing(op -> Math.abs(op.getQuantity() - item.getQuantity()))
                    .thenComparing(op -> calculatePotentialProfit(item, op))
                )
                .orElse(operations.get(0));
    }

    /**
     * Calcula lucro potencial da operação
     */
    private double calculatePotentialProfit(InvoiceItem sellItem, Operation buyOperation) {
        if (sellItem.getUnitPrice() == null || buyOperation.getEntryUnitPrice() == null) {
            return 0.0;
        }
        
        double sellPrice = sellItem.getUnitPrice().doubleValue();
        double buyPrice = buyOperation.getEntryUnitPrice().doubleValue();
        
        return (sellPrice - buyPrice) * Math.min(sellItem.getQuantity(), buyOperation.getQuantity());
    }

    /**
     * Cria plano para item ignorado
     */
    private ItemProcessingPlan createSkippedPlan(InvoiceItem item, String reason) {
        return ItemProcessingPlan.builder()
                .invoiceItem(item)
                .mappingType(OperationMappingType.SKIPPED)
                .tradeType(TradeType.SWING)
                .targetOperation(null)
                .notes(reason)
                .priority(99)
                .requiresNewOperation(false)
                .build();
    }

    /**
     * Incrementa contador no mapa
     */
    private void incrementCounter(Map<String, Integer> counters, String key) {
        counters.put(key, counters.getOrDefault(key, 0) + 1);
    }

    /**
     * Plano de processamento para um item específico
     */
    @lombok.Builder
    @lombok.Data
    public static class ItemProcessingPlan {
        private InvoiceItem invoiceItem;
        private OperationMappingType mappingType;
        private TradeType tradeType;
        private Operation targetOperation; // null para novas operações
        private String notes;
        private int priority; // menor = maior prioridade
        private boolean requiresNewOperation;
        private boolean dependsOnPreviousItem;
        
        /**
         * Verifica se o plano requer criação de nova operação
         */
        public boolean isNewOperation() {
            return requiresNewOperation || targetOperation == null;
        }
        
        /**
         * Verifica se o plano é para finalizar operação existente
         */
        public boolean isExitOperation() {
            return !isNewOperation() && targetOperation != null;
        }
        
        /**
         * Verifica se o item deve ser ignorado
         */
        public boolean isSkipped() {
            return mappingType == OperationMappingType.SKIPPED;
        }
    }

    /**
     * Resultado do matching de operações
     */
    @lombok.Builder
    @lombok.Data
    public static class OperationMatchingResult {
        private List<ItemProcessingPlan> processingPlans;
        private Map<String, Integer> operationCountByType;
        private int totalItemsToProcess;
        private int newOperationsCount;
        private int existingOperationExitsCount;
        private int dayTradeOperationsCount;
        private int skippedItemsCount;
        
        /**
         * Retorna planos ordenados por prioridade
         */
        public List<ItemProcessingPlan> getOrderedPlans() {
            return processingPlans.stream()
                    .sorted(Comparator.comparing(ItemProcessingPlan::getPriority))
                    .collect(Collectors.toList());
        }
        
        /**
         * Filtra planos por tipo
         */
        public List<ItemProcessingPlan> getPlansByType(OperationMappingType type) {
            return processingPlans.stream()
                    .filter(plan -> plan.getMappingType() == type)
                    .collect(Collectors.toList());
        }
        
        /**
         * Verifica se há algum trabalho a ser feito
         */
        public boolean hasWorkToDo() {
            return totalItemsToProcess > skippedItemsCount;
        }
        
        /**
         * Retorna percentual de itens que serão processados
         */
        public double getProcessingRate() {
            return totalItemsToProcess > 0 ? 
                   (double) (totalItemsToProcess - skippedItemsCount) / totalItemsToProcess * 100 : 0;
        }
    }
}