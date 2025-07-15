package com.olisystem.optionsmanager.service.invoice.processing.detection;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.repository.InvoiceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.olisystem.optionsmanager.service.invoice.processing.detection.DetectionResult;

/**
 * Motor de detecção de operações em invoices
 * ✅ CORREÇÃO: Usa repositório em vez de acessar coleção lazy
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationDetectionEngine {

    private final InvoiceItemRepository invoiceItemRepository;
    private final OperationPatternDetector patternDetector;
    private final OperationTypeClassifier typeClassifier;
    private final OperationConsolidator consolidator;

    /**
     * Detecta operações em múltiplas invoices
     * ✅ CORREÇÃO: Usa repositório para buscar itens
     */
    public DetectionResult detectOperations(List<Invoice> invoices, User user) {
        log.info("🔍 Detectando operações em {} invoices", invoices.size());
        
        DetectionResult result = DetectionResult.builder()
            .success(true)
            .totalInvoices(invoices.size())
            .build();
        
        try {
            // ✅ CORREÇÃO: Buscar todos os itens via repositório
            List<InvoiceItem> allItems = extractAllItems(invoices);
            result.setTotalItems(allItems.size());
            
            log.info("📊 Extraídos {} itens para análise", allItems.size());
            
            // Detectar padrões de operações
            List<DetectedOperation> detectedOps = patternDetector.detectPatterns(allItems, user);
            result.setDetectedOperations(detectedOps);
            
            log.info("🎯 Detectadas {} operações candidatas", detectedOps.size());
            
            // Classificar tipos de operação
            List<ClassifiedOperation> classifiedOps = typeClassifier.classifyOperations(detectedOps);
            result.setClassifiedOperations(classifiedOps);
            
            log.info("🏷️ Classificadas {} operações", classifiedOps.size());
            
            // Consolidar operações relacionadas
            List<ConsolidatedOperation> consolidatedOps = consolidator.consolidateOperations(classifiedOps);
            result.setConsolidatedOperations(consolidatedOps);
            
            log.info("🔗 Consolidadas {} operações finais", consolidatedOps.size());
            
            // Calcular estatísticas
            calculateDetectionStats(result);
            
            log.info("✅ Detecção concluída: {} operações finais de {} candidatas", 
                consolidatedOps.size(), detectedOps.size());
            
        } catch (Exception e) {
            log.error("❌ Erro durante detecção de operações: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage("Erro na detecção: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Extrai todos os itens de todas as invoices
     * ✅ CORREÇÃO: Usa repositório para buscar itens
     */
    private List<InvoiceItem> extractAllItems(List<Invoice> invoices) {
        List<InvoiceItem> allItems = new ArrayList<>();
        
        for (Invoice invoice : invoices) {
            List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithAllRelations(invoice.getId());
            allItems.addAll(items);
        }
        
        return allItems.stream()
            .filter(item -> item != null)
            .collect(Collectors.toList());
    }

    /**
     * Calcula estatísticas da detecção
     */
    private void calculateDetectionStats(DetectionResult result) {
        int totalDetected = result.getDetectedOperations().size();
        int totalClassified = result.getClassifiedOperations().size();
        int totalConsolidated = result.getConsolidatedOperations().size();
        
        result.setDetectionRate(totalDetected > 0 ? 
            (double) totalConsolidated / totalDetected * 100 : 0);
        result.setConsolidationRate(totalClassified > 0 ? 
            (double) totalConsolidated / totalClassified * 100 : 0);
        
        // Agrupar por tipo de operação
        Map<String, Long> typeDistribution = result.getConsolidatedOperations().stream()
            .collect(Collectors.groupingBy(
                op -> op.getTransactionType().name(),
                Collectors.counting()
            ));
        result.setTypeDistribution(typeDistribution);
    }

    /**
     * Detecta operações em uma única invoice
     */
    public DetectionResult detectOperationsForInvoice(Invoice invoice, User user) {
        return detectOperations(List.of(invoice), user);
    }

    /**
     * Valida se um item pode gerar uma operação
     */
    public boolean canGenerateOperation(InvoiceItem item) {
        if (item == null) return false;
        
        // Validar campos obrigatórios
        if (item.getAssetCode() == null || item.getAssetCode().trim().isEmpty()) {
            return false;
        }
        
        if (item.getOperationType() == null) {
            return false;
        }
        
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            return false;
        }
        
        if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Validar tipo de operação
        String operationType = item.getOperationType().toUpperCase();
        if (!"C".equals(operationType) && !"V".equals(operationType)) {
            return false;
        }
        
        return true;
    }
} 