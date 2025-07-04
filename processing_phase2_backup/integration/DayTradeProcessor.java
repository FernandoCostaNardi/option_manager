package com.olisystem.optionsmanager.service.invoice.processing.integration;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.service.invoice.processing.detection.TradeTypeAnalyzer.DayTradeGroup;
import com.olisystem.optionsmanager.service.invoice.processing.detection.OperationMatchingService.ItemProcessingPlan;
import com.olisystem.optionsmanager.model.enums.OperationMappingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço especializado para processar Day Trades completos
 * Coordena criação de entrada e finalização imediata
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DayTradeProcessor {

    private final NewOperationCreator operationCreator;
    private final ExistingOperationProcessor existingOperationProcessor;

    /**
     * Processa um grupo de Day Trade completo (entrada + saída)
     * 
     * @param dayTradeGroup Grupo de Day Trade com compras e vendas
     * @param entryPlans Planos para operações de entrada
     * @param exitPlans Planos para operações de saída
     * @return Resultado do processamento Day Trade
     */
    @Transactional
    public DayTradeProcessingResult processDayTradeGroup(
            DayTradeGroup dayTradeGroup,
            List<ItemProcessingPlan> entryPlans,
            List<ItemProcessingPlan> exitPlans) {
        
        log.debug("🎯 Processando Day Trade: {} (entradas: {}, saídas: {})",
                 dayTradeGroup.getAssetCode(), entryPlans.size(), exitPlans.size());
        
        validateDayTradeGroup(dayTradeGroup, entryPlans, exitPlans);
        
        List<Operation> entryOperations = new ArrayList<>();
        List<Operation> exitOperations = new ArrayList<>();
        List<ProcessingError> errors = new ArrayList<>();
        
        try {
            // Fase 1: Criar operações de entrada
            entryOperations = createDayTradeEntries(entryPlans, errors);
            
            // Fase 2: Finalizar com operações de saída (se entradas foram criadas)
            if (!entryOperations.isEmpty()) {
                exitOperations = processDayTradeExits(exitPlans, entryOperations, errors);
            }
            
        } catch (Exception e) {
            log.error("❌ Erro crítico no processamento Day Trade {}: {}", 
                     dayTradeGroup.getAssetCode(), e.getMessage(), e);
            
            errors.add(ProcessingError.builder()
                    .assetCode(dayTradeGroup.getAssetCode())
                    .errorMessage("Erro crítico: " + e.getMessage())
                    .exception(e)
                    .build());
        }
        
        DayTradeProcessingResult result = DayTradeProcessingResult.builder()
                .dayTradeGroup(dayTradeGroup)
                .entryOperations(entryOperations)
                .exitOperations(exitOperations)
                .errors(errors)
                .totalEntryPlans(entryPlans.size())
                .totalExitPlans(exitPlans.size())
                .successfulEntries(entryOperations.size())
                .successfulExits(exitOperations.size())
                .build();
        
        log.info("✅ Day Trade {} processado: {} entradas, {} saídas, {} erros",
                 dayTradeGroup.getAssetCode(), result.getSuccessfulEntries(),
                 result.getSuccessfulExits(), result.getErrors().size());
        
        return result;
    }

    /**
     * Processa múltiplos grupos de Day Trade
     * 
     * @param dayTradeGroups Lista de grupos Day Trade
     * @param allPlans Todos os planos de processamento
     * @return Resultado consolidado
     */
    @Transactional
    public MultipleDayTradeResult processMultipleDayTrades(
            List<DayTradeGroup> dayTradeGroups,
            List<ItemProcessingPlan> allPlans) {
        
        log.debug("🔄 Processando {} grupos Day Trade", dayTradeGroups.size());
        
        List<DayTradeProcessingResult> groupResults = new ArrayList<>();
        
        for (DayTradeGroup group : dayTradeGroups) {
            // Filtrar planos para este grupo específico
            List<ItemProcessingPlan> entryPlans = filterPlansForGroup(allPlans, group, true);
            List<ItemProcessingPlan> exitPlans = filterPlansForGroup(allPlans, group, false);
            
            try {
                DayTradeProcessingResult groupResult = processDayTradeGroup(group, entryPlans, exitPlans);
                groupResults.add(groupResult);
                
            } catch (Exception e) {
                log.error("❌ Erro ao processar grupo Day Trade {}: {}", 
                         group.getAssetCode(), e.getMessage(), e);
                
                // Criar resultado de erro para o grupo
                DayTradeProcessingResult errorResult = DayTradeProcessingResult.builder()
                        .dayTradeGroup(group)
                        .entryOperations(new ArrayList<>())
                        .exitOperations(new ArrayList<>())
                        .errors(Arrays.asList(ProcessingError.builder()
                                .assetCode(group.getAssetCode())
                                .errorMessage("Erro no grupo: " + e.getMessage())
                                .exception(e)
                                .build()))
                        .totalEntryPlans(entryPlans.size())
                        .totalExitPlans(exitPlans.size())
                        .build();
                
                groupResults.add(errorResult);
            }
        }
        
        MultipleDayTradeResult result = consolidateResults(groupResults);
        
        log.info("📊 Processamento múltiplo Day Trade concluído: {} grupos, {} operações totais",
                 result.getTotalGroups(), result.getTotalOperations());
        
        return result;
    }

    /**
     * Cria operações de entrada para Day Trade
     */
    private List<Operation> createDayTradeEntries(List<ItemProcessingPlan> entryPlans,
                                                 List<ProcessingError> errors) {
        
        log.debug("🆕 Criando {} operações de entrada Day Trade", entryPlans.size());
        
        List<Operation> entryOperations = new ArrayList<>();
        
        for (ItemProcessingPlan plan : entryPlans) {
            try {
                Operation entryOperation = operationCreator.createDayTradeEntry(plan);
                entryOperations.add(entryOperation);
                
                log.debug("✅ Entrada Day Trade criada: {} cotas de {} @ {}",
                         entryOperation.getQuantity(),
                         entryOperation.getOptionSeries().getCode(),
                         entryOperation.getEntryUnitPrice());
                
            } catch (Exception e) {
                errors.add(ProcessingError.builder()
                        .assetCode(plan.getInvoiceItem().getAssetCode())
                        .errorMessage("Erro na entrada: " + e.getMessage())
                        .exception(e)
                        .build());
                
                log.error("❌ Erro ao criar entrada Day Trade {}: {}", 
                         plan.getInvoiceItem().getAssetCode(), e.getMessage());
            }
        }
        
        return entryOperations;
    }

    /**
     * Processa saídas Day Trade finalizando operações de entrada
     */
    private List<Operation> processDayTradeExits(List<ItemProcessingPlan> exitPlans,
                                               List<Operation> entryOperations,
                                               List<ProcessingError> errors) {
        
        log.debug("🎯 Processando {} saídas Day Trade", exitPlans.size());
        
        List<Operation> exitOperations = new ArrayList<>();
        
        // Mapear operações de entrada por código do ativo para matching
        Map<String, Operation> entryByAsset = entryOperations.stream()
                .collect(Collectors.toMap(
                    op -> extractBaseAssetCode(op.getOptionSeries().getCode()),
                    op -> op,
                    (existing, replacement) -> existing // Manter primeira em caso de duplicata
                ));
        
        for (ItemProcessingPlan exitPlan : exitPlans) {
            try {
                // Encontrar operação de entrada correspondente
                String assetCode = extractBaseAssetCode(exitPlan.getInvoiceItem().getAssetCode());
                Operation entryOperation = entryByAsset.get(assetCode);
                
                if (entryOperation == null) {
                    errors.add(ProcessingError.builder()
                            .assetCode(assetCode)
                            .errorMessage("Operação de entrada não encontrada para saída")
                            .build());
                    continue;
                }
                
                // Configurar plano para usar a operação de entrada criada
                exitPlan.setTargetOperation(entryOperation);
                
                // Processar finalização
                Operation exitOperation = existingOperationProcessor.processExistingOperationExit(exitPlan);
                exitOperations.add(exitOperation);
                
                log.debug("✅ Saída Day Trade processada: {} @ {} (P&L: {})",
                         exitOperation.getQuantity(),
                         exitOperation.getExitUnitPrice(),
                         exitOperation.getProfitLoss());
                
            } catch (Exception e) {
                errors.add(ProcessingError.builder()
                        .assetCode(exitPlan.getInvoiceItem().getAssetCode())
                        .errorMessage("Erro na saída: " + e.getMessage())
                        .exception(e)
                        .build());
                
                log.error("❌ Erro ao processar saída Day Trade {}: {}", 
                         exitPlan.getInvoiceItem().getAssetCode(), e.getMessage());
            }
        }
        
        return exitOperations;
    }

    /**
     * Valida grupo Day Trade antes do processamento
     */
    private void validateDayTradeGroup(DayTradeGroup group,
                                     List<ItemProcessingPlan> entryPlans,
                                     List<ItemProcessingPlan> exitPlans) {
        
        if (group == null) {
            throw new IllegalArgumentException("Grupo Day Trade não pode ser null");
        }
        
        if (entryPlans.isEmpty()) {
            throw new IllegalArgumentException("Deve haver pelo menos uma entrada Day Trade");
        }
        
        if (exitPlans.isEmpty()) {
            throw new IllegalArgumentException("Deve haver pelo menos uma saída Day Trade");
        }
        
        // Verificar se todos os planos são do mesmo ativo
        String groupAsset = group.getAssetCode();
        
        boolean allEntryPlansSameAsset = entryPlans.stream()
                .allMatch(plan -> groupAsset.equals(
                    extractBaseAssetCode(plan.getInvoiceItem().getAssetCode())));
        
        boolean allExitPlansSameAsset = exitPlans.stream()
                .allMatch(plan -> groupAsset.equals(
                    extractBaseAssetCode(plan.getInvoiceItem().getAssetCode())));
        
        if (!allEntryPlansSameAsset || !allExitPlansSameAsset) {
            throw new IllegalArgumentException("Todos os planos devem ser do mesmo ativo: " + groupAsset);
        }
        
        // Verificar tipos de mapeamento
        boolean allEntryPlansCorrectType = entryPlans.stream()
                .allMatch(plan -> plan.getMappingType() == OperationMappingType.DAY_TRADE_ENTRY);
        
        boolean allExitPlansCorrectType = exitPlans.stream()
                .allMatch(plan -> plan.getMappingType() == OperationMappingType.DAY_TRADE_EXIT);
        
        if (!allEntryPlansCorrectType || !allExitPlansCorrectType) {
            throw new IllegalArgumentException("Tipos de mapeamento incorretos para Day Trade");
        }
    }

    /**
     * Filtra planos para um grupo específico
     */
    private List<ItemProcessingPlan> filterPlansForGroup(List<ItemProcessingPlan> allPlans,
                                                        DayTradeGroup group,
                                                        boolean isEntry) {
        
        String groupAsset = group.getAssetCode();
        OperationMappingType targetType = isEntry ? 
                OperationMappingType.DAY_TRADE_ENTRY : 
                OperationMappingType.DAY_TRADE_EXIT;
        
        return allPlans.stream()
                .filter(plan -> plan.getMappingType() == targetType)
                .filter(plan -> groupAsset.equals(
                    extractBaseAssetCode(plan.getInvoiceItem().getAssetCode())))
                .collect(Collectors.toList());
    }

    /**
     * Consolida resultados de múltiplos grupos
     */
    private MultipleDayTradeResult consolidateResults(List<DayTradeProcessingResult> groupResults) {
        List<Operation> allEntryOperations = new ArrayList<>();
        List<Operation> allExitOperations = new ArrayList<>();
        List<ProcessingError> allErrors = new ArrayList<>();
        
        for (DayTradeProcessingResult result : groupResults) {
            allEntryOperations.addAll(result.getEntryOperations());
            allExitOperations.addAll(result.getExitOperations());
            allErrors.addAll(result.getErrors());
        }
        
        return MultipleDayTradeResult.builder()
                .groupResults(groupResults)
                .allEntryOperations(allEntryOperations)
                .allExitOperations(allExitOperations)
                .allErrors(allErrors)
                .totalGroups(groupResults.size())
                .successfulGroups((int) groupResults.stream()
                        .filter(DayTradeProcessingResult::isFullySuccessful)
                        .count())
                .totalOperations(allEntryOperations.size() + allExitOperations.size())
                .build();
    }

    /**
     * Extrai código base do ativo
     */
    private String extractBaseAssetCode(String fullAssetCode) {
        if (fullAssetCode == null) return "";
        
        String cleaned = fullAssetCode.trim().toUpperCase();
        
        if (cleaned.matches("^[A-Z]{4,5}[FE]\\d+$")) {
            return cleaned.substring(0, cleaned.length() - 4);
        }
        
        if (cleaned.contains(" ON") || cleaned.contains(" PN")) {
            return cleaned.split(" ")[0];
        }
        
        return cleaned;
    }

    /**
     * Erro de processamento Day Trade
     */
    @lombok.Builder
    @lombok.Data
    public static class ProcessingError {
        private String assetCode;
        private String errorMessage;
        private Exception exception;
    }

    /**
     * Resultado do processamento de um grupo Day Trade
     */
    @lombok.Builder
    @lombok.Data
    public static class DayTradeProcessingResult {
        private DayTradeGroup dayTradeGroup;
        private List<Operation> entryOperations;
        private List<Operation> exitOperations;
        private List<ProcessingError> errors;
        private int totalEntryPlans;
        private int totalExitPlans;
        private int successfulEntries;
        private int successfulExits;
        
        /**
         * Verifica se o processamento foi totalmente bem-sucedido
         */
        public boolean isFullySuccessful() {
            return errors.isEmpty() && 
                   successfulEntries == totalEntryPlans && 
                   successfulExits == totalExitPlans;
        }
        
        /**
         * Calcula P&L total do grupo
         */
        public java.math.BigDecimal getTotalProfitLoss() {
            return exitOperations.stream()
                    .filter(op -> op.getProfitLoss() != null)
                    .map(Operation::getProfitLoss)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        }
        
        /**
         * Retorna número total de operações
         */
        public int getTotalOperations() {
            return entryOperations.size() + exitOperations.size();
        }
    }

    /**
     * Resultado de processamento múltiplo Day Trade
     */
    @lombok.Builder
    @lombok.Data
    public static class MultipleDayTradeResult {
        private List<DayTradeProcessingResult> groupResults;
        private List<Operation> allEntryOperations;
        private List<Operation> allExitOperations;
        private List<ProcessingError> allErrors;
        private int totalGroups;
        private int successfulGroups;
        private int totalOperations;
        
        /**
         * Taxa de sucesso dos grupos
         */
        public double getGroupSuccessRate() {
            return totalGroups > 0 ? (double) successfulGroups / totalGroups * 100 : 0;
        }
        
        /**
         * Verifica se houve erros
         */
        public boolean hasErrors() {
            return !allErrors.isEmpty();
        }
        
        /**
         * P&L total de todos os Day Trades
         */
        public java.math.BigDecimal getTotalProfitLoss() {
            return groupResults.stream()
                    .map(DayTradeProcessingResult::getTotalProfitLoss)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        }
    }
}