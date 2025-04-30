package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.OperationTarget;
import com.olisystem.optionsmanager.model.TargetType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationTargetRepository extends JpaRepository<OperationTarget, UUID> {
  List<OperationTarget> findByOperation_Id(UUID operationId);

  List<OperationTarget> findByOperation_IdAndType(UUID operationId, TargetType type);
}
