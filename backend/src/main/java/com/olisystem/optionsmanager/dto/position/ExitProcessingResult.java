package com.olisystem.optionsmanager.dto.position;

import com.olisystem.optionsmanager.model.position.EntryLot;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Classe para rastrear resultados de processamento de saída Guarda informações sobre o lote
 * processado, quantidade de saída e valores
 */
@Data
@AllArgsConstructor
public class ExitProcessingResult {
  private EntryLot entryLot;
  private Integer quantity;
  private BigDecimal costBasis;
  private BigDecimal exitValue;
  private BigDecimal profitLoss;
}
