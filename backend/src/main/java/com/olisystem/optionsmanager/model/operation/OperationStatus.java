package com.olisystem.optionsmanager.model.operation;

public enum OperationStatus {
  ACTIVE("Ativa"),
  WINNER("Vencedora"),
  LOSER("Perdedora"),
  HIDDEN("Oculta");

  private final String description;

  OperationStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
