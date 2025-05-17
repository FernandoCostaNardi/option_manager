package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AverageOperationItemRepository extends JpaRepository<AverageOperationItem, UUID> {

  // Buscar item pelo ID da operação e tipo de papel (roleType)
  AverageOperationItem findByOperation_IdAndRoleType(UUID operationId, OperationRoleType roleType);

  // Buscar todos os itens de um grupo
  List<AverageOperationItem> findByGroup(AverageOperationGroup group);

  // Buscar todos os itens de um grupo por ID do grupo
  List<AverageOperationItem> findByGroup_Id(UUID groupId);

  // Buscar itens por grupo e tipo de papel
  List<AverageOperationItem> findByGroupAndRoleType(
      AverageOperationGroup group, OperationRoleType roleType);

  // Buscar todos os itens de uma operação
  List<AverageOperationItem> findByOperation_Id(UUID operationId);

  AverageOperationItem findByOperation(Operation operation);
}
