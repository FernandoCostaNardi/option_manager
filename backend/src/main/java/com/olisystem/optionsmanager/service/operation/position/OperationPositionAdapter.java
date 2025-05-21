package com.olisystem.optionsmanager.service.operation.position;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.dto.position.PositionExitRequest;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.ExitStrategy;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.position.PositionOperationRepository;
import com.olisystem.optionsmanager.service.position.PositionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Classe adaptadora que integra o OperationService existente com o novo PositionService. Serve como
 * ponte entre os dois sistemas, permitindo que operações sejam convertidas em entradas e saídas de
 * posições.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OperationPositionAdapter {

  private final OperationRepository operationRepository;
  private final PositionOperationRepository positionOperationRepository;
  private final PositionService positionService;

  /** Processa a criação de uma operação, possivelmente integrando com uma posição. */
  @Transactional
  public void processOperationCreation(Operation operation, Operation hiddenOperation, OperationDataRequest request) {
    log.debug("Processando integração de criação de operação: {}", operation.getId());

    // Verificar se a operação é candidata a ser vinculada a uma posição
    // Exemplo: operações com status ACTIVE são candidatas

    // Verificar se já existe uma posição compatível
    OptionSerie optionSerie = operation.getOptionSeries();
    Optional<Position> positionOpt =
        positionService.findCompatiblePosition(optionSerie, operation.getTransactionType());

    if (positionOpt.isPresent()) {
      // Adicionar à posição existente
      Position position = positionOpt.get();
      log.info("Adicionando operação a posição existente: {}", hiddenOperation == null ? operation.getId() : hiddenOperation.getId());
      if(hiddenOperation == null){
        positionService.addEntryToPosition(position, operation);
      }else{
        positionService.addEntryToPosition(position, operation);
      }
    } else {
      // Criar nova posição
      log.info("Criando nova posição para operação: {}", hiddenOperation == null ? operation.getId() : hiddenOperation.getId());
      if(hiddenOperation == null){
        positionService.createPositionFromOperation(operation);
      }else{
        positionService.createPositionFromOperation(hiddenOperation);
      }
    }
  }

  /** Processa a finalização de uma operação, convertendo em saída de posição. */
  @Transactional
  public void processOperationFinalization(OperationFinalizationRequest request) {
    log.debug("Processando integração de finalização de operação: {}", request.getOperationId());

    // Buscar a operação original
    Operation operation =
        operationRepository
            .findById(request.getOperationId())
            .orElseThrow(() -> new IllegalArgumentException("Operação não encontrada"));

    // Verificar se a operação está vinculada a uma posição
    positionOperationRepository
        .findByOperation(operation)
        .ifPresent(
            posOp -> {
              Position position = posOp.getPosition();

              // Buscar lotes de entrada do mesmo dia da saída
              List<EntryLot> lotsSameDay =
                  position.getEntryLots().stream()
                      .filter(
                          lot ->
                              lot.getEntryDate().equals(request.getExitDate())
                                  && lot.getRemainingQuantity() > 0)
                      .collect(Collectors.toList());
              int qtySameDay = lotsSameDay.stream().mapToInt(EntryLot::getRemainingQuantity).sum();
              int qtyToExit = operation.getQuantity();

              // 1. Day trade (lote do dia)
              if (qtySameDay > 0) {
                int qtyDayTrade = Math.min(qtyToExit, qtySameDay);
                PositionExitRequest dayTradeExitRequest =
                    PositionExitRequest.builder()
                        .positionId(position.getId())
                        .exitDate(request.getExitDate())
                        .quantity(qtyDayTrade)
                        .exitUnitPrice(request.getExitUnitPrice())
                        .exitStrategy(ExitStrategy.AUTO)
                        .analysisHouseId(
                            operation.getAnalysisHouse() != null
                                ? operation.getAnalysisHouse().getId()
                                : null)
                        .build();
                log.info(
                    "Processando saída DAYTRADE para posição: {} quantidade: {}",
                    position.getId(),
                    qtyDayTrade);
                positionService.processExit(dayTradeExitRequest);
                qtyToExit -= qtyDayTrade;
              }

              // 2. Swing trade (restante)
              if (qtyToExit > 0) {
                PositionExitRequest swingExitRequest =
                    PositionExitRequest.builder()
                        .positionId(position.getId())
                        .exitDate(request.getExitDate())
                        .quantity(qtyToExit)
                        .exitUnitPrice(request.getExitUnitPrice())
                        .exitStrategy(ExitStrategy.AUTO)
                        .analysisHouseId(
                            operation.getAnalysisHouse() != null
                                ? operation.getAnalysisHouse().getId()
                                : null)
                        .build();
                log.info(
                    "Processando saída SWING para posição: {} quantidade: {}",
                    position.getId(),
                    qtyToExit);
                positionService.processExit(swingExitRequest);
              }
            });
  }
}
