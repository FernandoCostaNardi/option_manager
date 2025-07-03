package com.olisystem.optionsmanager.service.invoice.processing;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.Asset.AssetType;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.option_serie.OptionType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * 🔄 Mapper para converter InvoiceItem em OperationDataRequest
 * 
 * Responsável por mapear os dados da nota de corretagem
 * para o formato esperado pelo sistema de operações.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceOperationMapper {

    /**
     * Converte InvoiceItem para OperationDataRequest
     */
    public OperationDataRequest mapToOperationRequest(InvoiceItem invoiceItem, Invoice invoice, TradeType tradeType) {
        log.debug("🔄 Mapeando InvoiceItem {} para OperationDataRequest", invoiceItem.getId());
        
        try {
            // Extrair dados do ativo base e da série de opção
            AssetExtractionResult assetData = extractAssetData(invoiceItem);
            OptionSeriesExtractionResult optionData = extractOptionSeriesData(invoiceItem);
            
            // Determinar tipo de transação
            TransactionType transactionType = mapTransactionType(invoiceItem.getOperationType());
            
            // Construir request
            OperationDataRequest request = OperationDataRequest.builder()
                // === DADOS DO ATIVO BASE ===
                .baseAssetCode(assetData.getCode())
                .baseAssetName(assetData.getName())
                .baseAssetType(assetData.getType())
                .baseAssetLogoUrl(assetData.getLogoUrl())
                
                // === DADOS DA SÉRIE DE OPÇÃO ===
                .optionSeriesCode(optionData.getCode())
                .optionSeriesType(optionData.getOptionType())
                .optionSeriesStrikePrice(optionData.getStrikePrice())
                .optionSeriesExpirationDate(optionData.getExpirationDate())
                
                // === DADOS DA OPERAÇÃO ===
                .brokerageId(invoice.getBrokerage().getId())
                .analysisHouseId(null) // Invoices não têm casa de análise
                .transactionType(transactionType)
                .entryDate(invoice.getTradingDate())
                .exitDate(null) // Apenas entradas por enquanto
                .quantity(invoiceItem.getQuantity())
                .entryUnitPrice(invoiceItem.getUnitPrice())
                
                // === TARGETS (vazio por enquanto) ===
                .targets(new ArrayList<>())
                
                .build();
            
            log.debug("✅ Mapeamento concluído: {} {} @ {} - TradeType: {}", 
                     transactionType, optionData.getCode(), invoiceItem.getUnitPrice(), tradeType);
            
            return request;
            
        } catch (Exception e) {
            log.error("❌ Erro ao mapear InvoiceItem {}: {}", invoiceItem.getId(), e.getMessage());
            throw new IllegalArgumentException("Erro no mapeamento do item: " + e.getMessage(), e);
        }
    }

    /**
     * Extrai dados do ativo base a partir do código da especificação
     */
    private AssetExtractionResult extractAssetData(InvoiceItem invoiceItem) {
        String assetSpecification = invoiceItem.getAssetSpecification();
        String assetCode = invoiceItem.getAssetCode();
        
        // Se assetCode não está preenchido, extrair da especificação
        if (assetCode == null || assetCode.trim().isEmpty()) {
            assetCode = extractBaseAssetFromSpecification(assetSpecification);
        }
        
        // Determinar tipo do ativo baseado no market type e especificação
        AssetType assetType = determineAssetType(invoiceItem.getMarketType(), assetSpecification);
        
        return AssetExtractionResult.builder()
            .code(assetCode)
            .name(assetCode) // Por enquanto, nome = código
            .type(assetType)
            .logoUrl(null) // Será preenchido posteriormente se necessário
            .build();
    }

    /**
     * Extrai dados da série de opção
     */
    private OptionSeriesExtractionResult extractOptionSeriesData(InvoiceItem invoiceItem) {
        String marketType = invoiceItem.getMarketType();
        String assetSpecification = invoiceItem.getAssetSpecification();
        
        // Verificar se é opção baseado no market type
        if (!isOptionMarketType(marketType)) {
            // Para ações à vista, criar dados básicos
            return OptionSeriesExtractionResult.builder()
                .code(invoiceItem.getAssetCode())
                .optionType(null) // Não é opção
                .strikePrice(null)
                .expirationDate(null)
                .build();
        }
        
        // Para opções, extrair dados completos
        OptionType optionType = extractOptionType(marketType);
        BigDecimal strikePrice = invoiceItem.getStrikePrice();
        
        // Se strike não foi extraído, tentar extrair da especificação
        if (strikePrice == null) {
            strikePrice = extractStrikePriceFromSpecification(assetSpecification);
        }
        
        return OptionSeriesExtractionResult.builder()
            .code(assetSpecification) // Código completo da opção
            .optionType(optionType)
            .strikePrice(strikePrice)
            .expirationDate(invoiceItem.getExpirationDate())
            .build();
    }

    /**
     * Mapeia tipo de operação C/V para TransactionType
     */
    private TransactionType mapTransactionType(String operationType) {
        if (operationType == null) {
            throw new IllegalArgumentException("Tipo de operação não pode ser nulo");
        }
        
        switch (operationType.toUpperCase()) {
            case "C":
                return TransactionType.BUY;
            case "V":
                return TransactionType.SELL;
            default:
                throw new IllegalArgumentException("Tipo de operação inválido: " + operationType);
        }
    }

    /**
     * Extrai código do ativo base da especificação completa
     */
    private String extractBaseAssetFromSpecification(String specification) {
        if (specification == null || specification.trim().isEmpty()) {
            throw new IllegalArgumentException("Especificação do ativo não pode ser vazia");
        }
        
        // Exemplos de especificação:
        // "PETR4" -> "PETR4"
        // "CSANE165 ON" -> "CSAN"
        // "ASAIF980" -> "ASAI"
        
        // Para opções, extrair os primeiros 4 caracteres (padrão brasileiro)
        String cleaned = specification.trim().toUpperCase();
        
        // Se contém espaço, pegar parte antes do espaço
        if (cleaned.contains(" ")) {
            cleaned = cleaned.split(" ")[0];
        }
        
        // Se é código de opção (contém números no meio), extrair base
        if (cleaned.matches(".*\\d.*")) {
            // Extrair primeiros 4 caracteres alfabéticos
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
     * Determina tipo do ativo baseado no market type
     */
    private AssetType determineAssetType(String marketType, String specification) {
        if (marketType == null) {
            return AssetType.STOCK; // Default
        }
        
        String upperMarketType = marketType.toUpperCase();
        
        if (upperMarketType.contains("OPCAO") || upperMarketType.contains("OPTION")) {
            return AssetType.OPTION;
        }
        
        if (upperMarketType.contains("FII") || upperMarketType.contains("IMOBILIARIO")) {
            return AssetType.REIT;
        }
        
        if (upperMarketType.contains("ETF")) {
            return AssetType.ETF;
        }
        
        return AssetType.STOCK; // Default para ações à vista
    }

    /**
     * Verifica se o market type representa uma opção
     */
    private boolean isOptionMarketType(String marketType) {
        if (marketType == null) return false;
        
        String upper = marketType.toUpperCase();
        return upper.contains("OPCAO") || upper.contains("OPTION");
    }

    /**
     * Extrai tipo de opção (CALL/PUT) do market type
     */
    private OptionType extractOptionType(String marketType) {
        if (marketType == null) return null;
        
        String upper = marketType.toUpperCase();
        
        if (upper.contains("COMPRA") || upper.contains("CALL")) {
            return OptionType.CALL;
        }
        
        if (upper.contains("VENDA") || upper.contains("PUT")) {
            return OptionType.PUT;
        }
        
        return OptionType.CALL; // Default
    }

    /**
     * Extrai strike price da especificação quando não vem separado
     */
    private BigDecimal extractStrikePriceFromSpecification(String specification) {
        if (specification == null) return null;
        
        // Tentar extrair números decimais da especificação
        // Ex: "CSANE165 ON 16,50" -> 16.50
        try {
            String[] parts = specification.split(" ");
            for (String part : parts) {
                // Procurar por padrões como "16,50" ou "16.50"
                if (part.matches("\\d+[,.]\\d+")) {
                    return new BigDecimal(part.replace(",", "."));
                }
            }
        } catch (Exception e) {
            log.debug("Não foi possível extrair strike price de: {}", specification);
        }
        
        return null;
    }

    // === CLASSES DE RESULTADO INTERNO ===
    
    @lombok.Data
    @lombok.Builder
    private static class AssetExtractionResult {
        private String code;
        private String name;
        private AssetType type;
        private String logoUrl;
    }
    
    @lombok.Data
    @lombok.Builder
    private static class OptionSeriesExtractionResult {
        private String code;
        private OptionType optionType;
        private BigDecimal strikePrice;
        private java.time.LocalDate expirationDate;
    }
}