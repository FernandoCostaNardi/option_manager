package com.olisystem.optionsmanager.model.invoice;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "invoice_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id")
  private Invoice invoice;

  // === DADOS DA OPERAÇÃO ===
  @Column(name = "sequence_number")
  private Integer sequenceNumber;

  @Column(name = "operation_type", length = 10)
  private String operationType; // 'C' (Compra) ou 'V' (Venda)

  @Column(name = "market_type", length = 50)
  private String marketType; // 'VISTA', 'OPCAO DE COMPRA', etc.

  @Column(name = "asset_specification")
  private String assetSpecification; // Código completo do ativo/opção

  @Column(name = "asset_code", length = 20)
  private String assetCode; // Código extraído (ex: PETR4, CSANE165)

  @Column(name = "expiration_date")
  private LocalDate expirationDate; // Para opções

  @Column(name = "strike_price", precision = 15, scale = 4)
  private BigDecimal strikePrice; // Para opções

  // === QUANTIDADES E PREÇOS ===
  @Column(name = "quantity")
  private Integer quantity;

  @Column(name = "unit_price", precision = 15, scale = 4)
  private BigDecimal unitPrice;

  @Column(name = "total_value", precision = 15, scale = 4)
  private BigDecimal totalValue;

  // === OBSERVAÇÕES ===
  @Column(name = "observations")
  private String observations; // 'D' para Day Trade, '#' negócio direto, etc.

  @Column(name = "is_day_trade")
  private Boolean isDayTrade; // Calculado automaticamente

  @Column(name = "is_direct_deal")
  private Boolean isDirectDeal;

  // === CONTROLE ===
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // === CAMPOS LEGADOS (manter compatibilidade) ===
  @Deprecated
  private String asset;

  @Deprecated
  private Double price;

  @Deprecated
  private Double operationValue;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // === MÉTODOS DE CONVERSÃO (compatibilidade) ===
  public String getAsset() {
    return assetCode != null ? assetCode : asset;
  }

  public Double getPrice() {
    return unitPrice != null ? unitPrice.doubleValue() : price;
  }

  public Double getOperationValue() {
    return totalValue != null ? totalValue.doubleValue() : operationValue;
  }
}
