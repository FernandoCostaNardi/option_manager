package com.olisystem.optionsmanager.service.invoice.processing;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.Asset.AssetType;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.option_serie.OptionType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

/**
 * Mapeia InvoiceItems para OperationDataRequest
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-04
 */
@Service
@Slf4j
public class InvoiceToOperationMapper {

    /**
     * Converte InvoiceItem em OperationDataRequest
     * ✅ MELHORADO: Validações mais robustas e logs detalhados
     * ✅ CORREÇÃO: Validação específica do brokerageId
     */
    public OperationDataRequest mapToOperationRequest(InvoiceItem item) {
        log.info("🔄 MAPEANDO InvoiceItem {} para OperationDataRequest", item.getId());
        log.info("📋 Asset: '{}', Market: '{}', Strike: {}, Expiration: {}", 
                item.getAssetCode(), item.getMarketType(), item.getStrikePrice(), item.getExpirationDate());
        
        // ✅ MELHORADO: Validações mais robustas
        validateInvoiceItem(item);
        
        // ✅ NOVO: Log detalhado do operationType
        log.info("🔍 OPERATION TYPE: '{}' (tamanho: {})", 
                item.getOperationType(), 
                item.getOperationType() != null ? item.getOperationType().length() : "null");
        
        // ✅ NOVO: Log mais detalhado para debug
        System.out.println("=== DEBUG OPERATION TYPE ===");
        System.out.println("Item ID: " + item.getId());
        System.out.println("OperationType: '" + item.getOperationType() + "'");
        System.out.println("OperationType null? " + (item.getOperationType() == null));
        System.out.println("OperationType length: " + (item.getOperationType() != null ? item.getOperationType().length() : "null"));
        System.out.println("=============================");
        
        try {
            TransactionType mappedType = mapTransactionType(item.getOperationType());
            log.info("🎯 TransactionType mapeado: {} (original: '{}')", mappedType, item.getOperationType());
            
            // ✅ MELHORADO: Validação do TransactionType mapeado
            validateMappedTransactionType(mappedType, item.getOperationType());
            
            // ✅ NOVO: Validação específica do brokerage
            validateBrokerage(item);
            
            // ✅ NOVO: Log do brokerageId antes de criar o request
            UUID brokerageId = item.getInvoice().getBrokerage().getId();
            log.info("🏢 BrokerageId: {} (null? {})", brokerageId, brokerageId == null);
            System.out.println("=== DEBUG BROKERAGE ===");
            System.out.println("Invoice: " + item.getInvoice().getId());
            System.out.println("Brokerage: " + (item.getInvoice().getBrokerage() != null ? item.getInvoice().getBrokerage().getId() : "NULL"));
            System.out.println("BrokerageId: " + brokerageId);
            System.out.println("=========================");
            
            // ✅ NOVO: Log do OperationDataRequest criado
            OperationDataRequest request = OperationDataRequest.builder()
                // === ASSET INFORMATION ===
                .baseAssetCode(extractBaseAssetCode(item.getAssetCode()))
                .baseAssetName(item.getAssetSpecification())
                .baseAssetType(determineAssetType(item.getMarketType()))
                .baseAssetLogoUrl(null) // Será preenchido pelo sistema se necessário
                
                // === OPTION SERIES (se for opção) ===
                .optionSeriesCode(item.getAssetCode())
                .optionSeriesType(determineOptionType(item.getMarketType()))
                .optionSeriesStrikePrice(extractStrikePriceFromOptionCode(item.getAssetCode(), item.getStrikePrice()))
                .optionSeriesExpirationDate(item.getExpirationDate())
                
                // === OPERATION DETAILS ===
                .targets(Collections.emptyList()) // Sem targets por enquanto
                .brokerageId(brokerageId) // ✅ CORREÇÃO: Usar variável validada
                .analysisHouseId(null) // Invoice não tem casa de análise
                .transactionType(mappedType)
                .entryDate(item.getInvoice().getTradingDate())
                .exitDate(null) // Operação de entrada não tem data de saída
                .quantity(item.getQuantity())
                .entryUnitPrice(item.getUnitPrice())
                .build();
            
            // ✅ MELHORADO: Validação do request criado
            validateCreatedRequest(request, item);
            
            // ✅ NOVO: Log do request criado
            log.info("✅ OperationDataRequest criado com TransactionType: {}", request.getTransactionType());
            System.out.println("=== DEBUG REQUEST ===");
            System.out.println("TransactionType no request: " + request.getTransactionType());
            System.out.println("BrokerageId no request: " + request.getBrokerageId());
            System.out.println("=========================");
            
            return request;
                
        } catch (Exception e) {
            log.error("Erro ao mapear InvoiceItem {}: {}", item.getId(), e.getMessage(), e);
            throw new RuntimeException("Erro ao mapear invoice item para operação: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extrai o código base do ativo (remove sufixos de opção)
     */
    private String extractBaseAssetCode(String assetCode) {
        if (assetCode == null) return null;
        
        // Remove números e letras finais para opções (ex: PETR4F365 -> PETR4)
        String baseCode = assetCode.replaceAll("([A-Z]+)\\d+$", "$1");
        
        // Se ainda tem letras no final, remove (ex: PETR4F -> PETR4)
        baseCode = baseCode.replaceAll("([A-Z0-9]+)[A-Z]$", "$1");
        
        return baseCode;
    }
    
    /**
     * Extrai o strike price do código da opção ou usa o valor fornecido
     */
    private BigDecimal extractStrikePriceFromOptionCode(String assetCode, BigDecimal providedStrikePrice) {
        log.info("🔍 Extraindo strike price para assetCode: '{}', providedStrikePrice: {}", assetCode, providedStrikePrice);
        
        // Se o strike price fornecido não é nulo, usa ele
        if (providedStrikePrice != null) {
            log.info("✅ Usando strike price fornecido: {}", providedStrikePrice);
            return providedStrikePrice;
        }
        
        // Se o assetCode é nulo, não consegue extrair
        if (assetCode == null || assetCode.trim().isEmpty()) {
            log.warn("⚠️ AssetCode é nulo ou vazio, não é possível extrair strike price");
            return null;
        }
        
        // Tenta extrair o strike price do código da opção
        // Exemplo: VALEF541 (VALE + F + 541 = strike 54.10)
        String upperCode = assetCode.toUpperCase().trim();
        
        // Padrão para extrair números do final (strike price)
        // Exemplo: VALEF541 -> 541
        String strikePattern = ".*?(\\d+)$";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(strikePattern);
        java.util.regex.Matcher matcher = pattern.matcher(upperCode);
        
        if (matcher.find()) {
            String strikeStr = matcher.group(1);
            try {
                // Converte para BigDecimal dividindo por 10 (ex: 541 -> 54.10)
                BigDecimal strikePrice = new BigDecimal(strikeStr).divide(BigDecimal.valueOf(10));
                log.info("✅ Strike price extraído do código: {} -> {}", strikeStr, strikePrice);
                return strikePrice;
            } catch (NumberFormatException e) {
                log.warn("⚠️ Não foi possível converter '{}' para strike price", strikeStr);
            }
        }
        
        log.warn("⚠️ Não foi possível extrair strike price do código: {}", assetCode);
        return null;
    }
    
    /**
     * Determina o tipo de ativo baseado no market type
     */
    private AssetType determineAssetType(String marketType) {
        log.info("🔍 Determinando AssetType para marketType: '{}'", marketType);
        
        if (marketType == null) return AssetType.STOCK;
        
        String market = marketType.toUpperCase();
        AssetType result;
        
        if (market.contains("OPCAO") || market.contains("OPTION")) {
            result = AssetType.OPTION;
        } else if (market.contains("VISTA") || market.contains("SPOT")) {
            result = AssetType.STOCK;
        } else if (market.contains("FII") || market.contains("REIT")) {
            result = AssetType.REIT;
        } else if (market.contains("ETF")) {
            result = AssetType.ETF;
        } else {
            result = AssetType.STOCK; // Default
        }
        
        log.info("🎯 AssetType determinado: {} para marketType: '{}'", result, marketType);
        return result;
    }
    
    /**
     * Determina o tipo de opção
     */
    private OptionType determineOptionType(String marketType) {
        log.info("🔍 Determinando OptionType para marketType: '{}'", marketType);
        
        if (marketType == null) return null;
        
        String market = marketType.toUpperCase();
        OptionType result;
        
        if (market.contains("COMPRA") || market.contains("CALL")) {
            result = OptionType.CALL;
        } else if (market.contains("VENDA") || market.contains("PUT")) {
            result = OptionType.PUT;
        } else {
            result = OptionType.CALL; // Default para opções
        }
        
        log.info("🎯 OptionType determinado: {} para marketType: '{}'", result, marketType);
        return result;
    }
    
    /**
     * Mapeia tipo de transação
     * ✅ MELHORADO: Validações mais robustas e logs detalhados
     */
    private TransactionType mapTransactionType(String operationType) {
        log.info("🔍 MAPEANDO operationType: '{}' (null? {})", operationType, operationType == null);
        
        if (operationType == null) {
            log.warn("⚠️ operationType é null, retornando BUY como default");
            return TransactionType.BUY;
        }
        
        // ✅ MELHORADO: Validação de string vazia
        if (operationType.trim().isEmpty()) {
            log.warn("⚠️ operationType é vazio, retornando BUY como default");
            return TransactionType.BUY;
        }
        
        String upperOperationType = operationType.toUpperCase().trim();
        log.info("🔍 operationType normalizado: '{}'", upperOperationType);
        
        // ✅ NOVO: Log mais detalhado para debug
        System.out.println("=== MAPEAMENTO TRANSACTION TYPE ===");
        System.out.println("Original: '" + operationType + "'");
        System.out.println("Normalizado: '" + upperOperationType + "'");
        System.out.println("Equals 'V'? " + upperOperationType.equals("V"));
        System.out.println("Equals 'C'? " + upperOperationType.equals("C"));
        System.out.println("Contains 'VENDA'? " + upperOperationType.contains("VENDA"));
        System.out.println("Contains 'COMPRA'? " + upperOperationType.contains("COMPRA"));
        System.out.println("================================");
        
        TransactionType result;
        
        // ✅ MELHORADO: Mapeamento mais robusto com validações
        if ("V".equals(upperOperationType) || "VENDA".equals(upperOperationType) || "SELL".equals(upperOperationType)) {
            log.info("✅ Mapeando '{}' para SELL", upperOperationType);
            result = TransactionType.SELL;
        } else if ("C".equals(upperOperationType) || "COMPRA".equals(upperOperationType) || "BUY".equals(upperOperationType)) {
            log.info("✅ Mapeando '{}' para BUY", upperOperationType);
            result = TransactionType.BUY;
        } else {
            log.warn("⚠️ operationType '{}' não reconhecido, retornando BUY como default", upperOperationType);
            result = TransactionType.BUY; // Default
        }
        
        // ✅ MELHORADO: Validação do resultado
        if (result == null) {
            log.error("❌ ERRO CRÍTICO: TransactionType mapeado é null!");
            result = TransactionType.BUY; // Fallback seguro
        }
        
        log.info("🎯 Resultado final do mapeamento: {} (original: '{}')", result, operationType);
        System.out.println("Resultado final: " + result);
        System.out.println("================================");
        return result;
    }
    
    /**
     * ✅ NOVO MÉTODO: Validar InvoiceItem antes do mapeamento
     */
    private void validateInvoiceItem(InvoiceItem item) {
        if (item == null) {
            throw new IllegalArgumentException("InvoiceItem não pode ser nulo");
        }
        
        if (item.getId() == null) {
            throw new IllegalArgumentException("ID do InvoiceItem não pode ser nulo");
        }
        
        if (item.getAssetCode() == null || item.getAssetCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Código do ativo não pode ser nulo ou vazio");
        }
        
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero. Valor: " + item.getQuantity());
        }
        
        if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço unitário deve ser maior que zero. Valor: " + item.getUnitPrice());
        }
        
        if (item.getInvoice() == null) {
            throw new IllegalArgumentException("Invoice não pode ser nula");
        }
        
        if (item.getInvoice().getBrokerage() == null) {
            throw new IllegalArgumentException("Corretora não pode ser nula");
        }
        
        if (item.getInvoice().getTradingDate() == null) {
            throw new IllegalArgumentException("Data de negociação não pode ser nula");
        }
        
        log.debug("✅ InvoiceItem validado com sucesso: ID={}, Asset={}, Qty={}", 
            item.getId(), item.getAssetCode(), item.getQuantity());
    }
    
    /**
     * ✅ NOVO MÉTODO: Validar TransactionType mapeado
     */
    private void validateMappedTransactionType(TransactionType mappedType, String originalType) {
        if (mappedType == null) {
            throw new IllegalArgumentException("TransactionType mapeado não pode ser nulo");
        }
        
        log.debug("✅ TransactionType validado: {} (original: '{}')", mappedType, originalType);
    }
    
    /**
     * ✅ NOVO MÉTODO: Validar brokerage especificamente
     */
    private void validateBrokerage(InvoiceItem item) {
        if (item.getInvoice() == null) {
            throw new IllegalArgumentException("Invoice não pode ser nula");
        }
        
        if (item.getInvoice().getBrokerage() == null) {
            throw new IllegalArgumentException("Corretora não pode ser nula na invoice " + item.getInvoice().getId());
        }
        
        if (item.getInvoice().getBrokerage().getId() == null) {
            throw new IllegalArgumentException("ID da corretora não pode ser nulo na invoice " + item.getInvoice().getId());
        }
        
        log.info("✅ Brokerage validado: ID={}, Nome={}", 
            item.getInvoice().getBrokerage().getId(), 
            item.getInvoice().getBrokerage().getName());
    }
    
    /**
     * ✅ NOVO MÉTODO: Validar request criado
     */
    private void validateCreatedRequest(OperationDataRequest request, InvoiceItem originalItem) {
        if (request == null) {
            throw new IllegalArgumentException("OperationDataRequest criado não pode ser nulo");
        }
        
        if (request.getTransactionType() == null) {
            throw new IllegalArgumentException("TransactionType no request não pode ser nulo");
        }
        
        if (!request.getTransactionType().equals(mapTransactionType(originalItem.getOperationType()))) {
            throw new IllegalArgumentException("TransactionType no request não corresponde ao mapeamento original");
        }
        
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantidade no request deve ser maior que zero");
        }
        
        if (request.getEntryUnitPrice() == null || request.getEntryUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço unitário no request deve ser maior que zero");
        }
        
        log.debug("✅ OperationDataRequest validado com sucesso: TransactionType={}, Qty={}, Price={}", 
            request.getTransactionType(), request.getQuantity(), request.getEntryUnitPrice());
    }
}
