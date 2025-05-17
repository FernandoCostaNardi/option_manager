package com.olisystem.optionsmanager.model.operation;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AverageOperationGroup {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private LocalDate creationDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AverageOperationGroupStatus status;

  @Column(columnDefinition = "TEXT")
  private String notes;

  // Campos adicionais para gerenciar operações parciais
  @Column private Integer totalQuantity;

  @Column private Integer remainingQuantity;

  @Column private Integer closedQuantity;

  @Column private BigDecimal totalProfit;

  @Column private BigDecimal avgExitPrice;

  // Relacionamento com a posição (opcional)
  @Column private UUID positionId;

  @OneToMany(mappedBy = "group")
  private List<AverageOperationItem> items;

  public List<Operation> getOperations() {
    return items.stream().map(AverageOperationItem::getOperation).collect(Collectors.toList());
  }
}
