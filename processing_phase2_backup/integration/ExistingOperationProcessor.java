package com.olisystem.optionsmanager.service.invoice.processing.integration;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.OperationSourceMapping;
import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.service.operation.OperationServiceImpl;
import com.olisystem.optionsmanager.service.invoice.processing.detection.OperationMatchingService.ItemProcessingPlan;
import com.olisystem.optionsmanager.repository.OperationSourceMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Serviço para processar itens que finalizam operações existentes
 * Integra com OperationService existente para finalizar operações
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExistingOperationProcessor {

    private final OperationServiceImpl operationService;
    private final InvoiceToOperationMapper operationMapper;
    private final OperationSourceMappingRepository mappingRepository;

    /**
     * Processa finalização de uma operação existente
     * 
     * @param plan Plano de processamento do item
     * @return Operação finalizada
     */
    @Transactional
    public Operation processExistingOperationExit(ItemProcessingPlan plan) {
        log.debug("🎯 Processando finalização de operação existente: {}", 
                 plan.getInvoiceItem().getAssetCode());
        
        validatePlan(plan);
        
        // Mapear item para request de finalização
        OperationFinalizationRequest finalizationRequest = operationMapper.mapToFinalizationRequest(plan);
        
        // Usar serviço existente para finalizar operação
        Operation finalizedOperation = operationService.createExitOperation(finalizationRequest);
        
        // Criar mapeamento para rastreabilidade
        createSourceMapping(finalizedOperation, plan);
        
        log.info("✅ Operação {} finalizada com sucesso usando item da invoice {}",
                 finalizedOperation.getId().toString().substring(0, 8),
                 plan.getInvoiceItem().getInvoice().getInvoiceNumber());
        
        return finalizedOperation;
    }

    /**
     * Processa múltiplas finalizações de operações
     * 
     * @param plans Lista de planos de finalização
     * @return Resultado do processamento em lote
     */
    @Transactional
    public ExistingOperationProcessingResult processMultipleExits(List<ItemProcessingPlan> plans) {
        log.debug("🔄 Processando {} finalizações de operações existentes", plans.size());
        
        List<Operation> finalizedOperations = new ArrayList<>();
        List<ProcessingError> errors = new ArrayList<>();
        
        for (ItemProcessingPlan plan : plans) {
            try {
                Operation operation = processExistingOperationExit(plan);
                finalizedOperations.add(operation);
                
            } catch (Exception e) {
                ProcessingError error = ProcessingError.builder()
                        .plan(plan)
                        .errorMessage(e.getMessage())
                        .exception(e)
                        .build();
                
                errors.add(error);
                
                log.error("❌ Erro ao finalizar operação para item {}: {}", 
                         plan.getInvoiceItem().getAssetCode(), e.getMessage(), e);
            }
        }
        
        ExistingOperationProcessingResult result = ExistingOperationProcessingResult.builder()
                .finalizedOperations(finalizedOperations)
                .errors(errors)
                .totalPlans(plans.size())
                .successfulProcessing(finalizedOperations.size())
                .failedProcessing(errors.size())
                .build();
        
        log.info("📊 Processamento de finalizações concluído: {} sucessos, {} erros de {} planos",
                 result.getSuccessfulProcessing(), result.getFailedProcessing(), result.getTotalPlans());
        
        return result;
    }

    /**
     * Valida plano de processamento para finalização
     */
    private void validatePlan(ItemProcessingPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("Plano de processamento não pode ser null");
        }
        
        if (plan.getTargetOperation() == null) {
            throw new IllegalArgumentException("Operação alvo é obrigatória para finalização");
        }
        
        if (plan.getInvoiceItem() == null) {
            throw new IllegalArgumentException("Item da invoice é obrigatório");
        }
        
        // Verificar se operação ainda está ativa
        Operation targetOperation = plan.getTargetOperation();
        if (!"ACTIVE".equals(targetOperation.getStatus().name())) {
            throw new IllegalArgumentException(
                String.format("Operação %s não está ativa (status: %s)", 
                            targetOperation.getId(), targetOperation.getStatus()));
        }
        
        // Verificar se item é de venda (normalmente para finalizar operações de compra)
        InvoiceItem item = plan.getInvoiceItem();
        if (!"V".equals(item.getOperationType())) {
            log.warn("⚠️ Item de compra sendo usado para finalizar operação - verificar lógica");
        }
        
        // Verificar quantidade disponível
        if (item.getQuantity() > targetOperation.getQuantity()) {
            log.warn("⚠️ Quantidade do item ({}) maior que quantidade da operação ({})", 
                     item.getQuantity(), targetOperation.getQuantity());
        }
    }

    /**
     * Cria mapeamento de origem para rastreabilidade
     */
    private void createSourceMapping(Operation operation, ItemProcessingPlan plan) {
        try {
            OperationSourceMapping mapping = OperationSourceMapping.forExistingOperationExit(
                    operation, 
                    plan.getInvoiceItem(), 
                    getNextSequenceNumber(plan.getInvoiceItem().getInvoice())
            );
            
            mappingRepository.save(mapping);
            
            log.debug("📝 Mapeamento de origem criado: operação {} ← item {}", 
                     operation.getId().toString().substring(0, 8),
                     plan.getInvoiceItem().getAssetCode());
            
        } catch (Exception e) {
            log.error("❌ Erro ao criar mapeamento de origem: {}", e.getMessage(), e);
            // Não propagar erro - mapeamento é para auditoria
        }
    }

    /**
     * Obtém próximo número de sequência para a invoice
     */
    private Integer getNextSequenceNumber(com.olisystem.optionsmanager.model.invoice.Invoice invoice) {
        List<OperationSourceMapping> existingMappings = mappingRepository.findByInvoice(invoice);
        
        return existingMappings.stream()
                .filter(mapping -> mapping.getProcessingSequence() != null)
                .mapToInt(OperationSourceMapping::getProcessingSequence)
                .max()
                .orElse(0) + 1;
    }

    /**
     * Verifica se uma operação pode ser finalizada com um item
     */
    public boolean canFinalizeOperation(Operation operation, InvoiceItem item) {
        try {
            // Verificar status da operação
            if (!"ACTIVE".equals(operation.getStatus().name())) {
                return false;
            }
            
            // Verificar se é item de venda
            if (!"V".equals(item.getOperationType())) {
                return false;
            }
            
            // Verificar se o ativo corresponde
            String operationAssetCode = extractBaseAssetCode(operation.getOptionSeries().getCode());
            String itemAssetCode = extractBaseAssetCode(item.getAssetCode());
            
            if (!operationAssetCode.equals(itemAssetCode)) {
                return false;
            }
            
            // Verificar se quantidade é válida
            if (item.getQuantity() <= 0 || item.getQuantity() > operation.getQuantity()) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.warn("⚠️ Erro ao verificar se operação pode ser finalizada: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrai código base do ativo
     */
    private String extractBaseAssetCode(String fullAssetCode) {
        if (fullAssetCode == null) return "";
        
        String cleaned = fullAssetCode.trim().toUpperCase();
        
        // Para opções
        if (cleaned.matches("^[A-Z]{4,5}[FE]\\d+$")) {
            return cleaned.substring(0, cleaned.length() - 4);
        }
        
        // Para códigos com sufixos
        if (cleaned.contains(" ON") || cleaned.contains(" PN")) {
            return cleaned.split(" ")[0];
        }
        
        return cleaned;
    }

    /**
     * Busca operações que podem ser finalizadas por um item
     */
    public List<Operation> findFinalizableOperations(InvoiceItem item) {
        // Esta funcionalidade seria implementada se necessário
        // Por enquanto, assumimos que a detecção é feita pelo ActiveOperationDetector
        return new ArrayList<>();
    }

    /**
     * Erro de processamento
     */
    @lombok.Builder
    @lombok.Data
    public static class ProcessingError {
        private ItemProcessingPlan plan;
        private String errorMessage;
        private Exception exception;
        
        public String getAssetCode() {
            return plan != null && plan.getInvoiceItem() != null ? 
                   plan.getInvoiceItem().getAssetCode() : "UNKNOWN";
        }
        
        public String getInvoiceNumber() {
            return plan != null && plan.getInvoiceItem() != null && 
                   plan.getInvoiceItem().getInvoice() != null ?
                   plan.getInvoiceItem().getInvoice().getInvoiceNumber() : "UNKNOWN";
        }
    }

    /**
     * Resultado do processamento de operações existentes
     */
    @lombok.Builder
    @lombok.Data
    public static class ExistingOperationProcessingResult {
        private List<Operation> finalizedOperations;
        private List<ProcessingError> errors;
        private int totalPlans;
        private int successfulProcessing;
        private int failedProcessing;
        
        /**
         * Verifica se houve erros
         */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        /**
         * Taxa de sucesso do processamento
         */
        public double getSuccessRate() {
            return totalPlans > 0 ? (double) successfulProcessing / totalPlans * 100 : 0;
        }
        
        /**
         * Verifica se todo o processamento foi bem-sucedido
         */
        public boolean isFullySuccessful() {
            return failedProcessing == 0 && successfulProcessing > 0;
        }
        
        /**
         * Retorna IDs das operações finalizadas
         */
        public List<java.util.UUID> getFinalizedOperationIds() {
            return finalizedOperations.stream()
                    .map(Operation::getId)
                    .collect(java.util.stream.Collectors.toList());
        }
    }
}