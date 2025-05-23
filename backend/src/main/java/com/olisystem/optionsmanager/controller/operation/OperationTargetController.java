package com.olisystem.optionsmanager.controller.operation;

import com.olisystem.optionsmanager.dto.operation.OperationTargetResponseDto;
import com.olisystem.optionsmanager.mapper.operation.OperationTargetMapper;
import com.olisystem.optionsmanager.model.operation.OperationTarget;
import com.olisystem.optionsmanager.service.operation.target.OperationTargetService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OperationTargetController {

  @Autowired private OperationTargetService operationTargetService;

  /**
   * Endpoint para buscar todos os targets e stoploss de uma operação.
   *
   * @param operationId ID da operação
   * @return Lista de targets e stoploss da operação
   */
  @GetMapping("operations/{operationId}/targets")
  public ResponseEntity<List<OperationTargetResponseDto>> getOperationTargets(
      @PathVariable UUID operationId) {
    List<OperationTarget> targets = operationTargetService.findByOperationId(operationId);
    List<OperationTargetResponseDto> targetDtos =
        targets.stream().map(OperationTargetMapper::toDto).collect(Collectors.toList());
    return ResponseEntity.ok(targetDtos);
  }
}
