package com.olisystem.optionsmanager.service.invoice.processing;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.TradeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🎯 Detector de Trade Type para operações de invoice
 * 
 * Analisa operações da nota de corretagem para identificar
 * padrões de Day Trade vs Swing Trade automaticamente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceTradeTypeDetector {

    /**
     * Detecta tipos de trade para todos os itens de uma invoice
     */
    public List<TradeType> detectTradeTypes(Invoice invoice) {
        log.debug("🎯 Detectando trade types para invoice {}", invoice.getId());
        
        List<InvoiceItem> items = invoice.getItems();
        
        if (items == null || items.isEmpty()) {
            log.debug("Invoice sem itens, retornando lista vazia");
            return new ArrayList<>();
        }
        
        // Analisar padrões de Day Trade na invoice
        DayTradeAnalysis analysis = analyzeDayTradePatterns(items, invoice.getTradingDate());
        
        // Determinar trade type para cada item
        List<TradeType> tradeTypes = new ArrayList<>();
        
        for (InvoiceItem item : items) {
            TradeType tradeType = determineTradeType(item, analysis);
            tradeTypes.add(tradeType);
            
            log.debug("Item {}: {} {} -> TradeType: {}", 
                     item.getSequenceNumber(),
                     item.getOperationType(), 
                     item.getAssetCode(), 
                     tradeType);
        }
        
        logTradeTypesSummary(tradeTypes, analysis);
        
        return tradeTypes;
    }

    /**
     * Analisa padrões de Day Trade na invoice
     */
    private DayTradeAnalysis analyzeDayTradePatterns(List<InvoiceItem> items, LocalDate tradingDate) {
        Map<String, List<InvoiceItem>> itemsByAsset = groupItemsByAsset(items);
        
        Set<String> dayTradeAssets = new HashSet<>();
        Set<String> swingTradeAssets = new HashSet<>();
        
        for (Map.Entry<String, List<InvoiceItem>> entry : itemsByAsset.entrySet()) {
            String assetCode = entry.getKey();
            List<InvoiceItem> assetItems = entry.getValue();
            
            TradeTypePattern pattern = analyzeAssetPattern(assetItems);
            
            if (pattern == TradeTypePattern.DAY_TRADE) {
                dayTradeAssets.add(assetCode);
            } else if (pattern == TradeTypePattern.SWING_TRADE) {
                swingTradeAssets.add(assetCode);
            }
        }
        
        return DayTradeAnalysis.builder()
            .tradingDate(tradingDate)
            .dayTradeAssets(dayTradeAssets)
            .swingTradeAssets(swingTradeAssets)
            .totalAssets(itemsByAsset.size())
            .hasExplicitDayTradeMarkers(hasExplicitDayTradeMarkers(items))
            .build();
    }

    /**
     * Agrupa itens por ativo
     */
    private Map<String, List<InvoiceItem>> groupItemsByAsset(List<InvoiceItem> items) {
        return items.stream()
                   .filter(item -> getEffectiveAssetCode(item) != null)
                   .collect(Collectors.groupingBy(this::getEffectiveAssetCode));
    }

    /**
     * Analisa padrão de operações para um ativo específico
     */
    private TradeTypePattern analyzeAssetPattern(List<InvoiceItem> assetItems) {
        long buyCount = assetItems.stream()
                                 .filter(item -> "C".equals(item.getOperationType()))
                                 .count();
        
        long sellCount = assetItems.stream()
                                  .filter(item -> "V".equals(item.getOperationType()))
                                  .count();
        
        // Se tem compra E venda do mesmo ativo na mesma data = Day Trade
        if (buyCount > 0 && sellCount > 0) {
            return TradeTypePattern.DAY_TRADE;
        }
        
        // Se tem marcadores explícitos de Day Trade
        boolean hasExplicitMarkers = assetItems.stream()
                                               .anyMatch(this::hasExplicitDayTradeMarker);
        
        if (hasExplicitMarkers) {
            return TradeTypePattern.DAY_TRADE;
        }
        
        // Apenas compra ou apenas venda = possivelmente Swing
        return TradeTypePattern.SWING_TRADE;
    }

    /**
     * Verifica se tem marcadores explícitos de Day Trade na invoice
     */
    private boolean hasExplicitDayTradeMarkers(List<InvoiceItem> items) {
        return items.stream().anyMatch(this::hasExplicitDayTradeMarker);
    }

    /**
     * Verifica se um item tem marcador explícito de Day Trade
     */
    private boolean hasExplicitDayTradeMarker(InvoiceItem item) {
        // Verificar campo isDayTrade se foi preenchido
        if (item.getIsDayTrade() != null && item.getIsDayTrade()) {
            return true;
        }
        
        // Verificar observações por marcador 'D'
        String observations = item.getObservations();
        if (observations != null) {
            String upperObs = observations.toUpperCase().trim();
            if ("D".equals(upperObs) || upperObs.contains("DAY") || upperObs.contains("TRADE")) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Determina trade type para um item específico
     */
    private TradeType determineTradeType(InvoiceItem item, DayTradeAnalysis analysis) {
        String assetCode = getEffectiveAssetCode(item);
        
        // 1. Verificar marcador explícito no item
        if (hasExplicitDayTradeMarker(item)) {
            return TradeType.DAY;
        }
        
        // 2. Verificar se ativo está na lista de Day Trade detectados
        if (analysis.getDayTradeAssets().contains(assetCode)) {
            return TradeType.DAY;
        }
        
        // 3. Verificar se ativo está na lista de Swing Trade
        if (analysis.getSwingTradeAssets().contains(assetCode)) {
            return TradeType.SWING;
        }
        
        // 4. Default: SWING (mais conservador para IR)
        return TradeType.SWING;
    }

    /**
     * Obtém código efetivo do ativo
     */
    private String getEffectiveAssetCode(InvoiceItem item) {
        String assetCode = item.getAssetCode();
        
        if (assetCode != null && !assetCode.trim().isEmpty()) {
            return assetCode.trim().toUpperCase();
        }
        
        // Fallback: extrair da especificação
        String specification = item.getAssetSpecification();
        if (specification != null && !specification.trim().isEmpty()) {
            return extractAssetCodeFromSpecification(specification);
        }
        
        return null;
    }

    /**
     * Extrai código do ativo da especificação
     */
    private String extractAssetCodeFromSpecification(String specification) {
        if (specification == null) return null;
        
        String cleaned = specification.trim().toUpperCase();
        
        // Se contém espaço, pegar parte antes do espaço
        if (cleaned.contains(" ")) {
            cleaned = cleaned.split(" ")[0];
        }
        
        // Para opções, extrair código base (primeiros 4 caracteres alfabéticos)
        if (cleaned.matches(".*\\d.*")) {
            StringBuilder baseCode = new StringBuilder();
            for (char c : cleaned.toCharArray()) {
                if (Character.isLetter(c)) {
                    baseCode.append(c);
                    if (baseCode.length() >= 4) break;
                }
            }
            return baseCode.toString();
        }
        
        return cleaned;
    }

    /**
     * Log do resumo dos trade types detectados
     */
    private void logTradeTypesSummary(List<TradeType> tradeTypes, DayTradeAnalysis analysis) {
        long dayTradeCount = tradeTypes.stream()
                                      .filter(type -> type == TradeType.DAY)
                                      .count();
        
        long swingTradeCount = tradeTypes.stream()
                                        .filter(type -> type == TradeType.SWING)
                                        .count();
        
        log.info("🎯 Trade Types detectados: Day Trade={}, Swing Trade={}, Total={}", 
                dayTradeCount, swingTradeCount, tradeTypes.size());
        
        if (!analysis.getDayTradeAssets().isEmpty()) {
            log.info("📊 Ativos Day Trade detectados: {}", analysis.getDayTradeAssets());
        }
        
        if (analysis.isHasExplicitDayTradeMarkers()) {
            log.info("🏷️ Marcadores explícitos de Day Trade encontrados na nota");
        }
    }

    // === CLASSES DE APOIO ===
    
    private enum TradeTypePattern {
        DAY_TRADE,
        SWING_TRADE,
        MIXED
    }

    @lombok.Data
    @lombok.Builder
    private static class DayTradeAnalysis {
        private LocalDate tradingDate;
        private Set<String> dayTradeAssets;
        private Set<String> swingTradeAssets;
        private int totalAssets;
        private boolean hasExplicitDayTradeMarkers;
        
        public boolean hasDayTradeOperations() {
            return !dayTradeAssets.isEmpty() || hasExplicitDayTradeMarkers;
        }
    }

    /**
     * Detecta trade type para item único (método utilitário)
     */
    public TradeType detectSingleItemTradeType(InvoiceItem item) {
        if (hasExplicitDayTradeMarker(item)) {
            return TradeType.DAY;
        }
        
        return TradeType.SWING; // Default conservador
    }

    /**
     * Verifica se a invoice tem padrões de Day Trade
     */
    public boolean hasDayTradePatterns(Invoice invoice) {
        DayTradeAnalysis analysis = analyzeDayTradePatterns(invoice.getItems(), invoice.getTradingDate());
        return analysis.hasDayTradeOperations();
    }

    /**
     * Conta operações por tipo de trade
     */
    public Map<TradeType, Long> countOperationsByTradeType(Invoice invoice) {
        List<TradeType> tradeTypes = detectTradeTypes(invoice);
        
        return tradeTypes.stream()
                        .collect(Collectors.groupingBy(
                            type -> type,
                            Collectors.counting()
                        ));
    }
}