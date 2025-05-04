package com.olisystem.optionsmanager.model.invoice;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id")
  private Invoice invoice;

  private String asset;
  private String marketType;
  private String operationType; // Compra/Venda
  private Integer quantity;
  private Double price;
  private Double operationValue;
}
