package com.olisystem.optionsmanager.model.invoice;

import com.olisystem.optionsmanager.model.brokerage.Brokerage;
import com.olisystem.optionsmanager.model.auth.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "brokerage_id")
  private Brokerage brokerage;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  // === METADADOS DA NOTA ===
  @Column(name = "invoice_number", length = 50)
  private String invoiceNumber;

  @Column(name = "trading_date")
  private LocalDate tradingDate;

  @Column(name = "settlement_date")
  private LocalDate settlementDate;

  // === DADOS DO CLIENTE ===
  @Column(name = "client_name", length = 1000)
  private String clientName;

  @Column(name = "client_cpf", length = 20)
  private String cpfCnpj;

  @Column(name = "client_code", length = 100)
  private String clientCode;

  // === RESUMO FINANCEIRO ===
  @Column(name = "gross_operations_value", precision = 15, scale = 4)
  private BigDecimal grossOperationsValue;

  @Column(name = "net_operations_value", precision = 15, scale = 4)
  private BigDecimal netOperationsValue;

  @Column(name = "total_costs", precision = 15, scale = 4)
  private BigDecimal totalCosts;

  @Column(name = "total_taxes", precision = 15, scale = 4)
  private BigDecimal totalTaxes;

  @Column(name = "net_settlement_value", precision = 15, scale = 4)
  private BigDecimal netSettlementValue;

  // === IMPOSTOS E TAXAS ===
  @Column(name = "liquidation_tax", precision = 15, scale = 4)
  private BigDecimal liquidationTax;

  @Column(name = "registration_tax", precision = 15, scale = 4)
  private BigDecimal registrationTax;

  @Column(name = "emoluments", precision = 15, scale = 4)
  private BigDecimal emoluments;

  @Column(name = "ana_tax", precision = 15, scale = 4)
  private BigDecimal anaTax;

  @Column(name = "term_options_tax", precision = 15, scale = 4)
  private BigDecimal termOptionsTax;

  @Column(name = "brokerage_fee", precision = 15, scale = 4)
  private BigDecimal brokerageFee;

  @Column(name = "iss", precision = 15, scale = 4)
  private BigDecimal iss;

  @Column(name = "pis", precision = 15, scale = 4)
  private BigDecimal pis;

  @Column(name = "cofins", precision = 15, scale = 4)
  private BigDecimal cofins;

  // === IRRF ===
  @Column(name = "irrf_day_trade_basis", precision = 15, scale = 4)
  private BigDecimal irrfDayTradeBasis;

  @Column(name = "irrf_day_trade_value", precision = 15, scale = 4)
  private BigDecimal irrfDayTradeValue;

  @Column(name = "irrf_common_basis", precision = 15, scale = 4)
  private BigDecimal irrfCommonBasis;

  @Column(name = "irrf_common_value", precision = 15, scale = 4)
  private BigDecimal irrfCommonValue;

  // === DADOS BRUTOS ===
  @Column(name = "raw_content", columnDefinition = "TEXT")
  private String rawContent;

  @Column(name = "file_hash", length = 64)
  private String fileHash;

  // === RELACIONAMENTOS ===
  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InvoiceItem> items = new java.util.ArrayList<>();

  // === CONTROLE ===
  @Column(name = "imported_at")
  private LocalDateTime importedAt;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // === CAMPOS LEGADOS (manter compatibilidade) ===
  @Deprecated
  private Date tradeDate;

  @Deprecated
  private Double totalValue;

  @Deprecated
  private Double totalFees;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (importedAt == null) {
      importedAt = LocalDateTime.now();
    }
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
