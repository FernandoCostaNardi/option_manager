package com.olisystem.optionsmanager.service.operation.consolidate;

import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.repository.AverageOperationItemRepository;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.service.operation.average.AveragePriceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsolidatedOperationService {

    private final OperationRepository operationRepository;
    private final AverageOperationItemRepository groupItemRepository;
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
                .transactionType(originalEntry.getTransactionType())
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
    public Operation createConsolidatedExit(Operation firstExitOperation, AverageOperationGroup group) {
        log.info("🔧 Criando operação consolidadora de saída baseada na operação: {}", firstExitOperation.getId());
        log.info("🔧 PRIMEIRA OPERAÇÃO - entryTotal={}, profitLoss={}, quantity={}, percentage={}", 
                firstExitOperation.getEntryTotalValue(), firstExitOperation.getProfitLoss(), 
                firstExitOperation.getQuantity(), firstExitOperation.getProfitLossPercentage());

        // ✅ CORREÇÃO: Para primeira saída, usar a exitDate da própria operação sendo processada
        LocalDate exitDate = firstExitOperation.getExitDate();
        log.info("🔧 Usando exitDate da operação: {}", exitDate);

        Operation consolidatedExit = Operation.builder()
                .optionSeries(firstExitOperation.getOptionSeries())
                .brokerage(firstExitOperation.getBrokerage())
                .analysisHouse(firstExitOperation.getAnalysisHouse())
                .transactionType(firstExitOperation.getTransactionType())
                .tradeType(firstExitOperation.getTradeType())
                .entryDate(findFirstEntryDateInGroup(group))
                .exitDate(exitDate)  // ✅ Usar exitDate da operação atual
                .quantity(firstExitOperation.getQuantity())
                .entryUnitPrice(firstExitOperation.getEntryUnitPrice())
                .entryTotalValue(firstExitOperation.getEntryTotalValue())
                .exitUnitPrice(firstExitOperation.getExitUnitPrice())
                .exitTotalValue(firstExitOperation.getExitTotalValue())
                .profitLoss(firstExitOperation.getProfitLoss())
                .profitLossPercentage(firstExitOperation.getProfitLossPercentage())
                .status(firstExitOperation.getStatus())
                .user(firstExitOperation.getUser())
                .build();

        Operation savedConsolidatedExit = operationRepository.save(consolidatedExit);
        addOperationToGroup(savedConsolidatedExit, group, OperationRoleType.CONSOLIDATED_RESULT);

        log.info("🔧 Operação consolidadora criada com ID: {} e exitDate: {}", 
                savedConsolidatedExit.getId(), savedConsolidatedExit.getExitDate());
        
        return savedConsolidatedExit;
    }

    /**
     * Busca operação consolidadora de entrada no grupo
     */
    public Operation findConsolidatedEntry(AverageOperationGroup group) {
        if (group == null || group.getItems() == null) {
            return null;
        }
        return group.getItems().stream()
                .filter(item -> item.getRoleType() == OperationRoleType.CONSOLIDATED_ENTRY)
                .map(AverageOperationItem::getOperation)
                .findFirst()
                .orElse(null);
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
        return groupItemRepository.findByGroupAndRoleType(group, OperationRoleType.CONSOLIDATED_RESULT)
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
        
        consolidatedResult.setProfitLoss(newTotalProfitLoss);
        consolidatedResult.setProfitLossPercentage(newProfitLossPercentage);
        consolidatedResult.setQuantity(newTotalQuantity);
        consolidatedResult.setExitTotalValue(newTotalExitValue);
        // 🔧 CORREÇÃO: Atualizar entryTotalValue consolidado
        consolidatedResult.setEntryTotalValue(newTotalEntryValue);
        consolidatedResult.setExitUnitPrice(newAverageExitPrice);
        consolidatedResult.setExitDate(findLatestExitDateInGroup(group));
        
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
        
        if (newQuantity > 0) {
            consolidatedEntry.setStatus(OperationStatus.ACTIVE);
        } else {
            consolidatedEntry.setStatus(OperationStatus.HIDDEN);
        }
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
        consolidatedExit.setStatus(newTotalProfitLoss.compareTo(BigDecimal.ZERO) >= 0 ?
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
        
        if (consolidatedResult.getProfitLoss().compareTo(BigDecimal.ZERO) > 0) {
            consolidatedResult.setStatus(OperationStatus.WINNER);
        } else if (consolidatedResult.getProfitLoss().compareTo(BigDecimal.ZERO) < 0) {
            consolidatedResult.setStatus(OperationStatus.LOSER);  
        } else {
            consolidatedResult.setStatus(OperationStatus.WINNER);
        }
        
        Operation savedTotalExit = operationRepository.save(consolidatedResult);
        
        Optional<AverageOperationItem> itemOpt = groupItemRepository.findByGroupAndRoleType(group, OperationRoleType.CONSOLIDATED_RESULT)
                .stream().findFirst();
                
        if (itemOpt.isPresent()) {
            AverageOperationItem item = itemOpt.get();
            item.setRoleType(OperationRoleType.TOTAL_EXIT);
            groupItemRepository.save(item);
            log.info("Role type atualizado: CONSOLIDATED_RESULT → TOTAL_EXIT");
        }
        
        return savedTotalExit;
    }

    /**
     * Verifica se há operações consolidadas para uma combinação específica
     */
    public boolean hasConsolidatedOperations(Object user, Object optionSeries, Object brokerage) {
        List<AverageOperationItem> consolidatedItems = groupItemRepository.findAll().stream()
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
        int nextSequence = group.getItems().size() + 1;

        AverageOperationItem item = AverageOperationItem.builder()
                .group(group)
                .operation(operation)
                .roleType(roleType)
                .sequenceNumber(nextSequence)
                .inclusionDate(LocalDate.now())
                .build();

        groupItemRepository.save(item);
    }

    /**
     * ✅ CORREÇÃO: Encontra a primeira data de entrada no grupo
     * Resolve o bug de datas incorretas em operações consolidadas
     */
    private LocalDate findFirstEntryDateInGroup(AverageOperationGroup group) {
        log.info("=== BUSCANDO PRIMEIRA DATA DE ENTRADA NO GRUPO {} ===", group.getId());
        
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
        
        LocalDate latestExitDate = group.getItems().stream()
            .map(AverageOperationItem::getOperation)
            .filter(op -> op.getExitDate() != null)
            .map(Operation::getExitDate)
            .max(LocalDate::compareTo)
            .orElse(null);
        
        log.info("Última data de saída encontrada: {}", latestExitDate);
        return latestExitDate;
    }
}