package com.olisystem.optionsmanager.dto.position;

import com.olisystem.optionsmanager.model.Asset.AssetType;
import com.olisystem.optionsmanager.model.operation.OperationTarget;
import com.olisystem.optionsmanager.model.option_serie.OptionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionEntryRequest {
  private UUID positionId; // Para adição a posição existente

  // Dados do ativo/opção
  private String baseAssetCode;
  private String baseAssetName;
  private AssetType baseAssetType;
  private String baseAssetLogoUrl;
  private String optionSeriesCode;
  private OptionType optionSeriesType;
  private BigDecimal optionSeriesStrikePrice;
  private LocalDate optionSeriesExpirationDate;

  // Dados da operação
  private UUID brokerageId;
  private UUID analysisHouseId;
  private LocalDate entryDate;
  private Integer quantity;
  private BigDecimal unitPrice;
  private List<OperationTarget> targets;
}
