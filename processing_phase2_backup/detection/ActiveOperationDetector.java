package com.olisystem.optionsmanager.service.invoice.processing.detection;

import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para detectar operações ACTIVE relacionadas aos itens da invoice
 * Identifica se existem operações abertas que podem ser finalizadas
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActiveOperationDetector {

    private final OperationRepository operationRepository;

    /**
     * Detecta operações ativas para uma lista de invoice items
     * 
     * @param invoiceItems Itens da invoice a serem analisados
     * @param user Usuário proprietário das operações
     * @return Resultado da detecção com operações encontradas
     */
    public ActiveOperationDetectionResult detectActiveOperations(List<InvoiceItem> invoiceItems, User user) {
        log.debug("🔍 Detectando operações ativas para {} itens do usuário {}", 
                 invoiceItems.size(), user.getUsername());
        
        Map<String, List<Operation>> activeOperationsByAsset = new HashMap<>();
        Map<InvoiceItem, List<Operation>> itemMatches = new HashMap<>();
        Set<String> assetsWithActiveOperations = new HashSet<>();
        
        // Processar cada item da invoice
        for (InvoiceItem item : invoiceItems) {
            List<Operation> activeOpsForItem = detectActiveOperationsForItem(item, user);
            
            if (!activeOpsForItem.isEmpty()) {
                itemMatches.put(item, activeOpsForItem);
                assetsWithActiveOperations.add(item.getAssetCode());
                
                // Agrupar por código do ativo
                activeOperationsByAsset.computeIfAbsent(item.getAssetCode(), k -> new ArrayList<>())
                                     .addAll(activeOpsForItem);
                
                log.debug("📍 Item {} ({} {}): {} operações ativas encontradas", 
                         item.getAssetCode(), item.getOperationType(), item.getQuantity(),
                         activeOpsForItem.size());
            }
        }
        
        // Remover duplicatas nas operações por ativo
        activeOperationsByAsset.replaceAll((asset, operations) -> 
            operations.stream().distinct().collect(Collectors.toList())
        );
        
        ActiveOperationDetectionResult result = ActiveOperationDetectionResult.builder()
                .hasActiveOperations(!itemMatches.isEmpty())
                .itemMatches(itemMatches)
                .activeOperationsByAsset(activeOperationsByAsset)
                .assetsWithActiveOperations(assetsWithActiveOperations)
                .totalActiveOperations(activeOperationsByAsset.values().stream()
                                     .mapToInt(List::size).sum())
                .totalMatchedItems(itemMatches.size())
                .build();
        
        log.info("📊 Detecção concluída: {} itens com operações ativas, {} ativos afetados, {} operações encontradas",
                 result.getTotalMatchedItems(), result.getAssetsWithActiveOperations().size(), 
                 result.getTotalActiveOperations());
        
        return result;
    }

    /**
     * Detecta operações ativas para um item específico
     */
    private List<Operation> detectActiveOperationsForItem(InvoiceItem item, User user) {
        String assetCode = extractBaseAssetCode(item.getAssetCode());
        
        // Buscar operações ACTIVE do usuário para o ativo
        List<Operation> candidateOperations = operationRepository
                .findByUserAndStatusIn(user, Arrays.asList(OperationStatus.ACTIVE));
        
        // Filtrar por código do ativo
        List<Operation> matchingOperations = candidateOperations.stream()
                .filter(op -> isAssetMatch(op, assetCode))
                .filter(op -> isValidForFinalization(op, item))
                .collect(Collectors.toList());
        
        if (!matchingOperations.isEmpty()) {
            log.debug("🎯 Operações ativas para {}: {}", assetCode, 
                     matchingOperations.stream()
                                     .map(op -> String.format("%s(%d)", op.getId().toString().substring(0, 8), op.getQuantity()))
                                     .collect(Collectors.joining(", ")));
        }
        
        return matchingOperations;
    }

    /**
     * Extrai código base do ativo (remove sufixos de opção)
     */
    private String extractBaseAssetCode(String fullAssetCode) {
        if (fullAssetCode == null) return null;
        
        // Remover espaços e converter para maiúsculo
        String cleaned = fullAssetCode.trim().toUpperCase();
        
        // Para opções, extrair código base (ex: PETR4F336 -> PETR4)
        // Padrão comum: XXXXX[F|E][número]
        if (cleaned.matches("^[A-Z]{4,5}[FE]\\d+$")) {
            return cleaned.substring(0, cleaned.length() - 4); // Remove [F|E]XXX
        }
        
        // Para códigos com sufixos ON/PN (ex: PETR4 ON -> PETR4)
        if (cleaned.contains(" ON") || cleaned.contains(" PN")) {
            return cleaned.split(" ")[0];
        }
        
        // Retornar código original se não conseguir extrair
        return cleaned;
    }

    /**
     * Verifica se uma operação corresponde ao código do ativo
     */
    private boolean isAssetMatch(Operation operation, String assetCode) {
        if (operation.getOptionSeries() == null || operation.getOptionSeries().getCode() == null) {
            return false;
        }
        
        String opAssetCode = extractBaseAssetCode(operation.getOptionSeries().getCode());
        return assetCode.equals(opAssetCode);
    }

    /**
     * Verifica se uma operação pode ser finalizada com o invoice item
     */
    private boolean isValidForFinalization(Operation operation, InvoiceItem item) {
        // Verificar tipo de transação (item de venda pode finalizar operação de compra)
        if ("V".equals(item.getOperationType())) {
            // Venda pode finalizar operação de compra (BUY)
            return "BUY".equals(operation.getTransactionType().name());
        } else if ("C".equals(item.getOperationType())) {
            // Compra normalmente não finaliza operação existente, mas pode em casos especiais
            // Por enquanto, retornar false para compras
            return false;
        }
        
        return false;
    }

    /**
     * Busca operações ativas por múltiplos códigos de ativos
     */
    public Map<String, List<Operation>> findActiveOperationsByAssets(Set<String> assetCodes, User user) {
        log.debug("🔍 Buscando operações ativas para {} ativos", assetCodes.size());
        
        Map<String, List<Operation>> result = new HashMap<>();
        
        // Buscar todas as operações ativas do usuário de uma vez
        List<Operation> allActiveOperations = operationRepository
                .findByUserAndStatusIn(user, Arrays.asList(OperationStatus.ACTIVE));
        
        // Agrupar por código do ativo
        for (String assetCode : assetCodes) {
            List<Operation> operationsForAsset = allActiveOperations.stream()
                    .filter(op -> isAssetMatch(op, assetCode))
                    .collect(Collectors.toList());
            
            if (!operationsForAsset.isEmpty()) {
                result.put(assetCode, operationsForAsset);
                log.debug("📍 Ativo {}: {} operações ativas", assetCode, operationsForAsset.size());
            }
        }
        
        log.info("📊 Encontradas operações ativas para {}/{} ativos", result.size(), assetCodes.size());
        return result;
    }

    /**
     * Estatísticas de operações ativas para um usuário
     */
    public ActiveOperationStats getActiveOperationStats(User user) {
        List<Operation> activeOperations = operationRepository
                .findByUserAndStatusIn(user, Arrays.asList(OperationStatus.ACTIVE));
        
        // Agrupar por código do ativo
        Map<String, Long> operationsByAsset = activeOperations.stream()
                .filter(op -> op.getOptionSeries() != null && op.getOptionSeries().getCode() != null)
                .collect(Collectors.groupingBy(
                    op -> extractBaseAssetCode(op.getOptionSeries().getCode()),
                    Collectors.counting()
                ));
        
        // Calcular valor total em aberto
        double totalOpenValue = activeOperations.stream()
                .filter(op -> op.getEntryTotalValue() != null)
                .mapToDouble(op -> op.getEntryTotalValue().doubleValue())
                .sum();
        
        return ActiveOperationStats.builder()
                .totalActiveOperations(activeOperations.size())
                .uniqueAssets(operationsByAsset.size())
                .operationsByAsset(operationsByAsset)
                .totalOpenValue(totalOpenValue)
                .oldestOperationDate(activeOperations.stream()
                                   .map(Operation::getEntryDate)
                                   .min(LocalDate::compareTo)
                                   .orElse(null))
                .newestOperationDate(activeOperations.stream()
                                   .map(Operation::getEntryDate)
                                   .max(LocalDate::compareTo)
                                   .orElse(null))
                .build();
    }

    /**
     * Resultado da detecção de operações ativas
     */
    @lombok.Builder
    @lombok.Data
    public static class ActiveOperationDetectionResult {
        private boolean hasActiveOperations;
        private Map<InvoiceItem, List<Operation>> itemMatches;
        private Map<String, List<Operation>> activeOperationsByAsset;
        private Set<String> assetsWithActiveOperations;
        private int totalActiveOperations;
        private int totalMatchedItems;
        
        /**
         * Verifica se um item específico tem operações ativas
         */
        public boolean hasActiveOperationsForItem(InvoiceItem item) {
            return itemMatches.containsKey(item) && !itemMatches.get(item).isEmpty();
        }
        
        /**
         * Retorna operações ativas para um ativo específico
         */
        public List<Operation> getActiveOperationsForAsset(String assetCode) {
            return activeOperationsByAsset.getOrDefault(assetCode, new ArrayList<>());
        }
        
        /**
         * Retorna taxa de itens com operações ativas
         */
        public double getMatchRate() {
            int totalItems = itemMatches.keySet().size();
            return totalItems > 0 ? (double) totalMatchedItems / totalItems * 100 : 0;
        }
    }

    /**
     * Estatísticas de operações ativas de um usuário
     */
    @lombok.Builder
    @lombok.Data
    public static class ActiveOperationStats {
        private int totalActiveOperations;
        private int uniqueAssets;
        private Map<String, Long> operationsByAsset;
        private double totalOpenValue;
        private LocalDate oldestOperationDate;
        private LocalDate newestOperationDate;
        
        public boolean hasActiveOperations() {
            return totalActiveOperations > 0;
        }
        
        public double getAverageValuePerOperation() {
            return totalActiveOperations > 0 ? totalOpenValue / totalActiveOperations : 0;
        }
    }
}