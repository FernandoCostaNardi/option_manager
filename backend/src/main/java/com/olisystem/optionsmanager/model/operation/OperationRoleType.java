package com.olisystem.optionsmanager.model.operation;

public enum OperationRoleType {
  ORIGINAL("Original"),
  ADDITION("Adição"),
  RESULT("Resultado");

  private final String description;

  OperationRoleType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
