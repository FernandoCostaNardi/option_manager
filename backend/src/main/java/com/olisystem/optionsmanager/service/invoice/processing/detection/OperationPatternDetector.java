package com.olisystem.optionsmanager.service.invoice.processing.detection;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.service.option_series.OptionSerieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Detector de padrões de operações
 * Identifica operações candidatas nos itens das invoices
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationPatternDetector {

    private final OptionSerieService optionSerieService;

    /**
     * Detecta padrões de operações nos itens fornecidos
     */
    public List<DetectedOperation> detectPatterns(List<InvoiceItem> items, User user) {
        log.info("🔍 Detectando padrões em {} itens", items.size());
        
        List<DetectedOperation> detectedOperations = new ArrayList<>();
        
        try {
            // 1. Filtrar itens válidos para opções
            List<InvoiceItem> validItems = filterValidOptionItems(items);
            log.info("📊 {} itens válidos para análise", validItems.size());
            
            // 2. Detectar operações individuais
            for (InvoiceItem item : validItems) {
                DetectedOperation operation = detectOperationFromItem(item, user);
                if (operation != null) {
                    detectedOperations.add(operation);
                }
            }
            
            // 3. Detectar padrões de day trade
            List<DetectedOperation> dayTradePatterns = detectDayTradePatterns(validItems, user);
            detectedOperations.addAll(dayTradePatterns);
            
            // 4. Detectar padrões de swing trade
            List<DetectedOperation> swingTradePatterns = detectSwingTradePatterns(validItems, user);
            detectedOperations.addAll(swingTradePatterns);
            
            log.info("🎯 Detectadas {} operações candidatas", detectedOperations.size());
            
        } catch (Exception e) {
            log.error("❌ Erro durante detecção de padrões: {}", e.getMessage(), e);
        }
        
        return detectedOperations;
    }

    /**
     * Filtra itens válidos para análise de opções
     */
    private List<InvoiceItem> filterValidOptionItems(List<InvoiceItem> items) {
        return items.stream()
            .filter(this::isValidOptionItem)
            .collect(Collectors.toList());
    }

    /**
     * Verifica se um item é válido para análise de opções
     */
    private boolean isValidOptionItem(InvoiceItem item) {
        if (item == null || item.getAssetCode() == null) {
            return false;
        }
        
        // Verificar se é um ativo de opção válido
        // Por enquanto, aceita qualquer código que tenha pelo menos 6 caracteres
        String assetCode = item.getAssetCode().trim();
        return assetCode.length() >= 6 && 
               (assetCode.contains("OPCAO") || assetCode.contains("OPTION") || 
                assetCode.matches(".*[0-9]{2}[A-Z]{1}.*")); // Padrão de opções brasileiras
    }

    /**
     * Detecta operação individual a partir de um item
     */
    private DetectedOperation detectOperationFromItem(InvoiceItem item, User user) {
        try {
            // Determinar tipo de transação
            TransactionType transactionType = determineTransactionType(item.getOperationType());
            
            // Calcular confiança baseada na qualidade dos dados
            double confidence = calculateConfidenceScore(item);
            
            // Criar operação detectada
            DetectedOperation operation = DetectedOperation.builder()
                .id(UUID.randomUUID())
                .detectionId("DET_" + System.currentTimeMillis())
                .assetCode(item.getAssetCode())
                .optionCode(extractOptionCode(item.getAssetSpecification()))
                .transactionType(transactionType)
                .unitPrice(item.getUnitPrice())
                .totalValue(item.getTotalValue())
                .quantity(item.getQuantity())
                .tradeDate(item.getInvoice().getTradingDate())
                .sourceInvoice(item.getInvoice())
                .sourceItems(List.of(item))
                .user(user)
                .confidenceScore(confidence)
                .confidenceReason(generateConfidenceReason(item, confidence))
                .notes(generateNotes(item))
                .isConfirmed(confidence >= 0.8)
                .build();
            
            log.debug("🎯 Operação detectada: {} {} {} @ {}", 
                transactionType, item.getQuantity(), item.getAssetCode(), item.getUnitPrice());
            
            return operation;
            
        } catch (Exception e) {
            log.warn("⚠️ Erro ao detectar operação do item {}: {}", item.getAssetCode(), e.getMessage());
            return null;
        }
    }

    /**
     * Determina tipo de transação baseado no código da operação
     */
    private TransactionType determineTransactionType(String operationType) {
        if (operationType == null) {
            return TransactionType.BUY; // Default
        }
        
        return switch (operationType.toUpperCase()) {
            case "C", "COMPRA", "BUY" -> TransactionType.BUY;
            case "V", "VENDA", "SELL" -> TransactionType.SELL;
            default -> TransactionType.BUY; // Default
        };
    }

    /**
     * Calcula score de confiança baseado na qualidade dos dados
     */
    private double calculateConfidenceScore(InvoiceItem item) {
        double score = 0.5; // Base
        
        // Preço unitário válido
        if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
            score += 0.2;
        }
        
        // Quantidade válida
        if (item.getQuantity() != null && item.getQuantity() > 0) {
            score += 0.2;
        }
        
        // Código do ativo válido
        if (item.getAssetCode() != null && !item.getAssetCode().trim().isEmpty()) {
            score += 0.1;
        }
        
        // Data de negociação válida
        if (item.getInvoice().getTradingDate() != null) {
            score += 0.1;
        }
        
        // Day trade identificado
        if (Boolean.TRUE.equals(item.getIsDayTrade())) {
            score += 0.1;
        }
        
        return Math.min(score, 1.0);
    }

    /**
     * Extrai código da opção da especificação do ativo
     */
    private String extractOptionCode(String assetSpecification) {
        if (assetSpecification == null) {
            return null;
        }
        
        // Implementar lógica de extração do código da opção
        // Por enquanto, retorna a especificação completa
        return assetSpecification;
    }

    /**
     * Gera motivo da confiança
     */
    private String generateConfidenceReason(InvoiceItem item, double confidence) {
        List<String> reasons = new ArrayList<>();
        
        if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
            reasons.add("preço válido");
        }
        
        if (item.getQuantity() != null && item.getQuantity() > 0) {
            reasons.add("quantidade válida");
        }
        
        if (Boolean.TRUE.equals(item.getIsDayTrade())) {
            reasons.add("day trade identificado");
        }
        
        return String.format("Confiança %.1f%%: %s", confidence * 100, String.join(", ", reasons));
    }

    /**
     * Gera notas para a operação
     */
    private String generateNotes(InvoiceItem item) {
        List<String> notes = new ArrayList<>();
        
        if (Boolean.TRUE.equals(item.getIsDayTrade())) {
            notes.add("Day Trade");
        }
        
        if (item.getObservations() != null && !item.getObservations().trim().isEmpty()) {
            notes.add("Obs: " + item.getObservations());
        }
        
        return String.join("; ", notes);
    }

    /**
     * Detecta padrões de day trade
     */
    private List<DetectedOperation> detectDayTradePatterns(List<InvoiceItem> items, User user) {
        // Implementar detecção de padrões de day trade
        // Por exemplo: compra e venda no mesmo dia
        return new ArrayList<>();
    }

    /**
     * Detecta padrões de swing trade
     */
    private List<DetectedOperation> detectSwingTradePatterns(List<InvoiceItem> items, User user) {
        // Implementar detecção de padrões de swing trade
        // Por exemplo: compra em um dia, venda em outro
        return new ArrayList<>();
    }
} 