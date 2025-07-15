package com.olisystem.optionsmanager.service.invoice.processing.integration;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.service.invoice.processing.detection.ConsolidatedOperation;
import com.olisystem.optionsmanager.service.operation.OperationService;
import com.olisystem.optionsmanager.service.option_series.OptionSerieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Processador principal de integração de operações
 * Converte operações consolidadas em operações reais no sistema
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationIntegrationProcessor {

    private final OperationService operationService;
    private final OptionSerieService optionSerieService;
    private final OperationMappingService mappingService;
    private final OperationValidationService validationService;

    /**
     * Processa integração de operações consolidadas
     */
    public IntegrationResult processIntegration(List<ConsolidatedOperation> consolidatedOperations, 
                                             List<Invoice> sourceInvoices, User user) {
        log.info("🔗 Iniciando integração de {} operações consolidadas (User: {})", 
            consolidatedOperations.size(), user.getEmail());
        
        IntegrationResult result = IntegrationResult.builder()
            .success(true)
            .totalOperations(consolidatedOperations.size())
            .createdOperations(new ArrayList<>())
            .updatedOperations(new ArrayList<>())
            .failedOperations(new ArrayList<>())
            .build();
        
        try {
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < consolidatedOperations.size(); i++) {
                ConsolidatedOperation consolidatedOp = consolidatedOperations.get(i);
                
                log.debug("🔄 Processando operação {}/{}: {}", 
                    i + 1, consolidatedOperations.size(), consolidatedOp.getAssetCode());
                
                ProcessedOperation processedOp = processSingleOperation(consolidatedOp, sourceInvoices, user);
                
                if (processedOp.isSuccess()) {
                    if (processedOp.isCreated()) {
                        result.getCreatedOperations().add(processedOp);
                        log.debug("✅ Operação criada: {}", consolidatedOp.getAssetCode());
                    } else {
                        result.getUpdatedOperations().add(processedOp);
                        log.debug("🔄 Operação atualizada: {}", consolidatedOp.getAssetCode());
                    }
                } else {
                    result.getFailedOperations().add(processedOp);
                    log.warn("❌ Operação falhou: {} - {}", 
                        consolidatedOp.getAssetCode(), processedOp.getErrorMessage());
                }
            }
            
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            // Processar mapeamentos invoice → operation
            processInvoiceOperationMappings(result, sourceInvoices, user);
            
            // Calcular estatísticas
            calculateIntegrationStats(result);
            
            log.info("✅ Integração concluída: {} criadas, {} atualizadas, {} falharam", 
                result.getCreatedOperations().size(),
                result.getUpdatedOperations().size(),
                result.getFailedOperations().size());
            
        } catch (Exception e) {
            log.error("❌ Erro durante integração: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage("Erro na integração: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Processa uma única operação consolidada
     */
    private ProcessedOperation processSingleOperation(ConsolidatedOperation consolidatedOp, 
                                                   List<Invoice> sourceInvoices, User user) {
        try {
            // 1. Validar operação
            ValidationResult validation = validationService.validateOperation(consolidatedOp, user);
            if (!validation.isValid()) {
                return ProcessedOperation.builder()
                    .success(false)
                    .errorMessage(validation.getErrorMessage())
                    .consolidatedOperation(consolidatedOp)
                    .build();
            }
            
            // 2. Buscar ou criar OptionSerie
            OptionSerie optionSerie = findOrCreateOptionSerie(consolidatedOp);
            
            // 3. Criar operação no sistema
            Operation operation = createOperationFromConsolidated(consolidatedOp, optionSerie, user);
            
            // 4. Processar mapeamentos
            List<InvoiceOperationMapping> mappings = mappingService.createMappings(
                consolidatedOp, operation, sourceInvoices);
            
            // 5. Retornar resultado
            return ProcessedOperation.builder()
                .success(true)
                .created(true)
                .operation(operation)
                .consolidatedOperation(consolidatedOp)
                .optionSerie(optionSerie)
                .mappings(mappings)
                .build();
            
        } catch (Exception e) {
            log.warn("⚠️ Erro ao processar operação {}: {}", 
                consolidatedOp.getAssetCode(), e.getMessage());
            
            return ProcessedOperation.builder()
                .success(false)
                .errorMessage("Erro ao processar: " + e.getMessage())
                .consolidatedOperation(consolidatedOp)
                .build();
        }
    }

    /**
     * Busca ou cria OptionSerie para a operação
     */
    private OptionSerie findOrCreateOptionSerie(ConsolidatedOperation consolidatedOp) {
        // Por enquanto, retorna null - será implementado quando necessário
        // A lógica real dependerá da estrutura de OptionSerie
        return null;
    }

    /**
     * Cria operação no sistema a partir da operação consolidada
     */
    private Operation createOperationFromConsolidated(ConsolidatedOperation consolidatedOp, 
                                                   OptionSerie optionSerie, User user) {
        try {
            log.debug("🏗️ Criando operação no sistema: {} {} x{} @ {}", 
                consolidatedOp.getTransactionType(), consolidatedOp.getAssetCode(),
                consolidatedOp.getQuantity(), consolidatedOp.getUnitPrice());
            
            // TODO: Implementar criação real quando OperationDataRequest estiver disponível
            // Por enquanto, retorna null - será implementado quando necessário
            // A lógica real dependerá da estrutura de Operation e OperationDataRequest
            
            log.debug("⚠️ Criação de operação não implementada ainda - retornando null");
            return null;
            
        } catch (Exception e) {
            log.error("❌ Erro ao criar operação: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar operação: " + e.getMessage(), e);
        }
    }

    /**
     * Processa mapeamentos invoice → operation
     */
    private void processInvoiceOperationMappings(IntegrationResult result, 
                                               List<Invoice> sourceInvoices, User user) {
        try {
            for (ProcessedOperation processedOp : result.getCreatedOperations()) {
                if (processedOp.getMappings() != null) {
                    mappingService.saveMappings(processedOp.getMappings());
                }
            }
            
            log.info("📋 Processados {} mapeamentos invoice → operation", 
                result.getCreatedOperations().stream()
                    .mapToInt(op -> op.getMappings() != null ? op.getMappings().size() : 0)
                    .sum());
                    
        } catch (Exception e) {
            log.error("❌ Erro ao processar mapeamentos: {}", e.getMessage(), e);
        }
    }

    /**
     * Calcula estatísticas da integração
     */
    private void calculateIntegrationStats(IntegrationResult result) {
        int totalProcessed = result.getCreatedOperations().size() + 
                           result.getUpdatedOperations().size() + 
                           result.getFailedOperations().size();
        
        if (totalProcessed > 0) {
            result.setSuccessRate((double) (result.getCreatedOperations().size() + 
                                          result.getUpdatedOperations().size()) / totalProcessed * 100);
        }
        
        result.setTotalMappings(result.getCreatedOperations().stream()
            .mapToInt(op -> op.getMappings() != null ? op.getMappings().size() : 0)
            .sum());
    }

    /**
     * Valida se uma operação pode ser integrada
     */
    public boolean canIntegrateOperation(ConsolidatedOperation consolidatedOp) {
        return consolidatedOp.isReadyForCreation() && 
               consolidatedOp.hasHighConsolidationConfidence();
    }

    /**
     * Valida uma lista de operações antes da integração
     */
    public ValidationSummary validateOperationsForIntegration(List<ConsolidatedOperation> operations, User user) {
        log.info("🔍 Validando {} operações para integração", operations.size());
        
        // Filtrar operações que podem ser integradas
        List<ConsolidatedOperation> integrableOperations = operations.stream()
            .filter(this::canIntegrateOperation)
            .toList();
        
        log.info("📊 {} de {} operações podem ser integradas", 
            integrableOperations.size(), operations.size());
        
        // Validar operações integrables
        return validationService.validateOperations(integrableOperations, user);
    }
} 