package com.olisystem.optionsmanager.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisHouse {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String name;
  private String cnpj;
  private String website;
  private String contactEmail;
  private String contactPhone;
  private String subscriptionType; // e.g., "Free", "Premium", "Enterprise"

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user; // Reference to the logged-in user
}
