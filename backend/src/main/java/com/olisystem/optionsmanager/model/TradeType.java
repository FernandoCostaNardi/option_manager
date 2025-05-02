package com.olisystem.optionsmanager.model;

public enum TradeType {
  SWING("Swing trade"),
  DAY("Day trade");
  private final String description;

  TradeType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
