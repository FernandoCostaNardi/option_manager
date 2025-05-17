package com.olisystem.optionsmanager.service.position;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.exception.ResourceNotFoundException;

import com.olisystem.optionsmanager.model.operation.Operation;

import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.position.PositionOperationType;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.position.PositionOperationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para integração entre o sistema de posições e o sistema de operações existente. Permite
 * que o sistema de posições use o sistema de operações para suas funcionalidades.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PositionOperationIntegrationService {

  private final OperationRepository operationRepository;
  private final PositionOperationRepository positionOperationRepository;


  /** Processa a criação de uma operação e integra com o sistema de posições. */


  /** Atualiza a posição associada a uma operação, caso exista. */
  public void updatePositionAfterOperationUpdate(UUID operationId, OperationDataRequest request) {
    log.debug("Atualizando posição após modificação da operação: {}", operationId);

    // 1. Verificar se a operação está vinculada a uma posição
    Operation operation =
        operationRepository
            .findById(operationId)
            .orElseThrow(() -> new ResourceNotFoundException("Operação não encontrada"));

    positionOperationRepository
        .findByOperation(operation)
        .ifPresent(
            posOp -> {
              Position position = posOp.getPosition();

              // 2. Se for uma operação de entrada, atualizar a posição de acordo
              if (posOp.getType() == PositionOperationType.ENTRY) {
                // Atualizar lote correspondente e recalcular preço médio
                // Esta é uma simplificação; a implementação real seria mais complexa
                log.info("Atualizando lote de entrada para posição: {}", position.getId());
              }

              // 3. Se for uma operação de saída, a lógica seria mais complexa
              // e possivelmente não permitiria atualizações diretas
              else if (posOp.getType() == PositionOperationType.PARTIAL_EXIT
                  || posOp.getType() == PositionOperationType.FULL_EXIT) {
                log.warn("Tentativa de atualizar operação de saída: {}", operationId);
                // Talvez lançar uma exceção ou implementar uma lógica específica
              }
            });
  }


}
