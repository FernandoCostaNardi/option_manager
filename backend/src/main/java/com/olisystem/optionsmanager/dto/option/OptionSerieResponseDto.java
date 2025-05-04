package com.olisystem.optionsmanager.dto.option;

import com.olisystem.optionsmanager.model.option_serie.OptionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OptionSerieResponseDto {
  private UUID id;
  private UUID assetId;
  private String assetCode;
  private String assetName;
  private OptionType type;
  private String code;
  private BigDecimal strikePrice;
  private LocalDate expirationDate;
}
