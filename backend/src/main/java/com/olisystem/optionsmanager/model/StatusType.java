package com.olisystem.optionsmanager.model;

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
