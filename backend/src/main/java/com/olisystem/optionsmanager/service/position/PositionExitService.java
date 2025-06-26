package com.olisystem.optionsmanager.service.position;

import com.olisystem.optionsmanager.dto.position.EntryLotDto;
import com.olisystem.optionsmanager.dto.position.ExitRecordDto;
import com.olisystem.optionsmanager.dto.position.PositionExitRequest;
import com.olisystem.optionsmanager.dto.position.PositionExitResult;
import com.olisystem.optionsmanager.exception.ResourceNotFoundException;
import com.olisystem.optionsmanager.mapper.PositionMapper;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.ExitRecord;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.position.PositionOperation;
import com.olisystem.optionsmanager.model.position.PositionOperationType;
import com.olisystem.optionsmanager.service.operation.detector.PartialExitDetector;
import com.olisystem.optionsmanager.model.position.PositionStatus;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.position.EntryLotRepository;
import com.olisystem.optionsmanager.repository.position.ExitRecordRepository;
import com.olisystem.optionsmanager.repository.position.PositionOperationRepository;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationService;
import com.olisystem.optionsmanager.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço especializado no processamento de saídas de posições. Implementa a lógica de saídas
 * parciais e totais com regras FIFO/LIFO.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PositionExitService {

  private final PositionRepository positionRepository;
  private final EntryLotRepository entryLotRepository;
  private final ExitRecordRepository exitRecordRepository;
  private final PositionOperationRepository positionOperationRepository;
  private final OperationRepository operationRepository;
  private final PositionCalculator calculator;
  private final PositionMapper mapper;
  private final PartialExitDetector partialExitDetector;
  private final AverageOperationService averageOperationService;

  private static final int PRECISION = 6;

  /** Processa uma solicitação de saída (parcial ou total). */
  public PositionExitResult processExit(PositionExitRequest request) {
    log.debug("Processando saída: {}", request);

    // 1. Validar e obter a posição
    Position position =
        positionRepository
            .findById(request.getPositionId())
            .orElseThrow(() -> new ResourceNotFoundException("Posição não encontrada"));

    if (!position.getUser().equals(SecurityUtil.getLoggedUser())) {
      throw new ResourceNotFoundException("Posição não encontrada");
    }
    if (position.getStatus() == PositionStatus.CLOSED) {
      throw new IllegalStateException("A posição já está fechada");
    }
    if (request.getQuantity() <= 0 || request.getQuantity() > position.getRemainingQuantity()) {
      throw new IllegalArgumentException("Quantidade inválida para saída");
    }

    // ✅ CORREÇÃO: Usar PartialExitDetector para detecção sofisticada
    boolean isFirstPartial = partialExitDetector.isFirstPartialExit(position);
    boolean isSubsequentPartial = partialExitDetector.isSubsequentPartialExit(position);
    boolean isFinalFromPartial = partialExitDetector.isFinalExit(position, request.getQuantity()) 
            && position.getStatus() == PositionStatus.PARTIAL;
    boolean isPartialNormal = partialExitDetector.isPartialExit(position, request.getQuantity());
    boolean isFullExit = partialExitDetector.isFinalExit(position, request.getQuantity());

    log.debug("🔍 Análise de saída: isFirstPartial={}, isSubsequentPartial={}, isFinalFromPartial={}, isPartialNormal={}, isFullExit={}", 
            isFirstPartial, isSubsequentPartial, isFinalFromPartial, isPartialNormal, isFullExit);

    // 🎯 Determinar se é algum tipo de saída parcial
    boolean isAnyPartialExit = isFirstPartial || isSubsequentPartial || isFinalFromPartial || isPartialNormal;
    
    if (isFinalFromPartial) {
        log.info("🎯 FINAL_PARTIAL_EXIT detectada no PositionExitService");
    } else if (isFirstPartial) {
        log.info("🔄 FIRST_PARTIAL_EXIT detectada no PositionExitService");
    } else if (isSubsequentPartial) {
        log.info("➡️ SUBSEQUENT_PARTIAL_EXIT detectada no PositionExitService");
    } else if (isPartialNormal) {
        log.info("📝 PARTIAL_EXIT detectada no PositionExitService");
    } else if (isFullExit) {
        log.info("✅ TOTAL_EXIT detectada no PositionExitService");
    }

    List<EntryLot> availableLots =
        entryLotRepository.findByPositionOrderByEntryDateAsc(position).stream()
            .filter(lot -> lot.getRemainingQuantity() > 0)
            .collect(Collectors.toList());
    List<EntryLotDto> availableLotDtos = mapper.toEntryLotDtoList(availableLots);

    // Separar lotes do mesmo dia e de dias anteriores
    List<EntryLotDto> sameDayLots =
        availableLotDtos.stream()
            .filter(lot -> lot.getEntryDate().equals(request.getExitDate()))
            .collect(Collectors.toList());
    List<EntryLotDto> previousDaysLots =
        availableLotDtos.stream()
            .filter(lot -> lot.getEntryDate().isBefore(request.getExitDate()))
            .collect(Collectors.toList());

    int remainingToExit = request.getQuantity();
    int totalSameDayQty = sameDayLots.stream().mapToInt(EntryLotDto::getRemainingQuantity).sum();
    List<ExitRecordDto> exitRecordDtos = new ArrayList<>();
    List<Operation> exitOperations = new ArrayList<>();

    // 1. Day trade (lote do dia)
    if (totalSameDayQty > 0) {
      int qtyDayTrade = Math.min(remainingToExit, totalSameDayQty);
      List<ExitRecordDto> dayTradeRecords =
          calculator.processExitLIFO(
              sameDayLots, qtyDayTrade, request.getExitUnitPrice(), request.getExitDate());
      exitRecordDtos.addAll(dayTradeRecords);
      Operation dayTradeExit =
          createExitOperationWithTradeType(position, request, dayTradeRecords, TradeType.DAY);
      exitOperations.add(dayTradeExit);
      remainingToExit -= qtyDayTrade;
    }
    // 2. Swing trade (lotes anteriores)
    if (remainingToExit > 0 && !previousDaysLots.isEmpty()) {
      List<ExitRecordDto> swingRecords =
          calculator.processExitFIFO(
              previousDaysLots, remainingToExit, request.getExitUnitPrice(), request.getExitDate());
      exitRecordDtos.addAll(swingRecords);
      Operation swingExit =
          createExitOperationWithTradeType(position, request, swingRecords, TradeType.SWING);
      exitOperations.add(swingExit);
    }

    // Atualizar lotes, posição, consolidada, etc (como já faz)
    List<ExitRecord> exitRecords =
        saveExitRecords(
            exitRecordDtos,
            exitOperations.get(0),
            availableLots); // Salva para a primeira operação (pode ser ajustado para múltiplas)
    updateEntryLots(availableLotDtos, availableLots);
    updatePositionAfterExit(position, request.getQuantity(), exitRecords);

    // NOVO: Marcar como HIDDEN as operações de entrada cujos lotes foram totalmente consumidos
    for (ExitRecordDto exitDto : exitRecordDtos) {
      EntryLot lot = entryLotRepository.findById(exitDto.getEntryLotId()).orElse(null);
      if (lot != null && lot.getRemainingQuantity() == 0) {
        // Buscar operação de entrada correspondente
        List<PositionOperation> entryOps =
            positionOperationRepository.findByPositionAndTypeOrderByTimestampAsc(
                position, PositionOperationType.ENTRY);
        for (PositionOperation entryOp : entryOps) {
          Operation op = entryOp.getOperation();
          if (op.getEntryDate().equals(lot.getEntryDate())
              && op.getQuantity().equals(lot.getQuantity())
              && op.getEntryUnitPrice().compareTo(lot.getUnitPrice()) == 0) {
            if (op.getStatus() != OperationStatus.HIDDEN) {
              op.setStatus(OperationStatus.HIDDEN);
              operationRepository.save(op);
              log.info("Operação de entrada {} marcada como HIDDEN (lote consumido)", op.getId());
            }
          }
        }
      }
    }

    // NOVO: Garantir que sempre exista uma operação consolidada ACTIVE para os lotes abertos
    List<EntryLot> remainingLots =
        entryLotRepository.findByPositionOrderByEntryDateAsc(position).stream()
            .filter(l -> l.getRemainingQuantity() > 0)
            .collect(Collectors.toList());
    boolean allLotsConsumed = remainingLots.isEmpty();
    List<PositionOperation> allOps =
        positionOperationRepository.findByPositionOrderByTimestampAsc(position);
    Operation activeConsolidated = null;
    for (PositionOperation op : allOps) {
      Operation o = op.getOperation();
      if (o.getStatus() == OperationStatus.ACTIVE
          && o.getTransactionType() == position.getDirection()
          && o.getExitDate() == null) {
        activeConsolidated = o;
        break;
      }
    }
    if (!allLotsConsumed) {
      int newQty = remainingLots.stream().mapToInt(EntryLot::getRemainingQuantity).sum();
      BigDecimal newAvg = calculator.calculateRemainingAveragePrice(remainingLots);
      if (activeConsolidated == null) {
        // Criar nova consolidada ACTIVE
        activeConsolidated =
            Operation.builder()
                .optionSeries(position.getOptionSeries())
                .brokerage(position.getBrokerage())
                .transactionType(position.getDirection())
                .status(OperationStatus.ACTIVE)
                .entryDate(position.getOpenDate())
                .quantity(newQty)
                .entryUnitPrice(newAvg)
                .entryTotalValue(newAvg.multiply(BigDecimal.valueOf(newQty)))
                .user(position.getUser())
                .build();
        activeConsolidated = operationRepository.save(activeConsolidated);
        PositionOperation consolidatedOp =
            PositionOperation.builder()
                .position(position)
                .operation(activeConsolidated)
                .type(PositionOperationType.ENTRY)
                .timestamp(LocalDateTime.now())
                .sequenceNumber(allOps.size() + 1)
                .build();
        positionOperationRepository.save(consolidatedOp);
        log.info(
            "Operação consolidada ACTIVE criada: {} ({} unidades, preço médio {})",
            activeConsolidated.getId(),
            newQty,
            newAvg);
      } else {
        // Atualizar consolidada existente
        activeConsolidated.setQuantity(newQty);
        activeConsolidated.setEntryUnitPrice(newAvg);
        activeConsolidated.setEntryTotalValue(newAvg.multiply(BigDecimal.valueOf(newQty)));
        operationRepository.save(activeConsolidated);
        log.info(
            "Operação consolidada ACTIVE atualizada: {} ({} unidades, preço médio {})",
            activeConsolidated.getId(),
            newQty,
            newAvg);
      }
    } else if (activeConsolidated != null) {
      // Se todos os lotes foram consumidos, marcar consolidada como HIDDEN
      activeConsolidated.setStatus(OperationStatus.HIDDEN);
      operationRepository.save(activeConsolidated);
      log.info(
          "Operação consolidada ACTIVE {} marcada como HIDDEN (todos lotes consumidos)",
          activeConsolidated.getId());
    }

    // ✅ CORREÇÃO: Usar lógica sofisticada em vez de isFullExit simples
    // Se é qualquer tipo de saída parcial (incluindo final) → processParcialExit
    // Se é saída total simples (position OPEN) → processFullExit
    if (isAnyPartialExit) {
      log.info("🔄 Usando processParcialExit (saída parcial detectada)");
      averageOperationService.processParcialExit(
          exitOperations.get(0), exitOperations.get(1), null, activeConsolidated, position);
    } else {
      log.info("✅ Usando processFullExit (saída total simples)");
      averageOperationService.processFullExit(
          exitOperations.get(0), exitOperations.get(1), activeConsolidated, position);
    }

    // 3. Criar operação consolidada de saída (visível)
    if (!exitOperations.isEmpty()) {
      
      // ✅ CORREÇÃO: Para FINAL_PARTIAL_EXIT, usar dados da posição completa
      int consolidatedQuantity;
      BigDecimal consolidatedEntryTotalValue;
      BigDecimal consolidatedExitTotalValue;
      BigDecimal consolidatedProfitLoss;
      
      if (isFinalFromPartial) {
        log.info("🎯 Criando operação consolidada para FINAL_PARTIAL_EXIT com dados completos da posição");
        
        // Usar dados da posição completa
        consolidatedQuantity = position.getTotalQuantity();
        consolidatedEntryTotalValue = position.getAveragePrice().multiply(BigDecimal.valueOf(position.getTotalQuantity()));
        consolidatedExitTotalValue = position.getTotalRealizedProfit().add(consolidatedEntryTotalValue);
        consolidatedProfitLoss = position.getTotalRealizedProfit();
        
      } else {
        log.info("📝 Criando operação consolidada para saída normal com dados da saída atual");
        
        // Usar dados apenas da saída atual (lógica original)
        consolidatedQuantity = exitOperations.stream().mapToInt(Operation::getQuantity).sum();
        consolidatedEntryTotalValue = exitOperations.stream()
            .map(Operation::getEntryTotalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        consolidatedExitTotalValue = exitOperations.stream()
            .map(Operation::getExitTotalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        consolidatedProfitLoss = exitOperations.stream()
            .map(Operation::getProfitLoss)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
      }
      
      BigDecimal entryUnitPrice =
          consolidatedEntryTotalValue.divide(BigDecimal.valueOf(consolidatedQuantity), 6, RoundingMode.HALF_UP);
      BigDecimal averageExitPrice =
          consolidatedExitTotalValue.divide(BigDecimal.valueOf(consolidatedQuantity), 6, RoundingMode.HALF_UP);
      BigDecimal profitLossPercentage =
          consolidatedProfitLoss
              .divide(consolidatedEntryTotalValue, 6, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100));
      OperationStatus status =
          consolidatedProfitLoss.compareTo(BigDecimal.ZERO) > 0
              ? OperationStatus.WINNER
              : OperationStatus.LOSER;
      TradeType tradeType =
          exitOperations.stream().anyMatch(op -> op.getTradeType() == TradeType.SWING)
              ? TradeType.SWING
              : TradeType.DAY;
      Operation consolidatedExit =
          Operation.builder()
              .optionSeries(position.getOptionSeries())
              .brokerage(position.getBrokerage())
              .transactionType(exitOperations.get(0).getTransactionType())
              .tradeType(tradeType)
              .entryDate(position.getOpenDate())
              .exitDate(request.getExitDate())
              .quantity(consolidatedQuantity)
              .entryUnitPrice(entryUnitPrice)
              .entryTotalValue(consolidatedEntryTotalValue)
              .exitUnitPrice(averageExitPrice)
              .exitTotalValue(consolidatedExitTotalValue)
              .profitLoss(consolidatedProfitLoss)
              .profitLossPercentage(profitLossPercentage)
              .status(status)
              .user(SecurityUtil.getLoggedUser())
              .build();
      consolidatedExit = operationRepository.save(consolidatedExit);
      // Vincular à posição
      PositionOperation consolidatedOp =
          PositionOperation.builder()
              .position(position)
              .operation(consolidatedExit)
              .type(PositionOperationType.FULL_EXIT)
              .timestamp(LocalDateTime.now())
              .sequenceNumber(allOps.size() + 2)
              .build();
      positionOperationRepository.save(consolidatedOp);
      log.info(
          "Operação consolidada de saída criada: {} ({} unidades, preço médio {}, status {})",
          consolidatedExit.getId(),
          consolidatedQuantity,
          entryUnitPrice,
          status);
    }

    // Retornar resultado (pode ser ajustado para múltiplas operações)
    return PositionExitResult.builder()
        .positionId(position.getId())
        .newPositionStatus(position.getStatus())
        .exitQuantity(request.getQuantity())
        .totalExitValue(
            request.getExitUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
        .totalProfitLoss(calculateTotalProfitLoss(exitRecords))
        .profitLossPercentage(
            calculateAverageProfitLossPercentageForDtos(mapper.toExitRecordDtoList(exitRecords)))
        .remainingQuantity(position.getRemainingQuantity())
        .exitRecords(mapper.toExitRecordDtoList(exitRecords))
        .resultOperationId(null)
        .isFullExit(request.getQuantity().equals(position.getRemainingQuantity()))
        .message("Saída processada (day trade + swing trade, se aplicável)")
        .build();
  }

  /** Cria uma operação para a saída da posição. */
//  private Operation createExitOperation(
//      Position position, PositionExitRequest request, List<ExitRecordDto> exitRecords) {
//    // Calcular lucro/prejuízo total
//    BigDecimal totalProfitLoss =
//        exitRecords.stream()
//            .map(ExitRecordDto::getProfitLoss)
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//    // Calcular valor total de saída
//    BigDecimal exitTotalValue =
//        request.getExitUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
//
//    // Calcular valor total de entrada e preço médio dos lotes consumidos
//    BigDecimal entryTotalValue =
//        exitRecords.stream()
//            .map(r -> r.getEntryUnitPrice().multiply(BigDecimal.valueOf(r.getQuantity())))
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//    BigDecimal entryUnitPrice =
//        entryTotalValue.divide(
//            BigDecimal.valueOf(request.getQuantity()), PRECISION, RoundingMode.HALF_UP);
//
//    // Inverter a direção da transação original para a saída
//    TransactionType exitTransactionType =
//        position.getDirection() == TransactionType.BUY ? TransactionType.SELL : TransactionType.BUY;
//
//    // Determinar se é day trade (todos os lotes consumidos têm entryDate igual ao exitDate)
//    boolean isDayTrade =
//        exitRecords.stream().allMatch(r -> r.getEntryDate().equals(request.getExitDate()));
//    TradeType tradeType = isDayTrade ? TradeType.DAY : TradeType.SWING;
//    log.info(
//        "Criando operação de saída: {} | Tipo: {} | DayTrade? {}",
//        exitRecords.size(),
//        tradeType,
//        isDayTrade);
//
//    // Criar operação de saída
//    Operation exitOperation =
//        Operation.builder()
//            .optionSeries(position.getOptionSeries())
//            .brokerage(position.getBrokerage())
//            .transactionType(exitTransactionType)
//            .tradeType(tradeType)
//            .entryDate(position.getOpenDate())
//            .exitDate(request.getExitDate())
//            .quantity(request.getQuantity())
//            .entryUnitPrice(entryUnitPrice)
//            .entryTotalValue(entryTotalValue)
//            .exitUnitPrice(request.getExitUnitPrice())
//            .exitTotalValue(exitTotalValue)
//            .profitLoss(totalProfitLoss)
//            .profitLossPercentage(calculateAverageProfitLossPercentageForDtos(exitRecords))
//            .status(
//                OperationStatus
//                    .HIDDEN) // Esta operação fica HIDDEN, apenas a consolidada fica visível
//            .user(SecurityUtil.getLoggedUser())
//            .build();
//
//    // Adicionar casa de análise se fornecida
//    if (request.getAnalysisHouseId() != null) {
//      Optional<AnalysisHouse> analysisHouse =
//          analysisHouseService.findById(request.getAnalysisHouseId());
//      analysisHouse.ifPresent(exitOperation::setAnalysisHouse);
//    }
//
//    Operation savedOperation = operationRepository.save(exitOperation);
//
//    // Criar link de operação com a posição
//    PositionOperation posOp =
//        PositionOperation.builder()
//            .position(position)
//            .operation(savedOperation)
//            .type(
//                position.getRemainingQuantity() == request.getQuantity()
//                    ? PositionOperationType.FULL_EXIT
//                    : PositionOperationType.PARTIAL_EXIT)
//            .timestamp(LocalDateTime.now())
//            .sequenceNumber(
//                positionOperationRepository.findByPositionOrderByTimestampAsc(position).size() + 1)
//            .build();
//
//    positionOperationRepository.save(posOp);
//    log.info(
//        "Operação de saída criada: {} | Entrada média: {} | Entrada total: {} | Saída: {} | Lucro: {} | Tipo: {}",
//        savedOperation.getId(),
//        entryUnitPrice,
//        entryTotalValue,
//        exitTotalValue,
//        totalProfitLoss,
//        tradeType);
//
//    return savedOperation;
//  }

  /** Cria uma operação para a quantidade restante após uma saída parcial. */
//  private Operation createRemainderOperation(Position position, Operation originalOperation) {
//    // Criar operação para a quantidade restante
//    Operation remainderOperation =
//        Operation.builder()
//            .optionSeries(position.getOptionSeries())
//            .brokerage(position.getBrokerage())
//            .analysisHouse(originalOperation.getAnalysisHouse())
//            .transactionType(position.getDirection())
//            .entryDate(position.getOpenDate())
//            .quantity(position.getRemainingQuantity())
//            .entryUnitPrice(position.getAveragePrice())
//            .entryTotalValue(
//                position
//                    .getAveragePrice()
//                    .multiply(BigDecimal.valueOf(position.getRemainingQuantity())))
//            .status(OperationStatus.ACTIVE)
//            .user(SecurityUtil.getLoggedUser())
//            .build();
//
//    Operation savedOperation = operationRepository.save(remainderOperation);
//
//    // Criar link de operação com a posição
//    PositionOperation posOp =
//        PositionOperation.builder()
//            .position(position)
//            .operation(savedOperation)
//            .type(PositionOperationType.ENTRY) // É uma entrada pois representa a posição restante
//            .timestamp(LocalDateTime.now())
//            .sequenceNumber(
//                positionOperationRepository.findByPositionOrderByTimestampAsc(position).size() + 1)
//            .build();
//
//    positionOperationRepository.save(posOp);
//
//    return savedOperation;
//  }

  /** Cria ou atualiza a operação consolidada que representa o resultado agregado das saídas. */
//  private Operation createConsolidatedOperation(
//      Position position, Operation exitOperation, int exitQuantity) {
//    // Verificar se já existe uma operação consolidada
//    List<ExitRecord> allExits = exitRecordRepository.findByPositionId(position.getId());
//
//    // Calcular valores consolidados
//    BigDecimal totalProfitLoss =
//        allExits.stream().map(ExitRecord::getProfitLoss).reduce(BigDecimal.ZERO, BigDecimal::add);
//
//    // Calcular quantidade total de saída
//    int totalExitedQuantity = allExits.stream().mapToInt(ExitRecord::getQuantity).sum();
//
//    // Calcular preço médio ponderado de saída
//    BigDecimal totalExitValue =
//        allExits.stream()
//            .map(
//                record ->
//                    record.getExitUnitPrice().multiply(BigDecimal.valueOf(record.getQuantity())))
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//    BigDecimal averageExitPrice =
//        totalExitValue.divide(
//            BigDecimal.valueOf(totalExitedQuantity), PRECISION, RoundingMode.HALF_UP);
//
//    // Calcular percentual médio ponderado de lucro/prejuízo
//    BigDecimal profitLossPercentage = calculateAverageProfitLossPercentage(allExits);
//
//    // Determinar status (WINNER ou LOSER)
//    OperationStatus status =
//        totalProfitLoss.compareTo(BigDecimal.ZERO) > 0
//            ? OperationStatus.WINNER
//            : OperationStatus.LOSER;
//
//    // Criar operação consolidada
//    Operation consolidatedOperation =
//        Operation.builder()
//            .optionSeries(position.getOptionSeries())
//            .brokerage(position.getBrokerage())
//            .analysisHouse(exitOperation.getAnalysisHouse())
//            .transactionType(exitOperation.getTransactionType())
//            .entryDate(position.getOpenDate())
//            .exitDate(exitOperation.getExitDate()) // Usa a data da última saída
//            .quantity(totalExitedQuantity)
//            .entryUnitPrice(position.getAveragePrice())
//            .entryTotalValue(
//                position.getAveragePrice().multiply(BigDecimal.valueOf(totalExitedQuantity)))
//            .exitUnitPrice(averageExitPrice)
//            .exitTotalValue(totalExitValue)
//            .profitLoss(totalProfitLoss)
//            .profitLossPercentage(profitLossPercentage)
//            .status(status)
//            .user(SecurityUtil.getLoggedUser())
//            .build();
//
//    return operationRepository.save(consolidatedOperation);
//  }

  /** Salva registros de saída para cada lote consumido. */
  private List<ExitRecord> saveExitRecords(
      List<ExitRecordDto> exitRecordDtos, Operation exitOperation, List<EntryLot> availableLots) {
    List<ExitRecord> exitRecords = new ArrayList<>();

    for (ExitRecordDto dto : exitRecordDtos) {
      // Encontrar o lote correspondente
      EntryLot entryLot =
          availableLots.stream()
              .filter(lot -> lot.getId().equals(dto.getEntryLotId()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Lote não encontrado durante processamento de saída"));

      // Criar registro de saída
      ExitRecord exitRecord =
          ExitRecord.builder()
              .entryLot(entryLot)
              .exitOperation(exitOperation)
              .exitDate(dto.getExitDate())
              .quantity(dto.getQuantity())
              .entryUnitPrice(dto.getEntryUnitPrice())
              .exitUnitPrice(dto.getExitUnitPrice())
              .profitLoss(dto.getProfitLoss())
              .profitLossPercentage(dto.getProfitLossPercentage())
              .appliedStrategy(dto.getAppliedStrategy())
              .build();

      exitRecords.add(exitRecordRepository.save(exitRecord));
    }

    return exitRecords;
  }

  /** Atualiza os lotes de entrada após a saída. */
  private void updateEntryLots(List<EntryLotDto> updatedLotDtos, List<EntryLot> originalLots) {
    // Atualizar quantidades restantes e status dos lotes
    for (EntryLotDto dto : updatedLotDtos) {
      EntryLot lot =
          originalLots.stream()
              .filter(l -> l.getId().equals(dto.getId()))
              .findFirst()
              .orElseThrow(
                  () -> new IllegalStateException("Lote não encontrado durante atualização"));

      lot.setRemainingQuantity(dto.getRemainingQuantity());
      lot.setIsFullyConsumed(dto.getIsFullyConsumed());
      entryLotRepository.save(lot);
    }
  }

  /** Atualiza a posição após processamento da saída. */
  private void updatePositionAfterExit(
      Position position, int exitQuantity, List<ExitRecord> exitRecords) {
    // Calcular lucro/prejuízo realizado
    BigDecimal profitLoss = calculateTotalProfitLoss(exitRecords);

    // Atualizar quantidade restante
    position.setRemainingQuantity(
        position.getEntryLots().stream().mapToInt(EntryLot::getRemainingQuantity).sum());

    // Atualizar lucro total realizado
    BigDecimal newTotalRealizedProfit = position.getTotalRealizedProfit().add(profitLoss);
    position.setTotalRealizedProfit(newTotalRealizedProfit);

    // Calcular percentual médio de lucro/prejuízo
    BigDecimal profitLossPercentage = calculateAverageProfitLossPercentage(exitRecords);

    // Se todos os lotes foram consumidos, fechar posição
    boolean allLotsConsumed =
        position.getEntryLots().stream().allMatch(lot -> lot.getRemainingQuantity() == 0);
    if (allLotsConsumed) {
      position.setTotalRealizedProfitPercentage(profitLossPercentage);
      position.setStatus(PositionStatus.CLOSED);
      position.setCloseDate(exitRecords.get(0).getExitDate());
      log.info("Posição {} fechada. Todas as entradas consumidas.", position.getId());
    } else {
      // Se restarem lotes, manter como PARCIAL
      List<ExitRecord> allExits = exitRecordRepository.findByPositionId(position.getId());
      BigDecimal totalExitValue =
          allExits.stream()
              .map(
                  record ->
                      record.getExitUnitPrice().multiply(BigDecimal.valueOf(record.getQuantity())))
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      if (totalExitValue.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal weightedPercentage =
            allExits.stream()
                .map(
                    record -> {
                      BigDecimal exitValue =
                          record
                              .getExitUnitPrice()
                              .multiply(BigDecimal.valueOf(record.getQuantity()));
                      return exitValue.multiply(record.getProfitLossPercentage());
                    })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        position.setTotalRealizedProfitPercentage(
            weightedPercentage.divide(totalExitValue, PRECISION, RoundingMode.HALF_UP));
      }
      position.setStatus(PositionStatus.PARTIAL);
      log.info(
          "Posição {} permanece PARCIAL. Lotes restantes: {}.",
          position.getId(),
          position.getEntryLots().stream().filter(lot -> lot.getRemainingQuantity() > 0).count());
    }
    positionRepository.save(position);
  }

  /** Calcula o lucro/prejuízo total para uma lista de registros de saída. */
  private BigDecimal calculateTotalProfitLoss(List<ExitRecord> exitRecords) {
    return exitRecords.stream()
        .map(ExitRecord::getProfitLoss)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Calcula o percentual médio ponderado de lucro/prejuízo. */
  private BigDecimal calculateAverageProfitLossPercentage(List<ExitRecord> exitRecords) {
    if (exitRecords == null || exitRecords.isEmpty()) {
      return BigDecimal.ZERO;
    }

    BigDecimal totalExitValue =
        exitRecords.stream()
            .map(
                record ->
                    record.getExitUnitPrice().multiply(BigDecimal.valueOf(record.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (totalExitValue.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }

    BigDecimal weightedPercentage =
        exitRecords.stream()
            .map(
                record -> {
                  BigDecimal exitValue =
                      record.getExitUnitPrice().multiply(BigDecimal.valueOf(record.getQuantity()));
                  return exitValue.multiply(record.getProfitLossPercentage());
                })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    return weightedPercentage.divide(totalExitValue, PRECISION, RoundingMode.HALF_UP);
  }

  /** Calcula o percentual médio ponderado de lucro/prejuízo para DTOs. */
  private BigDecimal calculateAverageProfitLossPercentageForDtos(
      List<ExitRecordDto> exitRecordDtos) {
    if (exitRecordDtos == null || exitRecordDtos.isEmpty()) {
      return BigDecimal.ZERO;
    }

    BigDecimal totalExitValue =
        exitRecordDtos.stream()
            .map(
                record ->
                    record.getExitUnitPrice().multiply(BigDecimal.valueOf(record.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (totalExitValue.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }

    BigDecimal weightedPercentage =
        exitRecordDtos.stream()
            .map(
                record -> {
                  BigDecimal exitValue =
                      record.getExitUnitPrice().multiply(BigDecimal.valueOf(record.getQuantity()));
                  return exitValue.multiply(record.getProfitLossPercentage());
                })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    return weightedPercentage.divide(totalExitValue, PRECISION, RoundingMode.HALF_UP);
  }

  /**
   * Aplica regras FIFO/LIFO para processamento de saída. Implementação alternativa para suporte a
   * versões anteriores.
   */
//  private List<ExitProcessingResult> applyFifoLifoRules(
//      Position position, Integer exitQuantity, LocalDate exitDate, BigDecimal exitUnitPrice) {
//    List<ExitProcessingResult> results = new ArrayList<>();
//    Integer remainingExitQuantity = exitQuantity;
//
//    // Passo 1: Obtemos todos os lotes de entrada ainda disponíveis
//    List<EntryLot> availableLots =
//        entryLotRepository.findByPositionAndRemainingQuantityGreaterThan(
//            position, 0); // Ordenados por data (mais antigos primeiro)
//
//    // Passo 2: Calculamos o dia da operação de saída
//    String exitDayKey = exitDate.format(DateTimeFormatter.ISO_DATE);
//
//    // Passo 3: Separamos os lotes do mesmo dia e de dias anteriores
//    List<EntryLot> sameDayLots =
//        availableLots.stream()
//            .filter(lot -> exitDayKey.equals(lot.getEntryDate().format(DateTimeFormatter.ISO_DATE)))
//            .collect(Collectors.toList());
//
//    List<EntryLot> previousDaysLots =
//        availableLots.stream()
//            .filter(
//                lot -> !exitDayKey.equals(lot.getEntryDate().format(DateTimeFormatter.ISO_DATE)))
//            .collect(Collectors.toList());
//
//    // Passo 4: Aplicamos LIFO para lotes do mesmo dia (do mais recente para o mais antigo)
//    sameDayLots.sort(
//        Comparator.comparing(EntryLot::getEntryDate).reversed()); // Mais recentes primeiro (LIFO)
//
//    for (EntryLot lot : sameDayLots) {
//      if (remainingExitQuantity <= 0) break;
//
//      int qtyToExit = Math.min(remainingExitQuantity, lot.getRemainingQuantity());
//      BigDecimal costBasis = lot.getUnitPrice().multiply(new BigDecimal(qtyToExit));
//      BigDecimal exitValue = exitUnitPrice.multiply(new BigDecimal(qtyToExit));
//      BigDecimal profitLoss = exitValue.subtract(costBasis);
//
//      // Registrar resultado desta saída
//      results.add(new ExitProcessingResult(lot, qtyToExit, costBasis, exitValue, profitLoss));
//
//      // Atualizar o lote
//      lot.setRemainingQuantity(lot.getRemainingQuantity() - qtyToExit);
//      remainingExitQuantity -= qtyToExit;
//    }

    // Passo 5: Se ainda há quantidade a sair, aplicamos FIFO para dias anteriores
//    if (remainingExitQuantity > 0) {
//      for (EntryLot lot : previousDaysLots) { // Já estão ordenados por data (FIFO)
//        if (remainingExitQuantity <= 0) break;
//
//        int qtyToExit = Math.min(remainingExitQuantity, lot.getRemainingQuantity());
//        BigDecimal costBasis = lot.getUnitPrice().multiply(new BigDecimal(qtyToExit));
//        BigDecimal exitValue = exitUnitPrice.multiply(new BigDecimal(qtyToExit));
//        BigDecimal profitLoss = exitValue.subtract(costBasis);
//
//        // Registrar resultado desta saída
//        results.add(new ExitProcessingResult(lot, qtyToExit, costBasis, exitValue, profitLoss));
//
//        // Atualizar o lote
//        lot.setRemainingQuantity(lot.getRemainingQuantity() - qtyToExit);
//        remainingExitQuantity -= qtyToExit;
//      }
//    }
//
//    return results;
//  }

  /** Classe interna para armazenar resultados do processamento de saída. */
//  private static class ExitProcessingResult {
//    private final EntryLot entryLot;
//    private final int quantity;
//    private final BigDecimal costBasis;
//    private final BigDecimal exitValue;
//    private final BigDecimal profitLoss;
//
//    public ExitProcessingResult(
//        EntryLot entryLot,
//        int quantity,
//        BigDecimal costBasis,
//        BigDecimal exitValue,
//        BigDecimal profitLoss) {
//      this.entryLot = entryLot;
//      this.quantity = quantity;
//      this.costBasis = costBasis;
//      this.exitValue = exitValue;
//      this.profitLoss = profitLoss;
//    }

//    public EntryLot getEntryLot() {
//      return entryLot;
//    }
//
//    public int getQuantity() {
//      return quantity;
//    }
//
//    public BigDecimal getCostBasis() {
//      return costBasis;
//    }
//
//    public BigDecimal getExitValue() {
//      return exitValue;
//    }
//
//    public BigDecimal getProfitLoss() {
//      return profitLoss;
//    }
//  }

//  // Novo método: cria operação consolidada só para a saída atual
//  private Operation createConsolidatedOperationForCurrentExit(
//      Position position, Operation exitOperation, List<ExitRecord> exitRecords) {
//    if (exitRecords == null || exitRecords.isEmpty()) return null;
//
//    BigDecimal totalProfitLoss =
//        exitRecords.stream()
//            .map(ExitRecord::getProfitLoss)
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//    int totalExitedQuantity = exitRecords.stream().mapToInt(ExitRecord::getQuantity).sum();
//    BigDecimal totalExitValue =
//        exitRecords.stream()
//            .map(r -> r.getExitUnitPrice().multiply(BigDecimal.valueOf(r.getQuantity())))
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//    BigDecimal averageExitPrice =
//        totalExitValue.divide(
//            BigDecimal.valueOf(totalExitedQuantity), PRECISION, RoundingMode.HALF_UP);
//    BigDecimal entryTotalValue =
//        exitRecords.stream()
//            .map(r -> r.getEntryUnitPrice().multiply(BigDecimal.valueOf(r.getQuantity())))
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//    BigDecimal entryUnitPrice =
//        entryTotalValue.divide(
//            BigDecimal.valueOf(totalExitedQuantity), PRECISION, RoundingMode.HALF_UP);
//    BigDecimal profitLossPercentage = calculateAverageProfitLossPercentage(exitRecords);
//    OperationStatus status =
//        totalProfitLoss.compareTo(BigDecimal.ZERO) > 0
//            ? OperationStatus.WINNER
//            : OperationStatus.LOSER;
//
//    Operation consolidatedOperation =
//        Operation.builder()
//            .optionSeries(position.getOptionSeries())
//            .brokerage(position.getBrokerage())
//            .analysisHouse(exitOperation.getAnalysisHouse())
//            .transactionType(exitOperation.getTransactionType())
//            .entryDate(position.getOpenDate())
//            .exitDate(exitOperation.getExitDate())
//            .quantity(totalExitedQuantity)
//            .entryUnitPrice(entryUnitPrice)
//            .entryTotalValue(entryTotalValue)
//            .exitUnitPrice(averageExitPrice)
//            .exitTotalValue(totalExitValue)
//            .profitLoss(totalProfitLoss)
//            .profitLossPercentage(profitLossPercentage)
//            .status(status)
//            .user(SecurityUtil.getLoggedUser())
//            .build();
//
//    return operationRepository.save(consolidatedOperation);
//  }

  // Novo método auxiliar para criar operação de saída com tradeType forçado
  private Operation createExitOperationWithTradeType(
      Position position,
      PositionExitRequest request,
      List<ExitRecordDto> exitRecords,
      TradeType tradeType) {
    if (exitRecords == null || exitRecords.isEmpty()) return null;
    BigDecimal totalProfitLoss =
        exitRecords.stream()
            .map(ExitRecordDto::getProfitLoss)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    int totalExitedQuantity = exitRecords.stream().mapToInt(ExitRecordDto::getQuantity).sum();
    BigDecimal totalExitValue =
        exitRecords.stream()
            .map(r -> r.getExitUnitPrice().multiply(BigDecimal.valueOf(r.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal entryTotalValue =
        exitRecords.stream()
            .map(r -> r.getEntryUnitPrice().multiply(BigDecimal.valueOf(r.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal entryUnitPrice =
        entryTotalValue.divide(BigDecimal.valueOf(totalExitedQuantity), 6, RoundingMode.HALF_UP);
    BigDecimal profitLossPercentage = calculateAverageProfitLossPercentageForDtos(exitRecords);
    Operation exitOperation =
        Operation.builder()
            .optionSeries(position.getOptionSeries())
            .brokerage(position.getBrokerage())
            .transactionType(
                position.getDirection() == TransactionType.BUY
                    ? TransactionType.SELL
                    : TransactionType.BUY)
            .tradeType(tradeType)
            .entryDate(position.getOpenDate())
            .exitDate(request.getExitDate())
            .quantity(totalExitedQuantity)
            .entryUnitPrice(entryUnitPrice)
            .entryTotalValue(entryTotalValue)
            .exitUnitPrice(request.getExitUnitPrice())
            .exitTotalValue(totalExitValue)
            .profitLoss(totalProfitLoss)
            .profitLossPercentage(profitLossPercentage)
            .status(OperationStatus.HIDDEN)
            .user(SecurityUtil.getLoggedUser())
            .build();
    return operationRepository.save(exitOperation);
  }
}
