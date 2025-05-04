package com.olisystem.optionsmanager.model.operation;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AverageOperationGroup {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private LocalDate creationDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AverageOperationGroupStatus status;

  @Column(columnDefinition = "TEXT")
  private String notes;
}
