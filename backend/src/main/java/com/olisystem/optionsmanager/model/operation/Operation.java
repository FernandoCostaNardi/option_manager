package com.olisystem.optionsmanager.model.operation;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.olisystem.optionsmanager.model.analysis_house.AnalysisHouse;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.brokerage.Brokerage;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.model.transaction.TransactionType;

import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Operation {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "brokerage_id", nullable = false)
  private Brokerage brokerage;

  @ManyToOne
  @JoinColumn(name = "analysis_house_id")
  private AnalysisHouse analysisHouse;

  @ManyToOne
  @JoinColumn(name = "option_series_id", nullable = false)
  private OptionSerie optionSeries;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionType transactionType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TradeType tradeType;

  @Column(nullable = false)
  private LocalDate entryDate;

  private LocalDate exitDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OperationStatus status;

  @Column(nullable = false)
  private Integer quantity;

  @Column(nullable = false)
  private BigDecimal entryUnitPrice;

  @Column(nullable = false)
  private BigDecimal entryTotalValue;

  private BigDecimal exitUnitPrice;

  private BigDecimal exitTotalValue;

  private BigDecimal profitLoss;

  private BigDecimal profitLossPercentage;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user; // Reference to the logged-in user

  public BigDecimal getResult() {
    if (exitTotalValue != null && entryTotalValue != null) {
      return exitTotalValue.subtract(entryTotalValue);
    }
    return null;
  }
}
