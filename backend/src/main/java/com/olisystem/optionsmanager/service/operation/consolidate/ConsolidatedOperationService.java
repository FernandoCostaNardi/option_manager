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
     *
     * @param originalEntry Operação de entrada original
     * @param group         Grupo de operações
     * @return Operação consolidadora de entrada criada
     */
    @Transactional
    public Operation createConsolidatedEntry(Operation originalEntry, AverageOperationGroup group) {

        log.info("Criando operação consolidadora de entrada baseada na operação: {}", originalEntry.getId());

        // Criar nova operação baseada na original
        Operation consolidatedEntry = Operation.builder()
                .optionSeries(originalEntry.getOptionSeries())
                .brokerage(originalEntry.getBrokerage())
                .analysisHouse(originalEntry.getAnalysisHouse())
                .transactionType(originalEntry.getTransactionType())
                .tradeType(originalEntry.getTradeType())
                .entryDate(originalEntry.getEntryDate())
                .exitDate(null) // Entrada consolidada não tem data de saída
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

        // Salvar operação consolidada
        Operation savedConsolidatedEntry = operationRepository.save(consolidatedEntry);

        // Adicionar ao grupo com roleType CONSOLIDATED_ENTRY
        addOperationToGroup(savedConsolidatedEntry, group, OperationRoleType.CONSOLIDATED_ENTRY);

        log.info("Operação consolidadora de entrada criada: {} (baseada em: {})",
                savedConsolidatedEntry.getId(), originalEntry.getId());

        return savedConsolidatedEntry;
    }

    /**
     * Cria operação consolidadora de saída na primeira saída parcial
     *
     * @param firstExitOperation Primeira operação de saída
     * @param group              Grupo de operações
     * @return Operação consolidadora de saída criada
     */
    @Transactional
    public Operation createConsolidatedExit(Operation firstExitOperation, AverageOperationGroup group) {

        log.info("Criando operação consolidadora de saída baseada na operação: {}", firstExitOperation.getId());

        // Log para debug
        log.info("Valores da operação de saída original: P&L={}, Percentual={}%",
                firstExitOperation.getProfitLoss(),
                firstExitOperation.getProfitLossPercentage());

        // Validar se os valores estão preenchidos
        if (firstExitOperation.getProfitLoss() == null ||
                firstExitOperation.getProfitLoss().compareTo(BigDecimal.ZERO) == 0) {
            log.warn("ATENÇÃO: Operação de saída com profitLoss zerado! ID: {}",
                    firstExitOperation.getId());
        }

        // Criar nova operação baseada na primeira saída
        Operation consolidatedExit = Operation.builder()
                .optionSeries(firstExitOperation.getOptionSeries())
                .brokerage(firstExitOperation.getBrokerage())
                .analysisHouse(firstExitOperation.getAnalysisHouse())
                .transactionType(firstExitOperation.getTransactionType())
                .tradeType(firstExitOperation.getTradeType())
                .entryDate(firstExitOperation.getEntryDate())
                .exitDate(firstExitOperation.getExitDate())
                .quantity(firstExitOperation.getQuantity())
                .entryUnitPrice(firstExitOperation.getEntryUnitPrice())
                .entryTotalValue(firstExitOperation.getEntryTotalValue())
                .exitUnitPrice(firstExitOperation.getExitUnitPrice())
                .exitTotalValue(firstExitOperation.getExitTotalValue())
                .profitLoss(firstExitOperation.getProfitLoss())
                .profitLossPercentage(firstExitOperation.getProfitLossPercentage())
                .status(firstExitOperation.getStatus()) // WINNER ou LOSER
                .user(firstExitOperation.getUser())
                .build();

        // Salvar operação consolidada
        Operation savedConsolidatedExit = operationRepository.save(consolidatedExit);

        // Log para confirmar valores salvos
        log.info("Operação consolidadora criada com P&L={}, Percentual={}%",
                savedConsolidatedExit.getProfitLoss(),
                savedConsolidatedExit.getProfitLossPercentage());

        // Adicionar ao grupo com roleType CONSOLIDATED_RESULT
        addOperationToGroup(savedConsolidatedExit, group, OperationRoleType.CONSOLIDATED_RESULT);

        log.info("Operação consolidadora de saída criada: {} (baseada em: {})",
                savedConsolidatedExit.getId(), firstExitOperation.getId());

        return savedConsolidatedExit;
    }

    /**
     * Atualiza operação consolidadora de entrada com novo preço médio
     *
     * @param consolidatedEntry Operação consolidadora de entrada
     * @param newAveragePrice   Novo preço médio calculado
     * @param newQuantity       Nova quantidade restante
     * @param newTotalValue     Novo valor total restante
     */
    @Transactional
    public void updateConsolidatedEntry(Operation consolidatedEntry,
                                        BigDecimal newAveragePrice,
                                        Integer newQuantity,
                                        BigDecimal newTotalValue) {

        log.info("Atualizando operação consolidadora de entrada: {} - Novo preço médio: {}, " +
                        "Nova quantidade: {}, Novo valor total: {}",
                consolidatedEntry.getId(), newAveragePrice, newQuantity, newTotalValue);

        // Atualizar dados da operação consolidada
        consolidatedEntry.setEntryUnitPrice(newAveragePrice);
        consolidatedEntry.setQuantity(newQuantity);
        consolidatedEntry.setEntryTotalValue(newTotalValue);

        // Manter status ACTIVE enquanto há quantidade restante
        if (newQuantity > 0) {
            consolidatedEntry.setStatus(OperationStatus.ACTIVE);
        } else {
            // Se não há mais quantidade, a operação está finalizada
            consolidatedEntry.setStatus(OperationStatus.HIDDEN);
        }

        operationRepository.save(consolidatedEntry);

        log.debug("Operação consolidadora de entrada atualizada com sucesso");
    }

    /**
     * Atualiza operação consolidadora de saída com novos totais
     *
     * @param consolidatedExit     Operação consolidadora de saída
     * @param additionalProfitLoss Lucro/prejuízo adicional da nova saída
     * @param additionalQuantity   Quantidade adicional vendida
     * @param newExitDate          Nova data de saída (mais recente)
     * @param newExitUnitPrice     Novo preço unitário médio de saída
     */
    @Transactional
    public void updateConsolidatedExit(Operation consolidatedExit,
                                       BigDecimal additionalProfitLoss,
                                       Integer additionalQuantity,
                                       LocalDate newExitDate,
                                       BigDecimal newExitUnitPrice) {

        log.info("Atualizando operação consolidadora de saída: {} - P&L adicional: {}, " +
                        "Quantidade adicional: {}",
                consolidatedExit.getId(), additionalProfitLoss, additionalQuantity);

        // Acumular totais
        BigDecimal newTotalProfitLoss = consolidatedExit.getProfitLoss().add(additionalProfitLoss);
        Integer newTotalQuantity = consolidatedExit.getQuantity() + additionalQuantity;
        BigDecimal newTotalExitValue = consolidatedExit.getExitTotalValue().add(
                newExitUnitPrice.multiply(BigDecimal.valueOf(additionalQuantity)));

        // Calcular novo percentual baseado no valor total de entrada
        BigDecimal newProfitLossPercentage = BigDecimal.ZERO;
        if (consolidatedExit.getEntryTotalValue().compareTo(BigDecimal.ZERO) > 0) {
            newProfitLossPercentage = newTotalProfitLoss
                    .divide(consolidatedExit.getEntryTotalValue(), 6, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Calcular preço médio de saída
        BigDecimal newAverageExitPrice = averagePriceCalculator.calculateAverageExitPrice(
                newTotalExitValue, newTotalQuantity);

        // Atualizar operação consolidada
        consolidatedExit.setProfitLoss(newTotalProfitLoss);
        consolidatedExit.setProfitLossPercentage(newProfitLossPercentage);
        consolidatedExit.setQuantity(newTotalQuantity);
        consolidatedExit.setExitTotalValue(newTotalExitValue);
        consolidatedExit.setExitUnitPrice(newAverageExitPrice);
        consolidatedExit.setExitDate(newExitDate); // Data da saída mais recente

        // Atualizar status baseado no resultado
        consolidatedExit.setStatus(newTotalProfitLoss.compareTo(BigDecimal.ZERO) >= 0 ?
                OperationStatus.WINNER : OperationStatus.LOSER);

        operationRepository.save(consolidatedExit);

        log.info("Operação consolidadora de saída atualizada: P&L total: {}, Percentual: {}%, " +
                        "Quantidade total: {}, Preço médio saída: {}",
                newTotalProfitLoss, newProfitLossPercentage, newTotalQuantity, newAverageExitPrice);
    }

    /**
     * Busca operação consolidadora de entrada no grupo
     *
     * @param group Grupo de operações
     * @return Operação consolidadora de entrada ou null se não encontrada
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
     *
     * @param group Grupo de operações
     * @return Operação consolidadora de saída ou null se não encontrada
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
     *
     * @param operation Operação a ser marcada como HIDDEN
     */
    @Transactional
    public void markOperationAsHidden(Operation operation) {

        log.debug("Marcando operação {} como HIDDEN", operation.getId());

        operation.setStatus(OperationStatus.HIDDEN);
        operationRepository.save(operation);
    }

    /**
     * Adiciona operação ao grupo com role type específico
     */
    private void addOperationToGroup(Operation operation, AverageOperationGroup group, OperationRoleType roleType) {

        // Calcular próximo número de sequência
        int nextSequence = group.getItems().size() + 1;

        AverageOperationItem item = AverageOperationItem.builder()
                .group(group)
                .operation(operation)
                .roleType(roleType)
                .sequenceNumber(nextSequence)
                .inclusionDate(LocalDate.now())
                .build();

        groupItemRepository.save(item);

        log.debug("Operação {} adicionada ao grupo {} com roleType: {}",
                operation.getId(), group.getId(), roleType);
    }

    /**
     * Verifica se uma operação é consolidadora
     *
     * @param operation Operação a ser verificada
     * @param group     Grupo de operações
     * @return true se é consolidadora
     */
    public boolean isConsolidatedOperation(Operation operation, AverageOperationGroup group) {

        if (operation == null || group == null || group.getItems() == null) {
            return false;
        }

        return group.getItems().stream()
                .anyMatch(item -> item.getOperation().getId().equals(operation.getId()) &&
                        (item.getRoleType() == OperationRoleType.CONSOLIDATED_ENTRY ||
                                item.getRoleType() == OperationRoleType.CONSOLIDATED_RESULT));
    }

    /**
     * Busca operação CONSOLIDATED_RESULT existente no grupo
     * @param group Grupo de operações
     * @return Optional da operação consolidada de saída existente
     */
    public Optional<Operation> findExistingConsolidatedResult(AverageOperationGroup group) {
        return groupItemRepository.findByGroupAndRoleType(group, OperationRoleType.CONSOLIDATED_RESULT)
                .stream()
                .findFirst()
                .map(AverageOperationItem::getOperation);
    }

    /**
     * Atualiza operação CONSOLIDATED_RESULT existente com novos dados de saída
     * @param consolidatedResult Operação consolidada existente
     * @param newExitOperation Nova operação de saída
     * @param group Grupo de operações
     * @return Operação consolidada atualizada
     */
    @Transactional
    public Operation updateConsolidatedResult(Operation consolidatedResult, Operation newExitOperation, AverageOperationGroup group) {
        log.info("Atualizando operação CONSOLIDATED_RESULT existente: {}", consolidatedResult.getId());
        
        // Somar valores da nova saída aos existentes
        BigDecimal newTotalProfitLoss = consolidatedResult.getProfitLoss().add(newExitOperation.getProfitLoss());
        int newTotalQuantity = consolidatedResult.getQuantity() + newExitOperation.getQuantity();
        BigDecimal newTotalExitValue = consolidatedResult.getExitTotalValue().add(newExitOperation.getExitTotalValue());
        
        // Calcular novo percentual baseado no valor total de entrada
        BigDecimal newProfitLossPercentage = BigDecimal.ZERO;
        if (consolidatedResult.getEntryTotalValue().compareTo(BigDecimal.ZERO) > 0) {
            newProfitLossPercentage = newTotalProfitLoss
                    .divide(consolidatedResult.getEntryTotalValue(), 6, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        // Calcular novo preço médio de saída
        BigDecimal newAverageExitPrice = averagePriceCalculator.calculateAverageExitPrice(
                newTotalExitValue, newTotalQuantity);
        
        // Atualizar operação consolidada
        consolidatedResult.setProfitLoss(newTotalProfitLoss);
        consolidatedResult.setProfitLossPercentage(newProfitLossPercentage);
        consolidatedResult.setQuantity(newTotalQuantity);
        consolidatedResult.setExitTotalValue(newTotalExitValue);
        consolidatedResult.setExitUnitPrice(newAverageExitPrice);
        consolidatedResult.setExitDate(newExitOperation.getExitDate()); // Data da saída mais recente
        
        Operation savedConsolidatedResult = operationRepository.save(consolidatedResult);
        
        log.info("CONSOLIDATED_RESULT atualizada: P&L total={}, Quantidade total={}, Valor saída total={}",
                newTotalProfitLoss, newTotalQuantity, newTotalExitValue);
                
        return savedConsolidatedResult;
    }

    /**
     * Transforma operação CONSOLIDATED_RESULT em TOTAL_EXIT (saída final)
     * @param consolidatedResult Operação consolidada de resultado
     * @param group Grupo de operações  
     * @return Operação transformada em TOTAL_EXIT
     */
    @Transactional
    public Operation transformToTotalExit(Operation consolidatedResult, AverageOperationGroup group) {
        log.info("Transformando CONSOLIDATED_RESULT em TOTAL_EXIT: {}", consolidatedResult.getId());
        
        // Atualizar status da operação para refletir resultado final
        if (consolidatedResult.getProfitLoss().compareTo(BigDecimal.ZERO) > 0) {
            consolidatedResult.setStatus(OperationStatus.WINNER);
        } else if (consolidatedResult.getProfitLoss().compareTo(BigDecimal.ZERO) < 0) {
            consolidatedResult.setStatus(OperationStatus.LOSER);  
        } else {
            consolidatedResult.setStatus(OperationStatus.NEUTRAl);
        }
        
        Operation savedTotalExit = operationRepository.save(consolidatedResult);
        
        // Atualizar role type no grupo para TOTAL_EXIT
        Optional<AverageOperationItem> itemOpt = groupItemRepository.findByGroupAndRoleType(group, OperationRoleType.CONSOLIDATED_RESULT)
                .stream().findFirst();
                
        if (itemOpt.isPresent()) {
            AverageOperationItem item = itemOpt.get();
            item.setRoleType(OperationRoleType.TOTAL_EXIT);
            groupItemRepository.save(item);
            log.info("Role type atualizado: CONSOLIDATED_RESULT → TOTAL_EXIT");
        }
        
        log.info("Operação transformada em TOTAL_EXIT com sucesso: {}", savedTotalExit.getId());
        return savedTotalExit;
    }

    /**
     * Verifica se há operações consolidadas para uma combinação específica
     * @param user Usuário
     * @param optionSeries Série de opção  
     * @param brokerage Corretora
     * @return true se há operações consolidadas
     */
    public boolean hasConsolidatedOperations(Object user, Object optionSeries, Object brokerage) {
        // Buscar por grupos que tenham operações CONSOLIDATED_ENTRY ou CONSOLIDATED_RESULT
        // Implementação simplificada: buscar por qualquer operação com status ACTIVE que não seja HIDDEN
        // em posições que tenham o mesmo usuário, série e corretora
        
        // Para agora, vou implementar uma verificação mais direta:
        // Verificar se há operações com roleType CONSOLIDATED_ENTRY ou CONSOLIDATED_RESULT
        
        List<AverageOperationItem> consolidatedItems = groupItemRepository.findAll().stream()
                .filter(item -> item.getRoleType() == OperationRoleType.CONSOLIDATED_ENTRY ||
                              item.getRoleType() == OperationRoleType.CONSOLIDATED_RESULT)
                .filter(item -> item.getOperation().getStatus() != OperationStatus.HIDDEN)
                .filter(item -> item.getOperation().getUser().equals(user))
                .filter(item -> item.getOperation().getOptionSeries().equals(optionSeries))
                .filter(item -> item.getOperation().getBrokerage().equals(brokerage))
                .toList();
                
        boolean hasConsolidated = !consolidatedItems.isEmpty();
        
        if (hasConsolidated) {
            log.info("Operações consolidadas encontradas: {} itens", consolidatedItems.size());
            consolidatedItems.forEach(item -> 
                log.debug("  - Operação consolidada: {} ({})", item.getOperation().getId(), item.getRoleType())
            );
        }
        
        return hasConsolidated;
    }

    /**
     * ✅ NOVO MÉTODO: Atualiza valores de uma operação (substitui uso direto do repository)
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
        
        Operation savedOperation = operationRepository.save(operation);
        
        log.debug("Operação {} atualizada com sucesso", savedOperation.getId());
    }
}
