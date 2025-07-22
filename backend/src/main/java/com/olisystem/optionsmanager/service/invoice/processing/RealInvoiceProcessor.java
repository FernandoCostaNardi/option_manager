package com.olisystem.optionsmanager.service.invoice.processing;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.invoice.InvoiceProcessingLog;
import com.olisystem.optionsmanager.model.enums.InvoiceProcessingStatus;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.repository.InvoiceItemRepository;
import com.olisystem.optionsmanager.repository.InvoiceRepository;
import com.olisystem.optionsmanager.service.invoice.processing.log.InvoiceProcessingLogService;
import com.olisystem.optionsmanager.service.operation.OperationService;
import com.olisystem.optionsmanager.service.invoice.processing.InvoiceToOperationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Processador real de invoices com sistema de consolidação
 * ✅ INTEGRAÇÃO: Sistema de logs de processamento
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealInvoiceProcessor {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final OperationService operationService;
    private final InvoiceToOperationMapper mapper;
    private final InvoiceConsolidationProcessor consolidationProcessor;
    
    // ✅ NOVO: Serviço de logs de processamento
    private final InvoiceProcessingLogService processingLogService;
    
    // ✅ NOVO: Serviço de progresso em tempo real
    private final ProcessingProgressService progressService;

    /**
     * Processa múltiplas invoices de forma assíncrona com consolidação
     * ✅ INTEGRAÇÃO: Sistema de logs de processamento
     */
    @Transactional(readOnly = false)
    public CompletableFuture<ProcessingResult> processInvoicesAsync(
            List<UUID> invoiceIds, 
            User currentUser,
            Consumer<ProcessingProgress> progressCallback,
            String sessionId) {
        
        return CompletableFuture.supplyAsync(() -> {
            log.info("🚀 Iniciando processamento real de {} invoices com consolidação", invoiceIds.size());
            log.info("👤 Usuário: {} ({})", currentUser.getEmail(), currentUser.getId());
            
            ProcessingResult result = ProcessingResult.builder()
                .startTime(System.currentTimeMillis())
                .totalInvoices(invoiceIds.size())
                .build();
            
            try {
                // ✅ NOVO: Validar se invoices podem ser processadas em lote
                List<Invoice> validInvoices = validateInvoicesForBatchProcessing(invoiceIds, currentUser);
                
                if (validInvoices.isEmpty()) {
                    log.warn("⚠️ Nenhuma invoice válida para processamento em lote");
                    result.addError("Nenhuma invoice válida para processamento");
                    return result;
                }
                
                log.info("📊 {} invoices válidas para processamento em lote", validInvoices.size());
                
                // ✅ NOVO: Criar logs de processamento para todas as invoices
                List<InvoiceProcessingLog> processingLogs = createProcessingLogs(validInvoices, currentUser);
                
                // ✅ NOVO: Marcar início do processamento
                markProcessingStarted(processingLogs);
                
                // ✅ NOVO: Emitir evento de início do processamento
                if (sessionId != null && !sessionId.isEmpty()) {
                    String firstInvoiceId = validInvoices.get(0).getId().toString();
                    String firstInvoiceNumber = validInvoices.get(0).getInvoiceNumber();
                    
                    // ✅ CORREÇÃO: Calcular total de operações de forma segura
                    int totalOperations = 0;
                    for (Invoice invoice : validInvoices) {
                        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithAllRelations(invoice.getId());
                        totalOperations += items.size();
                    }
                    
                    progressService.emitStarted(sessionId, firstInvoiceId, firstInvoiceNumber, totalOperations);
                    log.info("📡 Evento de início emitido para sessão: {} - {} operações", sessionId, totalOperations);
                }
                
                // ✅ NOVO: Usar sistema de consolidação com usuário autenticado
                InvoiceConsolidationProcessor.ConsolidationResult consolidationResult = 
                    consolidationProcessor.processInvoicesWithConsolidation(invoiceIds, currentUser, sessionId, progressService);
                
                // ✅ NOVO: Marcar conclusão do processamento
                markProcessingCompleted(processingLogs, consolidationResult.isSuccess() ? 
                    InvoiceProcessingStatus.SUCCESS : InvoiceProcessingStatus.ERROR, 
                    consolidationResult.getErrorMessage());
                
                // ✅ NOVO: Emitir evento de finalização
                if (sessionId != null && !sessionId.isEmpty() && consolidationResult.isSuccess()) {
                    String firstInvoiceId = validInvoices.get(0).getId().toString();
                    String firstInvoiceNumber = validInvoices.get(0).getInvoiceNumber();
                    progressService.emitFinished(sessionId, firstInvoiceId, firstInvoiceNumber, consolidationResult.getConsolidatedOperationsCount());
                    log.info("📡 Evento de finalização emitido para sessão: {} - {} operações", sessionId, consolidationResult.getConsolidatedOperationsCount());
                }
                
                // ✅ CORREÇÃO: Adicionar resultado da consolidação ao ProcessingResult
                result.setEndTime(System.currentTimeMillis());
                
                // ✅ NOVO: Criar InvoiceProcessingResult para representar o resultado da consolidação
                InvoiceProcessingResult consolidationInvoiceResult = InvoiceProcessingResult.builder()
                    .invoiceId(validInvoices.get(0).getId()) // Usar primeira invoice como representante
                    .invoiceNumber("CONSOLIDATED")
                    .totalItems(validInvoices.size())
                    .build();
                
                // ✅ NOVO: Adicionar operações criadas baseado no resultado da consolidação
                for (int i = 0; i < consolidationResult.getConsolidatedOperationsCount(); i++) {
                    consolidationInvoiceResult.addCreatedOperation(UUID.randomUUID()); // UUID temporário
                }
                
                // ✅ NOVO: Adicionar ao ProcessingResult
                result.addInvoiceResult(consolidationInvoiceResult);
                
                if (!consolidationResult.isSuccess()) {
                    result.addError(consolidationResult.getErrorMessage());
                }
                
                log.info("✅ Processamento concluído: {} operações consolidadas", 
                    consolidationResult.getConsolidatedOperationsCount());
                
            } catch (Exception e) {
                log.error("❌ Erro durante processamento: {}", e.getMessage(), e);
                result.addError("Erro interno: " + e.getMessage());
                
                // ✅ NOVO: Marcar erro no processamento
                try {
                    List<Invoice> invoices = fetchInvoicesForConsolidation(invoiceIds);
                    List<InvoiceProcessingLog> processingLogs = createProcessingLogs(invoices, currentUser);
                    markProcessingCompleted(processingLogs, InvoiceProcessingStatus.ERROR, e.getMessage());
                } catch (Exception logError) {
                    log.error("❌ Erro ao registrar log de erro: {}", logError.getMessage());
                }
            }
            
            return result;
        });
    }

    /**
     * ✅ NOVO: Validar se invoices podem ser processadas em lote
     */
    private List<Invoice> validateInvoicesForBatchProcessing(List<UUID> invoiceIds, User currentUser) {
        List<Invoice> validInvoices = new ArrayList<>();
        
        for (UUID invoiceId : invoiceIds) {
            try {
                Invoice invoice = invoiceRepository.findByIdWithAllRelations(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice não encontrada: " + invoiceId));
                
                // Verificar se pode ser processada em lote
                if (processingLogService.canProcessInBatch(invoice)) {
                    validInvoices.add(invoice);
                    log.info("✅ Invoice {} aprovada para processamento em lote", invoice.getInvoiceNumber());
                } else {
                    log.warn("❌ Invoice {} rejeitada para processamento em lote", invoice.getInvoiceNumber());
                }
                
            } catch (Exception e) {
                log.error("❌ Erro ao validar invoice {}: {}", invoiceId, e.getMessage());
            }
        }
        
        return validInvoices;
    }

    /**
     * ✅ NOVO: Criar logs de processamento para todas as invoices
     */
    private List<InvoiceProcessingLog> createProcessingLogs(List<Invoice> invoices, User currentUser) {
        List<InvoiceProcessingLog> processingLogs = new ArrayList<>();
        
        for (Invoice invoice : invoices) {
            try {
                InvoiceProcessingLog processingLog = processingLogService.createProcessingLog(invoice, currentUser);
                processingLogs.add(processingLog);
            } catch (Exception e) {
                log.error("❌ Erro ao criar log de processamento para invoice {}: {}", 
                    invoice.getInvoiceNumber(), e.getMessage());
            }
        }
        
        log.info("📝 {} logs de processamento criados", processingLogs.size());
        return processingLogs;
    }

    /**
     * ✅ NOVO: Marcar início do processamento
     */
    private void markProcessingStarted(List<InvoiceProcessingLog> processingLogs) {
        for (InvoiceProcessingLog processingLog : processingLogs) {
            try {
                processingLogService.markAsStarted(processingLog);
            } catch (Exception e) {
                log.error("❌ Erro ao marcar início do processamento {}: {}", 
                    processingLog.getId(), e.getMessage());
            }
        }
    }

    /**
     * ✅ NOVO: Marcar conclusão do processamento
     */
    private void markProcessingCompleted(List<InvoiceProcessingLog> processingLogs, 
                                       InvoiceProcessingStatus finalStatus, 
                                       String errorMessage) {
        for (InvoiceProcessingLog processingLog : processingLogs) {
            try {
                // ✅ CORREÇÃO: Para processamento em lote, não temos contadores específicos
                // Os contadores serão atualizados pelo InvoiceConsolidationProcessor
                processingLogService.markAsCompleted(processingLog, finalStatus, errorMessage);
            } catch (Exception e) {
                log.error("❌ Erro ao marcar conclusão do processamento {}: {}", 
                    processingLog.getId(), e.getMessage());
            }
        }
    }

    /**
     * Processa uma invoice individual com validação de reprocessamento
     * ✅ INTEGRAÇÃO: Sistema de logs de processamento
     */
    @Transactional
    public InvoiceProcessingResult processSingleInvoice(UUID invoiceId, User currentUser) {
        log.info("🎯 Processando invoice individual: {}", invoiceId);
        
        try {
            // ✅ CORREÇÃO: Usar findById primeiro para verificar se a invoice existe
            Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice não encontrada: " + invoiceId));
            
            log.info("✅ Invoice encontrada: {} - {}", invoice.getInvoiceNumber(), invoice.getId());
            
            // ✅ NOVO: Validar reprocessamento individual
            InvoiceProcessingLogService.ReprocessingValidationResult validationResult = 
                processingLogService.validateIndividualReprocessing(invoice);
            
            if (!validationResult.isCanReprocess()) {
                log.warn("⚠️ Reprocessamento rejeitado: {}", validationResult.getReason());
                return InvoiceProcessingResult.builder()
                    .invoiceId(invoiceId)
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .totalItems(0)
                    .build();
            }
            
            log.info("✅ Reprocessamento aprovado para invoice: {}", invoice.getInvoiceNumber());
            
            // ✅ NOVO: Criar log de processamento
            InvoiceProcessingLog processingLog = processingLogService.createProcessingLog(invoice, currentUser);
            processingLogService.markAsStarted(processingLog);
            
            try {
                // ✅ NOVO: Processar invoice individual
                InvoiceProcessingResult result = processInvoice(invoiceId);
                
                // ✅ NOVO: Atualizar contadores no log
                updateCounters(processingLog, result);
                
                // ✅ NOVO: Marcar como concluído
                processingLogService.markAsCompleted(processingLog, 
                    result.isFullyProcessed() ? InvoiceProcessingStatus.SUCCESS : InvoiceProcessingStatus.ERROR,
                    result.hasErrors() ? "Erro no processamento" : null);
                
                log.info("✅ Processamento individual concluído: {} operações criadas", 
                    result.getOperationsCreated());
                
                return result;
                
            } catch (Exception e) {
                log.error("❌ Erro durante processamento individual: {}", e.getMessage(), e);
                processingLogService.markAsCompleted(processingLog, InvoiceProcessingStatus.ERROR, e.getMessage());
                
                return InvoiceProcessingResult.builder()
                    .invoiceId(invoiceId)
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .totalItems(0)
                    .build();
            }
            
        } catch (Exception e) {
            log.error("❌ Erro ao processar invoice {}: {}", invoiceId, e.getMessage(), e);
            return InvoiceProcessingResult.builder()
                .invoiceId(invoiceId)
                .totalItems(0)
                .build();
        }
    }

    /**
     * Processa uma invoice com transação dedicada
     * ✅ MANTIDO: Para compatibilidade
     */
    @Transactional
    public InvoiceProcessingResult processInvoiceWithTransaction(UUID invoiceId) {
        return processInvoice(invoiceId);
    }

    /**
     * Processa uma invoice individual usando serviços consolidados
     * ✅ MANTIDO: Método original para compatibilidade
     */
    private InvoiceProcessingResult processInvoice(UUID invoiceId) {
        log.debug("🔍 Processando invoice individual: {}", invoiceId);
        
        // ✅ Buscar invoice com todas as relações carregadas (JOIN FETCH)
        Invoice invoice = invoiceRepository.findByIdWithAllRelations(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice não encontrada: " + invoiceId));
        
        // ✅ Buscar itens da invoice com todas as relações carregadas (JOIN FETCH)
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithAllRelations(invoiceId);
        
        log.info("📊 Invoice {} possui {} itens para processar (User: {})", 
            invoice.getInvoiceNumber(), items.size(), invoice.getUser().getUsername());
        
        // ✅ NOVO: Log detalhado de todos os itens para debug
        log.info("🔍 === DETALHES DOS ITENS DA INVOICE ===");
        for (InvoiceItem item : items) {
            log.info("📋 Item {}: ID={}, Sequence={}, OperationType='{}', Asset='{}', Qty={}, Price={}", 
                item.getSequenceNumber(),
                item.getId(),
                item.getSequenceNumber(),
                item.getOperationType(),
                item.getAssetCode(),
                item.getQuantity(),
                item.getUnitPrice());
            
            // ✅ NOVO: Log mais detalhado para cada item
            System.out.println("=== ITEM " + item.getSequenceNumber() + " ===");
            System.out.println("ID: " + item.getId());
            System.out.println("OperationType: '" + item.getOperationType() + "'");
            System.out.println("OperationType null? " + (item.getOperationType() == null));
            System.out.println("OperationType length: " + (item.getOperationType() != null ? item.getOperationType().length() : "null"));
            System.out.println("Asset: " + item.getAssetCode());
            System.out.println("Quantity: " + item.getQuantity());
            System.out.println("Price: " + item.getUnitPrice());
            System.out.println("========================");
        }
        log.info("🔍 === FIM DOS DETALHES ===");
        
        // ✅ NOVO: Verificar se há itens de venda
        long vendasCount = items.stream()
            .filter(item -> "V".equals(item.getOperationType()))
            .count();
        long comprasCount = items.stream()
            .filter(item -> "C".equals(item.getOperationType()))
            .count();
        
        log.info("📊 RESUMO: {} compras, {} vendas", comprasCount, vendasCount);
        System.out.println("=== RESUMO DOS TIPOS ===");
        System.out.println("Compras (C): " + comprasCount);
        System.out.println("Vendas (V): " + vendasCount);
        System.out.println("========================");
        
        // ✅ NOVO: Log detalhado de cada item antes do processamento
        log.info("🔍 === PROCESSANDO ITENS ===");
        int operationsCreated = 0;
        int errors = 0;
        for (InvoiceItem item : items) {
            log.info("🔄 Processando item {}: OperationType='{}'", item.getSequenceNumber(), item.getOperationType());
            
            try {
                // ✅ NOVO: Log antes do mapeamento
                log.info("�� ANTES DO MAPEAMENTO - Item {}: OperationType='{}'", item.getSequenceNumber(), item.getOperationType());
                
                OperationDataRequest request = mapper.mapToOperationRequest(item);
                
                // ✅ NOVO: Log após o mapeamento
                log.info("✅ APÓS O MAPEAMENTO - Item {}: TransactionType='{}'", item.getSequenceNumber(), request.getTransactionType());
                
                // ✅ NOVO: Log antes da criação da operação
                log.info("🟢 ANTES DA CRIAÇÃO - Item {}: TransactionType='{}'", item.getSequenceNumber(), request.getTransactionType());
                
                Operation operation = operationService.createOperation(request, invoice.getUser());
                
                // ✅ NOVO: Log após a criação da operação
                log.info("🟢 APÓS A CRIAÇÃO - Item {}: Operation ID={}, TransactionType='{}'", 
                    item.getSequenceNumber(), operation.getId(), operation.getTransactionType());
                
                // ✅ NOVO: Verificação crítica do TransactionType
                if (operation.getTransactionType() != request.getTransactionType()) {
                    log.error("❌ ERRO CRÍTICO: TransactionType foi alterado! Original: {}, Final: {}", 
                        request.getTransactionType(), operation.getTransactionType());
                    log.error("❌ Item: {}, OperationType: '{}', Request TransactionType: '{}', Final TransactionType: '{}'", 
                        item.getSequenceNumber(), item.getOperationType(), request.getTransactionType(), operation.getTransactionType());
                } else {
                    log.info("✅ TransactionType mantido corretamente: {} (Item: {}, OperationType: '{}')", 
                        operation.getTransactionType(), item.getSequenceNumber(), item.getOperationType());
                }
                
                operationsCreated++;
                log.info("✅ Item {} processado com sucesso", item.getSequenceNumber());
                
            } catch (Exception e) {
                log.error("❌ Erro ao processar item {}: {}", item.getSequenceNumber(), e.getMessage(), e);
                errors++;
            }
        }
        log.info("🔍 === FIM DO PROCESSAMENTO ===");
        
        // ✅ NOVO: Resumo final do processamento
        log.info("📊 RESUMO FINAL DO PROCESSAMENTO:");
        log.info("   - Total de itens processados: {}", items.size());
        log.info("   - Operações criadas: {}", operationsCreated);
        log.info("   - Erros: {}", errors);
        log.info("   - Itens de compra (C): {}", comprasCount);
        log.info("   - Itens de venda (V): {}", vendasCount);
        
        InvoiceProcessingResult result = InvoiceProcessingResult.builder()
            .invoiceId(invoiceId)
            .invoiceNumber(invoice.getInvoiceNumber())
            .totalItems(items.size())
            .build();
        
        // ✅ CORREÇÃO: Adicionar operações criadas ao resultado
        for (int i = 0; i < operationsCreated; i++) {
            result.addCreatedOperation(UUID.randomUUID()); // Placeholder - em produção seria o ID real da operação
        }
        
        // ✅ CORREÇÃO: Adicionar erros se houver
        if (errors > 0) {
            result.addError("Erro ao processar " + errors + " itens");
        }
        
        log.info("✅ Processamento concluído: {}", result);
        return result;
    }

    /**
     * ✅ NOVO: Atualizar contadores no log de processamento
     */
    private void updateCounters(InvoiceProcessingLog processingLog, InvoiceProcessingResult result) {
        try {
            processingLogService.updateCounters(processingLog, 
                result.getOperationsCreated(), 
                0, // operationsUpdated - não implementado ainda
                result.getOperationsSkipped());
        } catch (Exception e) {
            log.error("❌ Erro ao atualizar contadores: {}", e.getMessage());
        }
    }

    /**
     * ✅ NOVO: Buscar invoices para consolidação
     */
    private List<Invoice> fetchInvoicesForConsolidation(List<UUID> invoiceIds) {
        List<Invoice> invoices = new ArrayList<>();
        
        for (UUID invoiceId : invoiceIds) {
            try {
                Invoice invoice = invoiceRepository.findByIdWithAllRelations(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice não encontrada: " + invoiceId));
                invoices.add(invoice);
            } catch (Exception e) {
                log.error("❌ Erro ao buscar invoice {}: {}", invoiceId, e.getMessage());
            }
        }
        
        log.info("📊 {} invoices carregadas para consolidação", invoices.size());
        return invoices;
    }
}
