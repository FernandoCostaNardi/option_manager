package com.olisystem.optionsmanager.model.position;

import com.olisystem.optionsmanager.model.operation.Operation;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExitRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "entry_lot_id", nullable = false)
  private EntryLot entryLot;

  @ManyToOne
  @JoinColumn(name = "exit_operation_id", nullable = false)
  private Operation exitOperation;

  @Column(nullable = false)
  private LocalDate exitDate;

  @Column(nullable = false)
  private Integer quantity;

  @Column(nullable = false)
  private BigDecimal entryUnitPrice;

  @Column(nullable = false)
  private BigDecimal exitUnitPrice;

  @Column(nullable = false)
  private BigDecimal profitLoss;

  @Column(nullable = false)
  private BigDecimal profitLossPercentage;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ExitStrategy appliedStrategy;
}
