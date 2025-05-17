package com.olisystem.optionsmanager.model.position;

import com.olisystem.optionsmanager.model.operation.Operation;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionOperation {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "position_id", nullable = false)
  private Position position;

  @ManyToOne
  @JoinColumn(name = "operation_id", nullable = false)
  private Operation operation;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PositionOperationType type;

  @Column(nullable = false)
  private LocalDateTime timestamp;

  @Column(nullable = false)
  private Integer sequenceNumber;
}
