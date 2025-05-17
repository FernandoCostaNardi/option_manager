package com.olisystem.optionsmanager.dto.position;

import com.olisystem.optionsmanager.model.position.PositionOperationType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionOperationResponseDto {
  private String id;
  private String operationId;
  private PositionOperationType type;
  private Integer quantity;
  private BigDecimal unitPrice;
  private BigDecimal totalAmount;
  private LocalDate date;
  private Integer sequence;
}
