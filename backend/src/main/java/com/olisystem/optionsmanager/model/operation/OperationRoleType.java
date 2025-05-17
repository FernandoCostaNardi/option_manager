package com.olisystem.optionsmanager.model.operation;

/**
 * Representa o papel/tipo de uma operação no contexto de uma estratégia de investimento. Categoriza
 * as operações de acordo com seu papel no ciclo de vida de uma estratégia.
 */
public enum OperationRoleType {

  /** Operação original que inicia uma estratégia */
  ORIGINAL("Original", true, false),

  /** Operação que representa uma nova entrada na mesma estratégia */
  NEW_ENTRY("Nova entrada na operação", true, false),

  /** Operação que representa uma saída parcial de uma posição */
  PARTIAL_EXIT("Saída Parcial", false, true),

  /** Operação que representa uma saída total de uma posição */
  TOTAL_EXIT("Saída Total", false, true),

  /** Registro que consolida múltiplas entradas para análise */
  CONSOLIDATED_ENTRY("Entradas consolidadas", true, false),

  /** Registro que representa o resultado consolidado de uma operação */
  CONSOLIDATED_RESULT("Resultado Consolidado", false, true);

  private final String description;
  private final boolean isEntry;
  private final boolean isExit;

  /**
   * Construtor para o enum OperationRoleType
   *
   * @param description Descrição textual do tipo de operação
   * @param isEntry Indica se o tipo representa uma entrada de posição
   * @param isExit Indica se o tipo representa uma saída de posição
   */
  OperationRoleType(String description, boolean isEntry, boolean isExit) {
    this.description = description;
    this.isEntry = isEntry;
    this.isExit = isExit;
  }

  /**
   * Obtém a descrição textual do tipo de operação
   *
   * @return A descrição do tipo de operação
   */
  public String getDescription() {
    return description;
  }

  /**
   * Verifica se este tipo representa uma entrada de posição
   *
   * @return true se for uma entrada, false caso contrário
   */
  public boolean isEntry() {
    return isEntry;
  }

  /**
   * Verifica se este tipo representa uma saída de posição
   *
   * @return true se for uma saída, false caso contrário
   */
  public boolean isExit() {
    return isExit;
  }

  /**
   * Verifica se este tipo representa um tipo de consolidação
   *
   * @return true se for um tipo de consolidação, false caso contrário
   */
  public boolean isConsolidation() {
    return this == CONSOLIDATED_ENTRY || this == CONSOLIDATED_RESULT;
  }
}
