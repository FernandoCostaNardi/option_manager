package com.olisystem.optionsmanager.service.invoice.processing.validation;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationSourceMapping;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.OperationSourceMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para detecção de duplicatas antes do processamento
 * Evita criar operações duplicadas baseado em critérios específicos
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DuplicateDetectionService {

    private final OperationRepository operationRepository;
    private final OperationSourceMappingRepository mappingRepository;

    /**
     * Detecta duplicatas em uma invoice antes do processamento
     * 
     * @param invoice Invoice a ser verificada
     * @return Resultado da detecção com itens duplicados e válidos
     */
    public DuplicateDetectionResult detectDuplicates(Invoice invoice) {
        log.debug("🔍 Detectando duplicatas na invoice {}", invoice.getInvoiceNumber());
        
        List<InvoiceItem> duplicateItems = new ArrayList<>();
        List<InvoiceItem> validItems = new ArrayList<>();
        Map<String, List<String>> duplicateReasons = new HashMap<>();
        
        for (InvoiceItem item : invoice.getItems()) {
            DuplicateCheckResult checkResult = checkItemForDuplicates(item);
            
            if (checkResult.isDuplicate()) {
                duplicateItems.add(item);
                duplicateReasons.put(item.getId().toString(), checkResult.getReasons());
                log.warn("⚠️ Item duplicado detectado: {} - {}", 
                         item.getAssetCode(), checkResult.getReasons());
            } else {
                validItems.add(item);
            }
        }
        
        DuplicateDetectionResult result = DuplicateDetectionResult.builder()
                .hasDuplicates(!duplicateItems.isEmpty())
                .duplicateItems(duplicateItems)
                .validItems(validItems)
                .duplicateReasons(duplicateReasons)
                .totalItems(invoice.getItems().size())
                .validItemsCount(validItems.size())
                .duplicateItemsCount(duplicateItems.size())
                .build();
        
        log.info("📊 Duplicatas detectadas na invoice {}: {} total, {} válidos, {} duplicados",
                 invoice.getInvoiceNumber(), result.getTotalItems(), 
                 result.getValidItemsCount(), result.getDuplicateItemsCount());
        
        return result;
    }

    /**
     * Verifica se um item específico é duplicata
     */
    private DuplicateCheckResult checkItemForDuplicates(InvoiceItem item) {
        List<String> reasons = new ArrayList<>();
        
        // 1. Verificar se já foi processado antes (mesmo invoice item)
        if (mappingRepository.existsByInvoiceItem(item)) {
            reasons.add("Item já foi processado anteriormente");
        }
        
        // 2. Verificar duplicatas por critérios de negócio
        checkBusinessDuplicates(item, reasons);
        
        // 3. Verificar duplicatas exatas
        checkExactDuplicates(item, reasons);
        
        return DuplicateCheckResult.builder()
                .isDuplicate(!reasons.isEmpty())
                .reasons(reasons)
                .build();
    }

    /**
     * Verifica duplicatas por regras de negócio
     * Critério: mesmo ativo + mesma data + mesma quantidade + mesmo tipo
     */
    private void checkBusinessDuplicates(InvoiceItem item, List<String> reasons) {
        // Buscar operations do mesmo usuário no mesmo dia para o mesmo ativo
        List<Operation> sameAssetOperations = operationRepository
                .findByUserAndAssetCodeAndEntryDate(
                        item.getInvoice().getUser(),
                        item.getAssetCode(),
                        item.getInvoice().getTradingDate()
                );
        
        for (Operation operation : sameAssetOperations) {
            // Verificar se é operação similar
            if (isSimilarOperation(item, operation)) {
                reasons.add(String.format(
                    "Operação similar já existe: %s %s cotas de %s em %s",
                    operation.getTransactionType(),
                    operation.getQuantity(),
                    item.getAssetCode(),
                    operation.getEntryDate()
                ));
            }
        }
    }

    /**
     * Verifica duplicatas exatas baseado em mapeamentos existentes
     */
    private void checkExactDuplicates(InvoiceItem item, List<String> reasons) {
        // Buscar outros invoice items com características idênticas
        List<OperationSourceMapping> similarMappings = findSimilarMappings(item);
        
        for (OperationSourceMapping mapping : similarMappings) {
            InvoiceItem existingItem = mapping.getInvoiceItem();
            
            if (isExactDuplicate(item, existingItem)) {
                reasons.add(String.format(
                    "Duplicata exata encontrada na invoice %s (item %s)",
                    existingItem.getInvoice().getInvoiceNumber(),
                    existingItem.getSequenceNumber()
                ));
            }
        }
    }

    /**
     * Verifica se uma operação é similar ao item da invoice
     */
    private boolean isSimilarOperation(InvoiceItem item, Operation operation) {
        // Mesmo tipo de transação
        String itemType = "C".equals(item.getOperationType()) ? "BUY" : "SELL";
        if (!itemType.equals(operation.getTransactionType().name())) {
            return false;
        }
        
        // Mesma quantidade
        if (!Objects.equals(item.getQuantity(), operation.getQuantity())) {
            return false;
        }
        
        // Preço similar (tolerância de 1%)
        if (item.getUnitPrice() != null && operation.getEntryUnitPrice() != null) {
            double priceDifference = Math.abs(
                item.getUnitPrice().doubleValue() - operation.getEntryUnitPrice().doubleValue()
            ) / operation.getEntryUnitPrice().doubleValue();
            
            if (priceDifference > 0.01) { // 1% de tolerância
                return false;
            }
        }
        
        return true;
    }

    /**
     * Busca mapeamentos similares para um item
     */
    private List<OperationSourceMapping> findSimilarMappings(InvoiceItem item) {
        // Por limitação do repository atual, vamos buscar por tipo e filtrar
        return mappingRepository.findLatestMappings(
                org.springframework.data.domain.PageRequest.of(0, 1000)
        ).stream()
                .filter(mapping -> isSimilarInvoiceItem(item, mapping.getInvoiceItem()))
                .collect(Collectors.toList());
    }

    /**
     * Verifica se dois invoice items são similares
     */
    private boolean isSimilarInvoiceItem(InvoiceItem item1, InvoiceItem item2) {
        return Objects.equals(item1.getAssetCode(), item2.getAssetCode()) &&
               Objects.equals(item1.getOperationType(), item2.getOperationType()) &&
               Objects.equals(item1.getQuantity(), item2.getQuantity()) &&
               Objects.equals(item1.getInvoice().getTradingDate(), 
                             item2.getInvoice().getTradingDate());
    }

    /**
     * Verifica se dois items são duplicatas exatas
     */
    private boolean isExactDuplicate(InvoiceItem item1, InvoiceItem item2) {
        return isSimilarInvoiceItem(item1, item2) &&
               Objects.equals(item1.getUnitPrice(), item2.getUnitPrice()) &&
               Objects.equals(item1.getTotalValue(), item2.getTotalValue()) &&
               Objects.equals(item1.getInvoice().getUser().getId(), 
                             item2.getInvoice().getUser().getId());
    }

    /**
     * Resultado da verificação de duplicata de um item
     */
    @lombok.Builder
    @lombok.Data
    public static class DuplicateCheckResult {
        private boolean isDuplicate;
        private List<String> reasons;
    }

    /**
     * Resultado da detecção de duplicatas de uma invoice
     */
    @lombok.Builder
    @lombok.Data
    public static class DuplicateDetectionResult {
        private boolean hasDuplicates;
        private List<InvoiceItem> duplicateItems;
        private List<InvoiceItem> validItems;
        private Map<String, List<String>> duplicateReasons;
        private int totalItems;
        private int validItemsCount;
        private int duplicateItemsCount;
        
        /**
         * Retorna taxa de duplicatas em percentual
         */
        public double getDuplicateRate() {
            return totalItems > 0 ? (double) duplicateItemsCount / totalItems * 100 : 0;
        }
        
        /**
         * Verifica se a invoice pode ser processada (tem itens válidos)
         */
        public boolean canBeProcessed() {
            return validItemsCount > 0;
        }
    }
}