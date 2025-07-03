package com.olisystem.optionsmanager.service.invoice.processing;

import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 🔍 Validador de operações para processamento de invoices
 * 
 * Responsável por validar se InvoiceItems podem ser processados
 * e detectar possíveis duplicatas no sistema.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceOperationValidator {

    private final OperationRepository operationRepository;
    
    // Cache da última razão de rejeição para logs
    private String lastSkipReason = "";

    /**
     * Verifica se um InvoiceItem pode ser processado
     */
    public boolean canProcessInvoiceItem(InvoiceItem invoiceItem) {
        log.debug("🔍 Validando InvoiceItem: {} {} @ {}", 
                 invoiceItem.getOperationType(), 
                 invoiceItem.getAssetCode(), 
                 invoiceItem.getUnitPrice());
        
        // 1. Validações básicas
        if (!hasValidBasicData(invoiceItem)) {
            return false;
        }
        
        // 2. Verificar se não é uma operação de exercício
        if (isExerciseOperation(invoiceItem)) {
            lastSkipReason = "Operação de exercício (não suportada nesta versão)";
            return false;
        }
        
        // 3. Verificar duplicatas
        if (isDuplicateOperation(invoiceItem)) {
            lastSkipReason = "Operação duplicada (já existe no sistema)";
            return false;
        }
        
        // 4. Validar tipo de mercado suportado
        if (!isSupportedMarketType(invoiceItem)) {
            return false;
        }
        
        lastSkipReason = "";
        return true;
    }

    /**
     * Retorna a razão pela qual o último item foi rejeitado
     */
    public String getSkipReason(InvoiceItem invoiceItem) {
        if (lastSkipReason.isEmpty()) {
            // Se não temos razão cached, validar novamente
            canProcessInvoiceItem(invoiceItem);
        }
        return lastSkipReason;
    }

    /**
     * Valida dados básicos obrigatórios
     */
    private boolean hasValidBasicData(InvoiceItem invoiceItem) {
        if (invoiceItem.getOperationType() == null || invoiceItem.getOperationType().trim().isEmpty()) {
            lastSkipReason = "Tipo de operação (C/V) não informado";
            return false;
        }
        
        if (invoiceItem.getAssetCode() == null || invoiceItem.getAssetCode().trim().isEmpty()) {
            if (invoiceItem.getAssetSpecification() == null || invoiceItem.getAssetSpecification().trim().isEmpty()) {
                lastSkipReason = "Código do ativo não informado";
                return false;
            }
        }
        
        if (invoiceItem.getQuantity() == null || invoiceItem.getQuantity() <= 0) {
            lastSkipReason = "Quantidade inválida";
            return false;
        }
        
        if (invoiceItem.getUnitPrice() == null || invoiceItem.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            lastSkipReason = "Preço unitário inválido";
            return false;
        }
        
        if (invoiceItem.getInvoice() == null || invoiceItem.getInvoice().getTradingDate() == null) {
            lastSkipReason = "Data de negociação não informada";
            return false;
        }
        
        return true;
    }

    /**
     * Verifica se é uma operação de exercício de opção
     */
    private boolean isExerciseOperation(InvoiceItem invoiceItem) {
        String marketType = invoiceItem.getMarketType();
        String observations = invoiceItem.getObservations();
        
        if (marketType != null) {
            String upperMarketType = marketType.toUpperCase();
            if (upperMarketType.contains("EXERCICIO") || 
                upperMarketType.contains("EXERCISE") ||
                upperMarketType.contains("DESIGNACAO")) {
                return true;
            }
        }
        
        if (observations != null) {
            String upperObs = observations.toUpperCase();
            if (upperObs.contains("EXERC") || upperObs.contains("DESIGN")) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Verifica se já existe uma operação similar no sistema
     */
    private boolean isDuplicateOperation(InvoiceItem invoiceItem) {
        try {
            // Buscar operações com critérios similares
            LocalDate tradingDate = invoiceItem.getInvoice().getTradingDate();
            String assetCode = getEffectiveAssetCode(invoiceItem);
            BigDecimal unitPrice = invoiceItem.getUnitPrice();
            Integer quantity = invoiceItem.getQuantity();
            
            // Buscar operações na mesma data com mesmo ativo
            List<Operation> existingOperations = operationRepository
                .findByEntryDateAndOptionSeriesCodeContaining(tradingDate, assetCode);
            
            for (Operation existingOp : existingOperations) {
                if (isSimilarOperation(existingOp, invoiceItem)) {
                    log.debug("🔍 Operação similar encontrada: {} (ID: {})", 
                             existingOp.getOptionSeries().getCode(), existingOp.getId());
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.warn("⚠️ Erro ao verificar duplicatas para item {}: {}", invoiceItem.getId(), e.getMessage());
            // Em caso de erro, permite processamento para não bloquear
            return false;
        }
    }

    /**
     * Verifica se duas operações são similares (possível duplicata)
     */
    private boolean isSimilarOperation(Operation existingOp, InvoiceItem invoiceItem) {
        // Mesma data
        if (!existingOp.getEntryDate().equals(invoiceItem.getInvoice().getTradingDate())) {
            return false;
        }
        
        // Mesmo tipo de transação
        String invoiceOpType = invoiceItem.getOperationType().toUpperCase();
        boolean isBuyOperation = "C".equals(invoiceOpType);
        boolean existingIsBuy = existingOp.getTransactionType().name().equals("BUY");
        
        if (isBuyOperation != existingIsBuy) {
            return false;
        }
        
        // Mesma quantidade
        if (!existingOp.getQuantity().equals(invoiceItem.getQuantity())) {
            return false;
        }
        
        // Preço similar (tolerância de 0.01)
        BigDecimal priceDiff = existingOp.getEntryUnitPrice().subtract(invoiceItem.getUnitPrice()).abs();
        if (priceDiff.compareTo(new BigDecimal("0.01")) > 0) {
            return false;
        }
        
        // Mesmo usuário (se disponível)
        if (existingOp.getUser() != null && invoiceItem.getInvoice().getUser() != null) {
            if (!existingOp.getUser().getId().equals(invoiceItem.getInvoice().getUser().getId())) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Verifica se o tipo de mercado é suportado
     */
    private boolean isSupportedMarketType(InvoiceItem invoiceItem) {
        String marketType = invoiceItem.getMarketType();
        
        if (marketType == null) {
            lastSkipReason = "Tipo de mercado não informado";
            return false;
        }
        
        String upperMarketType = marketType.toUpperCase();
        
        // Tipos suportados
        if (upperMarketType.contains("VISTA") ||
            upperMarketType.contains("OPCAO") ||
            upperMarketType.contains("OPTION") ||
            upperMarketType.contains("FII") ||
            upperMarketType.contains("ETF")) {
            return true;
        }
        
        // Tipos não suportados por enquanto
        if (upperMarketType.contains("TERMO") ||
            upperMarketType.contains("FUTURO") ||
            upperMarketType.contains("SWAP") ||
            upperMarketType.contains("MERCADORIA")) {
            lastSkipReason = "Tipo de mercado não suportado: " + marketType;
            return false;
        }
        
        // Default: permitir (com log de aviso)
        log.warn("⚠️ Tipo de mercado desconhecido, processando mesmo assim: {}", marketType);
        return true;
    }

    /**
     * Obtém o código efetivo do ativo para comparações
     */
    private String getEffectiveAssetCode(InvoiceItem invoiceItem) {
        String assetCode = invoiceItem.getAssetCode();
        
        if (assetCode != null && !assetCode.trim().isEmpty()) {
            return assetCode.trim().toUpperCase();
        }
        
        // Fallback para especificação
        String specification = invoiceItem.getAssetSpecification();
        if (specification != null && !specification.trim().isEmpty()) {
            return specification.trim().toUpperCase();
        }
        
        return "";
    }

    /**
     * Valida se o InvoiceItem tem dados suficientes para mapeamento
     */
    public boolean hasValidMappingData(InvoiceItem invoiceItem) {
        return hasValidBasicData(invoiceItem) && 
               invoiceItem.getInvoice().getBrokerage() != null &&
               invoiceItem.getInvoice().getBrokerage().getId() != null;
    }

    /**
     * Conta operações similares que seriam duplicatas
     */
    public long countPotentialDuplicates(List<InvoiceItem> items) {
        return items.stream()
                   .filter(this::isDuplicateOperation)
                   .count();
    }

    /**
     * Valida lista de items e retorna estatísticas
     */
    public ValidationStatistics validateItemsList(List<InvoiceItem> items) {
        int valid = 0;
        int invalid = 0;
        int duplicates = 0;
        int exercises = 0;
        int unsupportedMarkets = 0;
        
        for (InvoiceItem item : items) {
            if (isExerciseOperation(item)) {
                exercises++;
                continue;
            }
            
            if (isDuplicateOperation(item)) {
                duplicates++;
                continue;
            }
            
            if (!isSupportedMarketType(item)) {
                unsupportedMarkets++;
                continue;
            }
            
            if (hasValidBasicData(item)) {
                valid++;
            } else {
                invalid++;
            }
        }
        
        return ValidationStatistics.builder()
            .totalItems(items.size())
            .validItems(valid)
            .invalidItems(invalid)
            .duplicateItems(duplicates)
            .exerciseItems(exercises)
            .unsupportedMarketItems(unsupportedMarkets)
            .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class ValidationStatistics {
        private int totalItems;
        private int validItems;
        private int invalidItems;
        private int duplicateItems;
        private int exerciseItems;
        private int unsupportedMarketItems;
        
        public int getProcessableItems() {
            return validItems;
        }
        
        public int getSkippedItems() {
            return totalItems - validItems;
        }
    }
}