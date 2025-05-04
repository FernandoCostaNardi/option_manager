package com.olisystem.optionsmanager.dto.option;

import com.olisystem.optionsmanager.model.Asset.AssetType;
import com.olisystem.optionsmanager.model.option_serie.OptionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class OptionDataResponseDto {
  private String optionCode;
  private LocalDate optionExpirationDate;
  private BigDecimal optionStrikePrice;
  private OptionType optionType;
  private String baseAsset;
  private String baseAssetName;
  private String baseAssetUrlLogo;
  private AssetType baseAssetType;
}
