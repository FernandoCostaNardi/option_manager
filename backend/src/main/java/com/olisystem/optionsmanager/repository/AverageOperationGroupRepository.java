package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroupStatus;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AverageOperationGroupRepository
    extends JpaRepository<AverageOperationGroup, UUID> {

  // Buscar grupos por status
  List<AverageOperationGroup> findByStatus(AverageOperationGroupStatus status);

  // Buscar grupo por ID de posição
  AverageOperationGroup findByPositionId(UUID positionId);

  // Buscar grupos com saídas parciais
  List<AverageOperationGroup> findByStatusAndClosedQuantityGreaterThan(
      AverageOperationGroupStatus status, Integer minClosedQuantity);

}
