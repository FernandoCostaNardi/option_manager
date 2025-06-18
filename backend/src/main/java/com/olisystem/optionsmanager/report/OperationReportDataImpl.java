package com.olisystem.optionsmanager.report;

import com.olisystem.optionsmanager.dto.operation.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.operation.OperationItemDto;
import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.mapper.operation.OperationItemMapper;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class OperationReportDataImpl implements OperationReportData {

  private final OperationRepository operationRepository;
  private final OperationItemMapper operationItemMapper;

  @Override
  public OperationSummaryResponseDto findByFilters(OperationFilterCriteria criteria, Pageable pageable) {
    // Buscar operações com os filtros aplicados
    Page<Operation> operationsPage = operationRepository.findAll(pageable);
    List<Operation> operations = operationsPage.getContent();

    // Converter para DTOs
    List<OperationItemDto> operationDtos = operations.stream()
        .map(operationItemMapper::mapToDto)
        .collect(Collectors.toList());

    // Calcular totalizadores
    long totalActive = operations.stream()
        .filter(op -> op.getExitDate() == null)
        .count();

    long totalPut = operations.stream()
        .filter(op -> op.getOptionSeries().getType().toString().contains("PUT"))
        .count();

    long totalCall = operations.stream()
        .filter(op -> op.getOptionSeries().getType().toString().contains("CALL"))
        .count();

    BigDecimal totalEntry = operations.stream()
        .map(Operation::getEntryTotalValue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    long totalWinning = operations.stream()
        .filter(op -> op.getProfitLoss() != null && op.getProfitLoss().compareTo(BigDecimal.ZERO) > 0)
        .count();

    long totalLosing = operations.stream()
        .filter(op -> op.getProfitLoss() != null && op.getProfitLoss().compareTo(BigDecimal.ZERO) < 0)
        .count();

    long totalSwingTrade = operations.stream()
        .filter(op -> op.getTradeType() != null && op.getTradeType().toString().equals("SWING_TRADE"))
        .count();

    long totalDayTrade = operations.stream()
        .filter(op -> op.getTradeType() != null && op.getTradeType().toString().equals("DAY_TRADE"))
        .count();

    BigDecimal totalProfitLoss = operations.stream()
        .map(Operation::getProfitLoss)
        .filter(profit -> profit != null)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalProfitLossPercentage = totalEntry.compareTo(BigDecimal.ZERO) > 0 
        ? totalProfitLoss.multiply(new BigDecimal("100")).divide(totalEntry, 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;

    return OperationSummaryResponseDto.builder()
        .operations(operationDtos)
        .totalActiveOperations(totalActive)
        .totalPutOperations(totalPut)
        .totalCallOperations(totalCall)
        .totalEntryValue(totalEntry)
        .totalWinningOperations(totalWinning)
        .totalLosingOperations(totalLosing)
        .totalSwingTradeOperations(totalSwingTrade)
        .totalDayTradeOperations(totalDayTrade)
        .totalProfitLoss(totalProfitLoss)
        .totalProfitLossPercentage(totalProfitLossPercentage)
        .currentPage(operationsPage.getNumber())
        .totalPages(operationsPage.getTotalPages())
        .totalElements(operationsPage.getTotalElements())
        .pageSize(pageable.getPageSize())
        .build();
  }
} 