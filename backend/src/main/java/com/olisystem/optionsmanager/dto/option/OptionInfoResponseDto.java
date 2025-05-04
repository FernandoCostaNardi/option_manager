package com.olisystem.optionsmanager.dto.option;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.olisystem.optionsmanager.model.option_serie.OptionType;

public class OptionInfoResponseDto {
  private BigDecimal strikePrice;
  private LocalDate expirationDate;
  private OptionType optionType;
  private String baseAsset;

  public OptionInfoResponseDto(
      BigDecimal strike, LocalDate expirationDate, OptionType optionType, String baseAsset) {
    this.strikePrice = strike;
    this.expirationDate = expirationDate;
    this.optionType = optionType;
    this.baseAsset = baseAsset;
  }

  public BigDecimal getStrikePrice() {
    return strikePrice;
  }

  public LocalDate getExpirationDate() {
    return expirationDate;
  }

  public void setStrikePrice(BigDecimal strikePrice) {
    this.strikePrice = strikePrice;
  }

  public void setExpirationDate(LocalDate expirationDate) {
    this.expirationDate = expirationDate;
  }

  public OptionType getOptionType() {
    return optionType;
  }

  public void setOptionType(OptionType optionType) {
    this.optionType = optionType;
  }

  public String getBaseAsset() {
    return baseAsset;
  }

  public void setBaseAsset(String baseAsset) {
    this.baseAsset = baseAsset;
  }

  @Override
  public String toString() {
    return "OptionInfoResponseDto(strikePrice="
        + strikePrice
        + ", expirationDate="
        + expirationDate
        + ")"
        + ", optionType="
        + optionType
        + ", baseAsset="
        + baseAsset
        + ")";
  }
}
