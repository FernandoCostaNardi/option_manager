package com.olisystem.optionsmanager.controller;

import com.olisystem.optionsmanager.dto.OperationTargetResponseDto;
import com.olisystem.optionsmanager.mapper.OperationTargetMapper;
import com.olisystem.optionsmanager.service.OperationTargetService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Log4j2
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
    List<OperationTargetResponseDto> targetDtos = operationTargetService.findByOperationId(operationId)
        .stream()
        .map(OperationTargetMapper::toDto)
        .collect(Collectors.toList());
    return ResponseEntity.ok(targetDtos);
  }
}
