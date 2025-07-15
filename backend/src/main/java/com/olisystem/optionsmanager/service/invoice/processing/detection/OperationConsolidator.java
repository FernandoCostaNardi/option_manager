package com.olisystem.optionsmanager.service.invoice.processing.detection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Consolidador de operações
 * Agrupa operações relacionadas e cria operações consolidadas finais
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationConsolidator {

    /**
     * Consolida operações classificadas
     */
    public List<ConsolidatedOperation> consolidateOperations(List<ClassifiedOperation> classifiedOperations) {
        log.info("🔗 Consolidando {} operações classificadas", classifiedOperations.size());
        
        List<ConsolidatedOperation> consolidatedOperations = new ArrayList<>();
        
        try {
            // 1. Agrupar operações por chave de consolidação
            Map<String, List<ClassifiedOperation>> groupedOperations = groupOperationsByKey(classifiedOperations);
            
            // 2. Consolidar cada grupo
            for (Map.Entry<String, List<ClassifiedOperation>> entry : groupedOperations.entrySet()) {
                String groupKey = entry.getKey();
                List<ClassifiedOperation> group = entry.getValue();
                
                ConsolidatedOperation consolidatedOp = consolidateGroup(groupKey, group);
                if (consolidatedOp != null) {
                    consolidatedOperations.add(consolidatedOp);
                }
            }
            
            log.info("✅ Consolidadas {} operações finais", consolidatedOperations.size());
            
        } catch (Exception e) {
            log.error("❌ Erro durante consolidação: {}", e.getMessage(), e);
        }
        
        return consolidatedOperations;
    }

    /**
     * Agrupa operações por chave de consolidação
     */
    private Map<String, List<ClassifiedOperation>> groupOperationsByKey(List<ClassifiedOperation> operations) {
        return operations.stream()
            .collect(Collectors.groupingBy(this::generateConsolidationKey));
    }

    /**
     * Gera chave de consolidação para uma operação
     */
    private String generateConsolidationKey(ClassifiedOperation operation) {
        return String.format("%s_%s_%s_%s", 
            operation.getAssetCode(),
            operation.getTransactionType(),
            operation.getTradeType(),
            operation.getTradeDate());
    }

    /**
     * Consolida um grupo de operações
     */
    private ConsolidatedOperation consolidateGroup(String groupKey, List<ClassifiedOperation> group) {
        if (group.isEmpty()) {
            return null;
        }
        
        try {
            // Usar a primeira operação como base
            ClassifiedOperation baseOperation = group.get(0);
            
            // Calcular valores consolidados
            BigDecimal totalValue = group.stream()
                .map(ClassifiedOperation::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            int totalQuantity = group.stream()
                .mapToInt(ClassifiedOperation::getQuantity)
                .sum();
            
            BigDecimal averageUnitPrice = totalQuantity > 0 ? 
                totalValue.divide(BigDecimal.valueOf(totalQuantity), 4, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
            
            // Calcular confiança da consolidação
            double consolidationConfidence = calculateConsolidationConfidence(group);
            
            // Gerar motivo da consolidação
            String consolidationReason = generateConsolidationReason(group);
            
            // Criar operação consolidada
            ConsolidatedOperation consolidatedOp = ConsolidatedOperation.builder()
                .id(UUID.randomUUID())
                .consolidationId("CONS_" + System.currentTimeMillis())
                .assetCode(baseOperation.getAssetCode())
                .optionCode(baseOperation.getOptionCode())
                .transactionType(baseOperation.getTransactionType())
                .tradeType(baseOperation.getTradeType())
                .unitPrice(averageUnitPrice)
                .totalValue(totalValue)
                .quantity(totalQuantity)
                .tradeDate(baseOperation.getTradeDate())
                .sourceOperations(group)
                .consolidationReason(consolidationReason)
                .consolidationConfidence(consolidationConfidence)
                .notes(generateConsolidatedNotes(group))
                .isConfirmed(consolidationConfidence >= 0.8)
                .isReadyForCreation(consolidationConfidence >= 0.6)
                .build();
            
            log.debug("🔗 Operação consolidada: {} {} {} @ {} ({} operações)", 
                baseOperation.getTransactionType(), totalQuantity, baseOperation.getAssetCode(), 
                averageUnitPrice, group.size());
            
            return consolidatedOp;
            
        } catch (Exception e) {
            log.warn("⚠️ Erro ao consolidar grupo {}: {}", groupKey, e.getMessage());
            return null;
        }
    }

    /**
     * Calcula confiança da consolidação
     */
    private double calculateConsolidationConfidence(List<ClassifiedOperation> group) {
        if (group.isEmpty()) {
            return 0.0;
        }
        
        // Média das confianças das operações fonte
        double averageConfidence = group.stream()
            .mapToDouble(ClassifiedOperation::getClassificationConfidence)
            .average()
            .orElse(0.0);
        
        // Bônus para grupos maiores (mais confiável)
        double sizeBonus = Math.min(group.size() * 0.1, 0.3);
        
        return Math.min(averageConfidence + sizeBonus, 1.0);
    }

    /**
     * Gera motivo da consolidação
     */
    private String generateConsolidationReason(List<ClassifiedOperation> group) {
        if (group.size() == 1) {
            return "Operação única";
        }
        
        return String.format("Consolidadas %d operações do mesmo ativo/tipo/data", group.size());
    }

    /**
     * Gera notas consolidadas
     */
    private String generateConsolidatedNotes(List<ClassifiedOperation> group) {
        List<String> notes = new ArrayList<>();
        
        // Adicionar nota sobre consolidação
        if (group.size() > 1) {
            notes.add(String.format("Consolidada de %d operações", group.size()));
        }
        
        // Adicionar notas das operações fonte
        group.stream()
            .map(ClassifiedOperation::getNotes)
            .filter(note -> note != null && !note.trim().isEmpty())
            .distinct()
            .forEach(notes::add);
        
        return String.join("; ", notes);
    }
} 