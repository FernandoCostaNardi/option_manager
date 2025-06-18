package com.olisystem.optionsmanager.service.operation.averageOperation;

import com.olisystem.optionsmanager.dto.operation.OperationItemDto;
import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.mapper.operation.OperationItemMapper;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import com.olisystem.optionsmanager.repository.AverageOperationItemRepository;
import com.olisystem.optionsmanager.util.OperationSummaryCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço para consultas relacionadas a grupos de operações médias. Fornece métodos para recuperar
 * operações parciais, resultados consolidados e histórico completo de operações relacionadas.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AverageOperationQueryService {

  private final AverageOperationItemRepository itemRepository;
  private final OperationItemMapper operationItemMapper;

  /**
   * Obtém todas as operações de um grupo que tenham data de saída e não sejam consolidadas.
   *
   * @param groupId ID do grupo de operações
   * @return Lista de OperationSummaryResponseDto das operações que atendem aos critérios
   */
  public List<OperationSummaryResponseDto> getExitedNonConsolidatedOperations(String groupId) {
    log.debug("Buscando operações com saída não consolidadas para grupo: {}", groupId);

    try {
      UUID groupUuid = UUID.fromString(groupId);
      
      // Definir tipos consolidados a serem excluídos
      List<OperationRoleType> consolidatedTypes = Arrays.asList(
          OperationRoleType.CONSOLIDATED_ENTRY,
          OperationRoleType.CONSOLIDATED_RESULT
      );

      // Buscar operações que atendem aos critérios
      List<AverageOperationItem> items = itemRepository
          .findExitedNonConsolidatedOperationsByGroupId(groupUuid, consolidatedTypes);

      log.info("Encontradas {} operações com saída não consolidadas para grupo {}", items.size(), groupId);

      // Converter para DTOs e criar summaries individuais
      List<OperationSummaryResponseDto> summaries = items.stream()
          .map(item -> {
            Operation operation = item.getOperation();
            OperationItemDto dto = operationItemMapper.mapToDto(operation);
            
            // Adicionar informações específicas do grupo
            dto.setRoleType(item.getRoleType().getDescription());
            dto.setSequenceNumber(item.getSequenceNumber());
            
            // Criar um summary individual para cada operação
            List<OperationItemDto> singleOperationList = List.of(dto);
            
            return OperationSummaryCalculator.calculateSummary(
                singleOperationList, 
                0, // currentPage
                1, // totalPages 
                1L, // totalElements
                1  // pageSize
            );
          })
          .collect(Collectors.toList());

      return summaries;
      
    } catch (IllegalArgumentException e) {
      log.error("ID de grupo inválido: {}", groupId, e);
      return new ArrayList<>();
    } catch (Exception e) {
      log.error("Erro ao buscar operações do grupo {}: {}", groupId, e.getMessage(), e);
      return new ArrayList<>();
    }
  }

  /**
   * Obtém todas as operações parciais associadas a uma operação original.
   *
   * @param operationId ID da operação original
   * @return Lista de DTOs das operações parciais
   */
  public List<OperationItemDto> getPartialExits(String operationId) {
    log.debug("Buscando saídas parciais para operação: {}", operationId);

    // Buscar item original
    Optional<AverageOperationItem> originalItem =
        Optional.ofNullable(
            itemRepository.findByOperation_IdAndRoleType(
                UUID.fromString(operationId), OperationRoleType.ORIGINAL));

    if (originalItem.isEmpty()) {
      return new ArrayList<>();
    }

    // Obter grupo
//    AverageOperationGroup group = originalItem.get().getGroup();

    // Buscar itens parciais
//    List<AverageOperationItem> partialItems =
//        itemRepository.findByGroupAndRoleType(group, OperationRoleType.CONSOLIDATED_RESULT);

    // Converter para DTOs
    return null; /* partialItems.stream()
        .map(item -> operationService.mapToDto(item.getOperation()))
        .collect(Collectors.toList()); */
  }

  /**
   * Obtém o resultado consolidado para uma operação original.
   *
   * @param operationId ID da operação original
   * @return DTO do resultado consolidado, se existir
   */
  public Optional<OperationItemDto> getConsolidatedResult(String operationId) {
    log.debug("Buscando resultado consolidado para operação: {}", operationId);

    // Buscar item original
    Optional<AverageOperationItem> originalItem =
        Optional.ofNullable(
            itemRepository.findByOperation_IdAndRoleType(
                UUID.fromString(operationId), OperationRoleType.ORIGINAL));

    if (originalItem.isEmpty()) {
      return Optional.empty();
    }

    // Obter grupo
    AverageOperationGroup group = originalItem.get().getGroup();

    // Buscar item consolidado
    List<AverageOperationItem> consolidatedItems =
        itemRepository.findByGroupAndRoleType(group, OperationRoleType.CONSOLIDATED_RESULT);

    if (consolidatedItems.isEmpty()) {
      return Optional.empty();
    }

    // Converter para DTO
   /* OperationItemDto consolidatedDto =
        operationService.mapToDto(consolidatedItems.get(0).getOperation());*/
    return null; // Optional.of(consolidatedDto);
  }

  /**
   * Obtém todas as operações relacionadas a um grupo.
   *
   * @param groupId ID do grupo
   * @return Lista de DTOs de todas as operações relacionadas, organizadas por tipo
   */
  public List<OperationItemDto> getAllRelatedOperations(String groupId) {
    log.debug("Buscando todas as operações relacionadas para grupo: {}", groupId);

    // Buscar todos os itens do grupo
    List<AverageOperationItem> allItems = itemRepository.findByGroup_Id(UUID.fromString(groupId));

    // Ordená-los por tipo e sequência
    allItems.sort(
        Comparator.comparing(AverageOperationItem::getRoleType)
            .thenComparing(AverageOperationItem::getSequenceNumber));

    // Converter para DTOs
    return null; /** allItems.stream()
        .map(
            item -> {
              OperationItemDto dto = operationService.mapToDto(item.getOperation());
              dto.setRoleType(item.getRoleType().getDescription());
              dto.setSequenceNumber(item.getSequenceNumber());
              return dto;
            })
        .collect(Collectors.toList()); */
  }

//  /**
//   * Obtém o histórico completo de uma posição.
//   *
//   * @param positionId ID da posição
//   * @return Lista de DTOs de todas as operações relacionadas a esta posição
//   */
//  public List<OperationItemDto> getPositionHistory(String positionId) {
//    log.debug("Buscando histórico de posição: {}", positionId);
//
//    // Buscar todos os grupos associados a esta posição
//    AverageOperationGroup groups =
//        groupRepository.findByPositionId(UUID.fromString(positionId));
//
//    if (groups == null) {
//      return new ArrayList<>();
//    }
//
//    // Para cada grupo, buscar todos os itens
//    List<OperationItemDto> result = new ArrayList<>();
//    for (AverageOperationGroup group : groups) {
//      List<AverageOperationItem> items = itemRepository.findByGroup(group);
//
//      // Ordenar por tipo e sequência
//      items.sort(
//          Comparator.comparing(AverageOperationItem::getRoleType)
//              .thenComparing(AverageOperationItem::getSequenceNumber));
//
//      // Converter para DTOs
//      for (AverageOperationItem item : items) {
//        OperationItemDto dto = operationService.mapToDto(item.getOperation());
//        dto.setRoleType(item.getRoleType().getDescription());
//        dto.setSequenceNumber(item.getSequenceNumber());
//        result.add(dto);
//      }
//    }
//
//    return result;
//  }
}
