package com.olisystem.optionsmanager.controller.operation;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.dto.position.PositionDto;
import com.olisystem.optionsmanager.service.position.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para integração do sistema de operações com o sistema de posições. Fornece endpoints
 * para verificar posições compatíveis ao criar operações.
 */
@RestController
@RequestMapping("/api/operations/position")
@RequiredArgsConstructor
@Slf4j
public class OperationPositionController {

  private final PositionService positionService;

  /** Verifica se existe uma posição compatível para uma nova operação. */
  @PostMapping("/check-compatible")
  public ResponseEntity<PositionDto> checkCompatiblePosition(
      @RequestBody OperationDataRequest request) {
    log.debug("Verificando posição compatível para operação");

    PositionDto position =
        positionService.checkCompatiblePosition(
            request.getOptionSeriesCode(), request.getTransactionType());

    return ResponseEntity.ok(position);
  }
}
