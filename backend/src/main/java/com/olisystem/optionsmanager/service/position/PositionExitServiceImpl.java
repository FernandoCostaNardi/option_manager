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
import com.olisystem.optionsmanager.service.operation.OperationService;
import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import org.springframework.context.annotation.Lazy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PositionExitServiceImpl implements PositionExitService {

  private final PositionRepository positionRepository;
  private final EntryLotRepository entryLotRepository;
  private final ExitRecordRepository exitRecordRepository;
  private final PositionOperationRepository positionOperationRepository;
  private final OperationRepository operationRepository;
  private final PositionCalculator calculator;
  private final PositionMapper mapper;
  private final PartialExitDetector partialExitDetector;
  private final AverageOperationService averageOperationService;
  private final @Lazy OperationService operationService;

  private static final int PRECISION = 6;

  /** Processa uma solicitação de saída (parcial ou total). */
  @Override
  public PositionExitResult processExit(PositionExitRequest request) {
    log.info("[PositionExitService] Delegando saída para OperationService: {}", request);

    // Montar OperationFinalizationRequest a partir do PositionExitRequest
    OperationFinalizationRequest opRequest = OperationFinalizationRequest.builder()
        .quantity(request.getQuantity())
        .exitUnitPrice(request.getExitUnitPrice())
        .exitDate(request.getExitDate())
        .build();

    // Processar saída via OperationService
    try {
      operationService.createExitOperation(opRequest);
      return PositionExitResult.builder()
          .positionId(request.getPositionId())
          .exitQuantity(request.getQuantity())
          .message("Saída processada via OperationService com sucesso.")
          .build();
    } catch (Exception e) {
      log.error("Erro ao processar saída via OperationService", e);
      return PositionExitResult.builder()
          .positionId(request.getPositionId())
          .exitQuantity(request.getQuantity())
          .message("Erro ao processar saída: " + e.getMessage())
          .build();
    }
  }
} 