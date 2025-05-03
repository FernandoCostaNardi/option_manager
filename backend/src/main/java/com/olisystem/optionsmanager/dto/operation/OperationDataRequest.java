package com.olisystem.optionsmanager.dto;

import com.olisystem.optionsmanager.model.AssetType;
import com.olisystem.optionsmanager.model.OperationTarget;
import com.olisystem.optionsmanager.model.OptionType;
import com.olisystem.optionsmanager.model.TransactionType;
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
