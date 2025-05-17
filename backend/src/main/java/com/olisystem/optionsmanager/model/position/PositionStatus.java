package com.olisystem.optionsmanager.model.position;

public enum PositionStatus {
  OPEN, // Posição aberta com quantidade > 0
  CLOSED, // Posição totalmente fechada
  PARTIAL // Posição parcialmente fechada (após saídas parciais)
}
