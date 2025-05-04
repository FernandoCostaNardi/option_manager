package com.olisystem.optionsmanager.model.operation;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationTarget {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "operation_id", nullable = false)
  private Operation operation;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TargetType type;

  @Column(nullable = false)
  private Integer sequence;

  @Column(nullable = false)
  private BigDecimal value;
}
