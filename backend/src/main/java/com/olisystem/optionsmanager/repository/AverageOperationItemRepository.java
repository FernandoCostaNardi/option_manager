package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  // Buscar operações de um grupo que tenham data de saída e não sejam consolidadas
  @Query("SELECT ai FROM AverageOperationItem ai " +
         "WHERE ai.group.id = :groupId " +
         "AND ai.operation.exitDate IS NOT NULL " +
         "AND ai.roleType NOT IN (:consolidatedTypes) " +
         "ORDER BY ai.operation.exitDate ASC")
  List<AverageOperationItem> findExitedNonConsolidatedOperationsByGroupId(
      @Param("groupId") UUID groupId, 
      @Param("consolidatedTypes") List<OperationRoleType> consolidatedTypes);
}
