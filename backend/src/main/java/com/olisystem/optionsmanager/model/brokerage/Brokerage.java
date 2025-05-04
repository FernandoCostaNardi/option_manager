package com.olisystem.optionsmanager.model.brokerage;

import com.olisystem.optionsmanager.model.auth.User;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Brokerage {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String name;
  private String cnpj;
  private String account;
  private String agency;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user; // Reference to the logged-in user
}
