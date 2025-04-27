package com.olisystem.optionsmanager.model;

public enum OptionType {
  CALL("Call"),
  PUT("Put");

  private final String description;

  OptionType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
