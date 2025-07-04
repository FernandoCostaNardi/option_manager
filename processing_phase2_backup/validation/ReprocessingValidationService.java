package com.olisystem.optionsmanager.service.invoice.processing.validation;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceProcessingLog;
import com.olisystem.optionsmanager.model.enums.InvoiceProcessingStatus;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationSourceMapping;
import com.olisystem.optionsmanager.repository.InvoiceProcessingLogRepository;
import com.olisystem.optionsmanager.repository.OperationSourceMappingRepository;
import com.olisystem.optionsmanager.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Serviço para validação de reprocessamento de invoices
 * Verifica se é seguro reprocessar uma invoice já processada
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReprocessingValidationService {

    private final InvoiceProcessingLogRepository processingLogRepository;
    private final OperationSourceMappingRepository mappingRepository;

    /**
     * Valida se uma invoice pode ser reprocessada
     * 
     * @param invoice Invoice a ser reprocessada
     * @throws BusinessException se não for seguro reprocessar
     */
    public void validateReprocessing(Invoice invoice) {
        log.debug("🔄 Validando reprocessamento da invoice {}", invoice.getInvoiceNumber());
        
        List<String> errors = new ArrayList<>();
        
        // 1. Verificar histórico de processamento
        validateProcessingHistory(invoice, errors);
        
        // 2. Verificar operações existentes
        validateExistingOperations(invoice, errors);
        
        // 3. Verificar se não há processamento ativo
        validateNoActiveProcessing(invoice, errors);
        
        // 4. Verificar condições de segurança
        validateSafetyConditions(invoice, errors);
        
        if (!errors.isEmpty()) {
            String errorMessage = "Invoice não pode ser reprocessada:\n" + String.join("\n", errors);
            log.warn("❌ Reprocessing validation failed for invoice {}: {}", 
                     invoice.getInvoiceNumber(), errorMessage);
            throw new BusinessException(errorMessage);
        }
        
        log.info("✅ Invoice {} validada para reprocessamento", invoice.getInvoiceNumber());
    }

    /**
     * Valida histórico de processamento da invoice
     */
    private void validateProcessingHistory(Invoice invoice, List<String> errors) {
        Optional<InvoiceProcessingLog> logOpt = processingLogRepository.findByInvoice(invoice);
        
        if (logOpt.isEmpty()) {
            // Invoice nunca foi processada - ok para "primeiro processamento"
            log.debug("📝 Invoice {} nunca foi processada - primeiro processamento", 
                      invoice.getInvoiceNumber());
            return;
        }
        
        InvoiceProcessingLog log = logOpt.get();
        
        // Verificar se o último processamento não foi muito recente (< 5 minutos)
        if (log.getCompletedAt() != null) {
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            if (log.getCompletedAt().isAfter(fiveMinutesAgo)) {
                errors.add("- Último processamento foi muito recente. Aguarde 5 minutos");
            }
        }
        
        // Verificar status do último processamento
        switch (log.getStatus()) {
            case PROCESSING:
                errors.add("- Invoice está sendo processada no momento");
                break;
            case SUCCESS:
                // OK para reprocessar, mas alertar
                log.warn("⚠️ Reprocessando invoice {} que já foi processada com sucesso", 
                         invoice.getInvoiceNumber());
                break;
            case PARTIAL_SUCCESS:
                log.info("🔄 Reprocessando invoice {} com sucesso parcial anterior", 
                         invoice.getInvoiceNumber());
                break;
            case ERROR:
            case CANCELLED:
                log.info("🔄 Reprocessando invoice {} após erro/cancelamento", 
                         invoice.getInvoiceNumber());
                break;
        }
        
        // Verificar número de tentativas (máximo 5)
        long attemptCount = processingLogRepository.findByInvoice(invoice).stream().count();
        if (attemptCount >= 5) {
            errors.add("- Máximo de 5 tentativas de processamento atingido");
        }
    }

    /**
     * Valida operações existentes criadas a partir desta invoice
     */
    private void validateExistingOperations(Invoice invoice, List<String> errors) {
        List<OperationSourceMapping> existingMappings = mappingRepository.findByInvoice(invoice);
        
        if (existingMappings.isEmpty()) {
            log.debug("📝 Nenhuma operação existente para invoice {}", invoice.getInvoiceNumber());
            return;
        }
        
        log.info("🔍 Encontradas {} operações existentes para invoice {}", 
                 existingMappings.size(), invoice.getInvoiceNumber());
        
        // Verificar se há operações com posições abertas
        for (OperationSourceMapping mapping : existingMappings) {
            Operation operation = mapping.getOperation();
            
            if (operation.getStatus().name().equals("ACTIVE")) {
                log.warn("⚠️ Operação {} está ATIVA - reprocessamento pode causar inconsistências", 
                         operation.getId());
                // Não bloquear, mas alertar
            }
        }
        
        // Informar que operações existentes serão mantidas
        log.info("ℹ️ Reprocessamento manterá {} operações existentes e criará apenas novas", 
                 existingMappings.size());
    }

    /**
     * Verifica se não há processamento ativo
     */
    private void validateNoActiveProcessing(Invoice invoice, List<String> errors) {
        boolean hasActiveProcessing = processingLogRepository.hasActiveProcessing(invoice);
        
        if (hasActiveProcessing) {
            errors.add("- Invoice está sendo processada no momento por outro processo");
        }
    }

    /**
     * Valida condições de segurança para reprocessamento
     */
    private void validateSafetyConditions(Invoice invoice, List<String> errors) {
        // 1. Verificar se a invoice não foi modificada recentemente
        if (invoice.getUpdatedAt() != null) {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            if (invoice.getUpdatedAt().isAfter(oneHourAgo)) {
                log.warn("⚠️ Invoice {} foi modificada recentemente - possível inconsistência", 
                         invoice.getInvoiceNumber());
                // Não bloquear, apenas alertar
            }
        }
        
        // 2. Verificar se há itens suficientes para processar
        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            errors.add("- Invoice não possui itens para processar");
        }
        
        // 3. Verificar integridade dos dados
        validateDataIntegrity(invoice, errors);
    }

    /**
     * Valida integridade dos dados da invoice
     */
    private void validateDataIntegrity(Invoice invoice, List<String> errors) {
        // Verificar se todos os relacionamentos estão íntegros
        if (invoice.getBrokerage() == null) {
            errors.add("- Corretora da invoice não encontrada");
        }
        
        if (invoice.getUser() == null) {
            errors.add("- Usuário da invoice não encontrado");
        }
        
        // Verificar se os itens têm dados consistentes
        if (invoice.getItems() != null) {
            for (int i = 0; i < invoice.getItems().size(); i++) {
                var item = invoice.getItems().get(i);
                if (item.getInvoice() == null || !item.getInvoice().getId().equals(invoice.getId())) {
                    errors.add("- Item " + (i + 1) + " não está corretamente vinculado à invoice");
                }
            }
        }
    }

    /**
     * Retorna informações sobre processamentos anteriores
     */
    public ReprocessingInfo getReprocessingInfo(Invoice invoice) {
        Optional<InvoiceProcessingLog> logOpt = processingLogRepository.findByInvoice(invoice);
        List<OperationSourceMapping> existingMappings = mappingRepository.findByInvoice(invoice);
        
        return ReprocessingInfo.builder()
                .hasBeenProcessedBefore(logOpt.isPresent())
                .lastProcessingLog(logOpt.orElse(null))
                .existingOperationsCount(existingMappings.size())
                .existingMappings(existingMappings)
                .canBeReprocessed(true) // será false se validação falhar
                .build();
    }

    /**
     * Informações sobre reprocessamento de uma invoice
     */
    @lombok.Builder
    @lombok.Data
    public static class ReprocessingInfo {
        private boolean hasBeenProcessedBefore;
        private InvoiceProcessingLog lastProcessingLog;
        private int existingOperationsCount;
        private List<OperationSourceMapping> existingMappings;
        private boolean canBeReprocessed;
        private List<String> warnings;
        
        public boolean isFirstProcessing() {
            return !hasBeenProcessedBefore;
        }
        
        public boolean hasExistingOperations() {
            return existingOperationsCount > 0;
        }
    }
}