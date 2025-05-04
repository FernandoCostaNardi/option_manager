package com.olisystem.optionsmanager.model.option_serie;

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
