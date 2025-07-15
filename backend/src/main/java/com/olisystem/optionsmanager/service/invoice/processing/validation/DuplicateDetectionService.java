package com.olisystem.optionsmanager.service.invoice.processing.validation;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.OperationSourceMapping;
import com.olisystem.optionsmanager.repository.InvoiceItemRepository;
import com.olisystem.optionsmanager.repository.OperationSourceMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de detecção de duplicatas em invoices
 * ✅ CORREÇÃO: Usa repositório em vez de acessar coleção lazy
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DuplicateDetectionService {

    private final InvoiceItemRepository invoiceItemRepository;
    private final OperationSourceMappingRepository sourceMappingRepository;

    // Configurações de detecção
    private static final BigDecimal PRICE_SIMILARITY_THRESHOLD = BigDecimal.valueOf(0.01); // 1 centavo
    private static final int MAX_DUPLICATE_AGE_DAYS = 30; // 30 dias

    /**
     * Detecta duplicatas em uma lista de invoices
     * ✅ CORREÇÃO: Usa repositório para buscar itens
     */
    public DuplicateDetectionResult detectDuplicates(List<Invoice> invoices) {
        log.info("🔍 Detectando duplicatas em {} invoices", invoices.size());
        
        DuplicateDetectionResult result = new DuplicateDetectionResult();
        
        try {
            // ✅ CORREÇÃO: Buscar todos os itens via repositório
            List<InvoiceItem> allItems = new ArrayList<>();
            for (Invoice invoice : invoices) {
                List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithAllRelations(invoice.getId());
                allItems.addAll(items);
            }
            
            log.info("📊 Analisando {} itens para duplicatas", allItems.size());
            
            // Detectar diferentes tipos de duplicatas
            detectProcessedItems(invoices, result);
            detectBusinessRuleDuplicates(allItems, result);
            detectSimilarPriceDuplicates(allItems, result);
            detectTimeBasedDuplicates(allItems, result);
            
            log.info("✅ Detecção concluída: {} duplicatas encontradas", result.getDuplicateItems().size());
            
        } catch (Exception e) {
            log.error("❌ Erro durante detecção de duplicatas: {}", e.getMessage(), e);
            result.addError("Erro na detecção: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Detecta itens já processados anteriormente
     * ✅ CORREÇÃO: Usa repositório para buscar itens
     */
    private void detectProcessedItems(List<Invoice> invoices, DuplicateDetectionResult result) {
        List<InvoiceItem> allItems = new ArrayList<>();
        for (Invoice invoice : invoices) {
            List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithAllRelations(invoice.getId());
            allItems.addAll(items);
        }
        
        for (InvoiceItem item : allItems) {
            // Verificar se já existe OperationSourceMapping para este item
            Optional<OperationSourceMapping> existingMapping = sourceMappingRepository
                .findByInvoiceItem(item);
            
            if (existingMapping.isPresent()) {
                result.addDuplicateItem(item, "Item já processado anteriormente");
                log.debug("🔄 Item {} já foi processado anteriormente", item.getId());
            }
        }
    }

    /**
     * Detecta duplicatas por regras de negócio
     * ✅ CORREÇÃO: Recebe lista de itens como parâmetro
     */
    private void detectBusinessRuleDuplicates(List<InvoiceItem> allItems, DuplicateDetectionResult result) {
        // Agrupar itens por critérios de negócio
        Map<String, List<InvoiceItem>> itemsByBusinessKey = new HashMap<>();
        
        for (InvoiceItem item : allItems) {
            String businessKey = createBusinessKey(item);
            itemsByBusinessKey.computeIfAbsent(businessKey, k -> new ArrayList<>()).add(item);
        }
        
        // Verificar grupos com múltiplos itens
        for (Map.Entry<String, List<InvoiceItem>> entry : itemsByBusinessKey.entrySet()) {
            List<InvoiceItem> items = entry.getValue();
            
            if (items.size() > 1) {
                // Verificar se são realmente duplicatas (preços similares)
                detectSimilarPriceDuplicates(items, result);
            }
        }
    }

    /**
     * Detecta duplicatas por preços similares
     */
    private void detectSimilarPriceDuplicates(List<InvoiceItem> items, DuplicateDetectionResult result) {
        if (items.size() < 2) return;
        
        // Agrupar por ativo e tipo de operação
        Map<String, List<InvoiceItem>> itemsByAssetAndType = items.stream()
            .filter(item -> item.getAssetCode() != null && item.getOperationType() != null)
            .collect(Collectors.groupingBy(item -> 
                item.getAssetCode() + "_" + item.getOperationType()));
        
        for (List<InvoiceItem> group : itemsByAssetAndType.values()) {
            if (group.size() < 2) continue;
            
            // Verificar preços similares
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    InvoiceItem item1 = group.get(i);
                    InvoiceItem item2 = group.get(j);
                    
                    if (arePricesSimilar(item1.getUnitPrice(), item2.getUnitPrice())) {
                        String reason = String.format("Preços similares: %s vs %s", 
                            item1.getUnitPrice(), item2.getUnitPrice());
                        result.addDuplicateItem(item1, reason);
                        result.addDuplicateItem(item2, reason);
                    }
                }
            }
        }
    }

    /**
     * Detecta duplicatas baseadas em tempo
     */
    private void detectTimeBasedDuplicates(List<InvoiceItem> items, DuplicateDetectionResult result) {
        // Agrupar por ativo, tipo e data
        Map<String, List<InvoiceItem>> itemsByAssetTypeAndDate = items.stream()
            .filter(item -> item.getAssetCode() != null && item.getOperationType() != null)
            .collect(Collectors.groupingBy(item -> 
                item.getAssetCode() + "_" + item.getOperationType() + "_" + 
                (item.getInvoice() != null ? item.getInvoice().getTradingDate() : "unknown")));
        
        for (List<InvoiceItem> group : itemsByAssetTypeAndDate.values()) {
            if (group.size() > 1) {
                for (InvoiceItem item : group) {
                    result.addDuplicateItem(item, "Múltiplas operações no mesmo dia");
                }
            }
        }
    }

    /**
     * Cria chave de negócio para um item
     */
    private String createBusinessKey(InvoiceItem item) {
        return String.format("%s_%s_%s_%s_%s",
            item.getAssetCode(),
            item.getOperationType(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getInvoice() != null ? item.getInvoice().getTradingDate() : "unknown"
        );
    }

    /**
     * Verifica se dois preços são similares
     */
    private boolean arePricesSimilar(BigDecimal price1, BigDecimal price2) {
        if (price1 == null || price2 == null) return false;
        
        BigDecimal difference = price1.subtract(price2).abs();
        return difference.compareTo(PRICE_SIMILARITY_THRESHOLD) <= 0;
    }

    /**
     * Resultado da detecção de duplicatas
     */
    public static class DuplicateDetectionResult {
        private final Map<InvoiceItem, String> duplicateItems = new HashMap<>();
        private final List<String> errors = new ArrayList<>();
        
        public void addDuplicateItem(InvoiceItem item, String reason) {
            duplicateItems.put(item, reason);
        }
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public Map<InvoiceItem, String> getDuplicateItems() {
            return duplicateItems;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public boolean hasDuplicates() {
            return !duplicateItems.isEmpty();
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public int getDuplicateCount() {
            return duplicateItems.size();
        }
        
        public String getSummary() {
            return String.format("Encontradas %d duplicatas (%d erros)", 
                duplicateItems.size(), errors.size());
        }
    }
} 