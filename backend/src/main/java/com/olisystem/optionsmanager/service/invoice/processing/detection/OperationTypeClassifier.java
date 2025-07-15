package com.olisystem.optionsmanager.service.invoice.processing.detection;

import com.olisystem.optionsmanager.model.operation.TradeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Classificador de tipos de operação
 * Classifica operações detectadas com tipos específicos de trade
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationTypeClassifier {

    /**
     * Classifica operações detectadas
     */
    public List<ClassifiedOperation> classifyOperations(List<DetectedOperation> detectedOperations) {
        log.info("🏷️ Classificando {} operações detectadas", detectedOperations.size());
        
        List<ClassifiedOperation> classifiedOperations = new ArrayList<>();
        
        try {
            for (DetectedOperation detectedOp : detectedOperations) {
                ClassifiedOperation classifiedOp = classifyOperation(detectedOp);
                if (classifiedOp != null) {
                    classifiedOperations.add(classifiedOp);
                }
            }
            
            log.info("✅ Classificadas {} operações", classifiedOperations.size());
            
        } catch (Exception e) {
            log.error("❌ Erro durante classificação: {}", e.getMessage(), e);
        }
        
        return classifiedOperations;
    }

    /**
     * Classifica uma operação individual
     */
    private ClassifiedOperation classifyOperation(DetectedOperation detectedOp) {
        try {
            // Determinar tipo de trade
            TradeType tradeType = determineTradeType(detectedOp);
            
            // Calcular confiança da classificação
            double classificationConfidence = calculateClassificationConfidence(detectedOp, tradeType);
            
            // Gerar motivo da classificação
            String classificationReason = generateClassificationReason(detectedOp, tradeType);
            
            // Criar operação classificada
            ClassifiedOperation classifiedOp = ClassifiedOperation.builder()
                .id(UUID.randomUUID())
                .detectionId(detectedOp.getDetectionId())
                .assetCode(detectedOp.getAssetCode())
                .optionCode(detectedOp.getOptionCode())
                .transactionType(detectedOp.getTransactionType())
                .tradeType(tradeType)
                .unitPrice(detectedOp.getUnitPrice())
                .totalValue(detectedOp.getTotalValue())
                .quantity(detectedOp.getQuantity())
                .tradeDate(detectedOp.getTradeDate())
                .classificationReason(classificationReason)
                .classificationConfidence(classificationConfidence)
                .tradeTypeReason(generateTradeTypeReason(tradeType))
                .notes(detectedOp.getNotes())
                .isDayTrade(TradeType.DAY.equals(tradeType))
                .isSwingTrade(TradeType.SWING.equals(tradeType))
                .isPositionTrade(TradeType.SWING.equals(tradeType)) // Alias para swing trade
                .build();
            
            log.debug("🏷️ Operação classificada: {} {} como {}", 
                detectedOp.getTransactionType(), detectedOp.getAssetCode(), tradeType);
            
            return classifiedOp;
            
        } catch (Exception e) {
            log.warn("⚠️ Erro ao classificar operação {}: {}", 
                detectedOp.getAssetCode(), e.getMessage());
            return null;
        }
    }

    /**
     * Determina o tipo de trade baseado nas características da operação
     */
    private TradeType determineTradeType(DetectedOperation detectedOp) {
        // Verificar se é day trade baseado nas observações
        if (isDayTradeByObservations(detectedOp)) {
            return TradeType.DAY;
        }
        
        // Verificar se é day trade baseado na data
        if (isDayTradeByDate(detectedOp)) {
            return TradeType.DAY;
        }
        
        // Verificar se é swing trade baseado no contexto
        if (isSwingTradeByContext(detectedOp)) {
            return TradeType.SWING;
        }
        
        // Default: swing trade
        return TradeType.SWING;
    }

    /**
     * Verifica se é day trade baseado nas observações
     */
    private boolean isDayTradeByObservations(DetectedOperation detectedOp) {
        if (detectedOp.getNotes() == null) {
            return false;
        }
        
        String notes = detectedOp.getNotes().toUpperCase();
        return notes.contains("DAY TRADE") || 
               notes.contains("D") ||
               notes.contains("DT");
    }

    /**
     * Verifica se é day trade baseado na data
     */
    private boolean isDayTradeByDate(DetectedOperation detectedOp) {
        // Por enquanto, assume que todas as operações são swing trade
        // A lógica de day trade seria baseada na comparação com outras operações
        return false;
    }

    /**
     * Verifica se é swing trade baseado no contexto
     */
    private boolean isSwingTradeByContext(DetectedOperation detectedOp) {
        // Por enquanto, assume que operações sem indicação específica são swing trade
        return true;
    }

    /**
     * Calcula confiança da classificação
     */
    private double calculateClassificationConfidence(DetectedOperation detectedOp, TradeType tradeType) {
        double baseConfidence = detectedOp.getConfidenceScore();
        
        // Ajustar confiança baseado na qualidade da classificação
        if (TradeType.DAY.equals(tradeType) && isDayTradeByObservations(detectedOp)) {
            baseConfidence += 0.2; // Alta confiança para day trade com observações
        } else if (TradeType.SWING.equals(tradeType)) {
            baseConfidence += 0.1; // Confiança moderada para swing trade
        }
        
        return Math.min(baseConfidence, 1.0);
    }

    /**
     * Gera motivo da classificação
     */
    private String generateClassificationReason(DetectedOperation detectedOp, TradeType tradeType) {
        List<String> reasons = new ArrayList<>();
        
        if (TradeType.DAY.equals(tradeType)) {
            if (isDayTradeByObservations(detectedOp)) {
                reasons.add("observações indicam day trade");
            } else if (isDayTradeByDate(detectedOp)) {
                reasons.add("data indica day trade");
            }
        } else if (TradeType.SWING.equals(tradeType)) {
            reasons.add("padrão de swing trade");
        }
        
        return String.format("Classificado como %s: %s", 
            tradeType.getDescription(), String.join(", ", reasons));
    }

    /**
     * Gera motivo específico do tipo de trade
     */
    private String generateTradeTypeReason(TradeType tradeType) {
        return switch (tradeType) {
            case DAY -> "Operação realizada no mesmo dia";
            case SWING -> "Operação realizada em dias diferentes";
        };
    }
} 