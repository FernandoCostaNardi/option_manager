package com.olisystem.optionsmanager.model.Asset;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String urlLogo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AssetType type;
}
