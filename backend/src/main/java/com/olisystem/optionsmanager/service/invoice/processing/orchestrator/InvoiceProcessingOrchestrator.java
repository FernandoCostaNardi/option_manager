package com.olisystem.optionsmanager.service.invoice.processing.orchestrator;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.service.invoice.processing.detection.DetectionResult;
import com.olisystem.optionsmanager.service.invoice.processing.detection.OperationDetectionEngine;
import com.olisystem.optionsmanager.service.invoice.processing.integration.IntegrationResult;
import com.olisystem.optionsmanager.service.invoice.processing.integration.OperationIntegrationProcessor;
import com.olisystem.optionsmanager.service.invoice.processing.integration.ValidationSummary;
import com.olisystem.optionsmanager.service.invoice.processing.validation.InvoiceValidationService;
import com.olisystem.optionsmanager.service.invoice.processing.validation.DuplicateDetectionService;
import com.olisystem.optionsmanager.service.invoice.processing.validation.ReprocessingValidationService;
import com.olisystem.optionsmanager.service.invoice.processing.validation.InvoiceValidationService.InvoiceValidationResult;
import com.olisystem.optionsmanager.service.invoice.processing.validation.DuplicateDetectionService.DuplicateDetectionResult;
import com.olisystem.optionsmanager.service.invoice.processing.validation.ReprocessingValidationService.ReprocessingValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Orquestrador principal do processamento de invoices
 * Coordena todas as etapas: validação → detecção → integração
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceProcessingOrchestrator {

    // === SERVIÇOS DE VALIDAÇÃO ===
    private final InvoiceValidationService invoiceValidationService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final ReprocessingValidationService reprocessingValidationService;
    
    // === SERVIÇOS DE DETECÇÃO ===
    private final OperationDetectionEngine detectionEngine;
    
    // === SERVIÇOS DE INTEGRAÇÃO ===
    private final OperationIntegrationProcessor integrationProcessor;

    /**
     * Processa uma lista de invoices de forma completa
     */
    public OrchestrationResult processInvoices(List<UUID> invoiceIds, User user, 
                                             Consumer<OrchestrationProgress> progressCallback) {
        log.info("🚀 Iniciando orquestração para {} invoices", invoiceIds.size());
        
        OrchestrationResult result = OrchestrationResult.builder()
            .totalInvoices(invoiceIds.size())
            .build();
        
        try {
            // 1. VALIDAÇÃO DE INVOICES
            progressCallback.accept(createProgress(10, "Validando invoices..."));
            ValidationOrchestrationResult validationResult = validateInvoices(invoiceIds, user);
            result.setValidationResult(validationResult);
            
            if (!validationResult.isCanProceed()) {
                log.warn("❌ Validação falhou: {}", validationResult.getRejectionReason());
                result.setSuccess(false);
                result.setErrorMessage("Falha na validação: " + validationResult.getRejectionReason());
                return result;
            }
            
            // 2. BUSCAR INVOICES VÁLIDAS
            progressCallback.accept(createProgress(20, "Buscando invoices válidas..."));
            List<Invoice> validInvoices = fetchValidInvoices(validationResult.getValidInvoiceIds());
            result.setProcessedInvoices(validInvoices);
            
            // 3. DETECÇÃO DE OPERAÇÕES
            progressCallback.accept(createProgress(40, "Detectando operações..."));
            DetectionResult detectionResult = detectionEngine.detectOperations(validInvoices, user);
            result.setDetectionResult(detectionResult);
            
            if (!detectionResult.isSuccess()) {
                log.warn("❌ Detecção falhou: {}", detectionResult.getErrorMessage());
                result.setSuccess(false);
                result.setErrorMessage("Falha na detecção: " + detectionResult.getErrorMessage());
                return result;
            }
            
            // 4. VALIDAÇÃO PARA INTEGRAÇÃO
            progressCallback.accept(createProgress(60, "Validando operações para integração..."));
            ValidationSummary integrationValidation = integrationProcessor.validateOperationsForIntegration(
                detectionResult.getConsolidatedOperations(), user);
            result.setIntegrationValidation(integrationValidation);
            
            if (integrationValidation.getValidCount() == 0) {
                log.warn("❌ Nenhuma operação válida para integração");
                result.setSuccess(false);
                result.setErrorMessage("Nenhuma operação válida para integração");
                return result;
            }
            
            // 5. INTEGRAÇÃO DE OPERAÇÕES
            progressCallback.accept(createProgress(80, "Integrando operações..."));
            IntegrationResult integrationResult = integrationProcessor.processIntegration(
                detectionResult.getConsolidatedOperations(), validInvoices, user);
            result.setIntegrationResult(integrationResult);
            
            // 6. FINALIZAÇÃO
            progressCallback.accept(createProgress(100, "Processamento concluído!"));
            calculateFinalStatistics(result);
            
            log.info("✅ Orquestração concluída: {} operações criadas, {} erros", 
                integrationResult.getTotalSuccessfulOperations(), 
                integrationResult.getTotalFailedOperations());
            
        } catch (Exception e) {
            log.error("❌ Erro durante orquestração: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage("Erro na orquestração: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Valida uma lista de invoices
     */
    private ValidationOrchestrationResult validateInvoices(List<UUID> invoiceIds, User user) {
        log.info("🔍 Validando {} invoices", invoiceIds.size());
        
        ValidationOrchestrationResult result = ValidationOrchestrationResult.builder()
            .totalInvoices(invoiceIds.size())
            .build();
        
        try {
            // 1. Validação básica de invoices
            for (UUID invoiceId : invoiceIds) {
                try {
                    // TODO: Buscar invoice real quando repository estiver disponível
                    Invoice mockInvoice = createMockInvoice(invoiceId);
                    
                    InvoiceValidationResult validation = invoiceValidationService.validateInvoice(mockInvoice);
                    if (validation.isValid()) {
                        result.getValidInvoiceIds().add(invoiceId);
                    } else {
                        result.getInvalidInvoiceIds().add(invoiceId);
                        result.addValidationError(String.format("Invoice %s: %s", 
                        invoiceId, validation.getErrors().isEmpty() ? "Erro de validação" : validation.getErrors().get(0)));
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Erro ao validar invoice {}: {}", invoiceId, e.getMessage());
                    result.getInvalidInvoiceIds().add(invoiceId);
                    result.addValidationError(String.format("Invoice %s: Erro interno", invoiceId));
                }
            }
            
            // 2. Detecção de duplicatas
            if (!result.getValidInvoiceIds().isEmpty()) {
                // TODO: Buscar invoices válidas quando repository estiver disponível
                List<Invoice> validInvoices = result.getValidInvoiceIds().stream()
                    .map(this::createMockInvoice)
                    .toList();
                
                DuplicateDetectionResult duplicateResult = duplicateDetectionService.detectDuplicates(validInvoices);
                if (duplicateResult.hasDuplicates()) {
                    result.setHasDuplicates(true);
                    result.addValidationError("Duplicatas detectadas: " + duplicateResult.getSummary());
                }
            }
            
            // 3. Validação de reprocessamento
            for (UUID invoiceId : result.getValidInvoiceIds()) {
                try {
                    Invoice mockInvoice = createMockInvoice(invoiceId);
                    ReprocessingValidationResult reprocessingResult = reprocessingValidationService.validateReprocessing(mockInvoice, user);
                    
                    if (!reprocessingResult.isCanReprocess()) {
                        result.getInvalidInvoiceIds().add(invoiceId);
                        result.getValidInvoiceIds().remove(invoiceId);
                        result.addValidationError(String.format("Invoice %s: %s", 
                            invoiceId, reprocessingResult.getRejectionReason()));
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Erro ao validar reprocessamento da invoice {}: {}", invoiceId, e.getMessage());
                }
            }
            
            // 4. Calcular estatísticas
            result.setValidCount(result.getValidInvoiceIds().size());
            result.setInvalidCount(result.getInvalidInvoiceIds().size());
            result.setCanProceed(result.getValidCount() > 0 && !result.isHasDuplicates());
            
            log.info("✅ Validação concluída: {} válidas, {} inválidas, duplicatas: {}", 
                result.getValidCount(), result.getInvalidCount(), result.isHasDuplicates());
            
        } catch (Exception e) {
            log.error("❌ Erro durante validação: {}", e.getMessage(), e);
            result.setCanProceed(false);
            result.setRejectionReason("Erro interno na validação: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Busca invoices válidas
     */
    private List<Invoice> fetchValidInvoices(List<UUID> validInvoiceIds) {
        log.info("📋 Buscando {} invoices válidas", validInvoiceIds.size());
        
        // TODO: Implementar busca real quando repository estiver disponível
        return validInvoiceIds.stream()
            .map(this::createMockInvoice)
            .toList();
    }

    /**
     * Calcula estatísticas finais
     */
    private void calculateFinalStatistics(OrchestrationResult result) {
        // Estatísticas de validação
        if (result.getValidationResult() != null) {
            result.setValidInvoicesCount(result.getValidationResult().getValidCount());
            result.setInvalidInvoicesCount(result.getValidationResult().getInvalidCount());
        }
        
        // Estatísticas de detecção
        if (result.getDetectionResult() != null) {
            result.setDetectedOperationsCount(result.getDetectionResult().getDetectedOperations().size());
            result.setConsolidatedOperationsCount(result.getDetectionResult().getConsolidatedOperations().size());
        }
        
        // Estatísticas de integração
        if (result.getIntegrationResult() != null) {
            result.setCreatedOperationsCount(result.getIntegrationResult().getTotalSuccessfulOperations());
            result.setFailedOperationsCount(result.getIntegrationResult().getTotalFailedOperations());
        }
        
        // Taxa de sucesso geral
        if (result.getTotalInvoices() > 0) {
            result.setOverallSuccessRate((double) result.getCreatedOperationsCount() / result.getTotalInvoices() * 100);
        }
    }

    /**
     * Cria objeto de progresso
     */
    private OrchestrationProgress createProgress(int percentage, String message) {
        return OrchestrationProgress.builder()
            .percentage(percentage)
            .message(message)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * Cria invoice mock para testes
     */
    private Invoice createMockInvoice(UUID invoiceId) {
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInvoiceNumber("NOTA_" + invoiceId.toString().substring(0, 8));
        invoice.setTradingDate(java.time.LocalDate.now());
        
        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("usuario@teste.com");
        invoice.setUser(mockUser);
        
        return invoice;
    }

    /**
     * Processa uma única invoice
     */
    public OrchestrationResult processSingleInvoice(UUID invoiceId, User user) {
        log.info("🎼 Processando invoice individual: {}", invoiceId);
        
        return processInvoices(List.of(invoiceId), user, progress -> {
            log.debug("📊 Progresso: {}% - {}", progress.getPercentage(), progress.getMessage());
        });
    }
} 