package com.olisystem.optionsmanager.dto.operation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.olisystem.optionsmanager.model.operation.TradeType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationFinalizationRequest {

  @NotNull(message = "ID da operação é obrigatório")
  private UUID operationId;

  @NotNull(message = "Data de saída é obrigatória")
  private LocalDate exitDate;

  @NotNull(message = "Preço unitário de saída é obrigatório")
  @DecimalMin(value = "0.01", message = "Preço unitário de saída deve ser maior que zero")
  private BigDecimal exitUnitPrice;

  @Min(value = 1, message = "Quantidade deve ser maior que zero")
  private Integer quantity;
}
