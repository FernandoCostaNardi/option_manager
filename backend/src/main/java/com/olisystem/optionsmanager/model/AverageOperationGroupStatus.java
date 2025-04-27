package com.olisystem.optionsmanager.model;

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
