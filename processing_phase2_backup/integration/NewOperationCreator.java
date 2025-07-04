package com.olisystem.optionsmanager.service.invoice.processing.integration;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.OperationSourceMapping;
import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.service.operation.OperationServiceImpl;
import com.olisystem.optionsmanager.service.invoice.processing.detection.OperationMatchingService.ItemProcessingPlan;
import com.olisystem.optionsmanager.repository.OperationSourceMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Serviço para processar criação de novas operações
 * Integra com OperationService existente para criar operações
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewOperationCreator {

    private final OperationServiceImpl operationService;
    private final InvoiceToOperationMapper operationMapper;
    private final OperationSourceMappingRepository mappingRepository;

    /**
     * Cria nova operação a partir de um item da invoice
     * 
     * @param plan Plano de processamento do item
     * @return Nova operação criada
     */
    @Transactional
    public Operation createNewOperation(ItemProcessingPlan plan) {
        log.debug("🆕 Criando nova operação: {}", plan.getInvoiceItem().getAssetCode());
        
        validatePlan(plan);
        
        // Mapear item para request de criação
        OperationDataRequest operationRequest = operationMapper.mapToOperationDataRequest(plan);
        
        // Usar serviço existente para criar operação
        Operation newOperation = operationService.createOperation(operationRequest);
        
        // Criar mapeamento para rastreabilidade
        createSourceMapping(newOperation, plan);
        
        log.info("✅ Nova operação {} criada com sucesso para item da invoice {}",
                 newOperation.getId().toString().substring(0, 8),
                 plan.getInvoiceItem().getInvoice().getInvoiceNumber());
        
        return newOperation;
    }

    /**
     * Cria múltiplas operações em lote
     * 
     * @param plans Lista de planos para criação
     * @return Resultado do processamento em lote
     */
    @Transactional
    public NewOperationCreationResult createMultipleOperations(List<ItemProcessingPlan> plans) {
        log.debug("🔄 Criando {} novas operações", plans.size());
        
        List<Operation> createdOperations = new ArrayList<>();
        List<CreationError> errors = new ArrayList<>();
        
        // Processar cada plano individualmente
        for (ItemProcessingPlan plan : plans) {
            try {
                Operation operation = createNewOperation(plan);
                createdOperations.add(operation);
                
            } catch (Exception e) {
                CreationError error = CreationError.builder()
                        .plan(plan)
                        .errorMessage(e.getMessage())
                        .exception(e)
                        .build();
                
                errors.add(error);
                
                log.error("❌ Erro ao criar operação para item {}: {}", 
                         plan.getInvoiceItem().getAssetCode(), e.getMessage(), e);
            }
        }
        
        NewOperationCreationResult result = NewOperationCreationResult.builder()
                .createdOperations(createdOperations)
                .errors(errors)
                .totalPlans(plans.size())
                .successfulCreations(createdOperations.size())
                .failedCreations(errors.size())
                .build();
        
        log.info("📊 Criação de operações concluída: {} sucessos, {} erros de {} planos",
                 result.getSuccessfulCreations(), result.getFailedCreations(), result.getTotalPlans());
        
        return result;
    }

    /**
     * Cria operação em modo Day Trade (preparação para finalização imediata)
     * 
     * @param plan Plano de entrada Day Trade
     * @return Operação criada pronta para finalização
     */
    @Transactional
    public Operation createDayTradeEntry(ItemProcessingPlan plan) {
        log.debug("🎯 Criando entrada Day Trade: {}", plan.getInvoiceItem().getAssetCode());
        
        validateDayTradePlan(plan);
        
        // Criar operação normalmente
        Operation operation = createNewOperation(plan);
        
        // Marcar como Day Trade na auditoria
        log.info("🔄 Operação Day Trade {} criada - aguardando finalização",
                 operation.getId().toString().substring(0, 8));
        
        return operation;
    }

    /**
     * Valida plano de processamento para criação
     */
    private void validatePlan(ItemProcessingPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("Plano de processamento não pode ser null");
        }
        
        if (plan.getInvoiceItem() == null) {
            throw new IllegalArgumentException("Item da invoice é obrigatório");
        }
        
        if (!plan.isNewOperation()) {
            throw new IllegalArgumentException("Plano deve ser para nova operação");
        }
        
        InvoiceItem item = plan.getInvoiceItem();
        
        // Validar dados básicos do item
        operationMapper.validateItemForMapping(item);
        
        // Verificar se é operação de entrada (normalmente compra)
        if (!"C".equals(item.getOperationType())) {
            log.warn("⚠️ Criando operação para item de venda - verificar lógica de negócio");
        }
    }

    /**
     * Valida plano específico para Day Trade
     */
    private void validateDayTradePlan(ItemProcessingPlan plan) {
        validatePlan(plan);
        
        if (plan.getTradeType() != com.olisystem.optionsmanager.model.operation.TradeType.DAY) {
            throw new IllegalArgumentException("Plano deve ser do tipo DAY para Day Trade");
        }
        
        if (!plan.getMappingType().isDayTrade()) {
            throw new IllegalArgumentException("Tipo de mapeamento deve ser Day Trade");
        }
    }

    /**
     * Cria mapeamento de origem para rastreabilidade
     */
    private void createSourceMapping(Operation operation, ItemProcessingPlan plan) {
        try {
            OperationSourceMapping mapping = OperationSourceMapping.forNewOperation(
                    operation,
                    plan.getInvoiceItem(),
                    getNextSequenceNumber(plan.getInvoiceItem().getInvoice())
            );
            
            // Ajustar tipo de mapeamento baseado no plano
            mapping.setMappingType(plan.getMappingType());
            
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
     * Verifica se um item pode gerar nova operação
     */
    public boolean canCreateOperation(InvoiceItem item) {
        try {
            operationMapper.validateItemForMapping(item);
            return true;
        } catch (Exception e) {
            log.debug("❌ Item {} não pode gerar operação: {}", 
                     item.getAssetCode(), e.getMessage());
            return false;
        }
    }

    /**
     * Estima recursos necessários para criação de operações
     */
    public CreationEstimate estimateCreation(List<ItemProcessingPlan> plans) {
        int newOperations = 0;
        int dayTradeEntries = 0;
        int swingTradeEntries = 0;
        int estimatedComplexity = 0;
        
        for (ItemProcessingPlan plan : plans) {
            if (plan.isNewOperation()) {
                newOperations++;
                
                if (plan.getMappingType().isDayTrade()) {
                    dayTradeEntries++;
                    estimatedComplexity += 15; // Day Trade é mais complexo
                } else {
                    swingTradeEntries++;
                    estimatedComplexity += 10; // Swing Trade é padrão
                }
            }
        }
        
        return CreationEstimate.builder()
                .totalNewOperations(newOperations)
                .dayTradeEntries(dayTradeEntries)
                .swingTradeEntries(swingTradeEntries)
                .estimatedComplexity(estimatedComplexity)
                .estimatedDurationMs(estimatedComplexity * 50) // 50ms por ponto de complexidade
                .build();
    }

    /**
     * Erro de criação de operação
     */
    @lombok.Builder
    @lombok.Data
    public static class CreationError {
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
     * Resultado da criação de novas operações
     */
    @lombok.Builder
    @lombok.Data
    public static class NewOperationCreationResult {
        private List<Operation> createdOperations;
        private List<CreationError> errors;
        private int totalPlans;
        private int successfulCreations;
        private int failedCreations;
        
        /**
         * Verifica se houve erros
         */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        /**
         * Taxa de sucesso da criação
         */
        public double getSuccessRate() {
            return totalPlans > 0 ? (double) successfulCreations / totalPlans * 100 : 0;
        }
        
        /**
         * Verifica se toda a criação foi bem-sucedida
         */
        public boolean isFullySuccessful() {
            return failedCreations == 0 && successfulCreations > 0;
        }
        
        /**
         * Retorna IDs das operações criadas
         */
        public List<java.util.UUID> getCreatedOperationIds() {
            return createdOperations.stream()
                    .map(Operation::getId)
                    .collect(java.util.stream.Collectors.toList());
        }
        
        /**
         * Filtra operações criadas por tipo de trade
         */
        public List<Operation> getOperationsByTradeType(com.olisystem.optionsmanager.model.operation.TradeType tradeType) {
            return createdOperations.stream()
                    .filter(op -> op.getTradeType() == tradeType)
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    /**
     * Estimativa de criação de operações
     */
    @lombok.Builder
    @lombok.Data
    public static class CreationEstimate {
        private int totalNewOperations;
        private int dayTradeEntries;
        private int swingTradeEntries;
        private int estimatedComplexity;
        private long estimatedDurationMs;
        
        /**
         * Verifica se há trabalho significativo
         */
        public boolean hasSignificantWork() {
            return totalNewOperations > 5 || estimatedComplexity > 100;
        }
        
        /**
         * Retorna duração estimada em segundos
         */
        public double getEstimatedDurationSeconds() {
            return estimatedDurationMs / 1000.0;
        }
    }
}