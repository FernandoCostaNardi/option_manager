package com.olisystem.optionsmanager.dto.position;

import com.olisystem.optionsmanager.model.option_serie.OptionType;
import com.olisystem.optionsmanager.model.position.PositionStatus;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionDto {
  private UUID id;
  private String optionSeriesCode;
  private OptionType optionType;
  private BigDecimal strikePrice;
  private LocalDate expirationDate;
  private String baseAssetCode;
  private String baseAssetName;
  private String baseAssetLogoUrl;
  private TransactionType direction;
  private PositionStatus status;
  private LocalDate openDate;
  private LocalDate closeDate;
  private Integer totalQuantity;
  private BigDecimal averagePrice;
  private BigDecimal totalEntryValue;
  private Integer remainingQuantity;
  private BigDecimal totalRealizedProfit;
  private BigDecimal totalRealizedProfitPercentage;
  private String brokerageName;
  private UUID brokerageId;
  private Integer entryLotsCount;
  private Integer exitRecordsCount;
  private Integer daysOpen;
  private Boolean hasMultipleEntries;
  private Boolean hasPartialExits;
}
