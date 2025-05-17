package com.olisystem.optionsmanager.model.position;

public enum PositionDirection {
  LONG("Comprada"),
  SHORT("Vendida");

  private final String description;

  PositionDirection(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
