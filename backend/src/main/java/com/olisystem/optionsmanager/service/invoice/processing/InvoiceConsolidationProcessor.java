package com.olisystem.optionsmanager.service.invoice.processing;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.dto.option.OptionDataResponseDto;
import com.olisystem.optionsmanager.model.Asset.Asset;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.invoice.InvoiceProcessingLog;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.repository.InvoiceItemRepository;
import com.olisystem.optionsmanager.repository.InvoiceRepository;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.optionSerie.OptionSerieRepository;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import com.olisystem.optionsmanager.service.asset.AssetService;
import com.olisystem.optionsmanager.service.invoice.processing.log.InvoiceProcessingLogService;
import com.olisystem.optionsmanager.service.operation.OperationService;
import com.olisystem.optionsmanager.service.option_series.OptionSerieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Processador de consolidação de invoices que integra com o sistema de consolidação existente
 * ✅ CORREÇÃO: Adicionado @Transactional para resolver problema de lazy loading
 * ✅ NOVO: Integração com InvoiceProcessingLogService para contadores
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
    private final OperationRepository operationRepository;
    private final OptionSerieRepository optionSerieRepository;
    private final OptionSerieService optionSerieService;
    private final AssetService assetService;
    private final PositionRepository positionRepository;
    private final com.olisystem.optionsmanager.service.brokerage.BrokerageService brokerageService;
    private final com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationService averageOperationService;
    private final com.olisystem.optionsmanager.repository.BrokerageRepository brokerageRepository;
    
    // ✅ NOVO: Serviço de logs de processamento
    private final InvoiceProcessingLogService processingLogService;

    /**
     * Processa invoices com sistema de consolidação
     * ✅ CORREÇÃO: Adicionado @Transactional para resolver problema de lazy loading
     * ✅ NOVO: Tratamento específico para operações de opções
     * ✅ NOVO: Integração com InvoiceProcessingLogService para contadores
     */
    @Transactional(readOnly = false)
    public ConsolidationResult processInvoicesWithConsolidation(List<UUID> invoiceIds, User currentUser, 
                                                             String sessionId, ProcessingProgressService progressService) {
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
                
                // ✅ NOVO: Buscar ou criar log de processamento para esta invoice
                InvoiceProcessingLog processingLog = processingLogService.createProcessingLog(invoice, currentUser);
                
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
                
                // ✅ NOVO: Ordenar itens por sequenceNumber para garantir ordem correta
                items.sort((a, b) -> Integer.compare(a.getSequenceNumber(), b.getSequenceNumber()));
                log.info("📋 Itens ordenados por sequenceNumber: {}", 
                    items.stream().map(item -> item.getSequenceNumber() + "(" + item.getOperationType() + ")").collect(java.util.stream.Collectors.joining(", ")));
                
                // ✅ NOVO: Contadores para esta invoice
                int operationsCreatedForInvoice = 0;
                int operationsSkippedForInvoice = 0;
                
                int operationCounter = 0;
                for (InvoiceItem item : items) {
                    operationCounter++;
                    try {
                        log.info("🔄 Processando item {}/{} (sequence: {}) da invoice {}", 
                            operationCounter, items.size(), item.getSequenceNumber(), invoiceId);
                        
                        // ✅ NOVO: Emitir evento de processamento
                        if (sessionId != null && progressService != null) {
                            String invoiceIdStr = invoice.getId().toString();
                            String invoiceNumber = invoice.getInvoiceNumber();
                            progressService.emitProcessing(sessionId, invoiceIdStr, invoiceNumber, operationCounter, items.size());
                            log.debug("📡 Evento de processamento emitido: operação {}/{}", operationCounter, items.size());
                        }
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

                        // ✅ CORREÇÃO: Usar o TransactionType já mapeado pelo InvoiceToOperationMapper
                        TransactionType transactionType = operationRequest.getTransactionType();

                        OptionDataResponseDto optionDataResponseDto = optionSerieService.buscarOpcaoInfo(operationRequest.getOptionSeriesCode());
                        operationRequest.setBaseAssetCode(optionDataResponseDto.getBaseAsset());
                        operationRequest.setBaseAssetName(optionDataResponseDto.getBaseAssetName());
                        operationRequest.setBaseAssetLogoUrl(optionDataResponseDto.getBaseAssetUrlLogo());
                        operationRequest.setBaseAssetType(optionDataResponseDto.getBaseAssetType());
                        Asset asset = assetService.findOrCreateAsset(operationRequest);
                        OptionSerie optionSerie = optionSerieService.findOrCreateOptionSerie(operationRequest, asset);

                        if(transactionType == TransactionType.BUY){

                        // CHAMADA REAL: criar operação e adicionar ID real ao resultado
                            Operation operation = operationService.createOperation(operationRequest, currentUser);
                            log.info("✅ Operação criada: {} - TransactionType: {}", operation.getId(), operation.getTransactionType());
                        result.incrementConsolidatedOperations();
                        operationsCreatedForInvoice++;
                        log.info("📈 Contador de operações incrementado: {} (invoice: {})", result.getConsolidatedOperationsCount(), operationsCreatedForInvoice);
                        
                        // ✅ NOVO: Emitir evento de conclusão
                        if (sessionId != null && progressService != null) {
                            String invoiceIdStr = invoice.getId().toString();
                            String invoiceNumber = invoice.getInvoiceNumber();
                            progressService.emitCompleted(sessionId, invoiceIdStr, invoiceNumber, operationCounter, items.size());
                            log.debug("📡 Evento de conclusão emitido: operação {}/{}", operationCounter, items.size());
                        }
                        } else {

                                                      // ✅ CORREÇÃO: Buscar posição em vez de operação com status ACTIVE
                            // Buscar posição aberta para este ativo
                            Optional<Position> openPosition = positionRepository.findOpenPositionByUserAndOptionSeriesAndBrokerage(
                                currentUser, optionSerie, invoice.getBrokerage());
                            
                            if(openPosition.isEmpty()){
                                log.warn("⚠️ Posição não encontrada para saída: {} - criando operação de saída direta", item.getAssetCode());
                                // ✅ CORREÇÃO: Criar operação diretamente sem passar pelo fluxo de estratégia
                                Operation operation = createDirectOperation(operationRequest, currentUser);
                                
                                log.info("✅ Operação de saída criada: {} - TransactionType: {}, Status: {}", 
                                    operation.getId(), operation.getTransactionType(), operation.getStatus());
                                result.incrementConsolidatedOperations();
                                operationsCreatedForInvoice++;
                                log.info("📈 Contador de operações incrementado: {} (invoice: {})", result.getConsolidatedOperationsCount(), operationsCreatedForInvoice);
                                
                                // ✅ NOVO: Emitir evento de conclusão
                                if (sessionId != null && progressService != null) {
                                    String invoiceIdStr = invoice.getId().toString();
                                    String invoiceNumber = invoice.getInvoiceNumber();
                                    progressService.emitCompleted(sessionId, invoiceIdStr, invoiceNumber, operationCounter, items.size());
                                    log.debug("📡 Evento de conclusão emitido: operação {}/{}", operationCounter, items.size());
                                }
                                continue;
                            }
                            
                            Position position = openPosition.get();
                            log.info("✅ Posição encontrada: {} (status: {}, remaining: {})", 
                                position.getId(), position.getStatus(), position.getRemainingQuantity());
                            
                            // ✅ CORREÇÃO: Verificar se a posição ainda tem quantidade disponível
                            if (position.getRemainingQuantity() <= 0) {
                                log.warn("⚠️ Posição {} já foi totalmente fechada (remaining: {}) - criando operação de saída direta", 
                                    position.getId(), position.getRemainingQuantity());
                                
                                // Criar operação de saída direta quando a posição já foi fechada
                                Operation operation = createDirectOperation(operationRequest, currentUser);
                                
                                log.info("✅ Operação de saída criada (posição fechada): {} - TransactionType: {}, Status: {}", 
                                    operation.getId(), operation.getTransactionType(), operation.getStatus());
                                result.incrementConsolidatedOperations();
                                operationsCreatedForInvoice++;
                                log.info("📈 Contador de operações incrementado: {} (invoice: {})", result.getConsolidatedOperationsCount(), operationsCreatedForInvoice);
                                continue;
                            }
                            
                            // ✅ CORREÇÃO: Determinar se é saída total ou parcial
                            int requestedQuantity = operationRequest.getQuantity();
                            int availableQuantity = position.getRemainingQuantity();
                            int quantityToUse = requestedQuantity;
                            boolean isTotalExit = false;
                            
                            if (requestedQuantity >= availableQuantity) {
                                // Saída total: usar quantidade disponível
                                quantityToUse = availableQuantity;
                                isTotalExit = true;
                                log.info("🎯 Saída TOTAL detectada: solicitado={}, disponível={}, usando={}", 
                                    requestedQuantity, availableQuantity, quantityToUse);
                            } else {
                                // Saída parcial: usar quantidade solicitada
                                log.info("🎯 Saída PARCIAL detectada: solicitado={}, disponível={}, usando={}", 
                                    requestedQuantity, availableQuantity, quantityToUse);
                            }
                            
                            // ✅ CORREÇÃO: Buscar especificamente a operação CONSOLIDATED_ENTRY usando roleType
                            // Esta é a operação que deve ser usada para saídas parciais
                            Operation consolidatedEntryOperation = averageOperationService.findConsolidatedEntryOperation(
                                optionSerie, currentUser);
                            
                            if (consolidatedEntryOperation == null) {
                                log.error("❌ Operação CONSOLIDATED_ENTRY não encontrada: {}", item.getAssetCode());
                                
                                // 🔍 DEBUG: Verificar se existe alguma operação CONSOLIDATED_ENTRY com status diferente
                                List<Operation> allConsolidatedEntries = operationRepository.findByOptionSeriesAndUserAndTransactionType(
                                    optionSerie, currentUser, TransactionType.BUY);
                                log.info("🔍 DEBUG: Encontradas {} operações BUY para este ativo", allConsolidatedEntries.size());
                                for (Operation op : allConsolidatedEntries) {
                                    log.info("🔍   - ID: {}, Status: {}, Quantity: {}", 
                                        op.getId(), op.getStatus(), op.getQuantity());
                                }
                                
                                // 🔍 DEBUG: Tentar buscar por qualquer status
                                List<Operation> allOperations = operationRepository.findByOptionSeriesAndUserAndTransactionType(
                                    optionSerie, currentUser, TransactionType.BUY);
                                log.info("🔍 DEBUG: Tentando buscar operação com qualquer status...");
                                for (Operation op : allOperations) {
                                    log.info("🔍   - ID: {}, Status: {}, Quantity: {}, TransactionType: {}", 
                                        op.getId(), op.getStatus(), op.getQuantity(), op.getTransactionType());
                                    // ✅ CORREÇÃO: Aceitar qualquer status exceto HIDDEN
                                    if (op.getStatus() != OperationStatus.HIDDEN) {
                                        log.info("🔍   ✅ Encontrada operação válida: {}", op.getId());
                                        consolidatedEntryOperation = op;
                                        break;
                                    }
                                }
                                
                                // 🔍 DEBUG: Se ainda não encontrou, tentar buscar por qualquer TransactionType
                                if (consolidatedEntryOperation == null) {
                                    log.info("🔍 DEBUG: Tentando buscar por qualquer TransactionType...");
                                    List<Operation> allOperationsAnyType = operationRepository.findByOptionSeriesAndUser(optionSerie, currentUser);
                                    for (Operation op : allOperationsAnyType) {
                                        log.info("🔍   - ID: {}, Status: {}, Quantity: {}, TransactionType: {}", 
                                            op.getId(), op.getStatus(), op.getQuantity(), op.getTransactionType());
                                        if (op.getStatus() != OperationStatus.HIDDEN) {
                                            log.info("🔍   ✅ Encontrada operação válida (qualquer tipo): {}", op.getId());
                                            consolidatedEntryOperation = op;
                                            break;
                                        }
                                    }
                                }
                                
                                if (consolidatedEntryOperation == null) {
                                    log.error("❌ Nenhuma operação válida encontrada para saída: {}", item.getAssetCode());
                                    operationsSkippedForInvoice++;
                                    continue;
                                }
                            }
                            
                            log.info("✅ Operação CONSOLIDATED_ENTRY encontrada: {} (status: {}, quantity: {})", 
                                consolidatedEntryOperation.getId(), consolidatedEntryOperation.getStatus(), consolidatedEntryOperation.getQuantity());
                            
                            // ✅ CORREÇÃO: Criar operação de saída usando a estratégia correta
                            Operation exitOperation = createExitOperation(operationRequest, currentUser, consolidatedEntryOperation, quantityToUse, isTotalExit);
                            
                            log.info("✅ Operação de saída criada: {} - TransactionType: {}, Status: {}", 
                                exitOperation.getId(), exitOperation.getTransactionType(), exitOperation.getStatus());
                            result.incrementConsolidatedOperations();
                            operationsCreatedForInvoice++;
                            log.info("📈 Contador de operações incrementado: {} (invoice: {})", result.getConsolidatedOperationsCount(), operationsCreatedForInvoice);
                        }
                        
                    } catch (Exception e) {
                        log.error("❌ Erro ao processar item {} da invoice {}: {}", item.getSequenceNumber(), invoiceId, e.getMessage(), e);
                        operationsSkippedForInvoice++;
                        result.addError("Erro ao processar item " + item.getSequenceNumber() + ": " + e.getMessage());
                    }
                }
                
                // ✅ NOVO: Atualizar contadores no log de processamento
                processingLogService.updateCounters(processingLog, 
                    operationsCreatedForInvoice, 
                    0, // operationsUpdated - não implementado ainda
                    operationsSkippedForInvoice);
                
                log.info("📊 Invoice {} processada: {} operações criadas, {} ignoradas", 
                    invoice.getInvoiceNumber(), operationsCreatedForInvoice, operationsSkippedForInvoice);
            }
            
            log.info("✅ Processamento de consolidação concluído: {} operações criadas", result.getConsolidatedOperationsCount());
            
        } catch (Exception e) {
            log.error("❌ Erro durante processamento de consolidação: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage("Erro interno: " + e.getMessage());
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
     * ✅ NOVO MÉTODO: Cria operação diretamente sem passar pelo fluxo de estratégia
     */
    private Operation createDirectOperation(OperationDataRequest request, User currentUser) {
        // Criar operação diretamente
        Operation operation = new Operation();
        operation.setId(UUID.randomUUID());
        operation.setUser(currentUser);
        operation.setBrokerage(brokerageRepository.findById(request.getBrokerageId()).orElseThrow(() -> new IllegalArgumentException("Brokerage não encontrada")));
        operation.setOptionSeries(optionSerieService.getOptionSerieByCode(request.getOptionSeriesCode()));
        operation.setTransactionType(request.getTransactionType());
        operation.setQuantity(request.getQuantity());
        operation.setEntryDate(request.getEntryDate());
        operation.setEntryUnitPrice(request.getEntryUnitPrice());
        operation.setEntryTotalValue(request.getEntryUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        operation.setTradeType(TradeType.DAY); // Default para operações diretas
        
        // ✅ CORREÇÃO: Para operações de saída direta, usar preço de saída diferente
        if (request.getTransactionType() == TransactionType.SELL) {
            operation.setExitDate(request.getEntryDate());
            operation.setExitUnitPrice(request.getEntryUnitPrice());
            operation.setExitTotalValue(request.getEntryUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
            BigDecimal averageEntryPrice = calculateAverageEntryPrice(request.getOptionSeriesCode(), currentUser);
            if (averageEntryPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal entryTotalValue = averageEntryPrice.multiply(BigDecimal.valueOf(request.getQuantity()));
                BigDecimal exitTotalValue = operation.getExitTotalValue();
                BigDecimal profitLoss = exitTotalValue.subtract(entryTotalValue);
                operation.setProfitLoss(profitLoss);
                if (entryTotalValue.compareTo(BigDecimal.ZERO) != 0) {
                    operation.setProfitLossPercentage(profitLoss.divide(entryTotalValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
                } else {
                    operation.setProfitLossPercentage(BigDecimal.ZERO);
                }
                // ✅ CORREÇÃO: Operações individuais devem ser HIDDEN
                operation.setStatus(OperationStatus.HIDDEN);
            } else {
                operation.setProfitLoss(BigDecimal.ZERO);
                operation.setProfitLossPercentage(BigDecimal.ZERO);
                operation.setStatus(OperationStatus.ACTIVE);
            }
        } else {
            operation.setExitDate(null);
            operation.setExitUnitPrice(null);
            operation.setExitTotalValue(null);
            operation.setProfitLoss(null);
            operation.setProfitLossPercentage(null);
            operation.setStatus(OperationStatus.ACTIVE);
        }
        
        // Salvar operação
        operation = operationRepository.save(operation);
        
        // Buscar grupo de média existente para o usuário e série de opções
        List<Operation> entryOperations = operationRepository.findByOptionSeriesAndUserAndTransactionType(
            operation.getOptionSeries(), currentUser, TransactionType.BUY);
        AverageOperationGroup group = null;
        if (!entryOperations.isEmpty()) {
            // Pega a última operação de entrada
            Operation lastEntry = entryOperations.get(entryOperations.size() - 1);
            group = averageOperationService.getGroupByOperation(lastEntry);
        }
        if (group != null) {
            // ✅ CORREÇÃO: Usar roleType correto baseado no tipo de transação
            OperationRoleType roleType = (request.getTransactionType() == TransactionType.SELL) ? 
                OperationRoleType.PARTIAL_EXIT : OperationRoleType.ORIGINAL;
            averageOperationService.addNewItemGroup(group, operation, roleType);
        } else {
            log.warn("⚠️ Não foi encontrado AverageOperationGroup para usuário {} e série {} ao adicionar operação {}.", currentUser.getId(), operation.getOptionSeries().getId(), operation.getId());
        }
        return operation;
    }

    /**
     * ✅ NOVO MÉTODO: Calcula preço médio das operações de entrada
     */
    private BigDecimal calculateAverageEntryPrice(String optionSeriesCode, User currentUser) {
        try {
            OptionSerie optionSerie = optionSerieService.getOptionSerieByCode(optionSeriesCode);
            List<Operation> entryOperations = operationRepository.findByOptionSeriesAndUserAndTransactionType(
                optionSerie, currentUser, TransactionType.BUY);
            
            if (entryOperations.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            BigDecimal totalValue = BigDecimal.ZERO;
            int totalQuantity = 0;
            
            for (Operation op : entryOperations) {
                if (op.getEntryTotalValue() != null && op.getQuantity() != null) {
                    totalValue = totalValue.add(op.getEntryTotalValue());
                    totalQuantity += op.getQuantity();
                }
            }
            
            if (totalQuantity > 0) {
                return totalValue.divide(BigDecimal.valueOf(totalQuantity), 4, RoundingMode.HALF_UP);
            }
            
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("⚠️ Erro ao calcular preço médio para {}: {}", optionSeriesCode, e.getMessage());
            return BigDecimal.ZERO;
        }
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
     * ✅ NOVO: Cria operação de saída usando a estratégia correta
     */
    private Operation createExitOperation(OperationDataRequest operationRequest, User currentUser, 
                                       Operation consolidatedEntryOperation, int quantityToUse, boolean isTotalExit) {
        log.info("🔄 Criando operação de saída: quantidade={}, total={}", quantityToUse, isTotalExit);
        
        // Converter operationRequest para OperationFinalizationRequest
        OperationFinalizationRequest finalizationRequest = new OperationFinalizationRequest();
        finalizationRequest.setOperationId(consolidatedEntryOperation.getId());
        finalizationRequest.setQuantity(quantityToUse);
        finalizationRequest.setExitUnitPrice(operationRequest.getEntryUnitPrice());
        finalizationRequest.setExitDate(operationRequest.getEntryDate());
        
        // Criar operação de saída usando o serviço
        Operation exitOperation = operationService.createExitOperation(finalizationRequest, currentUser);
        
        log.info("✅ Operação de saída criada: {} - TransactionType: {}, Status: {}", 
            exitOperation.getId(), exitOperation.getTransactionType(), exitOperation.getStatus());
        
        return exitOperation;
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
