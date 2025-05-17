package com.olisystem.optionsmanager.model.position;

public enum ExitStrategy {
  FIFO, // First In, First Out - Para operações de dias diferentes
  LIFO, // Last In, First Out - Para operações do mesmo dia
  AUTO // Aplicação automática - LIFO para mesmo dia, FIFO para dias diferentes
}
