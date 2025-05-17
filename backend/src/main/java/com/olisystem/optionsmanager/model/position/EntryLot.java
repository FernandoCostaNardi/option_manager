package com.olisystem.optionsmanager.model.position;

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
public class EntryLot {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "position_id", nullable = false)
  private Position position;

  @Column(nullable = false)
  private LocalDate entryDate;

  @Column(nullable = false)
  private Integer quantity;

  @Column(nullable = false)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private BigDecimal totalValue;

  @Column(nullable = false)
  private Integer remainingQuantity;

  @Column(nullable = false)
  private Integer sequenceNumber;

  @Column(nullable = false)
  private Boolean isFullyConsumed;
}
