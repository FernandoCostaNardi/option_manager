package com.olisystem.optionsmanager.model;

public enum AssetType {
  STOCK("Ações"),
  ETF("ETFs"),
  REIT("FIIs"),
  OPTION("Opções");

  private final String description;

  AssetType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
