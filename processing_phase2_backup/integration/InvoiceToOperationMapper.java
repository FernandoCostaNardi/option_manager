package com.olisystem.optionsmanager.service.invoice.processing.integration;

import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.Asset.AssetType;
import com.olisystem.optionsmanager.model.option_serie.OptionType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.service.invoice.processing.detection.OperationMatchingService.ItemProcessingPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Serviço para converter InvoiceItems em DTOs de Operation
 * Mapeia dados de invoice para criação ou finalização de operações
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@Slf4j
public class InvoiceToOperationMapper {

    /**
     * Converte InvoiceItem para OperationDataRequest (nova operação)
     * 
     * @param plan Plano de processamento do item
     * @return Request para criar nova operação
     */
    public OperationDataRequest mapToOperationDataRequest(ItemProcessingPlan plan) {
        InvoiceItem item = plan.getInvoiceItem();
        Invoice invoice = item.getInvoice();
        
        log.debug("🔄 Mapeando item {} para OperationDataRequest", item.getAssetCode());
        
        // Extrair informações do código do ativo
        AssetInfo assetInfo = extractAssetInfo(item.getAssetCode(), item.getAssetSpecification());
        
        OperationDataRequest request = OperationDataRequest.builder()
                // Dados do ativo base
                .baseAssetCode(assetInfo.getBaseCode())
                .baseAssetName(assetInfo.getBaseName())
                .baseAssetType(assetInfo.getAssetType())
                .baseAssetLogoUrl(null) // Será preenchido pelo sistema
                
                // Dados da série de opção
                .optionSeriesCode(item.getAssetCode())
                .optionSeriesType(assetInfo.getOptionType())
                .optionSeriesStrikePrice(item.getStrikePrice())
                .optionSeriesExpirationDate(item.getExpirationDate())
                
                // Dados da operação
                .brokerageId(invoice.getBrokerage().getId())
                .analysisHouseId(invoice.getUser().getId()) // Usar user como analysis house temporariamente
                .transactionType(mapTransactionType(item.getOperationType()))
                .entryDate(invoice.getTradingDate())
                .quantity(item.getQuantity())
                .entryUnitPrice(item.getUnitPrice())
                
                // Targets vazios por enquanto
                .targets(null)
                .build();
        
        log.debug("✅ Mapeamento concluído: {} {} cotas de {} @ {}",
                 request.getTransactionType(), request.getQuantity(),
                 request.getOptionSeriesCode(), request.getEntryUnitPrice());
        
        return request;
    }

    /**
     * Converte InvoiceItem para OperationFinalizationRequest (finalizar operação)
     * 
     * @param plan Plano de processamento do item
     * @return Request para finalizar operação existente
     */
    public OperationFinalizationRequest mapToFinalizationRequest(ItemProcessingPlan plan) {
        InvoiceItem item = plan.getInvoiceItem();
        Operation targetOperation = plan.getTargetOperation();
        
        if (targetOperation == null) {
            throw new IllegalArgumentException("Target operation é obrigatória para finalização");
        }
        
        log.debug("🎯 Mapeando item {} para finalização da operação {}", 
                 item.getAssetCode(), targetOperation.getId().toString().substring(0, 8));
        
        OperationFinalizationRequest request = OperationFinalizationRequest.builder()
                .operationId(targetOperation.getId())
                .exitDate(item.getInvoice().getTradingDate())
                .exitUnitPrice(item.getUnitPrice())
                .build();
        
        log.debug("✅ Finalização mapeada: operação {} @ {} em {}", 
                 targetOperation.getId().toString().substring(0, 8),
                 request.getExitUnitPrice(), request.getExitDate());
        
        return request;
    }

    /**
     * Extrai informações do ativo a partir do código
     */
    private AssetInfo extractAssetInfo(String assetCode, String assetSpecification) {
        if (assetCode == null || assetCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Código do ativo é obrigatório");
        }
        
        String cleanCode = assetCode.trim().toUpperCase();
        
        // Determinar se é opção baseado no padrão do código
        boolean isOption = isOptionCode(cleanCode, assetSpecification);
        
        if (isOption) {
            return extractOptionInfo(cleanCode, assetSpecification);
        } else {
            return extractStockInfo(cleanCode);
        }
    }

    /**
     * Verifica se o código representa uma opção
     */
    private boolean isOptionCode(String code, String specification) {
        // Verificar se contém indicadores de opção
        if (specification != null && 
            (specification.contains("OPCAO") || specification.contains("OPTION"))) {
            return true;
        }
        
        // Padrão comum de opções: XXXXX[F|E]XXX
        return code.matches("^[A-Z]{4,5}[FE]\\d+$");
    }

    /**
     * Extrai informações de opção
     */
    private AssetInfo extractOptionInfo(String optionCode, String specification) {
        // Extrair código base (ex: PETR4F336 -> PETR4)
        String baseCode = optionCode.replaceAll("[FE]\\d+$", "");
        
        // Determinar tipo da opção baseado na especificação
        OptionType optionType = OptionType.CALL; // Default
        if (specification != null) {
            if (specification.contains("VENDA") || specification.contains("PUT")) {
                optionType = OptionType.PUT;
            }
        }
        
        return AssetInfo.builder()
                .baseCode(baseCode)
                .baseName(baseCode) // Nome será resolvido pelo sistema
                .assetType(AssetType.OPTION)
                .optionType(optionType)
                .build();
    }

    /**
     * Extrai informações de ação
     */
    private AssetInfo extractStockInfo(String stockCode) {
        // Remover sufixos ON/PN se presentes
        String baseCode = stockCode.split(" ")[0];
        
        return AssetInfo.builder()
                .baseCode(baseCode)
                .baseName(baseCode)
                .assetType(AssetType.STOCK)
                .optionType(null)
                .build();
    }

    /**
     * Mapeia tipo de operação da invoice para TransactionType
     */
    private TransactionType mapTransactionType(String operationType) {
        if ("C".equals(operationType)) {
            return TransactionType.BUY;
        } else if ("V".equals(operationType)) {
            return TransactionType.SELL;
        } else {
            throw new IllegalArgumentException("Tipo de operação inválido: " + operationType);
        }
    }

    /**
     * Valida se um item pode ser mapeado
     */
    public void validateItemForMapping(InvoiceItem item) {
        if (item == null) {
            throw new IllegalArgumentException("InvoiceItem não pode ser null");
        }
        
        if (item.getAssetCode() == null || item.getAssetCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Código do ativo é obrigatório");
        }
        
        if (item.getOperationType() == null || 
            (!item.getOperationType().equals("C") && !item.getOperationType().equals("V"))) {
            throw new IllegalArgumentException("Tipo de operação deve ser 'C' ou 'V'");
        }
        
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        
        if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço unitário deve ser maior que zero");
        }
        
        if (item.getInvoice() == null) {
            throw new IllegalArgumentException("Invoice é obrigatória");
        }
        
        if (item.getInvoice().getTradingDate() == null) {
            throw new IllegalArgumentException("Data de pregão é obrigatória");
        }
    }

    /**
     * Informações extraídas do ativo
     */
    @lombok.Builder
    @lombok.Data
    private static class AssetInfo {
        private String baseCode;
        private String baseName;
        private AssetType assetType;
        private OptionType optionType;
    }

    /**
     * Mapeia múltiplos itens para requests de operação
     */
    public OperationMappingResult mapMultipleItems(java.util.List<ItemProcessingPlan> plans) {
        log.debug("🔄 Mapeando {} planos de processamento", plans.size());
        
        java.util.List<OperationDataRequest> newOperationRequests = new java.util.ArrayList<>();
        java.util.List<OperationFinalizationRequest> finalizationRequests = new java.util.ArrayList<>();
        java.util.List<String> errors = new java.util.ArrayList<>();
        
        for (ItemProcessingPlan plan : plans) {
            try {
                validateItemForMapping(plan.getInvoiceItem());
                
                if (plan.isNewOperation()) {
                    OperationDataRequest request = mapToOperationDataRequest(plan);
                    newOperationRequests.add(request);
                } else if (plan.isExitOperation()) {
                    OperationFinalizationRequest request = mapToFinalizationRequest(plan);
                    finalizationRequests.add(request);
                }
                
            } catch (Exception e) {
                String error = String.format("Erro ao mapear item %s: %s", 
                                            plan.getInvoiceItem().getAssetCode(), e.getMessage());
                errors.add(error);
                log.warn("⚠️ {}", error);
            }
        }
        
        OperationMappingResult result = OperationMappingResult.builder()
                .newOperationRequests(newOperationRequests)
                .finalizationRequests(finalizationRequests)
                .errors(errors)
                .totalPlans(plans.size())
                .successfulMappings(newOperationRequests.size() + finalizationRequests.size())
                .failedMappings(errors.size())
                .build();
        
        log.info("✅ Mapeamento múltiplo concluído: {} sucessos, {} erros de {} planos",
                 result.getSuccessfulMappings(), result.getFailedMappings(), result.getTotalPlans());
        
        return result;
    }

    /**
     * Resultado do mapeamento de múltiplos itens
     */
    @lombok.Builder
    @lombok.Data
    public static class OperationMappingResult {
        private java.util.List<OperationDataRequest> newOperationRequests;
        private java.util.List<OperationFinalizationRequest> finalizationRequests;
        private java.util.List<String> errors;
        private int totalPlans;
        private int successfulMappings;
        private int failedMappings;
        
        /**
         * Verifica se houve erros no mapeamento
         */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        /**
         * Retorna taxa de sucesso do mapeamento
         */
        public double getSuccessRate() {
            return totalPlans > 0 ? (double) successfulMappings / totalPlans * 100 : 0;
        }
        
        /**
         * Verifica se há trabalho para fazer
         */
        public boolean hasWork() {
            return !newOperationRequests.isEmpty() || !finalizationRequests.isEmpty();
        }
    }
}