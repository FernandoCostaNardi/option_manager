package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationTarget;
import com.olisystem.optionsmanager.model.operation.TargetType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface OperationTargetRepository extends JpaRepository<OperationTarget, UUID> {
  List<OperationTarget> findByOperation_Id(UUID operationId);

  List<OperationTarget> findByOperation_IdAndType(UUID operationId, TargetType type);

  @Modifying
  @Transactional
  void deleteByOperation(Operation operation);
}
