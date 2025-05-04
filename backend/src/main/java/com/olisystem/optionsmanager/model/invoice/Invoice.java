package com.olisystem.optionsmanager.model.invoice;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

import com.olisystem.optionsmanager.model.brokerage.Brokerage;

import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "brokerage_id")
  private Brokerage brokerage;

  private String clientName;
  private String cpfCnpj;
  private String invoiceNumber;
  private Date tradeDate;

  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InvoiceItem> items;

  private Double totalValue;
  private Double totalFees;
}
