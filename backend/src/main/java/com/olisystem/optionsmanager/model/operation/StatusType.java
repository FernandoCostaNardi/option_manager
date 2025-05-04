package com.olisystem.optionsmanager.model.operation;

public enum StatusType {
  ACTIVE("Ativo"),
  INACTIVE("Inativo");

  private final String description;

  StatusType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
