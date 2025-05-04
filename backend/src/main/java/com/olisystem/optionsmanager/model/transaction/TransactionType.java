package com.olisystem.optionsmanager.model.transaction;

public enum TransactionType {
  BUY("Compra"),
  SELL("Venda");

  private final String description;

  TransactionType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
