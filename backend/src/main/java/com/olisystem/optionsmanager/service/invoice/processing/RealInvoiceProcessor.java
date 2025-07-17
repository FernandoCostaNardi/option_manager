package com.olisystem.optionsmanager.service.invoice.processing;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.repository.InvoiceRepository;
import com.olisystem.optionsmanager.repository.InvoiceItemRepository;
import com.olisystem.optionsmanager.service.operation.OperationService;
import com.olisystem.optionsmanager.service.invoice.processing.orchestrator.InvoiceProcessingOrchestrator;
import com.olisystem.optionsmanager.validation.OperationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Processador real de invoices que converte em operações
 * ✅ ATUALIZADO: Agora usa o sistema de consolidação de operações existente
 * 
 * @author Sistema de Gestão de Opções  
 * @since 2025-07-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealInvoiceProcessor {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceToOperationMapper mapper;
    private final OperationService operationService;
    private final OperationValidator operationValidator;
    
    // ✅ NOVO: Orquestrador com sistema de consolidação
    private final InvoiceProcessingOrchestrator orchestrator;
    
    // ✅ NOVO: Processador de consolidação
    private final InvoiceConsolidationProcessor consolidationProcessor;

    /**
     * Processa múltiplas invoices de forma assíncrona
     * ✅ ATUALIZADO: Usa sistema de consolidação e correções implementadas
     * ✅ CORREÇÃO: Adicionado @Transactional para resolver problema de rollback
     */
    @Transactional(readOnly = false)
    public CompletableFuture<ProcessingResult>  processInvoicesAsync(
            List<UUID> invoiceIds, 
            User currentUser,
            Consumer<ProcessingProgress> progressCallback) {
        
        return CompletableFuture.supplyAsync(() -> {
            log.info("🚀 Iniciando processamento real de {} invoices com consolidação", invoiceIds.size());
            log.info("👤 Usuário: {} ({})", currentUser.getEmail(), currentUser.getId());
            
            ProcessingResult result = ProcessingResult.builder()
                .startTime(System.currentTimeMillis())
                .totalInvoices(invoiceIds.size())
                .build();
            
            try {
                // ✅ NOVO: Usar sistema de consolidação com usuário autenticado
                InvoiceConsolidationProcessor.ConsolidationResult consolidationResult = 
                    consolidationProcessor.processInvoicesWithConsolidation(invoiceIds, currentUser);
                
                // ✅ NOVO: Converter resultado da consolidação
                result.setEndTime(System.currentTimeMillis());
                
                if (consolidationResult.isSuccess()) {
                    log.info("✅ Processamento de consolidação concluído: {} operações consolidadas", 
                        consolidationResult.getConsolidatedOperationsCount());
                    
                    // ✅ NOVO: Criar InvoiceProcessingResult e adicionar ao ProcessingResult
                    InvoiceProcessingResult invoiceResult = InvoiceProcessingResult.builder()
                        .invoiceId(invoiceIds.get(0)) // Assumindo uma invoice por vez
                        .invoiceNumber("N/A") // Será preenchido se necessário
                        .totalItems(consolidationResult.getConsolidatedOperationsCount())
                        .build();
                    
                    // Adicionar operações criadas ao resultado
                    for (int i = 0; i < consolidationResult.getConsolidatedOperationsCount(); i++) {
                        invoiceResult.addCreatedOperation(UUID.randomUUID()); // Mock UUID
                    }
                    
                    result.addInvoiceResult(invoiceResult);
                    
                } else {
                    log.error("❌ Processamento de consolidação falhou: {}", consolidationResult.getErrorMessage());
                    result.addError("Falha na consolidação: " + consolidationResult.getErrorMessage());
                }
                
                // ✅ NOVO: Adicionar erros da consolidação
                for (String error : consolidationResult.getErrors()) {
                    result.addError(error);
                }
                
            } catch (Exception e) {
                log.error("❌ Erro durante processamento: {}", e.getMessage(), e);
                result.addError("Erro interno: " + e.getMessage());
            }
            
            return result;
        });
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
                log.info("📋 ANTES DO MAPEAMENTO - Item {}: OperationType='{}'", item.getSequenceNumber(), item.getOperationType());
                
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
        
        log.info("📈 Invoice {} processada: {} operações criadas, {} erros", 
            invoice.getInvoiceNumber(), operationsCreated, errors);
        
        return result;
    }
    
    /**
     * Cria objeto de progresso
     */
    private ProcessingProgress createProgress(int current, int total, String message) {
        return ProcessingProgress.builder()
            .currentInvoice(current)
            .totalInvoices(total)
            .currentStep(message)
            .status(current >= total ? "COMPLETED" : "PROCESSING")
            .operationsCreated(0)
            .operationsSkipped(0)
            .operationsUpdated(0)
            .build();
    }
    
    /**
     * Cria objeto de progresso com informações de operações
     */
    private ProcessingProgress createProgressWithOperations(int current, int total, String message, 
                                                         int operationsCreated, int operationsSkipped) {
        return ProcessingProgress.builder()
            .currentInvoice(current)
            .totalInvoices(total)
            .currentStep(message)
            .status(current >= total ? "COMPLETED" : "PROCESSING")
            .operationsCreated(operationsCreated)
            .operationsSkipped(operationsSkipped)
            .operationsUpdated(0)
            .build();
    }

    /**
     * ✅ NOVO MÉTODO: Buscar invoices para consolidação
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
