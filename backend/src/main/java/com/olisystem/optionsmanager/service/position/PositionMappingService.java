package com.olisystem.optionsmanager.service.position;

import com.olisystem.optionsmanager.dto.position.EntryLotResponseDto;
import com.olisystem.optionsmanager.dto.position.PositionOperationResponseDto;
import com.olisystem.optionsmanager.dto.position.PositionResponseDto;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.position.PositionOperation;
import com.olisystem.optionsmanager.repository.position.PositionOperationRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Serviço para mapeamento entre entidades e DTOs de posição */
@Service
@RequiredArgsConstructor
public class PositionMappingService {

  private final PositionOperationRepository positionOperationRepository;

  /** Mapeia uma entidade Position para PositionResponseDto */
  public PositionResponseDto mapToDto(Position position) {
    if (position == null) {
      return null;
    }

    String brokerageName = null;
    String analysisHouseName = null;

    // Buscar operação de entrada mais recente para obter corretora e casa de análise
    Optional<PositionOperation> entryOperation =
        positionOperationRepository.findByPositionOrderByTimestampAsc(position).stream()
            .findFirst();

    if (entryOperation.isPresent() && entryOperation.get().getOperation() != null) {
      if (entryOperation.get().getOperation().getBrokerage() != null) {
        brokerageName = entryOperation.get().getOperation().getBrokerage().getName();
      }
      if (entryOperation.get().getOperation().getAnalysisHouse() != null) {
        analysisHouseName = entryOperation.get().getOperation().getAnalysisHouse().getName();
      }
    }

    return PositionResponseDto.builder()
        .id(position.getId().toString())
        .optionSeriesCode(position.getOptionSeries().getCode())
        .optionType(position.getOptionSeries().getType().toString())
        .baseAssetLogoUrl(position.getOptionSeries().getAsset().getUrlLogo())
        .status(position.getStatus())
        .totalQuantity(position.getTotalQuantity())
        .brokerageName(brokerageName)
        .analysisHouseName(analysisHouseName)
        .build();
  }

  /** Mapeia uma entidade EntryLot para EntryLotResponseDto */
  public EntryLotResponseDto mapToDto(EntryLot entryLot) {
    if (entryLot == null) {
      return null;
    }

    return EntryLotResponseDto.builder()
        .id(entryLot.getId().toString())
        .entryDate(entryLot.getEntryDate())
        .unitPrice(entryLot.getUnitPrice())
        .remainingQuantity(entryLot.getRemainingQuantity())
        .totalValue(entryLot.getTotalValue())
        .build();
  }

  /** Mapeia uma entidade PositionOperation para PositionOperationResponseDto */
  public PositionOperationResponseDto mapToDto(PositionOperation operation) {
    if (operation == null) {
      return null;
    }

    return PositionOperationResponseDto.builder()
        .id(operation.getId().toString())
        .operationId(
            operation.getOperation() != null ? operation.getOperation().getId().toString() : null)
        .type(operation.getType())
        .build();
  }
}
