package com.olisystem.optionsmanager.model;

public enum TargetType {
  TARGET("Target"),
  STOP_LOSS("Stop Loss");

  private final String description;

  TargetType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
