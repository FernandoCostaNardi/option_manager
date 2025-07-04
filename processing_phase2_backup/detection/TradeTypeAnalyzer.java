package com.olisystem.optionsmanager.service.invoice.processing.detection;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.TradeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para análise de tipos de trade dentro de uma invoice
 * Detecta Day Trade vs Swing Trade baseado nas operações
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@Slf4j
public class TradeTypeAnalyzer {

    /**
     * Analisa os tipos de trade em uma invoice
     * 
     * @param invoice Invoice a ser analisada
     * @return Resultado da análise com detalhes dos trades
     */
    public TradeTypeAnalysisResult analyzeTradeTypes(Invoice invoice) {
        log.debug("🔍 Analisando tipos de trade na invoice {}", invoice.getInvoiceNumber());
        
        List<InvoiceItem> items = invoice.getItems();
        if (items == null || items.isEmpty()) {
            return TradeTypeAnalysisResult.builder()
                    .hasDayTrades(false)
                    .hasSwingTrades(false)
                    .dayTradeGroups(new ArrayList<>())
                    .swingTradeItems(new ArrayList<>())
                    .orphanItems(new ArrayList<>())
                    .build();
        }
        
        // Agrupar itens por código do ativo
        Map<String, List<InvoiceItem>> itemsByAsset = groupItemsByAsset(items);
        
        List<DayTradeGroup> dayTradeGroups = new ArrayList<>();
        List<InvoiceItem> swingTradeItems = new ArrayList<>();
        List<InvoiceItem> orphanItems = new ArrayList<>();
        
        // Analisar cada ativo
        for (Map.Entry<String, List<InvoiceItem>> entry : itemsByAsset.entrySet()) {
            String assetCode = entry.getKey();
            List<InvoiceItem> assetItems = entry.getValue();
            
            AssetTradeAnalysis assetAnalysis = analyzeAssetTrades(assetCode, assetItems);
            
            dayTradeGroups.addAll(assetAnalysis.getDayTradeGroups());
            swingTradeItems.addAll(assetAnalysis.getSwingTradeItems());
            orphanItems.addAll(assetAnalysis.getOrphanItems());
            
            log.debug("📊 Ativo {}: {} day trades, {} swing trades, {} órfãos",
                     assetCode, assetAnalysis.getDayTradeGroups().size(),
                     assetAnalysis.getSwingTradeItems().size(),
                     assetAnalysis.getOrphanItems().size());
        }
        
        TradeTypeAnalysisResult result = TradeTypeAnalysisResult.builder()
                .hasDayTrades(!dayTradeGroups.isEmpty())
                .hasSwingTrades(!swingTradeItems.isEmpty())
                .hasOrphanItems(!orphanItems.isEmpty())
                .dayTradeGroups(dayTradeGroups)
                .swingTradeItems(swingTradeItems)
                .orphanItems(orphanItems)
                .totalDayTradeOperations(dayTradeGroups.stream()
                                       .mapToInt(group -> group.getBuyItems().size() + group.getSellItems().size())
                                       .sum())
                .totalSwingTradeOperations(swingTradeItems.size())
                .totalOrphanOperations(orphanItems.size())
                .uniqueAssetsInDayTrade(dayTradeGroups.stream()
                                      .map(DayTradeGroup::getAssetCode)
                                      .collect(Collectors.toSet()).size())
                .build();
        
        log.info("✅ Análise concluída: {} day trades, {} swing trades, {} órfãos em {} ativos",
                 result.getTotalDayTradeOperations(), result.getTotalSwingTradeOperations(),
                 result.getTotalOrphanOperations(), itemsByAsset.size());
        
        return result;
    }

    /**
     * Agrupa itens por código do ativo
     */
    private Map<String, List<InvoiceItem>> groupItemsByAsset(List<InvoiceItem> items) {
        return items.stream()
                   .filter(item -> item.getAssetCode() != null)
                   .collect(Collectors.groupingBy(
                       item -> extractBaseAssetCode(item.getAssetCode()),
                       LinkedHashMap::new,
                       Collectors.toList()
                   ));
    }

    /**
     * Analisa trades de um ativo específico
     */
    private AssetTradeAnalysis analyzeAssetTrades(String assetCode, List<InvoiceItem> items) {
        // Separar compras e vendas
        List<InvoiceItem> buyItems = items.stream()
                .filter(item -> "C".equals(item.getOperationType()))
                .sorted(Comparator.comparing(InvoiceItem::getSequenceNumber))
                .collect(Collectors.toList());
        
        List<InvoiceItem> sellItems = items.stream()
                .filter(item -> "V".equals(item.getOperationType()))
                .sorted(Comparator.comparing(InvoiceItem::getSequenceNumber))
                .collect(Collectors.toList());
        
        List<DayTradeGroup> dayTradeGroups = new ArrayList<>();
        List<InvoiceItem> remainingBuys = new ArrayList<>(buyItems);
        List<InvoiceItem> remainingSells = new ArrayList<>(sellItems);
        
        // Tentar formar Day Trades (pareamento de compras e vendas)
        formDayTradeGroups(assetCode, remainingBuys, remainingSells, dayTradeGroups);
        
        // Itens restantes são Swing Trades ou órfãos
        List<InvoiceItem> swingTradeItems = new ArrayList<>();
        List<InvoiceItem> orphanItems = new ArrayList<>();
        
        // Compras restantes são Swing Trades (posições que ficam abertas)
        swingTradeItems.addAll(remainingBuys);
        
        // Vendas restantes podem ser órfãs (não têm compra correspondente na invoice)
        // mas podem estar finalizando operações de outros dias
        orphanItems.addAll(remainingSells);
        
        return AssetTradeAnalysis.builder()
                .assetCode(assetCode)
                .dayTradeGroups(dayTradeGroups)
                .swingTradeItems(swingTradeItems)
                .orphanItems(orphanItems)
                .build();
    }

    /**
     * Forma grupos de Day Trade pareando compras e vendas
     */
    private void formDayTradeGroups(String assetCode, List<InvoiceItem> buyItems, 
                                  List<InvoiceItem> sellItems, List<DayTradeGroup> dayTradeGroups) {
        
        Iterator<InvoiceItem> buyIterator = buyItems.iterator();
        
        while (buyIterator.hasNext()) {
            InvoiceItem buyItem = buyIterator.next();
            
            // Procurar venda correspondente
            InvoiceItem matchingSell = findMatchingSell(buyItem, sellItems);
            
            if (matchingSell != null) {
                // Formar Day Trade
                DayTradeGroup dayTrade = DayTradeGroup.builder()
                        .assetCode(assetCode)
                        .buyItems(Arrays.asList(buyItem))
                        .sellItems(Arrays.asList(matchingSell))
                        .totalBuyQuantity(buyItem.getQuantity())
                        .totalSellQuantity(matchingSell.getQuantity())
                        .isBalanced(Objects.equals(buyItem.getQuantity(), matchingSell.getQuantity()))
                        .build();
                
                dayTradeGroups.add(dayTrade);
                
                // Remover itens processados
                buyIterator.remove();
                sellItems.remove(matchingSell);
                
                log.debug("🎯 Day Trade formado: {} - Compra {} x Venda {} ({})",
                         assetCode, buyItem.getQuantity(), matchingSell.getQuantity(),
                         dayTrade.isBalanced() ? "balanceado" : "desbalanceado");
            }
        }
    }

    /**
     * Encontra venda correspondente para uma compra
     */
    private InvoiceItem findMatchingSell(InvoiceItem buyItem, List<InvoiceItem> sellItems) {
        // Estratégia: procurar venda com mesma quantidade primeiro
        Optional<InvoiceItem> exactMatch = sellItems.stream()
                .filter(sell -> Objects.equals(buyItem.getQuantity(), sell.getQuantity()))
                .findFirst();
        
        if (exactMatch.isPresent()) {
            return exactMatch.get();
        }
        
        // Se não encontrar quantidade exata, pegar qualquer venda
        return sellItems.isEmpty() ? null : sellItems.get(0);
    }

    /**
     * Extrai código base do ativo (mesmo método do ActiveOperationDetector)
     */
    private String extractBaseAssetCode(String fullAssetCode) {
        if (fullAssetCode == null) return null;
        
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
     * Verifica se um item é marcado como Day Trade no campo de observações
     */
    public boolean isMarkedAsDayTrade(InvoiceItem item) {
        return Boolean.TRUE.equals(item.getIsDayTrade()) || 
               (item.getObservations() != null && item.getObservations().contains("D"));
    }

    /**
     * Determina o tipo de trade para um item individual
     */
    public TradeType determineTradeType(InvoiceItem item, boolean isPartOfDayTrade) {
        // Se está marcado explicitamente como Day Trade
        if (isMarkedAsDayTrade(item) || isPartOfDayTrade) {
            return TradeType.DAY;
        }
        
        return TradeType.SWING;
    }

    /**
     * Análise de trades de um ativo específico
     */
    @lombok.Builder
    @lombok.Data
    private static class AssetTradeAnalysis {
        private String assetCode;
        private List<DayTradeGroup> dayTradeGroups;
        private List<InvoiceItem> swingTradeItems;
        private List<InvoiceItem> orphanItems;
    }

    /**
     * Grupo de Day Trade (compra + venda no mesmo ativo)
     */
    @lombok.Builder
    @lombok.Data
    public static class DayTradeGroup {
        private String assetCode;
        private List<InvoiceItem> buyItems;
        private List<InvoiceItem> sellItems;
        private Integer totalBuyQuantity;
        private Integer totalSellQuantity;
        private boolean isBalanced;
        
        /**
         * Calcula lucro/prejuízo potencial do Day Trade
         */
        public double calculatePotentialProfit() {
            double sellValue = sellItems.stream()
                    .filter(item -> item.getTotalValue() != null)
                    .mapToDouble(item -> item.getTotalValue().doubleValue())
                    .sum();
            
            double buyValue = buyItems.stream()
                    .filter(item -> item.getTotalValue() != null)
                    .mapToDouble(item -> item.getTotalValue().doubleValue())
                    .sum();
            
            return sellValue - buyValue;
        }
        
        /**
         * Retorna todos os itens do grupo
         */
        public List<InvoiceItem> getAllItems() {
            List<InvoiceItem> allItems = new ArrayList<>();
            allItems.addAll(buyItems);
            allItems.addAll(sellItems);
            return allItems;
        }
    }

    /**
     * Resultado da análise de tipos de trade
     */
    @lombok.Builder
    @lombok.Data
    public static class TradeTypeAnalysisResult {
        private boolean hasDayTrades;
        private boolean hasSwingTrades;
        private boolean hasOrphanItems;
        private List<DayTradeGroup> dayTradeGroups;
        private List<InvoiceItem> swingTradeItems;
        private List<InvoiceItem> orphanItems;
        private int totalDayTradeOperations;
        private int totalSwingTradeOperations;
        private int totalOrphanOperations;
        private int uniqueAssetsInDayTrade;
        
        /**
         * Verifica se a invoice é puramente Day Trade
         */
        public boolean isPureDayTrade() {
            return hasDayTrades && !hasSwingTrades && !hasOrphanItems;
        }
        
        /**
         * Verifica se a invoice é puramente Swing Trade
         */
        public boolean isPureSwingTrade() {
            return hasSwingTrades && !hasDayTrades && !hasOrphanItems;
        }
        
        /**
         * Verifica se a invoice é mista (Day + Swing)
         */
        public boolean isMixed() {
            return hasDayTrades && (hasSwingTrades || hasOrphanItems);
        }
        
        /**
         * Retorna total de operações analisadas
         */
        public int getTotalOperations() {
            return totalDayTradeOperations + totalSwingTradeOperations + totalOrphanOperations;
        }
        
        /**
         * Retorna percentual de Day Trades
         */
        public double getDayTradePercentage() {
            int total = getTotalOperations();
            return total > 0 ? (double) totalDayTradeOperations / total * 100 : 0;
        }
    }
}