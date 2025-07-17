package com.olisystem.optionsmanager.service.operation.consolidate;

import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.repository.AverageOperationItemRepository;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.service.operation.average.AveragePriceCalculator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsolidatedOperationService {

    private final OperationRepository operationRepository;
    private final AverageOperationItemRepository itemRepository;
    private final AveragePriceCalculator averagePriceCalculator;

    /**
     * Cria operação consolidadora de entrada na primeira saída parcial
     */
    @Transactional
    public Operation createConsolidatedEntry(Operation originalEntry, AverageOperationGroup group) {
        log.info("Criando operação consolidadora de entrada baseada na operação: {}", originalEntry.getId());

        Operation consolidatedEntry = Operation.builder()
                .optionSeries(originalEntry.getOptionSeries())
                .brokerage(originalEntry.getBrokerage())
                .analysisHouse(originalEntry.getAnalysisHouse())
                .transactionType(com.olisystem.optionsmanager.model.transaction.TransactionType.BUY) // ✅ CORREÇÃO: CONSOLIDATED_ENTRY sempre representa entrada (BUY)
                .tradeType(originalEntry.getTradeType())
                .entryDate(originalEntry.getEntryDate())
                .exitDate(null)
                .quantity(originalEntry.getQuantity())
                .entryUnitPrice(originalEntry.getEntryUnitPrice())
                .entryTotalValue(originalEntry.getEntryTotalValue())
                .exitUnitPrice(null)
                .exitTotalValue(null)
                .profitLoss(BigDecimal.ZERO)
                .profitLossPercentage(BigDecimal.ZERO)
                .status(OperationStatus.ACTIVE)
                .user(originalEntry.getUser())
                .build();

        Operation savedConsolidatedEntry = operationRepository.save(consolidatedEntry);
        addOperationToGroup(savedConsolidatedEntry, group, OperationRoleType.CONSOLIDATED_ENTRY);
        return savedConsolidatedEntry;
    }
    /**
     * Cria operação consolidadora de saída na primeira saída parcial
     */
    @Transactional
    public Operation createConsolidatedExit(AverageOperationGroup group, Operation exitOperation, BigDecimal exitUnitPrice, LocalDate exitDate) {
        log.info("🔧 Criando operação consolidadora de saída baseada na operação: {}", exitOperation.getId());
        log.info("🔧 PRIMEIRA OPERAÇÃO - entryTotal={}, profitLoss={}, quantity={}, percentage={}", 
            exitOperation.getEntryTotalValue(), exitOperation.getProfitLoss(), 
            exitOperation.getQuantity(), exitOperation.getProfitLossPercentage());
        
        log.info("🔧 Usando exitDate da operação: {}", exitDate);
        
        LocalDate entryDate = findFirstEntryDateInGroup(group);
        if (entryDate == null) {
            entryDate = exitOperation.getEntryDate();
            log.warn("⚠️ EntryDate não encontrada no grupo, usando da operação: {}", entryDate);
        }
        
        log.info("🔧 DEBUG: Iniciando cálculo para CONSOLIDATED_RESULT do grupo: {}", group.getId());
        
        // Buscar todas as operações no grupo
        List<AverageOperationItem> allItems = itemRepository.findByGroup(group);
        log.info("🔧 DEBUG: Total de items no grupo: {}", allItems.size());
        
        // Filtrar operações SELL (incluindo a atual que pode não estar persistida ainda)
        List<Operation> sellOperations = allItems.stream()
            .map(AverageOperationItem::getOperation)
            .filter(op -> op.getTransactionType() == TransactionType.SELL)
            .collect(Collectors.toList());
        
        // Adicionar a operação atual se não estiver na lista
        if (!sellOperations.contains(exitOperation)) {
            sellOperations.add(exitOperation);
            log.info("🔧 DEBUG: Operação atual adicionada à lista de SELL operations");
        }
        
        log.info("🔧 DEBUG: Total de operações SELL (sem filtro de status): {}", sellOperations.size());
        
        // Log detalhado de cada operação SELL
        for (int i = 0; i < sellOperations.size(); i++) {
            Operation op = sellOperations.get(i);
            log.info("🔧 DEBUG: SELL Operation {}: ID={}, roleType={}, transactionType={}, status={}, quantity={}, profitLoss={}, exitUnitPrice={}", 
                i+1, op.getId(), 
                getOperationRoleType(op, allItems),
                op.getTransactionType(), 
                op.getStatus(), 
                op.getQuantity(), 
                op.getProfitLoss(), 
                op.getExitUnitPrice());
        }
        
        // Filtrar operações SELL válidas (incluindo HIDDEN que são PARTIAL_EXIT válidas)
        List<Operation> validSellOperations = sellOperations.stream()
            .filter(op -> op.getStatus() == OperationStatus.ACTIVE || op.getStatus() == OperationStatus.HIDDEN)
            .collect(Collectors.toList());
        
        log.info("🔧 DEBUG: Operações SELL válidas (após filtro): {}", validSellOperations.size());
        
        // Calcular quantidade total das saídas
        Integer totalExitQuantity = validSellOperations.stream()
            .mapToInt(Operation::getQuantity)
            .sum();
            
        // Incluir a operação atual se ela for SELL e não estiver já na lista
        if (exitOperation.getTransactionType() == TransactionType.SELL) {
            boolean operationAlreadyInList = validSellOperations.stream()
                .anyMatch(op -> op.getId().equals(exitOperation.getId()));
                
            if (!operationAlreadyInList) {
                totalExitQuantity += exitOperation.getQuantity();
                validSellOperations.add(exitOperation);
                log.info("🔧 DEBUG: Operação atual SELL incluída - Quantity: {}, TotalQuantity agora: {}", 
                    exitOperation.getQuantity(), totalExitQuantity);
            } else {
                log.info("🔧 DEBUG: Operação atual SELL já está na lista - ID: {}, Quantity: {}", 
                    exitOperation.getId(), exitOperation.getQuantity());
            }
        }
        
        log.info("🔧 Quantidade total das SAÍDAS (SELL) para CONSOLIDATED_RESULT: {}", totalExitQuantity);
        log.info("🔧 DEBUG: Operações SELL encontradas: {}", validSellOperations.size());
        
        // Recalcular quantidade total após possível inclusão da operação atual
        totalExitQuantity = validSellOperations.stream()
            .mapToInt(Operation::getQuantity)
            .sum();
            
        log.info("🔧 DEBUG: Quantidade total RECALCULADA: {}", totalExitQuantity);
        
        // Calcular valor total de saída
        BigDecimal totalExitValue = validSellOperations.stream()
            .map(op -> BigDecimal.valueOf(op.getQuantity()).multiply(op.getExitUnitPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("🔧 DEBUG: totalExitValue calculado: {}", totalExitValue);
        log.info("🔧 DEBUG: totalExitQuantity calculado: {}", totalExitQuantity);
        
        // Calcular preço médio de saída
        BigDecimal averageExitPrice = totalExitQuantity > 0 ? 
            totalExitValue.divide(BigDecimal.valueOf(totalExitQuantity), 6, RoundingMode.HALF_UP) : 
            exitUnitPrice;
        
        log.info("🔧 DEBUG: averageExitPrice calculado: {}", averageExitPrice);
        
        // Calcular P&L total
        BigDecimal totalProfitLoss = validSellOperations.stream()
            .map(Operation::getProfitLoss)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("🔧 DEBUG: totalProfitLoss calculado: {}", totalProfitLoss);
        
        // Calcular valor total de entrada
        BigDecimal totalEntryValue = validSellOperations.stream()
            .map(Operation::getEntryTotalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("🔧 DEBUG: totalEntryValue calculado: {}", totalEntryValue);
        
        // Calcular percentual de P&L
        BigDecimal profitLossPercentage = totalEntryValue.compareTo(BigDecimal.ZERO) > 0 ?
            totalProfitLoss.divide(totalEntryValue, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
            BigDecimal.ZERO;
        
        log.info("🔧 DEBUG: profitLossPercentage calculado: {}", profitLossPercentage);
        
        // Determinar status baseado no P&L
        OperationStatus status = totalProfitLoss.compareTo(BigDecimal.ZERO) >= 0 ? 
            OperationStatus.WINNER : OperationStatus.LOSER;
        
        // Criar operação consolidada usando o padrão builder
        Operation consolidatedExit = Operation.builder()
            .optionSeries(exitOperation.getOptionSeries())
            .brokerage(exitOperation.getBrokerage())
            .analysisHouse(exitOperation.getAnalysisHouse())
            .user(exitOperation.getUser())
            .quantity(totalExitQuantity)
            .entryUnitPrice(totalEntryValue.divide(BigDecimal.valueOf(totalExitQuantity), 6, RoundingMode.HALF_UP))
            .exitUnitPrice(averageExitPrice)
            .entryDate(entryDate)
            .exitDate(exitDate)
            .transactionType(TransactionType.SELL)
            .tradeType(exitOperation.getTradeType())
            .status(status)
            .entryTotalValue(totalEntryValue)
            .exitTotalValue(totalExitValue)
            .profitLoss(totalProfitLoss)
            .profitLossPercentage(profitLossPercentage)
            .build();
        
        // Valores já foram definidos no builder, não precisamos redefini-los
        
        operationRepository.save(consolidatedExit);
        
        // Adicionar ao grupo como CONSOLIDATED_RESULT
        addOperationToGroup(consolidatedExit, group, OperationRoleType.CONSOLIDATED_RESULT);
        
        log.info("🔧 Operação consolidadora criada com ID: {} e exitDate: {}", consolidatedExit.getId(), exitDate);
        
        return consolidatedExit;
    }

    /**
     * Busca operação consolidadora de entrada no grupo
     */
    public Operation findConsolidatedEntry(AverageOperationGroup group) {
        return findExistingConsolidatedEntry(group).orElse(null);
    }
    /**
     * Busca operação consolidadora de saída no grupo
     */
    public Operation findConsolidatedExit(AverageOperationGroup group) {
        if (group == null || group.getItems() == null) {
            return null;
        }
        return group.getItems().stream()
                .filter(item -> item.getRoleType() == OperationRoleType.CONSOLIDATED_RESULT)
                .map(AverageOperationItem::getOperation)
                .findFirst()
                .orElse(null);
    }

    /**
     * Marca operação original como HIDDEN
     */
    @Transactional
    public void markOperationAsHidden(Operation operation) {
        log.debug("Marcando operação {} como HIDDEN", operation.getId());
        operation.setStatus(OperationStatus.HIDDEN);
        operationRepository.save(operation);
    }

    /**
     * Atualiza valores de uma operação (P&L e percentual)
     */
    @Transactional
    public void updateOperationValues(Operation operation, BigDecimal profitLoss, BigDecimal profitLossPercentage) {
        if (operation == null) {
            throw new IllegalArgumentException("Operação não pode ser nula");
        }
        log.debug("Atualizando valores da operação {}: P&L={}, Percentual={}%", 
                operation.getId(), profitLoss, profitLossPercentage);
        operation.setProfitLoss(profitLoss);
        operation.setProfitLossPercentage(profitLossPercentage);
        operationRepository.save(operation);
    }
    /**
     * Busca operação CONSOLIDATED_RESULT existente no grupo
     */
    public Optional<Operation> findExistingConsolidatedResult(AverageOperationGroup group) {
        return itemRepository.findByGroupAndRoleType(group, OperationRoleType.CONSOLIDATED_RESULT)
                .stream()
                .findFirst()
                .map(AverageOperationItem::getOperation);
    }

    /**
     * Busca operação CONSOLIDATED_ENTRY existente no grupo
     */
    public Optional<Operation> findExistingConsolidatedEntry(AverageOperationGroup group) {
        return itemRepository.findByGroupAndRoleType(group, OperationRoleType.CONSOLIDATED_ENTRY)
                .stream()
                .findFirst()
                .map(AverageOperationItem::getOperation);
    }

    /**
     * Atualiza operação CONSOLIDATED_RESULT existente com novos dados de saída
     */
    @Transactional
    public Operation updateConsolidatedResult(Operation consolidatedResult, Operation newExitOperation, AverageOperationGroup group) {
        log.info("🔧 Atualizando operação CONSOLIDATED_RESULT existente: {}", consolidatedResult.getId());
        log.info("🔧 ANTES - Consolidated: entryTotal={}, profitLoss={}, quantity={}", 
                consolidatedResult.getEntryTotalValue(), consolidatedResult.getProfitLoss(), consolidatedResult.getQuantity());
        log.info("🔧 NOVA OPERAÇÃO - entryTotal={}, profitLoss={}, quantity={}", 
                newExitOperation.getEntryTotalValue(), newExitOperation.getProfitLoss(), newExitOperation.getQuantity());
        
        BigDecimal newTotalProfitLoss = consolidatedResult.getProfitLoss().add(newExitOperation.getProfitLoss());
        int newTotalQuantity = consolidatedResult.getQuantity() + newExitOperation.getQuantity();
        BigDecimal newTotalExitValue = consolidatedResult.getExitTotalValue().add(newExitOperation.getExitTotalValue());
        
        // 🔧 CORREÇÃO: Calcular novo entryTotalValue consolidado
        BigDecimal newTotalEntryValue = consolidatedResult.getEntryTotalValue().add(newExitOperation.getEntryTotalValue());
        log.info("🔧 CALCULADO - newTotalEntryValue={}, newTotalProfitLoss={}, newTotalQuantity={}", 
                newTotalEntryValue, newTotalProfitLoss, newTotalQuantity);
        
        // 🔧 CORREÇÃO: Usar entryTotalValue consolidado para calcular percentual
        BigDecimal newProfitLossPercentage = BigDecimal.ZERO;
        if (newTotalEntryValue.compareTo(BigDecimal.ZERO) > 0) {
            newProfitLossPercentage = newTotalProfitLoss
                    .divide(newTotalEntryValue, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        BigDecimal newAverageExitPrice = averagePriceCalculator.calculateAverageExitPrice(
                newTotalExitValue, newTotalQuantity);
        
        log.info("🔧 PERCENTUAL CALCULADO: newProfitLossPercentage={}", newProfitLossPercentage);
        
        // ✅ CORREÇÃO: Usar exitDate da nova operação como fallback se não encontrar no grupo
        LocalDate latestExitDate = findLatestExitDateInGroup(group);
        if (latestExitDate == null) {
            latestExitDate = newExitOperation.getExitDate();
            log.warn("⚠️ LatestExitDate não encontrada no grupo, usando da nova operação: {}", latestExitDate);
        }
        
        consolidatedResult.setProfitLoss(newTotalProfitLoss);
        consolidatedResult.setProfitLossPercentage(newProfitLossPercentage);
        consolidatedResult.setQuantity(newTotalQuantity);
        consolidatedResult.setExitTotalValue(newTotalExitValue);
        // 🔧 CORREÇÃO: Atualizar entryTotalValue consolidado
        consolidatedResult.setEntryTotalValue(newTotalEntryValue);
        consolidatedResult.setExitUnitPrice(newAverageExitPrice);
        consolidatedResult.setExitDate(latestExitDate);
        
        log.info("🔧 FINAL - ANTES DO SAVE: entryTotal={}, profitLoss={}, percentage={}, quantity={}", 
                consolidatedResult.getEntryTotalValue(), consolidatedResult.getProfitLoss(), 
                consolidatedResult.getProfitLossPercentage(), consolidatedResult.getQuantity());
        
        return operationRepository.save(consolidatedResult);
    }
    /**
     * Atualiza operação consolidadora de entrada com novo preço médio
     */
    @Transactional
    public void updateConsolidatedEntry(Operation consolidatedEntry, BigDecimal newAveragePrice, 
                                       Integer newQuantity, BigDecimal newTotalValue) {
        log.info("Atualizando operação consolidadora de entrada: {}", consolidatedEntry.getId());
        
        consolidatedEntry.setEntryUnitPrice(newAveragePrice);
        consolidatedEntry.setQuantity(newQuantity);
        consolidatedEntry.setEntryTotalValue(newTotalValue);
        
        // ✅ CORREÇÃO: CONSOLIDATED_ENTRY deve permanecer ACTIVE até saída total
        // Só será marcada como HIDDEN quando uma operação TOTAL_EXIT for processada
        consolidatedEntry.setStatus(OperationStatus.ACTIVE);
        
        operationRepository.save(consolidatedEntry);
    }

    /**
     * Atualiza operação consolidadora de saída com novos totais
     */
    @Transactional
    public void updateConsolidatedExit(Operation consolidatedExit, BigDecimal additionalProfitLoss,
                                      Integer additionalQuantity, LocalDate newExitDate, BigDecimal newExitUnitPrice,
                                      BigDecimal additionalEntryValue) {
        log.info("🔧 Atualizando operação consolidadora de saída: {}", consolidatedExit.getId());
        log.info("🔧 ANTES - Consolidated: entryTotal={}, profitLoss={}, quantity={}", 
                consolidatedExit.getEntryTotalValue(), consolidatedExit.getProfitLoss(), consolidatedExit.getQuantity());
        log.info("🔧 ADICIONANDO - profitLoss={}, quantity={}, exitUnitPrice={}, entryValue={}", 
                additionalProfitLoss, additionalQuantity, newExitUnitPrice, additionalEntryValue);
        
        BigDecimal newTotalProfitLoss = consolidatedExit.getProfitLoss().add(additionalProfitLoss);
        Integer newTotalQuantity = consolidatedExit.getQuantity() + additionalQuantity;
        BigDecimal newTotalExitValue = consolidatedExit.getExitTotalValue().add(
                newExitUnitPrice.multiply(BigDecimal.valueOf(additionalQuantity)));

        // 🔧 CORREÇÃO: Usar entryTotalValue correto passado como parâmetro
        BigDecimal newTotalEntryValue = consolidatedExit.getEntryTotalValue().add(additionalEntryValue);
        
        log.info("🔧 CALCULADO - newTotalEntryValue={}, newTotalProfitLoss={}", 
                newTotalEntryValue, newTotalProfitLoss);

        // 🔧 CORREÇÃO: Usar entryTotalValue consolidado para calcular percentual
        BigDecimal newProfitLossPercentage = BigDecimal.ZERO;
        if (newTotalEntryValue.compareTo(BigDecimal.ZERO) > 0) {
            newProfitLossPercentage = newTotalProfitLoss
                    .divide(newTotalEntryValue, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        BigDecimal newAverageExitPrice = averagePriceCalculator.calculateAverageExitPrice(
                newTotalExitValue, newTotalQuantity);

        log.info("🔧 PERCENTUAL CALCULADO: newProfitLossPercentage={}", newProfitLossPercentage);

        consolidatedExit.setProfitLoss(newTotalProfitLoss);
        consolidatedExit.setProfitLossPercentage(newProfitLossPercentage);
        consolidatedExit.setQuantity(newTotalQuantity);
        consolidatedExit.setExitTotalValue(newTotalExitValue);
        // 🔧 CORREÇÃO: Atualizar entryTotalValue consolidado
        consolidatedExit.setEntryTotalValue(newTotalEntryValue);
        consolidatedExit.setExitUnitPrice(newAverageExitPrice);
        consolidatedExit.setExitDate(newExitDate);
        consolidatedExit.setStatus(newTotalProfitLoss.compareTo(BigDecimal.ZERO) > 0 ?
                OperationStatus.WINNER : OperationStatus.LOSER);

        log.info("🔧 FINAL - ANTES DO SAVE: entryTotal={}, profitLoss={}, percentage={}, quantity={}", 
                consolidatedExit.getEntryTotalValue(), consolidatedExit.getProfitLoss(), 
                consolidatedExit.getProfitLossPercentage(), consolidatedExit.getQuantity());

        operationRepository.save(consolidatedExit);
    }
    /**
     * Transforma operação CONSOLIDATED_RESULT em TOTAL_EXIT (saída final)
     */
    @Transactional
    public Operation transformToTotalExit(Operation consolidatedResult, AverageOperationGroup group) {
        log.info("Transformando CONSOLIDATED_RESULT em TOTAL_EXIT: {}", consolidatedResult.getId());
        
        // ✅ CORREÇÃO: Recalcular quantidade total correta baseada em TODAS as operações de saída do grupo
        int totalQuantityFromAllExits = calculateTotalExitQuantityFromGroup(group);
        log.info("Quantidade total de TODAS as saídas calculada para TOTAL_EXIT: {}", totalQuantityFromAllExits);
        
        // ✅ CORREÇÃO: Recalcular valores totais baseados em TODAS as operações de saída
        ExitCalculationResult exitTotals = calculateTotalExitValuesFromGroup(group);
        
        // ✅ CORREÇÃO: Atualizar operação com valores corretos de TODAS as saídas
        consolidatedResult.setQuantity(totalQuantityFromAllExits);
        consolidatedResult.setExitTotalValue(exitTotals.totalExitValue);
        consolidatedResult.setEntryTotalValue(exitTotals.totalEntryValue);
        consolidatedResult.setProfitLoss(exitTotals.totalProfitLoss);
        
        // Recalcular percentual baseado nos novos valores
        BigDecimal newProfitLossPercentage = exitTotals.totalEntryValue.compareTo(BigDecimal.ZERO) > 0 ?
                exitTotals.totalProfitLoss.divide(exitTotals.totalEntryValue, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                BigDecimal.ZERO;
        consolidatedResult.setProfitLossPercentage(newProfitLossPercentage);
        
        // Recalcular preço médio de saída
        BigDecimal newExitUnitPrice = totalQuantityFromAllExits > 0 ?
                exitTotals.totalExitValue.divide(BigDecimal.valueOf(totalQuantityFromAllExits), 6, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
        consolidatedResult.setExitUnitPrice(newExitUnitPrice);
        
        log.info("✅ CONSOLIDATED_RESULT recalculada: quantity={}, exitValue={}, entryValue={}, profitLoss={}, percentage={}%",
                totalQuantityFromAllExits, exitTotals.totalExitValue, exitTotals.totalEntryValue, exitTotals.totalProfitLoss, newProfitLossPercentage);
        
        if (consolidatedResult.getProfitLoss().compareTo(BigDecimal.ZERO) > 0) {
            consolidatedResult.setStatus(OperationStatus.WINNER);
        } else if (consolidatedResult.getProfitLoss().compareTo(BigDecimal.ZERO) < 0) {
            consolidatedResult.setStatus(OperationStatus.LOSER);  
        } else {
            consolidatedResult.setStatus(OperationStatus.WINNER);
        }
        
        Operation savedTotalExit = operationRepository.save(consolidatedResult);
        
        Optional<AverageOperationItem> itemOpt = itemRepository.findByGroupAndRoleType(group, OperationRoleType.CONSOLIDATED_RESULT)
                .stream().findFirst();
                
        if (itemOpt.isPresent()) {
            AverageOperationItem item = itemOpt.get();
            item.setRoleType(OperationRoleType.TOTAL_EXIT);
            itemRepository.save(item);
            log.info("Role type atualizado: CONSOLIDATED_RESULT → TOTAL_EXIT");
        }
        
        // ✅ CORREÇÃO: Marcar CONSOLIDATED_ENTRY como HIDDEN quando há saída total
        markConsolidatedEntryAsHidden(group);
        
        return savedTotalExit;
    }
    
    /**
     * ✅ NOVO MÉTODO: Marcar CONSOLIDATED_ENTRY como HIDDEN quando há saída total
     */
    @Transactional
    public void markConsolidatedEntryAsHidden(AverageOperationGroup group) {
        log.info("🔧 INICIANDO markConsolidatedEntryAsHidden para grupo: {}", group.getId());
        
        Optional<Operation> consolidatedEntryOpt = findExistingConsolidatedEntry(group);
        if (consolidatedEntryOpt.isPresent()) {
            Operation consolidatedEntry = consolidatedEntryOpt.get();
            log.info("🔧 ANTES de marcar como HIDDEN: ID={}, Status={}", 
                consolidatedEntry.getId(), consolidatedEntry.getStatus());
            
            consolidatedEntry.setStatus(OperationStatus.HIDDEN);
            Operation saved = operationRepository.save(consolidatedEntry);
            
            log.info("🔧 APÓS save: ID={}, Status={}", 
                saved.getId(), saved.getStatus());
            
            // ✅ VERIFICAÇÃO ADICIONAL: Forçar flush e refresh para garantir persistência
            operationRepository.flush();
            Operation refreshed = operationRepository.findById(saved.getId()).orElse(null);
            if (refreshed != null) {
                log.info("🔧 VERIFICAÇÃO FINAL: ID={}, Status={}", 
                    refreshed.getId(), refreshed.getStatus());
            } else {
                log.error("🔧 ERRO: Operação não encontrada após save e refresh!");
            }
            
            log.info("✅ CONSOLIDATED_ENTRY marcada como HIDDEN com sucesso: {}", consolidatedEntry.getId());
        } else {
            log.warn("❌ CONSOLIDATED_ENTRY não encontrada para marcar como HIDDEN no grupo: {}", group.getId());
        }
    }
    
    /**
     * ✅ NOVO MÉTODO: Calcular quantidade total correta para operação WINNER
     */
    private int calculateTotalQuantityFromGroup(AverageOperationGroup group) {
        log.info("Calculando quantidade total do grupo: {}", group.getId());
        
        if (group.getItems() == null) {
            log.warn("Items é null para o grupo: {}", group.getId());
            return 0;
        }
        
        int totalQuantity = group.getItems().stream()
            .map(AverageOperationItem::getOperation)
            .filter(op -> op.getTransactionType() == TransactionType.BUY &&
                         (op.getStatus() == OperationStatus.ACTIVE || op.getStatus() == OperationStatus.HIDDEN))
            .mapToInt(Operation::getQuantity)
            .sum();
        
        log.info("Quantidade total calculada: {}", totalQuantity);
        return totalQuantity;
    }

    /**
     * Verifica se há operações consolidadas para uma combinação específica
     */
    public boolean hasConsolidatedOperations(Object user, Object optionSeries, Object brokerage) {
        List<AverageOperationItem> consolidatedItems = itemRepository.findAll().stream()
                .filter(item -> item.getRoleType() == OperationRoleType.CONSOLIDATED_ENTRY ||
                              item.getRoleType() == OperationRoleType.CONSOLIDATED_RESULT)
                .filter(item -> item.getOperation().getStatus() != OperationStatus.HIDDEN)
                .filter(item -> item.getOperation().getUser().equals(user))
                .filter(item -> item.getOperation().getOptionSeries().equals(optionSeries))
                .filter(item -> item.getOperation().getBrokerage().equals(brokerage))
                .toList();
                
        return !consolidatedItems.isEmpty();
    }
    /**
     * Adiciona operação ao grupo com role type específico
     */
    private void addOperationToGroup(Operation operation, AverageOperationGroup group, OperationRoleType roleType) {
        // ✅ CORREÇÃO: Verificar se items não é null antes de usar size()
        int nextSequence = 1;
        if (group.getItems() != null) {
            nextSequence = group.getItems().size() + 1;
        }

        AverageOperationItem item = AverageOperationItem.builder()
                .group(group)
                .operation(operation)
                .roleType(roleType)
                .sequenceNumber(nextSequence)
                .inclusionDate(LocalDate.now())
                .build();

        itemRepository.save(item);
    }

    /**
     * ✅ CORREÇÃO: Encontra a primeira data de entrada no grupo
     * Resolve o bug de datas incorretas em operações consolidadas
     */
    private LocalDate findFirstEntryDateInGroup(AverageOperationGroup group) {
        log.info("=== BUSCANDO PRIMEIRA DATA DE ENTRADA NO GRUPO {} ===", group.getId());
        
        // ✅ CORREÇÃO: Verificar se items não é null antes de usar stream()
        if (group.getItems() == null) {
            log.warn("⚠️ Items é null para o grupo: {}", group.getId());
            return null;
        }
        
        LocalDate firstEntryDate = group.getItems().stream()
            .map(AverageOperationItem::getOperation)
            .filter(op -> op.getEntryDate() != null)
            .map(Operation::getEntryDate)
            .min(LocalDate::compareTo)
            .orElse(null);
        
        log.info("Primeira data de entrada encontrada: {}", firstEntryDate);
        return firstEntryDate;
    }
    
    /**
     * ✅ CORREÇÃO: Encontra a última data de saída no grupo
     * Resolve o bug de datas incorretas em operações consolidadas
     */
    private LocalDate findLatestExitDateInGroup(AverageOperationGroup group) {
        log.info("=== BUSCANDO ÚLTIMA DATA DE SAÍDA NO GRUPO {} ===", group.getId());
        
        // ✅ CORREÇÃO: Verificar se items não é null antes de usar stream()
        if (group.getItems() == null) {
            log.warn("⚠️ Items é null para o grupo: {}", group.getId());
            return null;
        }
        
        LocalDate latestExitDate = group.getItems().stream()
            .map(AverageOperationItem::getOperation)
            .filter(op -> op.getExitDate() != null)
            .map(Operation::getExitDate)
            .max(LocalDate::compareTo)
            .orElse(null);
        
        log.info("Última data de saída encontrada: {}", latestExitDate);
        return latestExitDate;
    }

    private String getOperationRoleType(Operation operation, List<AverageOperationItem> allItems) {
        return allItems.stream()
            .filter(item -> item.getOperation().getId().equals(operation.getId()))
            .map(item -> item.getRoleType().toString())
            .findFirst()
            .orElse("NOT_IN_GROUP");
    }
    
    /**
     * ✅ NOVO MÉTODO: Calcular quantidade total de TODAS as operações de saída no grupo
     */
    private int calculateTotalExitQuantityFromGroup(AverageOperationGroup group) {
        log.info("Calculando quantidade total de TODAS as operações de saída do grupo: {}", group.getId());
        
        if (group.getItems() == null) {
            log.warn("Items é null para o grupo: {}", group.getId());
            return 0;
        }
        
        int totalExitQuantity = group.getItems().stream()
            .map(AverageOperationItem::getOperation)
            .filter(op -> op.getTransactionType() == TransactionType.SELL &&
                         (op.getStatus() == OperationStatus.ACTIVE || op.getStatus() == OperationStatus.HIDDEN))
            .mapToInt(Operation::getQuantity)
            .sum();
        
        log.info("Quantidade total de saídas calculada: {}", totalExitQuantity);
        return totalExitQuantity;
    }
    
    /**
     * ✅ NOVO MÉTODO: Calcular valores totais de TODAS as operações de saída no grupo
     */
    private ExitCalculationResult calculateTotalExitValuesFromGroup(AverageOperationGroup group) {
        log.info("Calculando valores totais de TODAS as operações de saída do grupo: {}", group.getId());
        
        if (group.getItems() == null) {
            log.warn("Items é null para o grupo: {}", group.getId());
            return new ExitCalculationResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        List<Operation> exitOperations = group.getItems().stream()
            .map(AverageOperationItem::getOperation)
            .filter(op -> op.getTransactionType() == TransactionType.SELL &&
                         (op.getStatus() == OperationStatus.ACTIVE || op.getStatus() == OperationStatus.HIDDEN))
            .collect(Collectors.toList());
        
        BigDecimal totalExitValue = exitOperations.stream()
            .map(Operation::getExitTotalValue)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalEntryValue = exitOperations.stream()
            .map(Operation::getEntryTotalValue)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalProfitLoss = exitOperations.stream()
            .map(Operation::getProfitLoss)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Valores totais calculados: exitValue={}, entryValue={}, profitLoss={}", 
                totalExitValue, totalEntryValue, totalProfitLoss);
        
        return new ExitCalculationResult(totalExitValue, totalEntryValue, totalProfitLoss);
    }
    
    /**
     * ✅ NOVA CLASSE: Resultado do cálculo de valores de saída
     */
    private static class ExitCalculationResult {
        final BigDecimal totalExitValue;
        final BigDecimal totalEntryValue;
        final BigDecimal totalProfitLoss;
        
        ExitCalculationResult(BigDecimal totalExitValue, BigDecimal totalEntryValue, BigDecimal totalProfitLoss) {
            this.totalExitValue = totalExitValue;
            this.totalEntryValue = totalEntryValue;
            this.totalProfitLoss = totalProfitLoss;
        }
    }
}
