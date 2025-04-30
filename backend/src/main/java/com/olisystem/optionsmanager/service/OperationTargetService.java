package com.olisystem.optionsmanager.service;

import com.olisystem.optionsmanager.model.OperationTarget;
import com.olisystem.optionsmanager.model.TargetType;
import com.olisystem.optionsmanager.repository.OperationTargetRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationTargetService {

  private final OperationTargetRepository operationTargetRepository;

  /**
   * Busca todos os targets associados a uma operação específica.
   *
   * @param operationId ID da operação
   * @return Lista de targets da operação
   */
  public List<OperationTarget> findByOperationId(UUID operationId) {
    return operationTargetRepository.findAll().stream()
        .filter(target -> target.getOperation().getId().equals(operationId))
        .collect(Collectors.toList());
  }

  /**
   * Busca todos os targets de um tipo específico associados a uma operação.
   *
   * @param operationId ID da operação
   * @param type Tipo do target (TARGET ou STOPLOSS)
   * @return Lista de targets do tipo especificado para a operação
   */
  public List<OperationTarget> findByOperationIdAndType(UUID operationId, TargetType type) {
    return findByOperationId(operationId).stream()
        .filter(target -> target.getType() == type)
        .collect(Collectors.toList());
  }

  /**
   * Salva um target de operação.
   *
   * @param operationTarget Target a ser salvo
   * @return Target salvo
   */
  public OperationTarget save(OperationTarget operationTarget) {
    return operationTargetRepository.save(operationTarget);
  }

  /**
   * Remove um target pelo ID.
   *
   * @param id ID do target a ser removido
   */
  public void deleteById(UUID id) {
    operationTargetRepository.deleteById(id);
  }
}
