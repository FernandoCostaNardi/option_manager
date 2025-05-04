package com.olisystem.optionsmanager.dto.operation;

import com.olisystem.optionsmanager.model.Asset.AssetType;
import com.olisystem.optionsmanager.model.operation.OperationTarget;
import com.olisystem.optionsmanager.model.option_serie.OptionType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;

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
public class OperationDataRequest {
  private UUID id;
  private String baseAssetCode;
  private String baseAssetName;
  private AssetType baseAssetType;
  private String baseAssetLogoUrl;

  private String optionSeriesCode;
  private OptionType optionSeriesType;
  private BigDecimal optionSeriesStrikePrice;
  private LocalDate optionSeriesExpirationDate;

  private List<OperationTarget> targets;

  private UUID brokerageId;
  private UUID analysisHouseId;
  private TransactionType transactionType;
  private LocalDate entryDate;
  private LocalDate exitDate;
  private Integer quantity;
  private BigDecimal entryUnitPrice;
}
