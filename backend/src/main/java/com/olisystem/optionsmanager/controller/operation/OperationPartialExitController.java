package com.olisystem.optionsmanager.controller.operation;

import com.olisystem.optionsmanager.dto.operation.OperationItemDto;
import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationQueryService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para gerenciamento de operações parciais. Fornece endpoints para visualizar saídas
 * parciais, resultados consolidados e histórico completo de operações relacionadas.
 */
@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
@Slf4j
public class OperationPartialExitController {

  private final AverageOperationQueryService averageOperationQueryService;

  /** 
   * Retorna todas as operações de um grupo que tenham data de saída e não sejam consolidadas.
   */
  @GetMapping("/group/{groupId}/exited-operations")
  public ResponseEntity<List<OperationSummaryResponseDto>> getExitedNonConsolidatedOperations(
      @PathVariable String groupId) {
    log.debug("Requisição para obter operações com saída não consolidadas do grupo: {}", groupId);

    try {
      List<OperationSummaryResponseDto> operations = 
          averageOperationQueryService.getExitedNonConsolidatedOperations(groupId);
      
      if (operations.isEmpty()) {
        log.info("Nenhuma operação com saída não consolidada encontrada para grupo: {}", groupId);
        return ResponseEntity.ok(operations);
      }
      
      log.info("Retornando {} operações com saída não consolidadas para grupo: {}", 
               operations.size(), groupId);
      return ResponseEntity.ok(operations);
      
    } catch (Exception e) {
      log.error("Erro ao buscar operações com saída do grupo {}: {}", groupId, e.getMessage(), e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Retorna todas as saídas parciais para uma operação original. */
  @GetMapping("/{operationId}/partial-exits")
  public ResponseEntity<List<OperationItemDto>> getPartialExits(@PathVariable String operationId) {
    log.debug("Requisição para obter saídas parciais da operação: {}", operationId);

    List<OperationItemDto> partialExits = averageOperationQueryService.getPartialExits(operationId);
    return ResponseEntity.ok(partialExits);
  }

  /** Retorna o resultado consolidado para uma operação original. */
  @GetMapping("/{operationId}/consolidated-result")
  public ResponseEntity<OperationItemDto> getConsolidatedResult(@PathVariable String operationId) {
    log.debug("Requisição para obter resultado consolidado da operação: {}", operationId);

    Optional<OperationItemDto> result =
        averageOperationQueryService.getConsolidatedResult(operationId);
    return result.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  /** Retorna todas as operações relacionadas a um grupo. */
  @GetMapping("/group/{groupId}/operations")
  public ResponseEntity<List<OperationItemDto>> getGroupOperations(@PathVariable String groupId) {
    log.debug("Requisição para obter operações do grupo: {}", groupId);

    List<OperationItemDto> operations =
        averageOperationQueryService.getAllRelatedOperations(groupId);
    return ResponseEntity.ok(operations);
  }

  /** Retorna o histórico completo de operações para uma posição. */
  @GetMapping("/position/{positionId}/history")
  public ResponseEntity<List<OperationItemDto>> getPositionHistory(
      @PathVariable String positionId) {
    log.debug("Requisição para obter histórico da posição: {}", positionId);

    List<OperationItemDto> history =
        null; // averageOperationQueryService.getPositionHistory(positionId);
    return ResponseEntity.ok(history);
  }
}
