package com.olisystem.optionsmanager.model.operation;

public enum AverageOperationGroupStatus {
  ACTIVE("Ativo"),
  CLOSED("Fechado");

  private final String description;

  AverageOperationGroupStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
