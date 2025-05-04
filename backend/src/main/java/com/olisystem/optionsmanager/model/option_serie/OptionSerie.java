package com.olisystem.optionsmanager.model.option_serie;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.olisystem.optionsmanager.model.Asset.Asset;

import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionSerie {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "asset_id", nullable = false)
  private Asset asset;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OptionType type;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private BigDecimal strikePrice;

  @Column(nullable = false)
  private LocalDate expirationDate;
}
