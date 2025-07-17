package com.olisystem.optionsmanager.service.position;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.dto.position.*;
import com.olisystem.optionsmanager.exception.ResourceNotFoundException;
import com.olisystem.optionsmanager.mapper.PositionMapper;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.model.position.*;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.repository.position.EntryLotRepository;
import com.olisystem.optionsmanager.repository.position.ExitRecordRepository;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import com.olisystem.optionsmanager.service.option_series.OptionSerieService;
import com.olisystem.optionsmanager.util.SecurityUtil;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação principal do serviço de posições. Esta classe coordena as chamadas para serviços
 * especializados.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PositionServiceImpl implements PositionService {

  private final PositionRepository positionRepository;
  private final EntryLotRepository entryLotRepository;
  private final ExitRecordRepository exitRecordRepository;
  private final OptionSerieService optionSerieService;
  private final PositionMapper mapper;

  // Serviços especializados
  private final PositionQueryService queryService;
  private final PositionEntryService entryService;
  private final PositionExitProcessor exitProcessor;
  private final PositionOperationIntegrationService operationIntegrationService;

  @Override
  public PositionSummaryResponseDto findByStatuses(
      List<PositionStatus> statuses, Pageable pageable) {
    return queryService.findByStatuses(statuses, pageable);
  }

  @Override
  public PositionSummaryResponseDto findByFilters(
      PositionFilterCriteria criteria, Pageable pageable) {
    return queryService.findByFilters(criteria, pageable);
  }

  @Override
  public PositionDto findById(UUID id) {
    Position position =
        positionRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Posição não encontrada"));

    // Verificar se pertence ao usuário atual
    if (!position.getUser().equals(SecurityUtil.getLoggedUser())) {
      throw new ResourceNotFoundException("Posição não encontrada");
    }

    return mapper.toDto(position);
  }

  @Override
  public List<EntryLotDto> findLotsByPositionId(UUID positionId) {
    Position position =
        positionRepository
            .findById(positionId)
            .orElseThrow(() -> new ResourceNotFoundException("Posição não encontrada"));

    if (!position.getUser().equals(SecurityUtil.getLoggedUser())) {
      throw new ResourceNotFoundException("Posição não encontrada");
    }

    List<EntryLot> lots = entryLotRepository.findByPositionOrderByEntryDateAsc(position);
    return mapper.toEntryLotDtoList(lots);
  }

  @Override
  public List<ExitRecordDto> findExitsByPositionId(UUID positionId) {
    Position position =
        positionRepository
            .findById(positionId)
            .orElseThrow(() -> new ResourceNotFoundException("Posição não encontrada"));

    if (!position.getUser().equals(SecurityUtil.getLoggedUser())) {
      throw new ResourceNotFoundException("Posição não encontrada");
    }

    List<ExitRecord> exits = exitRecordRepository.findByPositionId(positionId);
    return mapper.toExitRecordDtoList(exits);
  }

  @Override
  public Position createPositionFromOperation(Operation operation) {
    return entryService.createPositionFromOperation(operation);
  }

  @Override
  public Position addEntryToPosition(Position position,  Operation operation) {
    return entryService.addEntryToPosition(position, operation);
  }

  @Override
  public Position consumePositionForExit(Position position, Operation exitOperation) {
    // Calcular quantidade a ser consumida
    int quantityToConsume = exitOperation.getQuantity();
    int currentRemaining = position.getRemainingQuantity();
    
    if (quantityToConsume > currentRemaining) {
      throw new IllegalArgumentException(
        String.format("Quantidade a consumir (%d) maior que quantidade restante (%d)", 
          quantityToConsume, currentRemaining));
    }
    
    // Atualizar quantidade restante
    int newRemaining = currentRemaining - quantityToConsume;
    position.setRemainingQuantity(newRemaining);
    
    // Se toda a posição foi consumida, fechar
    if (newRemaining == 0) {
      position.setStatus(PositionStatus.CLOSED);
      position.setCloseDate(exitOperation.getExitDate());
    } else {
      position.setStatus(PositionStatus.PARTIAL);
    }
    
    // Calcular e atualizar P&L realizado
    BigDecimal exitValue = exitOperation.getExitTotalValue();
    if (exitValue == null) {
      // Se não veio preenchido, calcular: preço de venda * quantidade
      exitValue = exitOperation.getEntryUnitPrice().multiply(BigDecimal.valueOf(quantityToConsume));
    }
    BigDecimal entryValue = position.getAveragePrice().multiply(BigDecimal.valueOf(quantityToConsume));
    BigDecimal profitLoss = exitValue.subtract(entryValue);
    
    BigDecimal currentRealizedProfit = position.getTotalRealizedProfit() != null ? 
      position.getTotalRealizedProfit() : BigDecimal.ZERO;
    position.setTotalRealizedProfit(currentRealizedProfit.add(profitLoss));
    
    // Calcular percentual de P&L
    if (entryValue.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal profitLossPercentage = profitLoss.divide(entryValue, 6, java.math.RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100));
      position.setTotalRealizedProfitPercentage(profitLossPercentage);
    }
    
    return positionRepository.save(position);
  }

  @Override
  public PositionDto processEntry(PositionEntryRequest request) {
    return null;
  }

  @Override
  public PositionExitResult processExit(PositionExitRequest request) {
    return exitProcessor.processExit(request);
  }

  @Override
  public Optional<Position> findCompatiblePosition(
      OptionSerie optionSeries, TransactionType direction) {
    // ✅ CORREÇÃO: Este método não tem acesso à corretora, então não pode ser usado
    // para busca precisa. Deve ser usado apenas para verificações básicas.
    // log.warn("⚠️ findCompatiblePosition chamado sem corretora - uso limitado"); // Original code had this line commented out
    return Optional.empty(); // Não retornar posição sem corretora específica
  }

  @Override
  public PositionDto checkCompatiblePosition(
      String optionSeriesCode, TransactionType transactionType) {
    OptionSerie optionSerie = optionSerieService.getOptionSerieByCode(optionSeriesCode);
    if (optionSerie == null) {
      return null;
    }

    Optional<Position> positionOpt = findCompatiblePosition(optionSerie, transactionType);
    return positionOpt.map(mapper::toDto).orElse(null);
  }

  @Override
  public void updatePositionAfterOperationUpdate(UUID operationId, OperationDataRequest request) {
    operationIntegrationService.updatePositionAfterOperationUpdate(operationId, request);
  }
}
