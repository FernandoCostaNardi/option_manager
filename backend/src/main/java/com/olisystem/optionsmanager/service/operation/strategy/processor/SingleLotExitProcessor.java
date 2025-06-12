package com.olisystem.optionsmanager.service.operation.strategy.processor;

import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.exception.BusinessException;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.PositionOperationType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.resolver.tradeType.TradeTypeResolver;
import com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationGroupService;
import com.olisystem.optionsmanager.service.operation.consolidate.ConsolidatedOperationService;
import com.olisystem.optionsmanager.service.operation.creation.OperationCreationService;
import com.olisystem.optionsmanager.service.operation.exitRecord.ExitRecordService;
import com.olisystem.optionsmanager.service.operation.profit.ProfitCalculationService;
import com.olisystem.optionsmanager.service.operation.status.OperationStatusService;
import com.olisystem.optionsmanager.service.position.entrylots.EntryLotUpdateService;
import com.olisystem.optionsmanager.service.position.positionOperation.PositionOperationService;
import com.olisystem.optionsmanager.service.position.update.PositionUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class SingleLotExitProcessor {

    private final TradeTypeResolver tradeTypeResolver;
    private final ProfitCalculationService profitCalculationService;
    private final EntryLotUpdateService entryLotUpdateService;
    private final PositionUpdateService positionUpdateService;
    private final OperationStatusService operationStatusService;
    private final ExitRecordService exitRecordService;
    private final PositionOperationService positionOperationService;
    private final AverageOperationGroupService averageOperationGroupService;
    private final OperationCreationService operationCreationService;
    private final ConsolidatedOperationService consolidatedOperationService;

    /**
     * Processa a saída de operação com lote único
     * MELHORADO: Agora com validações e logs detalhados
     */
    @Transactional
    public Operation process(OperationExitPositionContext context) {
        log.info("=== INICIANDO PROCESSAMENTO DE SAÍDA COM LOTE ÚNICO ===");
        log.info("Operação ID: {}, Posição ID: {}",
                context.context().activeOperation().getId(),
                context.position().getId());

        try {
            // 1. VALIDAÇÕES INICIAIS
            validateProcessingContext(context);

            // 2. OBTER DADOS BÁSICOS
            EntryLot lot = context.availableLots().get(0);
            OperationFinalizationRequest request = context.context().request();
            Operation activeOperation = context.context().activeOperation();

            log.info("Lote único encontrado: ID={}, quantidade_restante={}, preço_entrada={}",
                    lot.getId(), lot.getRemainingQuantity(), lot.getUnitPrice());

            // 3. VALIDAR QUANTIDADE SOLICITADA
            validateRequestedQuantity(lot, request);

            // 4. DETERMINAR TIPO DE OPERAÇÃO (DAY/SWING)
            TradeType tradeType = tradeTypeResolver.determineTradeType(
                    lot.getEntryDate(), request.getExitDate());

            log.info("Tipo de operação determinado: {} (entrada: {}, saída: {})",
                    tradeType, lot.getEntryDate(), request.getExitDate());

            // 5. CALCULAR RESULTADOS FINANCEIROS
            // ✅ CORREÇÃO: Determinar preço de entrada correto baseado no status da Position
            BigDecimal entryPriceToUse;
            BigDecimal entryTotalValueToUse;
            
            boolean isSubsequentOperation = context.position().getStatus() == 
                    com.olisystem.optionsmanager.model.position.PositionStatus.PARTIAL;
            
            if (isSubsequentOperation) {
                // Para operações após a primeira parcial, usar preço break-even da Position
                entryPriceToUse = context.position().getAveragePrice();
                entryTotalValueToUse = entryPriceToUse.multiply(BigDecimal.valueOf(request.getQuantity()));
                log.info("Operação subsequente detectada - usando preço break-even: {}", entryPriceToUse);
            } else {
                // Para primeira operação, usar preço original
                entryPriceToUse = activeOperation.getEntryUnitPrice();
                entryTotalValueToUse = activeOperation.getEntryTotalValue();
                log.info("Primeira operação detectada - usando preço original: {}", entryPriceToUse);
            }
            
            // CORREÇÃO: Usar preços corretos para validação e cálculo
            profitCalculationService.validateCalculationInputs(
                    entryPriceToUse,
                    request.getExitUnitPrice(),
                    request.getQuantity()
            );

            BigDecimal profitLoss = profitCalculationService.calculateProfitLoss(
                    entryPriceToUse,
                    request.getExitUnitPrice(),
                    request.getQuantity());

            BigDecimal profitLossPercentage = profitCalculationService.calculateProfitLossPercentage(
                    profitLoss, entryTotalValueToUse);

            log.info("Resultados financeiros calculados: lucro/prejuízo={}, percentual={}%, preço_entrada_usado={}",
                    profitLoss, profitLossPercentage, entryPriceToUse);

            // 6. ATUALIZAR POSIÇÃO (não afeta o lote)
            log.debug("Atualizando posição...");
            boolean isTotalExit = request.getQuantity().equals(context.position().getRemainingQuantity());
            
            if (isTotalExit) {
                log.debug("Saída TOTAL detectada - usando updatePosition()");
                positionUpdateService.updatePosition(context.position(), request, profitLoss, profitLossPercentage);
            } else {
                log.debug("Saída PARCIAL detectada - usando updatePositionPartial()");
                positionUpdateService.updatePositionPartial(context.position(), request, profitLoss, profitLossPercentage, request.getQuantity());
            }

            // 7. ATUALIZAR STATUS DA OPERAÇÃO DE ENTRADA
            log.debug("Atualizando status da operação de entrada para HIDDEN...");
            operationStatusService.updateOperationStatus(activeOperation, OperationStatus.HIDDEN);

            // 8. CRIAR OPERAÇÃO DE SAÍDA
            log.debug("Criando operação de saída...");
            Operation exitOperation = operationCreationService.createExitOperation(
                    context, tradeType, profitLoss, context.transactionType(), request.getQuantity());

            log.info("Operação de saída criada: ID={}, status={}, P&L={}", 
                    exitOperation.getId(), exitOperation.getStatus(), profitLoss);

            // 9. ✅ CRIAR EXIT RECORD IMEDIATAMENTE (preservar estado do lote)
            log.debug("Criando ExitRecord ANTES de atualizar lote...");
            exitRecordService.createExitRecord(lot, exitOperation, context.context(), request.getQuantity());
            log.info("✅ ExitRecord criado com sucesso com lote intacto");

            // 10. CRIAR POSITION OPERATION (também antes de atualizar lote)
            log.debug("Criando PositionOperation...");
            positionOperationService.createPositionOperation(
                    context.position(), exitOperation, request, PositionOperationType.FULL_EXIT);

            // 11. ✅ ATUALIZAR LOTE DE ENTRADA (POR ÚLTIMO!)
            log.debug("Atualizando lote de entrada...");
            entryLotUpdateService.updateEntryLot(lot, request.getQuantity());

            // 12. ATUALIZAR AVERAGE OPERATION GROUP (final)
            log.debug("Atualizando AverageOperationGroup...");
            averageOperationGroupService.updateOperationGroup(
                    context.group(), context.position(), exitOperation, profitLoss);

            // 13. ✅ CONSOLIDAÇÃO FINAL (se posição foi totalmente fechada)
            Operation finalOperation = exitOperation;
            if (isTotalExit) {
                log.info("🔄 Posição totalmente fechada - iniciando consolidação final...");
                finalOperation = createFinalConsolidatedOperation(context, exitOperation, profitLoss, profitLossPercentage);
                log.info("✅ Operação consolidada final criada: ID={}, P&L={}", 
                    finalOperation.getId(), finalOperation.getProfitLoss());
            }

            log.info("=== PROCESSAMENTO DE SAÍDA COM LOTE ÚNICO CONCLUÍDO COM SUCESSO ===");
            log.info("Operação final ID: {}, Status: {}, P&L total: {}",
                    finalOperation.getId(), finalOperation.getStatus(), finalOperation.getProfitLoss());

            return finalOperation;

        } catch (Exception e) {
            log.error("Erro durante processamento de saída com lote único para operação {}: {}",
                    context.context().activeOperation().getId(), e.getMessage(), e);
            throw new BusinessException("Falha no processamento da saída: " + e.getMessage());
        }
    }

    /**
     * NOVO MÉTODO: Validações do contexto de processamento
     */
    private void validateProcessingContext(OperationExitPositionContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Contexto de processamento não pode ser nulo");
        }

        if (context.availableLots() == null || context.availableLots().isEmpty()) {
            throw new BusinessException("Nenhum lote disponível para processamento");
        }

        if (context.availableLots().size() != 1) {
            throw new BusinessException(
                    String.format("Processador de lote único recebeu %d lotes. Esperado: 1",
                            context.availableLots().size())
            );
        }

        if (context.context() == null || context.context().request() == null) {
            throw new IllegalArgumentException("Request de finalização não pode ser nulo");
        }

        if (context.position() == null) {
            throw new IllegalArgumentException("Posição não pode ser nula");
        }

        log.debug("Validações do contexto de processamento aprovadas");
    }

    /**
     * NOVO MÉTODO: Validar quantidade solicitada vs disponível
     */
    private void validateRequestedQuantity(EntryLot lot, OperationFinalizationRequest request) {
        Integer requestedQuantity = request.getQuantity();
        Integer availableQuantity = lot.getRemainingQuantity();

        if (requestedQuantity == null || requestedQuantity <= 0) {
            throw new IllegalArgumentException("Quantidade solicitada deve ser maior que zero");
        }

        if (requestedQuantity > availableQuantity) {
            throw new BusinessException(
                    String.format("Quantidade solicitada (%d) excede quantidade disponível no lote (%d). Lote ID: %s",
                            requestedQuantity, availableQuantity, lot.getId())
            );
        }

        // Para lote único, geralmente esperamos saída total
        if (!requestedQuantity.equals(availableQuantity)) {
            log.warn("ATENÇÃO: Quantidade solicitada ({}) é diferente da quantidade total do lote ({}). " +
                    "Isso pode indicar uma saída parcial inesperada.", requestedQuantity, availableQuantity);
        }

        log.debug("Validação de quantidade aprovada: solicitada={}, disponível={}",
                requestedQuantity, availableQuantity);
    }

    /**
     * NOVO MÉTODO: Verificar se é uma saída total
     */
    private boolean isTotalExit(EntryLot lot, Integer requestedQuantity) {
        return lot.getRemainingQuantity().equals(requestedQuantity);
    }

    /**
     * NOVO MÉTODO: Log de resumo da operação
     */
    private void logOperationSummary(Operation exitOperation, TradeType tradeType,
                                     BigDecimal profitLoss, BigDecimal profitLossPercentage) {
        String resultType = profitLoss.compareTo(BigDecimal.ZERO) >= 0 ? "LUCRO" : "PREJUÍZO";

        log.info("=== RESUMO DA OPERAÇÃO ===");
        log.info("Tipo: {} {}", tradeType, resultType);
        log.info("Valor: {} ({}%)", profitLoss, profitLossPercentage);
        log.info("Status final: {}", exitOperation.getStatus());
        log.info("========================");
    }

    /**
     * ✅ NOVO MÉTODO: Criar operação consolidada final após fechamento total da posição
     */
    private Operation createFinalConsolidatedOperation(OperationExitPositionContext context, 
                                                      Operation lastExitOperation, 
                                                      BigDecimal finalProfitLoss, 
                                                      BigDecimal finalProfitLossPercentage) {
        log.info("🔄 Criando operação consolidada final...");
        
        try {
            // ✅ CORREÇÃO: Calcular resultado absoluto correto (Total Investido vs Total Recebido)
            BigDecimal absoluteFinalProfitLoss = calculateAbsoluteFinalResult(context);
            BigDecimal absoluteFinalPercentage = calculateAbsoluteFinalPercentage(context, absoluteFinalProfitLoss);
            
            log.info("📊 Resultado absoluto calculado: P&L={}, Percentual={}%", 
                absoluteFinalProfitLoss, absoluteFinalPercentage);
            
            // Atualizar a operação com os valores absolutos corretos
            lastExitOperation.setProfitLoss(absoluteFinalProfitLoss);
            lastExitOperation.setProfitLossPercentage(absoluteFinalPercentage);
            
            // 1. Usar o serviço de consolidação para transformar em TOTAL_EXIT
            Operation consolidatedFinal = consolidatedOperationService.transformToTotalExit(
                lastExitOperation, context.group());
            
            // 2. Marcar todas as operações como HIDDEN (exceto a consolidada)
            markAllOperationsAsHidden(context);
            
            // 3. A operação consolidada deve ficar ATIVA para representar o resultado final
            OperationStatus finalStatus;
            if (absoluteFinalProfitLoss.compareTo(BigDecimal.ZERO) > 0) {
                finalStatus = OperationStatus.WINNER;
            } else if (absoluteFinalProfitLoss.compareTo(BigDecimal.ZERO) < 0) {
                finalStatus = OperationStatus.LOSER;
            } else {
                finalStatus = OperationStatus.NEUTRAl; // Break-even 
            }
            
            consolidatedFinal.setStatus(finalStatus);
            
            // ✅ CORREÇÃO: Garantir que os valores absolutos estão na operação final
            consolidatedFinal.setProfitLoss(absoluteFinalProfitLoss);
            consolidatedFinal.setProfitLossPercentage(absoluteFinalPercentage);
            
            // ✅ IMPORTANTE: Salvar a operação com os valores corrigidos
            // (O transformToTotalExit já salva, mas vamos garantir que os valores estão corretos)
            
            log.info("✅ Consolidação final concluída: status={}, P&L absoluto={}, Percentual={}%", 
                finalStatus, absoluteFinalProfitLoss, absoluteFinalPercentage);
            
            return consolidatedFinal;
            
        } catch (Exception e) {
            log.error("❌ Erro na consolidação final: {}", e.getMessage(), e);
            // Em caso de erro, retornar a operação original
            return lastExitOperation;
        }
    }
    
    /**
     * ✅ NOVO MÉTODO: Calcular resultado absoluto final (VERSÃO SIMPLIFICADA)
     */
    private BigDecimal calculateAbsoluteFinalResult(OperationExitPositionContext context) {
        log.debug("Calculando resultado absoluto final (versão simplificada)...");
        
        // ✅ ESTRATÉGIA SIMPLES: Usar só a operação ORIGINAL para investimento
        BigDecimal totalInvested = BigDecimal.ZERO;
        Operation originalOperation = findOriginalOperation(context);
        
        if (originalOperation != null && originalOperation.getEntryTotalValue() != null) {
            totalInvested = originalOperation.getEntryTotalValue();
            log.info("💰 === INVESTIMENTO ORIGINAL: {} (da operação {}) ===", 
                totalInvested, originalOperation.getId());
        } else {
            log.error("❌ Não foi possível encontrar operação ORIGINAL!");
            return BigDecimal.ZERO;
        }
        
        // ✅ ESTRATÉGIA SIMPLES: Calcular total recebido diretamente
        BigDecimal totalReceived = calculateTotalReceivedSimple(context);
        
        // Resultado absoluto = Total Recebido - Total Investido
        BigDecimal absoluteResult = totalReceived.subtract(totalInvested);
        
        log.info("📊 === CÁLCULO FINAL SIMPLIFICADO ===");
        log.info("📊 Investido: {}", totalInvested);
        log.info("📊 Recebido: {}", totalReceived);
        log.info("📊 Resultado: {}", absoluteResult);
        log.info("📊 ================================");
            
        return absoluteResult;
    }
    
    /**
     * ✅ NOVO MÉTODO: Encontrar operação ORIGINAL
     */
    private Operation findOriginalOperation(OperationExitPositionContext context) {
        for (var item : context.group().getItems()) {
            if (item.getRoleType() == OperationRoleType.ORIGINAL) {
                Operation op = item.getOperation();
                if (op != null && op.getTransactionType() == TransactionType.BUY) {
                    log.debug("🔍 Operação ORIGINAL encontrada: ID={}, valor={}", 
                        op.getId(), op.getEntryTotalValue());
                    return op;
                }
            }
        }
        
        // Fallback: usar operação ativa
        Operation activeOp = context.context().activeOperation();
        if (activeOp != null && activeOp.getEntryTotalValue() != null) {
            log.warn("⚠️ Usando operação ativa como fallback: ID={}, valor={}", 
                activeOp.getId(), activeOp.getEntryTotalValue());
            return activeOp;
        }
        
        return null;
    }
    
    /**
     * ✅ NOVO MÉTODO: Calcular total recebido de forma simples
     */
    private BigDecimal calculateTotalReceivedSimple(OperationExitPositionContext context) {
        log.info("💸 === CÁLCULO SIMPLIFICADO DO TOTAL RECEBIDO ===");
        
        BigDecimal totalReceived = BigDecimal.ZERO;
        
        // 1. Somar lucro já realizado da Position (que vem das saídas anteriores)
        if (context.position().getTotalRealizedProfit() != null) {
            // Total realizado = Valor investido original + lucro realizado
            BigDecimal totalOriginalInvested = findOriginalOperation(context).getEntryTotalValue();
            BigDecimal totalFromPreviousExits = totalOriginalInvested.add(context.position().getTotalRealizedProfit());
            
            // Mas precisamos calcular só o que foi RECEBIDO das saídas anteriores
            // Se position tem profit realizado, significa que já houve saídas
            // Vou usar uma abordagem mais direta: buscar exitTotalValue das operações já finalizadas
            
            for (var item : context.group().getItems()) {
                Operation op = item.getOperation();
                if (op != null && op.getTransactionType() == TransactionType.SELL && 
                    op.getExitTotalValue() != null && op.getStatus() != OperationStatus.HIDDEN) {
                    
                    totalReceived = totalReceived.add(op.getExitTotalValue());
                    log.info("💸 Saída anterior: ID={}, valor={}, acumulado={}", 
                        op.getId(), op.getExitTotalValue(), totalReceived);
                }
            }
        }
        
        // 2. Adicionar valor da saída atual
        BigDecimal currentExitValue = context.context().request().getExitUnitPrice()
            .multiply(BigDecimal.valueOf(context.context().request().getQuantity()));
        
        totalReceived = totalReceived.add(currentExitValue);
        log.info("💸 Saída ATUAL: valor={}", currentExitValue);
        log.info("💸 === TOTAL RECEBIDO: {} ===", totalReceived);
        
        return totalReceived;
    }
    
    /**
     * ✅ NOVO MÉTODO: Calcular investimento original total correto
     */
    private BigDecimal calculateOriginalTotalInvestment(OperationExitPositionContext context) {
        log.debug("Calculando investimento original total...");
        
        BigDecimal totalInvestment = BigDecimal.ZERO;
        
        log.info("🔍 === ANÁLISE COMPLETA DAS OPERAÇÕES DE ENTRADA ===");
        log.info("🔍 Total de itens no grupo: {}", context.group().getItems().size());
        
        // Somar todas operações de ENTRADA (BUY) do grupo para obter investimento real
        for (var item : context.group().getItems()) {
            Operation op = item.getOperation();
            
            log.info("🔍 Item do grupo: roleType={}, operação_id={}", 
                item.getRoleType(), op != null ? op.getId() : "NULL");
            
            if (op != null) {
                log.info("🔍   - TransactionType: {}", op.getTransactionType());
                log.info("🔍   - EntryTotalValue: {}", op.getEntryTotalValue());
                log.info("🔍   - ExitTotalValue: {}", op.getExitTotalValue());
                log.info("🔍   - Status: {}", op.getStatus());
                log.info("🔍   - Quantidade: {}", op.getQuantity());
                log.info("🔍   - Preço entrada: {}", op.getEntryUnitPrice());
            }
            
            if (op != null && op.getTransactionType() == TransactionType.BUY && 
                op.getEntryTotalValue() != null) {
                
                // ✅ CORREÇÃO CRÍTICA: Só contar operações ORIGINAIS, não consolidadas artificiais
                if (item.getRoleType() == OperationRoleType.ORIGINAL && 
                    (op.getStatus() == OperationStatus.ACTIVE || op.getStatus() == OperationStatus.HIDDEN)) {
                    
                    totalInvestment = totalInvestment.add(op.getEntryTotalValue());
                    log.info("💰 ✅ CONTABILIZADA - Operação ORIGINAL: ID={}, valor={}, total_acumulado={}", 
                        op.getId(), op.getEntryTotalValue(), totalInvestment);
                        
                } else {
                    log.info("💰 ❌ IGNORADA - Operação artificial: ID={}, roleType={}, status={}, valor={}", 
                        op.getId(), item.getRoleType(), op.getStatus(), op.getEntryTotalValue());
                }
            }
        }
        
        log.info("🔍 === FIM DA ANÁLISE DAS OPERAÇÕES DE ENTRADA ===");
        
        // Se não encontrou operações de entrada no grupo, usar cálculo alternativo
        if (totalInvestment.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("⚠️ Nenhuma operação de entrada válida encontrada no grupo. Usando cálculo alternativo...");
            
            // Buscar operação original através da position
            Operation originalOperation = context.context().activeOperation();
            if (originalOperation != null && originalOperation.getEntryTotalValue() != null) {
                totalInvestment = originalOperation.getEntryTotalValue();
                log.info("💰 Usando operação original: valor={}", totalInvestment);
            } else {
                // Último recurso: calcular baseado na quantity total da position e preço original
                log.warn("⚠️ Usando último recurso para calcular investimento...");
                // Buscar a primeira operação de entrada para obter preço original
                BigDecimal originalPrice = findOriginalEntryPrice(context);
                totalInvestment = originalPrice.multiply(BigDecimal.valueOf(context.position().getTotalQuantity()));
                log.info("💰 Cálculo último recurso: preço_original={}, quantidade={}, total={}", 
                    originalPrice, context.position().getTotalQuantity(), totalInvestment);
            }
        }
        
        log.info("💰 === INVESTIMENTO ORIGINAL TOTAL CALCULADO: {} ===", totalInvestment);
        return totalInvestment;
    }
    
    /**
     * ✅ NOVO MÉTODO: Encontrar preço original de entrada 
     */
    private BigDecimal findOriginalEntryPrice(OperationExitPositionContext context) {
        // Buscar primeira operação ORIGINAL do grupo
        for (var item : context.group().getItems()) {
            if (item.getRoleType() == OperationRoleType.ORIGINAL) {
                
                Operation originalOp = item.getOperation();
                if (originalOp != null && originalOp.getEntryUnitPrice() != null) {
                    log.debug("🔍 Preço original encontrado: {}", originalOp.getEntryUnitPrice());
                    return originalOp.getEntryUnitPrice();
                }
            }
        }
        
        // Se não encontrou, usar operação ativa
        Operation activeOp = context.context().activeOperation();
        if (activeOp != null && activeOp.getEntryUnitPrice() != null) {
            log.debug("🔍 Usando preço da operação ativa: {}", activeOp.getEntryUnitPrice());
            return activeOp.getEntryUnitPrice();
        }
        
        log.warn("⚠️ Não foi possível encontrar preço original! Usando BigDecimal.ZERO");
        return BigDecimal.ZERO;
    }
    
    /**
     * ✅ NOVO MÉTODO: Calcular percentual absoluto final (VERSÃO SIMPLIFICADA)
     */
    private BigDecimal calculateAbsoluteFinalPercentage(OperationExitPositionContext context, BigDecimal absoluteResult) {
        // ✅ CORREÇÃO: Usar operação original para investimento
        Operation originalOperation = findOriginalOperation(context);
        
        if (originalOperation == null || originalOperation.getEntryTotalValue() == null) {
            log.warn("⚠️ Não foi possível encontrar operação original - retornando percentual zero");
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalInvested = originalOperation.getEntryTotalValue();
        
        if (totalInvested.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("⚠️ Total investido é zero - retornando percentual zero");
            return BigDecimal.ZERO;
        }
        
        BigDecimal percentage = absoluteResult.divide(totalInvested, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
            
        log.info("📊 Percentual calculado: {}% (resultado={}, investido={})", 
            percentage, absoluteResult, totalInvested);
            
        return percentage;
    }
    
    /**
     * ✅ NOVO MÉTODO: Marcar todas operações do grupo como HIDDEN
     */
    private void markAllOperationsAsHidden(OperationExitPositionContext context) {
        log.debug("Marcando todas operações intermediárias como HIDDEN...");
        
        try {
            // Buscar todas operações do grupo através dos itens
            context.group().getItems().forEach(item -> {
                try {
                    Operation op = item.getOperation();
                    if (op != null && op.getStatus() != OperationStatus.HIDDEN) {
                        operationStatusService.updateOperationStatus(op, OperationStatus.HIDDEN);
                        log.debug("Operação {} marcada como HIDDEN", op.getId());
                    }
                } catch (Exception e) {
                    log.warn("Erro ao marcar operação como HIDDEN: {}", e.getMessage());
                }
            });
            
            log.info("✅ Todas operações intermediárias marcadas como HIDDEN");
            
        } catch (Exception e) {
            log.error("❌ Erro ao marcar operações como HIDDEN: {}", e.getMessage(), e);
        }
    }
}
