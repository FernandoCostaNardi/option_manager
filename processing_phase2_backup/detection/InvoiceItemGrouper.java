package com.olisystem.optionsmanager.service.invoice.processing.detection;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.enums.OperationMappingType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para agrupar itens relacionados de uma invoice
 * Facilita o processamento em lote e organiza dependências
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@Slf4j
public class InvoiceItemGrouper {

    /**
     * Agrupa itens de uma invoice para processamento otimizado
     * 
     * @param invoice Invoice a ser agrupada
     * @param matchingResult Resultado do matching de operações
     * @return Grupos organizados para processamento
     */
    public InvoiceGroupingResult groupInvoiceItems(
            Invoice invoice,
            OperationMatchingService.OperationMatchingResult matchingResult) {
        
        log.debug("🔍 Agrupando {} itens da invoice {}", 
                 invoice.getItems().size(), invoice.getInvoiceNumber());
        
        List<OperationMatchingService.ItemProcessingPlan> plans = matchingResult.getProcessingPlans();
        
        // Agrupar por diferentes critérios
        Map<String, ProcessingGroup> groupsByAsset = groupByAsset(plans);
        List<ProcessingGroup> groupsByDependency = groupByDependency(plans);
        List<ProcessingGroup> groupsByPriority = groupByPriority(plans);
        
        // Criar sequência de processamento otimizada
        List<ProcessingSequence> processingSequences = createProcessingSequences(plans);
        
        InvoiceGroupingResult result = InvoiceGroupingResult.builder()
                .groupsByAsset(groupsByAsset)
                .groupsByDependency(groupsByDependency)
                .groupsByPriority(groupsByPriority)
                .processingSequences(processingSequences)
                .totalGroups(groupsByAsset.size())
                .totalSequences(processingSequences.size())
                .isOptimizedForBatch(true)
                .build();
        
        log.info("✅ Agrupamento concluído: {} grupos por ativo, {} sequências de processamento",
                 result.getTotalGroups(), result.getTotalSequences());
        
        return result;
    }

    /**
     * Agrupa planos por código do ativo
     */
    private Map<String, ProcessingGroup> groupByAsset(List<OperationMatchingService.ItemProcessingPlan> plans) {
        Map<String, ProcessingGroup> groups = new LinkedHashMap<>();
        
        // Agrupar por código do ativo extraído
        Map<String, List<OperationMatchingService.ItemProcessingPlan>> plansByAsset = plans.stream()
                .collect(Collectors.groupingBy(
                    plan -> extractBaseAssetCode(plan.getInvoiceItem().getAssetCode()),
                    LinkedHashMap::new,
                    Collectors.toList()
                ));
        
        for (Map.Entry<String, List<OperationMatchingService.ItemProcessingPlan>> entry : plansByAsset.entrySet()) {
            String assetCode = entry.getKey();
            List<OperationMatchingService.ItemProcessingPlan> assetPlans = entry.getValue();
            
            ProcessingGroup group = ProcessingGroup.builder()
                    .groupType(GroupType.BY_ASSET)
                    .groupKey(assetCode)
                    .plans(assetPlans)
                    .totalItems(assetPlans.size())
                    .hasDayTrades(hasTradeType(assetPlans, TradeType.DAY))
                    .hasSwingTrades(hasTradeType(assetPlans, TradeType.SWING))
                    .requiresSequentialProcessing(hasInterdependentPlans(assetPlans))
                    .estimatedComplexity(calculateGroupComplexity(assetPlans))
                    .build();
            
            groups.put(assetCode, group);
            
            log.debug("📊 Grupo ativo {}: {} itens, complexidade {}, day trades: {}, swing trades: {}",
                     assetCode, group.getTotalItems(), group.getEstimatedComplexity(),
                     group.isHasDayTrades(), group.isHasSwingTrades());
        }
        
        return groups;
    }

    /**
     * Agrupa planos por dependências
     */
    private List<ProcessingGroup> groupByDependency(List<OperationMatchingService.ItemProcessingPlan> plans) {
        List<ProcessingGroup> groups = new ArrayList<>();
        
        // Separar planos independentes dos dependentes
        List<OperationMatchingService.ItemProcessingPlan> independentPlans = plans.stream()
                .filter(plan -> !plan.isDependsOnPreviousItem())
                .collect(Collectors.toList());
        
        List<OperationMatchingService.ItemProcessingPlan> dependentPlans = plans.stream()
                .filter(OperationMatchingService.ItemProcessingPlan::isDependsOnPreviousItem)
                .collect(Collectors.toList());
        
        // Grupo de planos independentes
        if (!independentPlans.isEmpty()) {
            ProcessingGroup independentGroup = ProcessingGroup.builder()
                    .groupType(GroupType.INDEPENDENT)
                    .groupKey("INDEPENDENT")
                    .plans(independentPlans)
                    .totalItems(independentPlans.size())
                    .requiresSequentialProcessing(false)
                    .canProcessInParallel(true)
                    .estimatedComplexity(calculateGroupComplexity(independentPlans))
                    .build();
            
            groups.add(independentGroup);
        }
        
        // Grupo de planos dependentes
        if (!dependentPlans.isEmpty()) {
            ProcessingGroup dependentGroup = ProcessingGroup.builder()
                    .groupType(GroupType.DEPENDENT)
                    .groupKey("DEPENDENT")
                    .plans(dependentPlans)
                    .totalItems(dependentPlans.size())
                    .requiresSequentialProcessing(true)
                    .canProcessInParallel(false)
                    .estimatedComplexity(calculateGroupComplexity(dependentPlans))
                    .build();
            
            groups.add(dependentGroup);
        }
        
        return groups;
    }

    /**
     * Agrupa planos por prioridade
     */
    private List<ProcessingGroup> groupByPriority(List<OperationMatchingService.ItemProcessingPlan> plans) {
        Map<Integer, List<OperationMatchingService.ItemProcessingPlan>> plansByPriority = plans.stream()
                .collect(Collectors.groupingBy(
                    OperationMatchingService.ItemProcessingPlan::getPriority,
                    TreeMap::new,
                    Collectors.toList()
                ));
        
        List<ProcessingGroup> groups = new ArrayList<>();
        
        for (Map.Entry<Integer, List<OperationMatchingService.ItemProcessingPlan>> entry : plansByPriority.entrySet()) {
            Integer priority = entry.getKey();
            List<OperationMatchingService.ItemProcessingPlan> priorityPlans = entry.getValue();
            
            ProcessingGroup group = ProcessingGroup.builder()
                    .groupType(GroupType.BY_PRIORITY)
                    .groupKey("PRIORITY_" + priority)
                    .plans(priorityPlans)
                    .totalItems(priorityPlans.size())
                    .priority(priority)
                    .canProcessInParallel(priority < 90) // Baixa prioridade pode ser paralela
                    .estimatedComplexity(calculateGroupComplexity(priorityPlans))
                    .build();
            
            groups.add(group);
        }
        
        return groups;
    }

    /**
     * Cria sequências de processamento otimizadas
     */
    private List<ProcessingSequence> createProcessingSequences(List<OperationMatchingService.ItemProcessingPlan> plans) {
        List<ProcessingSequence> sequences = new ArrayList<>();
        
        // Ordenar planos por prioridade
        List<OperationMatchingService.ItemProcessingPlan> orderedPlans = plans.stream()
                .sorted(Comparator.comparing(OperationMatchingService.ItemProcessingPlan::getPriority))
                .collect(Collectors.toList());
        
        ProcessingSequence currentSequence = null;
        int sequenceNumber = 1;
        
        for (OperationMatchingService.ItemProcessingPlan plan : orderedPlans) {
            // Criar nova sequência se necessário
            if (currentSequence == null || shouldStartNewSequence(currentSequence, plan)) {
                currentSequence = ProcessingSequence.builder()
                        .sequenceNumber(sequenceNumber++)
                        .plans(new ArrayList<>())
                        .canRunInParallel(true)
                        .estimatedDuration(0)
                        .build();
                
                sequences.add(currentSequence);
            }
            
            // Adicionar plano à sequência atual
            currentSequence.getPlans().add(plan);
            currentSequence.setEstimatedDuration(
                currentSequence.getEstimatedDuration() + estimateProcessingTime(plan)
            );
            
            // Verificar se sequência pode rodar em paralelo
            if (plan.isDependsOnPreviousItem() || plan.getPriority() <= 2) {
                currentSequence.setCanRunInParallel(false);
            }
        }
        
        // Otimizar sequências
        optimizeSequences(sequences);
        
        return sequences;
    }

    /**
     * Verifica se deve iniciar nova sequência
     */
    private boolean shouldStartNewSequence(ProcessingSequence currentSequence, 
                                         OperationMatchingService.ItemProcessingPlan plan) {
        
        // Limite de itens por sequência
        if (currentSequence.getPlans().size() >= 10) {
            return true;
        }
        
        // Mudança significativa de prioridade
        int currentPriority = currentSequence.getPlans().get(0).getPriority();
        if (Math.abs(plan.getPriority() - currentPriority) > 2) {
            return true;
        }
        
        // Planos que dependem de itens anteriores
        if (plan.isDependsOnPreviousItem()) {
            return true;
        }
        
        return false;
    }

    /**
     * Otimiza sequências para melhor performance
     */
    private void optimizeSequences(List<ProcessingSequence> sequences) {
        // Reagrupar sequências pequenas
        sequences.removeIf(seq -> seq.getPlans().isEmpty());
        
        // Balancear carga entre sequências paralelas
        List<ProcessingSequence> parallelSequences = sequences.stream()
                .filter(ProcessingSequence::isCanRunInParallel)
                .collect(Collectors.toList());
        
        if (parallelSequences.size() > 1) {
            balanceSequenceLoad(parallelSequences);
        }
    }

    /**
     * Balanceia carga entre sequências paralelas
     */
    private void balanceSequenceLoad(List<ProcessingSequence> sequences) {
        // Algoritmo simples de balanceamento
        sequences.sort(Comparator.comparing(ProcessingSequence::getEstimatedDuration));
        
        log.debug("🔧 Balanceando carga entre {} sequências paralelas", sequences.size());
    }

    /**
     * Calcula complexidade estimada de um grupo
     */
    private int calculateGroupComplexity(List<OperationMatchingService.ItemProcessingPlan> plans) {
        int complexity = 0;
        
        for (OperationMatchingService.ItemProcessingPlan plan : plans) {
            // Base: 10 pontos por item
            complexity += 10;
            
            // Day Trade: +5 pontos
            if (plan.getTradeType() == TradeType.DAY) {
                complexity += 5;
            }
            
            // Operação existente: +3 pontos
            if (plan.isExitOperation()) {
                complexity += 3;
            }
            
            // Dependência: +2 pontos
            if (plan.isDependsOnPreviousItem()) {
                complexity += 2;
            }
        }
        
        return complexity;
    }

    /**
     * Estima tempo de processamento de um plano
     */
    private int estimateProcessingTime(OperationMatchingService.ItemProcessingPlan plan) {
        int baseTime = 100; // milissegundos
        
        if (plan.getTradeType() == TradeType.DAY) {
            baseTime += 50;
        }
        
        if (plan.isExitOperation()) {
            baseTime += 30;
        }
        
        return baseTime;
    }

    /**
     * Verifica se há planos interdependentes
     */
    private boolean hasInterdependentPlans(List<OperationMatchingService.ItemProcessingPlan> plans) {
        return plans.stream().anyMatch(OperationMatchingService.ItemProcessingPlan::isDependsOnPreviousItem);
    }

    /**
     * Verifica se há planos de um tipo específico de trade
     */
    private boolean hasTradeType(List<OperationMatchingService.ItemProcessingPlan> plans, TradeType tradeType) {
        return plans.stream().anyMatch(plan -> plan.getTradeType() == tradeType);
    }

    /**
     * Extrai código base do ativo
     */
    private String extractBaseAssetCode(String fullAssetCode) {
        if (fullAssetCode == null) return "UNKNOWN";
        
        String cleaned = fullAssetCode.trim().toUpperCase();
        
        // Para opções, extrair código base
        if (cleaned.matches("^[A-Z]{4,5}[FE]\\d+$")) {
            return cleaned.substring(0, cleaned.length() - 4);
        }
        
        // Para códigos com sufixos ON/PN
        if (cleaned.contains(" ON") || cleaned.contains(" PN")) {
            return cleaned.split(" ")[0];
        }
        
        return cleaned;
    }

    /**
     * Tipos de agrupamento
     */
    public enum GroupType {
        BY_ASSET, BY_PRIORITY, INDEPENDENT, DEPENDENT
    }

    /**
     * Grupo de processamento
     */
    @lombok.Builder
    @lombok.Data
    public static class ProcessingGroup {
        private GroupType groupType;
        private String groupKey;
        private List<OperationMatchingService.ItemProcessingPlan> plans;
        private int totalItems;
        private boolean hasDayTrades;
        private boolean hasSwingTrades;
        private boolean requiresSequentialProcessing;
        private boolean canProcessInParallel;
        private int priority;
        private int estimatedComplexity;
        
        /**
         * Retorna planos ordenados por prioridade
         */
        public List<OperationMatchingService.ItemProcessingPlan> getOrderedPlans() {
            return plans.stream()
                    .sorted(Comparator.comparing(OperationMatchingService.ItemProcessingPlan::getPriority))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Sequência de processamento
     */
    @lombok.Builder
    @lombok.Data
    public static class ProcessingSequence {
        private int sequenceNumber;
        private List<OperationMatchingService.ItemProcessingPlan> plans;
        private boolean canRunInParallel;
        private int estimatedDuration; // em milissegundos
        
        /**
         * Retorna número de itens na sequência
         */
        public int getItemCount() {
            return plans.size();
        }
        
        /**
         * Verifica se a sequência está vazia
         */
        public boolean isEmpty() {
            return plans.isEmpty();
        }
    }

    /**
     * Resultado do agrupamento de uma invoice
     */
    @lombok.Builder
    @lombok.Data
    public static class InvoiceGroupingResult {
        private Map<String, ProcessingGroup> groupsByAsset;
        private List<ProcessingGroup> groupsByDependency;
        private List<ProcessingGroup> groupsByPriority;
        private List<ProcessingSequence> processingSequences;
        private int totalGroups;
        private int totalSequences;
        private boolean isOptimizedForBatch;
        
        /**
         * Retorna grupos que podem ser processados em paralelo
         */
        public List<ProcessingGroup> getParallelProcessableGroups() {
            return groupsByAsset.values().stream()
                    .filter(ProcessingGroup::isCanProcessInParallel)
                    .collect(Collectors.toList());
        }
        
        /**
         * Retorna sequências que podem rodar em paralelo
         */
        public List<ProcessingSequence> getParallelSequences() {
            return processingSequences.stream()
                    .filter(ProcessingSequence::isCanRunInParallel)
                    .collect(Collectors.toList());
        }
        
        /**
         * Calcula complexidade total estimada
         */
        public int getTotalComplexity() {
            return groupsByAsset.values().stream()
                    .mapToInt(ProcessingGroup::getEstimatedComplexity)
                    .sum();
        }
    }
}