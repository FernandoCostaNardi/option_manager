package com.olisystem.optionsmanager.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AverageOperationComponent {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "group_id", nullable = false)
  private AverageOperationGroup group;

  @ManyToOne
  @JoinColumn(name = "operation_id", nullable = false)
  private Operation operation;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OperationRoleType roleType;

  @Column(nullable = false)
  private Integer sequenceNumber;

  @Column(nullable = false)
  private LocalDate inclusionDate;
}
