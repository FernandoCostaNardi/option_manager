package com.olisystem.optionsmanager.service.operation.averageOperation;

import com.olisystem.optionsmanager.model.operation.*;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.repository.AverageOperationGroupRepository;
import com.olisystem.optionsmanager.repository.AverageOperationItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço especializado no gerenciamento de grupos de operações médias. Responsável por criar e
 * atualizar os grupos que representam operações parciais e seus resultados.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AverageOperationService {

  @Autowired
  private AverageOperationGroupRepository groupRepository;
  @Autowired
  private AverageOperationItemRepository itemRepository;



  /** Cria um novo grupo para uma operação inicial. */
  public AverageOperationGroup createGroupForNewPosition(Operation operation, Position position) {
    log.debug(
        "Criando grupo para nova posição: {} - Operação: {}", position.getId(), operation.getId());

    // Criar o grupo
    AverageOperationGroup group =
        AverageOperationGroup.builder()
            .creationDate(LocalDate.now())
            .status(AverageOperationGroupStatus.ACTIVE)
            .totalQuantity(operation.getQuantity())
            .remainingQuantity(operation.getQuantity())
            .closedQuantity(0)
            .totalProfit(BigDecimal.ZERO)
            .avgExitPrice(BigDecimal.ZERO)
            .positionId(position.getId())
            .build();

    AverageOperationGroup savedGroup = groupRepository.save(group);

    // Adicionar a operação original ao grupo
    AverageOperationItem item =
        AverageOperationItem.builder()
            .group(savedGroup)
            .operation(operation)
            .roleType(OperationRoleType.ORIGINAL)
            .sequenceNumber(1)
            .inclusionDate(LocalDate.now())
            .build();

    itemRepository.save(item);

    return savedGroup;
  }

  /** Processa uma saída parcial. */
  public AverageOperationGroup processParcialExit(
      Operation originalOperation,
      Operation partialExitOperation,
      Operation remainderOperation,
      Operation consolidatedResultOperation,
      Position position) {

    log.debug("Processando saída parcial - Posição: {}", position.getId());

    // Buscar grupo existente para a operação original
    AverageOperationItem originalItemOpt =
        itemRepository.findByOperation_IdAndRoleType(
            originalOperation.getId(), OperationRoleType.ORIGINAL);

    AverageOperationGroup group;

    if (originalItemOpt != null) {
      // Atualizar grupo existente
      group = originalItemOpt.getGroup();

      // Atualizar quantidades e status
      int exitQuantity = partialExitOperation.getQuantity();
      group.setRemainingQuantity(group.getRemainingQuantity() - exitQuantity);
      group.setClosedQuantity(group.getClosedQuantity() + exitQuantity);

      // Atualizar lucro e preço médio de saída
      //BigDecimal exitTotalValue = partialExitOperation.getExitTotalValue();
      BigDecimal exitProfit = partialExitOperation.getProfitLoss();

      if (group.getTotalProfit() == null) {
        group.setTotalProfit(exitProfit);
      } else {
        group.setTotalProfit(group.getTotalProfit().add(exitProfit));
      }

      // Calcular preço médio de saída ponderado pela quantidade
      if (group.getAvgExitPrice() == null
          || group.getAvgExitPrice().compareTo(BigDecimal.ZERO) == 0) {
        group.setAvgExitPrice(partialExitOperation.getExitUnitPrice());
      } else {
        BigDecimal prevClosedQty = BigDecimal.valueOf(group.getClosedQuantity() - exitQuantity);
        BigDecimal prevExitTotal = group.getAvgExitPrice().multiply(prevClosedQty);
        BigDecimal newExitTotal =
            partialExitOperation.getExitUnitPrice().multiply(BigDecimal.valueOf(exitQuantity));
        BigDecimal totalExitValue = prevExitTotal.add(newExitTotal);

        group.setAvgExitPrice(
            totalExitValue.divide(
                BigDecimal.valueOf(group.getClosedQuantity()), 6, RoundingMode.HALF_UP));
      }

      // Atualizar status do grupo
      if (group.getRemainingQuantity() == 0) {
        group.setStatus(AverageOperationGroupStatus.CLOSED);
      } else {
        group.setStatus(AverageOperationGroupStatus.PARTIALLY_CLOSED);
      }

      groupRepository.save(group);
    } else {
      // Criar novo grupo para esta saída parcial
      group =
          AverageOperationGroup.builder()
              .creationDate(LocalDate.now())
              .status(AverageOperationGroupStatus.PARTIALLY_CLOSED)
              .totalQuantity(originalOperation.getQuantity())
              .remainingQuantity(
                  originalOperation.getQuantity() - partialExitOperation.getQuantity())
              .closedQuantity(partialExitOperation.getQuantity())
              .totalProfit(partialExitOperation.getProfitLoss())
              .avgExitPrice(partialExitOperation.getExitUnitPrice())
              .positionId(position.getId())
              .build();

      group = groupRepository.save(group);

      // Adicionar item original
      AverageOperationItem originalItem =
          AverageOperationItem.builder()
              .group(group)
              .operation(originalOperation)
              .roleType(OperationRoleType.ORIGINAL)
              .sequenceNumber(1)
              .inclusionDate(LocalDate.now())
              .build();

      itemRepository.save(originalItem);
    }

    // Adicionar item para a saída parcial
    AverageOperationItem exitItem =
        AverageOperationItem.builder()
            .group(group)
            .operation(partialExitOperation)
            .roleType(OperationRoleType.PARTIAL_EXIT)
            .sequenceNumber(getNextSequenceNumber(group))
            .inclusionDate(LocalDate.now())
            .build();

    itemRepository.save(exitItem);

    // Adicionar item para a operação restante
    AverageOperationItem remainderItem =
        AverageOperationItem.builder()
            .group(group)
            .operation(remainderOperation)
            .roleType(OperationRoleType.CONSOLIDATED_ENTRY)
            .sequenceNumber(getNextSequenceNumber(group))
            .inclusionDate(LocalDate.now())
            .build();

    itemRepository.save(remainderItem);

    // Adicionar ou atualizar o item para o resultado consolidado
    updateConsolidatedResult(group, consolidatedResultOperation);

    return group;
  }

  /** Processa uma saída total. */
  public AverageOperationGroup processFullExit(
      Operation originalOperation,
      Operation exitOperation,
      Operation consolidatedResultOperation,
      Position position) {

    log.debug("Processando saída total - Posição: {}", position.getId());

    // Buscar grupo existente para a operação original
    AverageOperationItem originalItemOpt =
        itemRepository.findByOperation_IdAndRoleType(
            originalOperation.getId(), OperationRoleType.ORIGINAL);

    AverageOperationGroup group;

    if (originalItemOpt != null) {
      // Atualizar grupo existente
      group = originalItemOpt.getGroup();

      // Atualizar quantidades e status
      group.setRemainingQuantity(0);
      group.setClosedQuantity(group.getTotalQuantity());
      group.setStatus(AverageOperationGroupStatus.CLOSED);

      // Atualizar lucro e preço médio de saída
      if (group.getClosedQuantity() > exitOperation.getQuantity()) {
        // Já houve saídas parciais anteriores
        BigDecimal prevClosedQty =
            BigDecimal.valueOf(group.getClosedQuantity() - exitOperation.getQuantity());
        BigDecimal prevExitTotal = group.getAvgExitPrice().multiply(prevClosedQty);
        BigDecimal newExitTotal =
            exitOperation
                .getExitUnitPrice()
                .multiply(BigDecimal.valueOf(exitOperation.getQuantity()));
        BigDecimal totalExitValue = prevExitTotal.add(newExitTotal);

        group.setAvgExitPrice(
            totalExitValue.divide(
                BigDecimal.valueOf(group.getClosedQuantity()), 6, RoundingMode.HALF_UP));

        group.setTotalProfit(group.getTotalProfit().add(exitOperation.getProfitLoss()));
      } else {
        // Primeira e única saída
        group.setAvgExitPrice(exitOperation.getExitUnitPrice());
        group.setTotalProfit(exitOperation.getProfitLoss());
      }

      groupRepository.save(group);
    } else {
      // Criar novo grupo para esta saída total
      group =
          AverageOperationGroup.builder()
              .creationDate(LocalDate.now())
              .status(AverageOperationGroupStatus.CLOSED)
              .totalQuantity(originalOperation.getQuantity())
              .remainingQuantity(0)
              .closedQuantity(originalOperation.getQuantity())
              .totalProfit(exitOperation.getProfitLoss())
              .avgExitPrice(exitOperation.getExitUnitPrice())
              .positionId(position.getId())
              .build();

      group = groupRepository.save(group);

      // Adicionar item original
      AverageOperationItem originalItem =
          AverageOperationItem.builder()
              .group(group)
              .operation(originalOperation)
              .roleType(OperationRoleType.ORIGINAL)
              .sequenceNumber(1)
              .inclusionDate(LocalDate.now())
              .build();

      itemRepository.save(originalItem);
    }

    // Adicionar item para a saída total
    AverageOperationItem exitItem =
        AverageOperationItem.builder()
            .group(group)
            .operation(exitOperation)
            .roleType(OperationRoleType.TOTAL_EXIT)
            .sequenceNumber(getNextSequenceNumber(group))
            .inclusionDate(LocalDate.now())
            .build();

    itemRepository.save(exitItem);

    // Adicionar ou atualizar o item para o resultado consolidado
    updateConsolidatedResult(group, consolidatedResultOperation);

    return group;
  }

  /** Obtém todas as operações de saída parcial para uma operação original. */
  public List<Operation> getParcialExits(UUID originalOperationId) {
    AverageOperationItem originalItemOpt =
        itemRepository.findByOperation_IdAndRoleType(
            originalOperationId, OperationRoleType.ORIGINAL);

    if (originalItemOpt != null) {
      AverageOperationGroup group = originalItemOpt.getGroup();

      List<AverageOperationItem> parcialItems =
          itemRepository.findByGroupAndRoleType(group, OperationRoleType.NEW_ENTRY);

      return parcialItems.stream().map(AverageOperationItem::getOperation).toList();
    }

    return List.of();
  }

  /** Adiciona ou atualiza o item para o resultado consolidado. */
  private void updateConsolidatedResult(
      AverageOperationGroup group, Operation consolidatedOperation) {
    // Buscar item consolidado existente
    Optional<AverageOperationItem> consolidatedItemOpt =
        itemRepository.findByGroupAndRoleType(group, OperationRoleType.CONSOLIDATED_RESULT).stream()
            .findFirst();

    if (consolidatedItemOpt.isPresent()) {
      // Atualizar item existente
      AverageOperationItem consolidatedItem = consolidatedItemOpt.get();
      consolidatedItem.setOperation(consolidatedOperation);
      itemRepository.save(consolidatedItem);
    } else {
      // Criar novo item consolidado
      AverageOperationItem consolidatedItem =
          AverageOperationItem.builder()
              .group(group)
              .operation(consolidatedOperation)
              .roleType(OperationRoleType.CONSOLIDATED_RESULT)
              .sequenceNumber(getNextSequenceNumber(group))
              .inclusionDate(LocalDate.now())
              .build();

      itemRepository.save(consolidatedItem);
    }
  }

  /** Obtém o próximo número de sequência para um grupo. */
  private int getNextSequenceNumber(AverageOperationGroup group) {
    return itemRepository.findByGroup(group).size() + 1;
  }

  /** Adiciona um novo item ao grupo. */
  @Transactional
  public AverageOperationItem addNewItemGroup(AverageOperationGroup group, Operation operation, OperationRoleType roleType) {
    // 1. Criar um novo itemGroup
    AverageOperationItem itemGroup = new AverageOperationItem();
    itemGroup.setGroup(group);
    itemGroup.setOperation(operation);
    itemGroup.setInclusionDate(LocalDate.now());
    itemGroup.setRoleType(roleType);
    itemGroup.setSequenceNumber(group.getOperations().size() + 1);
    return itemRepository.save(itemGroup);
  }

  /**
   * Retorna o grupo que a operação pertence
   * @param operation
   * @return
   */
  public AverageOperationGroup getGroupByOperation(Operation operation) {
    AverageOperationItem item = itemRepository.findByOperation(operation);
    return item != null ? item.getGroup() : null;
  }


  /**
   * Atualiza o grupo de operações médias
   * @param operation
   * @param position
   * @return
   */
  public AverageOperationGroup updateAverageOperationGroup(AverageOperationGroup group, Position position) {
    group.setTotalQuantity(position.getTotalQuantity());
    group.setRemainingQuantity(position.getRemainingQuantity());

    groupRepository.save(group);

    return group;
}
}
