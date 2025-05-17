package com.olisystem.optionsmanager.dto.position;

import com.olisystem.optionsmanager.model.position.ExitStrategy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionExitRequest {
  private UUID positionId;
  private LocalDate exitDate;
  private Integer quantity; // Quantidade a ser fechada
  private BigDecimal exitUnitPrice;
  private ExitStrategy exitStrategy; // FIFO, LIFO ou AUTO
  private UUID analysisHouseId; // Opcional para registro com análise
}
