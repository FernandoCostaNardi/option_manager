package com.olisystem.optionsmanager.repository.position;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.position.PositionOperation;
import com.olisystem.optionsmanager.model.position.PositionOperationType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PositionOperationRepository extends JpaRepository<PositionOperation, UUID> {

  List<PositionOperation> findByPositionOrderByTimestampAsc(Position position);

  List<PositionOperation> findByPositionAndTypeOrderByTimestampAsc(
      Position position, PositionOperationType type);

  Optional<PositionOperation> findByOperation(Operation operation);
}
