package com.olisystem.optionsmanager.service.invoice.processing.integration;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.service.invoice.processing.detection.ConsolidatedOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de validação de operações
 * Valida operações antes da integração no sistema
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationValidationService {

    /**
     * Valida uma operação consolidada
     */
    public ValidationResult validateOperation(ConsolidatedOperation operation, User user) {
        log.debug("🔍 Validando operação: {} {} {} @ {}", 
            operation.getTransactionType(), operation.getQuantity(), 
            operation.getAssetCode(), operation.getUnitPrice());
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            // 1. Validações básicas
            validateBasicFields(operation, errors);
            
            // 2. Validações de negócio
            validateBusinessRules(operation, user, errors, warnings);
            
            // 3. Validações de dados
            validateDataIntegrity(operation, errors, warnings);
            
            // 4. Determinar resultado
            boolean isValid = errors.isEmpty();
            String errorMessage = errors.isEmpty() ? null : String.join("; ", errors);
            String warningMessage = warnings.isEmpty() ? null : String.join("; ", warnings);
            
            ValidationResult result = ValidationResult.builder()
                .valid(isValid)
                .errorMessage(errorMessage)
                .warningMessage(warningMessage)
                .errorCount(errors.size())
                .warningCount(warnings.size())
                .build();
            
            if (isValid) {
                log.debug("✅ Operação válida: {}", operation.getAssetCode());
            } else {
                log.warn("❌ Operação inválida: {} - {}", operation.getAssetCode(), errorMessage);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Erro durante validação da operação {}: {}", 
                operation.getAssetCode(), e.getMessage());
            
            return ValidationResult.builder()
                .valid(false)
                .errorMessage("Erro durante validação: " + e.getMessage())
                .errorCount(1)
                .build();
        }
    }

    /**
     * Valida campos básicos da operação
     */
    private void validateBasicFields(ConsolidatedOperation operation, List<String> errors) {
        // Código do ativo
        if (operation.getAssetCode() == null || operation.getAssetCode().trim().isEmpty()) {
            errors.add("Código do ativo é obrigatório");
        }
        
        // Quantidade
        if (operation.getQuantity() == null || operation.getQuantity() <= 0) {
            errors.add("Quantidade deve ser maior que zero");
        }
        
        // Preço unitário
        if (operation.getUnitPrice() == null || operation.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Preço unitário deve ser maior que zero");
        }
        
        // Tipo de transação
        if (operation.getTransactionType() == null) {
            errors.add("Tipo de transação é obrigatório");
        }
        
        // Data de negociação
        if (operation.getTradeDate() == null) {
            errors.add("Data de negociação é obrigatória");
        }
    }

    /**
     * Valida regras de negócio
     */
    private void validateBusinessRules(ConsolidatedOperation operation, User user, 
                                    List<String> errors, List<String> warnings) {
        // Verificar se o usuário pode criar operações
        if (user == null) {
            errors.add("Usuário não informado");
            return;
        }
        
        // Verificar confiança da consolidação
        if (!operation.hasHighConsolidationConfidence()) {
            warnings.add("Baixa confiança na consolidação (" + 
                String.format("%.1f%%", operation.getConsolidationConfidence() * 100) + ")");
        }
        
        // Verificar se está pronto para criação
        if (!operation.isReadyForCreation()) {
            errors.add("Operação não está pronta para criação");
        }
        
        // Verificar quantidade máxima (exemplo: 10000 lotes)
        if (operation.getQuantity() != null && operation.getQuantity() > 10000) {
            warnings.add("Quantidade muito alta (" + operation.getQuantity() + " lotes)");
        }
        
        // Verificar valor total máximo (exemplo: R$ 1.000.000)
        if (operation.getTotalValue() != null && 
            operation.getTotalValue().compareTo(BigDecimal.valueOf(1000000)) > 0) {
            warnings.add("Valor total muito alto (R$ " + operation.getTotalValue() + ")");
        }
    }

    /**
     * Valida integridade dos dados
     */
    private void validateDataIntegrity(ConsolidatedOperation operation, List<String> errors, List<String> warnings) {
        // Verificar se o valor total está correto
        if (operation.getQuantity() != null && operation.getUnitPrice() != null && 
            operation.getTotalValue() != null) {
            
            BigDecimal expectedTotal = operation.getUnitPrice()
                .multiply(BigDecimal.valueOf(operation.getQuantity()));
            
            if (operation.getTotalValue().compareTo(expectedTotal) != 0) {
                errors.add("Valor total não confere com quantidade × preço unitário");
            }
        }
        
        // Verificar se há operações fonte
        if (operation.getSourceOperations() == null || operation.getSourceOperations().isEmpty()) {
            errors.add("Operação não possui operações fonte");
        }
        
        // Verificar se o código da opção é válido
        if (operation.getOptionCode() != null && operation.getOptionCode().length() < 6) {
            warnings.add("Código da opção pode estar incompleto");
        }
    }

    /**
     * Valida se uma operação pode ser integrada
     */
    public boolean canIntegrateOperation(ConsolidatedOperation operation, User user) {
        ValidationResult validation = validateOperation(operation, user);
        return validation.isValid();
    }

    /**
     * Valida uma lista de operações e retorna estatísticas
     */
    public ValidationSummary validateOperations(List<ConsolidatedOperation> operations, User user) {
        log.info("🔍 Validando {} operações", operations.size());
        
        ValidationSummary summary = ValidationSummary.builder()
            .totalOperations(operations.size())
            .validOperations(new ArrayList<>())
            .invalidOperations(new ArrayList<>())
            .build();
        
        for (ConsolidatedOperation operation : operations) {
            ValidationResult result = validateOperation(operation, user);
            
            if (result.isValid()) {
                summary.getValidOperations().add(operation);
            } else {
                summary.getInvalidOperations().add(operation);
            }
        }
        
        summary.setSuccessRate((double) summary.getValidOperations().size() / operations.size() * 100);
        
        log.info("✅ Validação concluída: {} válidas, {} inválidas ({:.1f}% sucesso)", 
            summary.getValidOperations().size(), 
            summary.getInvalidOperations().size(),
            summary.getSuccessRate());
        
        return summary;
    }
} 