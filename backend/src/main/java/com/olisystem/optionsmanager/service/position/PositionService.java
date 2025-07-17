package com.olisystem.optionsmanager.service.position;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.dto.position.*;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.position.PositionStatus;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface PositionService {

  // Métodos para consulta
  PositionSummaryResponseDto findByStatuses(List<PositionStatus> statuses, Pageable pageable);

  PositionSummaryResponseDto findByFilters(PositionFilterCriteria criteria, Pageable pageable);

  PositionDto findById(UUID id);

  List<EntryLotDto> findLotsByPositionId(UUID positionId);

  List<ExitRecordDto> findExitsByPositionId(UUID positionId);

  // Métodos para operações em posições
  Position createPositionFromOperation(Operation operation);

  Position addEntryToPosition(Position position, Operation operation);

  Position consumePositionForExit(Position position, Operation exitOperation);

  PositionDto processEntry(PositionEntryRequest request);

  PositionExitResult processExit(PositionExitRequest request);

  // Métodos de suporte
  Optional<Position> findCompatiblePosition(OptionSerie optionSeries, TransactionType direction);

  PositionDto checkCompatiblePosition(String optionSeriesCode, TransactionType transactionType);

  void updatePositionAfterOperationUpdate(UUID operationId, OperationDataRequest request);
}
