package com.olisystem.optionsmanager.controller.position;

import com.olisystem.optionsmanager.dto.position.*;
import com.olisystem.optionsmanager.model.option_serie.OptionType;
import com.olisystem.optionsmanager.model.position.PositionStatus;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.service.position.PositionService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Controller para gerenciamento de posições. */
@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
@Slf4j
public class PositionController {

  private final PositionService positionService;

  /** Lista posições com filtros de status. */
  @GetMapping
  public ResponseEntity<PositionSummaryResponseDto> getPositions(
      @RequestParam(required = false) List<PositionStatus> status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false, defaultValue = "openDate") String sort,
      @RequestParam(required = false, defaultValue = "desc") String direction) {

    log.debug("Requisição para listar posições com status: {}", status);

    // Se status não for fornecido, usar OPEN e PARTIAL como padrão
    if (status == null || status.isEmpty()) {
      status = List.of(PositionStatus.OPEN, PositionStatus.PARTIAL);
    }

    // Criar paginação com ordenação
    Sort.Direction sortDir =
        "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sort));

    PositionSummaryResponseDto result = positionService.findByStatuses(status, pageable);
    return ResponseEntity.ok(result);
  }

  /** Busca posições com filtros avançados. */
  @GetMapping("/filter")
  public ResponseEntity<PositionSummaryResponseDto> filterPositions(
      @RequestParam(required = false) List<PositionStatus> status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate openDateStart,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate openDateEnd,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate closeDateStart,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate closeDateEnd,
      @RequestParam(required = false) String brokerageName,
      @RequestParam(required = false) TransactionType direction,
      @RequestParam(required = false) OptionType optionType,
      @RequestParam(required = false) String optionSeriesCode,
      @RequestParam(required = false) String baseAssetCode,
      @RequestParam(required = false) Boolean hasMultipleEntries,
      @RequestParam(required = false) Boolean hasPartialExits,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false, defaultValue = "openDate") String sort,
      @RequestParam(required = false, defaultValue = "desc") String sortDirection) {

    log.debug("Requisição para filtrar posições");

    // Se status não for fornecido, usar OPEN e PARTIAL como padrão
    if (status == null || status.isEmpty()) {
      status = List.of(PositionStatus.OPEN, PositionStatus.PARTIAL);
    }

    // Criar critérios de filtro
    PositionFilterCriteria criteria =
        PositionFilterCriteria.builder()
            .status(status)
            .openDateStart(openDateStart)
            .openDateEnd(openDateEnd)
            .closeDateStart(closeDateStart)
            .closeDateEnd(closeDateEnd)
            .brokerageName(brokerageName)
            .direction(direction)
            .optionType(optionType)
            .optionSeriesCode(optionSeriesCode)
            .baseAssetCode(baseAssetCode)
            .hasMultipleEntries(hasMultipleEntries)
            .hasPartialExits(hasPartialExits)
            .build();

    // Criar paginação com ordenação
    Sort.Direction sortDir =
        "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sort));

    PositionSummaryResponseDto result = positionService.findByFilters(criteria, pageable);
    return ResponseEntity.ok(result);
  }

  /** Busca uma posição específica por ID. */
  @GetMapping("/{id}")
  public ResponseEntity<PositionDto> getPositionById(@PathVariable UUID id) {
    log.debug("Requisição para buscar posição: {}", id);

    PositionDto position = positionService.findById(id);
    return ResponseEntity.ok(position);
  }

  /** Busca lotes de uma posição. */
  @GetMapping("/{id}/lots")
  public ResponseEntity<List<EntryLotDto>> getLotsByPositionId(@PathVariable UUID id) {
    log.debug("Requisição para buscar lotes da posição: {}", id);

    List<EntryLotDto> lots = positionService.findLotsByPositionId(id);
    return ResponseEntity.ok(lots);
  }

  /** Busca registros de saída de uma posição. */
  @GetMapping("/{id}/exits")
  public ResponseEntity<List<ExitRecordDto>> getExitsByPositionId(@PathVariable UUID id) {
    log.debug("Requisição para buscar saídas da posição: {}", id);

    List<ExitRecordDto> exits = positionService.findExitsByPositionId(id);
    return ResponseEntity.ok(exits);
  }

  /** Verifica se existe uma posição compatível para uma nova operação. */
  @GetMapping("/compatible")
  public ResponseEntity<PositionDto> checkCompatiblePosition(
      @RequestParam String optionSeriesCode, @RequestParam TransactionType transactionType) {

    log.debug("Verificando posição compatível para: {} - {}", optionSeriesCode, transactionType);

    PositionDto position =
        positionService.checkCompatiblePosition(optionSeriesCode, transactionType);
    return ResponseEntity.ok(position);
  }

  /** Processa uma nova entrada (criação de posição ou adição a existente). */
  @PostMapping("/entry")
  public ResponseEntity<PositionDto> processEntry(@RequestBody PositionEntryRequest request) {
    log.debug("Processando entrada: {}", request);

    PositionDto result = positionService.processEntry(request);
    return ResponseEntity.ok(result);
  }

  /** Processa uma saída (parcial ou total). */
  @PostMapping("/exit")
  public ResponseEntity<PositionExitResult> processExit(@RequestBody PositionExitRequest request) {
    log.debug("Processando saída: {}", request);

    PositionExitResult result = positionService.processExit(request);
    return ResponseEntity.ok(result);
  }
}
