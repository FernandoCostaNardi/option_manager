package com.olisystem.optionsmanager.service.invoice.processing;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.service.operation.consolidate.ConsolidatedOperationService;
import com.olisystem.optionsmanager.service.operation.OperationService;
import com.olisystem.optionsmanager.service.operation.creation.OperationCreationService;
import com.olisystem.optionsmanager.service.invoice.processing.InvoiceToOperationMapper;
import com.olisystem.optionsmanager.repository.InvoiceRepository;
import com.olisystem.optionsmanager.repository.InvoiceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Processador de consolidação de invoices que integra com o sistema de consolidação existente
 * ✅ CORREÇÃO: Adicionado @Transactional para resolver problema de lazy loading
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceConsolidationProcessor {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceToOperationMapper mapper;
    private final OperationService operationService;
    private final OperationCreationService operationCreationService;
    private final ConsolidatedOperationService consolidatedOperationService;

    /**
     * Processa invoices com sistema de consolidação
     * ✅ CORREÇÃO: Adicionado @Transactional para resolver problema de lazy loading
     * ✅ NOVO: Tratamento específico para operações de opções
     */
    @Transactional(readOnly = false)
    public ConsolidationResult processInvoicesWithConsolidation(List<UUID> invoiceIds, User currentUser) {
        log.info("🔄 Processando {} invoices com sistema de consolidação", invoiceIds.size());
        log.info("👤 Usuário: {}", currentUser.getEmail());
        
        ConsolidationResult result = ConsolidationResult.builder()
            .success(true)
            .totalInvoices(invoiceIds.size())
            .consolidatedOperationsCount(0)
            .build();
        
        try {
            for (UUID invoiceId : invoiceIds) {
                log.info("📋 Processando invoice: {}", invoiceId);
                
                Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice não encontrada: " + invoiceId));
                
                log.info("📄 Invoice encontrada: {} - {}", invoice.getInvoiceNumber(), invoice.getTradingDate());
                
                List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithAllRelations(invoiceId);
                log.info("📦 Invoice {} tem {} items", invoiceId, items.size());
                
                if (items.isEmpty()) {
                    log.warn("⚠️ Invoice {} não tem items para processar", invoiceId);
                    result.addWarning("Invoice " + invoiceId + " não tem items para processar");
                    continue;
                }
                
                log.info("🔍 === DETALHES DOS ITENS ===");
                for (InvoiceItem item : items) {
                    log.info("📋 Item {}: OperationType='{}', Asset='{}', Qty={}, Price={}, Market='{}'", 
                        item.getSequenceNumber(), item.getOperationType(), item.getAssetCode(), 
                        item.getQuantity(), item.getUnitPrice(), item.getMarketType());
                }
                log.info("🔍 === FIM DOS DETALHES ===");
                
                for (InvoiceItem item : items) {
                    try {
                        log.info("🔄 Processando item {} da invoice {}", item.getId(), invoiceId);
                        log.info("📋 Mapeando item {} para OperationDataRequest", item.getSequenceNumber());
                        
                        // ✅ NOVO: Validação específica para opções
                        if (isOptionOperation(item)) {
                            log.info("🎯 Item {} é uma operação de opção - MarketType: '{}'", 
                                item.getSequenceNumber(), item.getMarketType());
                        }
                        
                        OperationDataRequest operationRequest = mapper.mapToOperationRequest(item);
                        log.info("✅ Item {} mapeado com TransactionType: {}", item.getSequenceNumber(), operationRequest.getTransactionType());
                        log.info("🔍 Validando OperationDataRequest para item {}", item.getSequenceNumber());
                        validateOperationRequest(operationRequest);
                        log.info("✅ OperationDataRequest validado para item {}", item.getSequenceNumber());
                        log.info("🟢 Criando operação para item {}", item.getSequenceNumber());
                        
                        // ✅ NOVO: Log detalhado antes da criação da operação
                        log.info("🔍 === DADOS DA OPERAÇÃO ===");
                        log.info("Asset: {}", operationRequest.getBaseAssetCode());
                        log.info("OptionSeries: {}", operationRequest.getOptionSeriesCode());
                        log.info("TransactionType: {}", operationRequest.getTransactionType());
                        log.info("Quantity: {}", operationRequest.getQuantity());
                        log.info("Price: {}", operationRequest.getEntryUnitPrice());
                        log.info("BrokerageId: {}", operationRequest.getBrokerageId());
                        log.info("=============================");
                        
                        // CHAMADA REAL: criar operação e adicionar ID real ao resultado
                        Operation operation = operationService.createOperation(operationRequest, currentUser);
                        log.info("✅ Operação criada: {} - TransactionType: {}", operation.getId(), operation.getTransactionType());
                        result.incrementConsolidatedOperations();
                        log.info("📈 Contador de operações incrementado: {}", result.getConsolidatedOperationsCount());
                        
                    } catch (Exception e) {
                        log.error("❌ Erro ao processar item {} da invoice {}: {}", 
                            item.getId(), invoiceId, e.getMessage(), e);
                        result.addError("Erro ao processar item " + item.getId() + ": " + e.getMessage());
                        
                        // ✅ NOVO: Log detalhado do erro para debug
                        log.error("🔍 === DETALHES DO ERRO ===");
                        log.error("Item ID: {}", item.getId());
                        log.error("Sequence: {}", item.getSequenceNumber());
                        log.error("Asset: {}", item.getAssetCode());
                        log.error("OperationType: {}", item.getOperationType());
                        log.error("MarketType: {}", item.getMarketType());
                        log.error("Quantity: {}", item.getQuantity());
                        log.error("Price: {}", item.getUnitPrice());
                        log.error("Exception: {}", e.getClass().getSimpleName());
                        log.error("Message: {}", e.getMessage());
                        if (e.getCause() != null) {
                            log.error("Cause: {}", e.getCause().getMessage());
                        }
                        log.error("=============================");
                    }
                }
            }
            
            log.info("✅ Processamento de consolidação concluído: {} operações consolidadas", 
                result.getConsolidatedOperationsCount());
            
        } catch (Exception e) {
            log.error("❌ Erro no processamento de consolidação: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage("Erro no processamento: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * ✅ NOVO MÉTODO: Validação básica do OperationDataRequest
     */
    private void validateOperationRequest(OperationDataRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("OperationDataRequest não pode ser nulo");
        }
        
        if (request.getBrokerageId() == null) {
            throw new IllegalArgumentException("BrokerageId é obrigatório");
        }
        
        if (request.getTransactionType() == null) {
            throw new IllegalArgumentException("TransactionType é obrigatório");
        }
        
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity deve ser maior que zero");
        }
        
        log.debug("✅ OperationDataRequest validado: TransactionType={}, Quantity={}", 
            request.getTransactionType(), request.getQuantity());
    }

    /**
     * ✅ NOVO MÉTODO: Verifica se é uma operação de opção
     */
    private boolean isOptionOperation(InvoiceItem item) {
        if (item.getMarketType() == null) return false;
        
        String marketType = item.getMarketType().toUpperCase();
        return marketType.contains("OPCAO") || marketType.contains("OPTION");
    }

    /**
     * Resultado da consolidação
     */
    public static class ConsolidationResult {
        private boolean success;
        private String errorMessage;
        private int totalInvoices;
        private int consolidatedOperationsCount;
        private List<String> warnings = new java.util.ArrayList<>();
        private List<String> errors = new java.util.ArrayList<>();
        
        public ConsolidationResult(boolean success, String errorMessage, int totalInvoices, int consolidatedOperationsCount) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.totalInvoices = totalInvoices;
            this.consolidatedOperationsCount = consolidatedOperationsCount;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public int getTotalInvoices() { return totalInvoices; }
        public void setTotalInvoices(int totalInvoices) { this.totalInvoices = totalInvoices; }
        
        public int getConsolidatedOperationsCount() { return consolidatedOperationsCount; }
        public void incrementConsolidatedOperations() { this.consolidatedOperationsCount++; }
        
        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
        
        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
        
        public static class Builder {
            private boolean success = true;
            private String errorMessage;
            private int totalInvoices;
            private int consolidatedOperationsCount;
            
            public Builder success(boolean success) {
                this.success = success;
                return this;
            }
            
            public Builder errorMessage(String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }
            
            public Builder totalInvoices(int totalInvoices) {
                this.totalInvoices = totalInvoices;
                return this;
            }
            
            public Builder consolidatedOperationsCount(int count) {
                this.consolidatedOperationsCount = count;
                return this;
            }
            
            public ConsolidationResult build() {
                return new ConsolidationResult(success, errorMessage, totalInvoices, consolidatedOperationsCount);
            }
        }
    }
} 