package com.olisystem.optionsmanager.service.position;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.position.*;
import com.olisystem.optionsmanager.repository.position.EntryLotRepository;
import com.olisystem.optionsmanager.repository.position.ExitRecordRepository;
import com.olisystem.optionsmanager.repository.position.PositionOperationRepository;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import com.olisystem.optionsmanager.util.SecurityUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço especializado em processamento de entradas de posições. Gerencia criação de posições e
 * adição de entradas às posições existentes.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PositionEntryService {

  private final PositionRepository positionRepository;
  private final EntryLotRepository entryLotRepository;
  private final PositionOperationRepository positionOperationRepository;
  private final PositionCalculator calculator;
  private final ExitRecordRepository exitRecordRepository;

  /** Cria uma nova posição a partir de uma operação existente. */
  public Position createPositionFromOperation(Operation operation) {
    log.debug("Criando nova posição a partir da operação: {}", operation.getId());

    // Criar uma nova posição
    Position position =
        Position.builder()
            .optionSeries(operation.getOptionSeries())
            .brokerage(operation.getBrokerage())
            .direction(operation.getTransactionType())
            .status(PositionStatus.OPEN)
            .openDate(operation.getEntryDate())
            .totalQuantity(operation.getQuantity())
            .averagePrice(operation.getEntryUnitPrice())
            .remainingQuantity(operation.getQuantity())
            .totalRealizedProfit(BigDecimal.ZERO)
            .totalRealizedProfitPercentage(BigDecimal.ZERO)
            .user(operation.getUser())
            .build();

    // Salvar a posição
    position = positionRepository.save(position);

    // Buscar do banco a quantidade de EntryLots já existentes para garantir sequenceNumber correto
    int nextSequence = (int) entryLotRepository.countByPosition(position) + 1;

    // Criar o lote de entrada inicial
    EntryLot entryLot =
        EntryLot.builder()
            .position(position)
            .entryDate(operation.getEntryDate())
            .quantity(operation.getQuantity())
            .unitPrice(operation.getEntryUnitPrice())
            .totalValue(operation.getEntryTotalValue())
            .remainingQuantity(operation.getQuantity())
            .sequenceNumber(nextSequence)
            .isFullyConsumed(false)
            .build();

    entryLotRepository.save(entryLot);

    // Criar o link de operação
    PositionOperation posOp =
        PositionOperation.builder()
            .position(position)
            .operation(operation)
            .type(PositionOperationType.ENTRY)
            .timestamp(LocalDateTime.now())
            .sequenceNumber(nextSequence)
            .build();

    positionOperationRepository.save(posOp);

    return position;
  }

  /** Adiciona uma nova entrada a uma posição existente. */
  public Position addEntryToPosition(Position position, Operation operation) {
    log.debug("Adicionando entrada à posição: {}", position.getId());

    // Buscar do banco a quantidade de EntryLots já existentes para garantir sequenceNumber correto
    int nextSequence = (int) entryLotRepository.countByPosition(position) + 1;

    // CAPTURAR quantidade restante ANTES de atualizar a Position
    int originalRemainingQuantity = position.getRemainingQuantity();
    
    // Atualizar quantidades e preço médio
    int newTotalQuantity = position.getTotalQuantity() + operation.getQuantity();
    position.setTotalQuantity(newTotalQuantity);
    position.setRemainingQuantity(position.getRemainingQuantity() + operation.getQuantity());

    // CALCULAR PREÇO MÉDIO baseado no valor efetivo dos lotes existentes
    List<EntryLot> existingLots = entryLotRepository.findByPositionOrderByEntryDateAsc(position);
    
    // Calcular valor efetivo dos lotes EXISTENTES apenas
    BigDecimal currentEffectiveValue = BigDecimal.ZERO;
    
    for (EntryLot lot : existingLots) {
        if (lot.getRemainingQuantity() > 0) {
            // Valor original do lote total
            BigDecimal originalLotValue = lot.getUnitPrice().multiply(BigDecimal.valueOf(lot.getQuantity()));
            
            // Buscar ExitRecords para este lote para saber o valor das saídas
            BigDecimal totalExitValue = exitRecordRepository.findByEntryLot(lot)
                .stream()
                .map(exitRecord -> exitRecord.getExitUnitPrice().multiply(BigDecimal.valueOf(exitRecord.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Valor efetivo = valor original - valor das saídas
            BigDecimal effectiveLotValue = originalLotValue.subtract(totalExitValue);
            currentEffectiveValue = currentEffectiveValue.add(effectiveLotValue);
        }
    }
    
    BigDecimal newEntryValue = operation.getEntryTotalValue();
    BigDecimal totalValue = currentEffectiveValue.add(newEntryValue);
    
    // Dividir pela quantidade RESTANTE, não pela quantidade TOTAL
    int remainingQuantityAfterEntry = originalRemainingQuantity + operation.getQuantity();
    
    BigDecimal newAveragePrice = totalValue.divide(
        BigDecimal.valueOf(remainingQuantityAfterEntry), 6, java.math.RoundingMode.HALF_UP);
    
    position.setAveragePrice(newAveragePrice);

    // Criar o novo lote
    EntryLot entryLot =
        EntryLot.builder()
            .position(position)
            .entryDate(operation.getEntryDate())
            .quantity(operation.getQuantity())
            .unitPrice(operation.getEntryUnitPrice())
            .totalValue(operation.getEntryTotalValue())
            .remainingQuantity(operation.getQuantity())
            .sequenceNumber(nextSequence)
            .isFullyConsumed(false)
            .build();

    entryLotRepository.save(entryLot);

    // Criar o link de operação
    PositionOperation posOp =
        PositionOperation.builder()
            .position(position)
            .operation(operation)
            .type(PositionOperationType.ENTRY)
            .timestamp(LocalDateTime.now())
            .sequenceNumber(nextSequence)
            .build();

    positionOperationRepository.save(posOp);

//    // Criar/atualizar operação consolidada (ACTIVE)
//    if(operation != null){
//      operation.setQuantity(newTotalQuantity);
//      operation.setEntryUnitPrice(newAveragePrice);
//      operation.setEntryTotalValue(newAveragePrice.multiply(BigDecimal.valueOf(newTotalQuantity)));
//      operation = operationRepository.save(operation);
//
//       // Vincular à posição
//       PositionOperation consolidatedOp =
//       PositionOperation.builder()
//           .position(position)
//           .operation(operation)
//           .type(PositionOperationType.ENTRY)
//           .timestamp(LocalDateTime.now())
//           .sequenceNumber(nextSequence + 1)
//           .build();
//       positionOperationRepository.save(consolidatedOp);
//        log.info("Operação consolidada atualizada: {} (ACTIVE)", operation.getId());
//    }
//    if (operation == null) {
//      // Criar nova consolidada
//      operation =
//          Operation.builder()
//              .optionSeries(position.getOptionSeries())
//              .brokerage(position.getBrokerage())
//              .transactionType(position.getDirection())
//              .entryDate(position.getOpenDate())
//              .quantity(position.getTotalQuantity())
//              .entryUnitPrice(position.getAveragePrice())
//              .entryTotalValue(
//                  position
//                      .getAveragePrice()
//                      .multiply(BigDecimal.valueOf(position.getTotalQuantity())))
//              .status(OperationStatus.ACTIVE)
//              .user(position.getUser())
//              .build();
//      // Copiar casa de análise da operação original
//      if (operation != null && operation.getAnalysisHouse() != null) {
//        operation.setAnalysisHouse(operation.getAnalysisHouse());
//      }
//      operation = operationRepository.save(operation);
//
//       // Vincular à posição
//       PositionOperation consolidatedOp =
//       PositionOperation.builder()
//           .position(position)
//           .operation(operation)
//           .type(PositionOperationType.ENTRY)
//           .timestamp(LocalDateTime.now())
//           .sequenceNumber(nextSequence + 1)
//           .build();
//       positionOperationRepository.save(consolidatedOp);
//       log.info("Operação consolidada criada: {} (ACTIVE)", operation.getId());
//    }

    // Se a posição estava como PARTIAL, mantê-la como PARTIAL
    if (position.getStatus() != PositionStatus.PARTIAL) {
      position.setStatus(PositionStatus.OPEN);
    }

    Position updatedPosition = positionRepository.save(position);
    log.info("Posição atualizada com sucesso: {}", position.getId());

    return updatedPosition;
  }

  /**
   * Processa uma solicitação de entrada. Pode criar uma nova posição ou adicionar a uma existente.
   */
  /**
  public PositionDto processEntry(PositionEntryRequest request) {
    log.debug("Processando entrada: {}", request);

    Position position;
    Operation operation;
    TransactionType direction = TransactionType.BUY; // Por padrão
    if (request.getBaseAssetType() != null) {
      // Pode definir a direção com base no tipo de operação se necessário
    }

    // Buscar ou criar OptionSerie
    OptionSerie optionSerie =
        optionSerieService.getOptionSerieByCode(request.getOptionSeriesCode());
    if (optionSerie == null) {
      // Cria se não existir
      Asset asset = assetService.getAssetByCode(request.getBaseAssetCode());
      if (asset == null) {
        asset =
            Asset.builder()
                .code(request.getBaseAssetCode().toLowerCase())
                .name(request.getBaseAssetName())
                .type(request.getBaseAssetType())
                .urlLogo(request.getBaseAssetLogoUrl())
                .build();
        asset = assetService.save(asset);
      }
      optionSerie =
          OptionSerie.builder()
              .code(request.getOptionSeriesCode().toUpperCase())
              .type(request.getOptionSeriesType())
              .strikePrice(request.getOptionSeriesStrikePrice())
              .expirationDate(request.getOptionSeriesExpirationDate())
              .asset(asset)
              .build();
      optionSerie = optionSerieService.save(optionSerie);
    }

    // Se vier positionId, adiciona à posição existente
    if (request.getPositionId() != null) {
      position =
          positionRepository
              .findById(request.getPositionId())
              .orElseThrow(() -> new ResourceNotFoundException("Posição não encontrada"));
      if (!position.getUser().equals(SecurityUtil.getLoggedUser())
          || (position.getStatus() != PositionStatus.OPEN
              && position.getStatus() != PositionStatus.PARTIAL)) {
        throw new ResourceNotFoundException("Posição não disponível para adição");
      }
      operation = createOperationFromEntryRequest(request, position.getDirection());
      position = addEntryToPosition(position, operation);
      return mapper.toDto(position);
    }

    // Se não vier positionId, buscar posição aberta compatível
    position =
        positionRepository
            .findOpenPositionByUserAndOptionSeriesAndDirection(
                SecurityUtil.getLoggedUser(), optionSerie, direction)
            .orElse(null);
    if (position != null) {
      operation = createOperationFromEntryRequest(request, position.getDirection());
      position = addEntryToPosition(position, operation);
      return mapper.toDto(position);
    }

    // Se não encontrou posição, criar nova
    operation = createOperationFromEntryRequest(request, direction);
    position = createPositionFromOperation(operation);
    return mapper.toDto(position);
  }*/

}
