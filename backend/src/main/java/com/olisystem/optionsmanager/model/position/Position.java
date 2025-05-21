package com.olisystem.optionsmanager.model.position;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.brokerage.Brokerage;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "option_series_id", nullable = false)
  private OptionSerie optionSeries;

  @ManyToOne
  @JoinColumn(name = "brokerage_id", nullable = false)
  private Brokerage brokerage;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionType direction;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PositionStatus status;

  @Column(nullable = false)
  private LocalDate openDate;

  private LocalDate closeDate;

  @Column(nullable = false)
  private Integer totalQuantity;

  @Column(nullable = false)
  private BigDecimal averagePrice;

  private BigDecimal totalRealizedProfit;

  private BigDecimal totalRealizedProfitPercentage;

  @Column(nullable = false)
  private Integer remainingQuantity;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @OneToMany(mappedBy = "position", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<EntryLot> entryLots = new ArrayList<>();

  @OneToMany(mappedBy = "position", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PositionOperation> operations = new ArrayList<>();

  // Método para adicionar um lote de entrada
  public void addEntryLot(EntryLot entryLot) {
    entryLots.add(entryLot);
    entryLot.setPosition(this);
  }

  // Método para adicionar uma operação
  public void addOperation(PositionOperation operation) {
    operations.add(operation);
    operation.setPosition(this);
  }

  public List<EntryLot> getEntryLot() {
    return entryLots;
  }
}
