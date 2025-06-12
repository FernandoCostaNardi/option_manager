package com.olisystem.optionsmanager.repository.position;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.position.PositionStatus;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PositionRepository
    extends JpaRepository<Position, UUID>, JpaSpecificationExecutor<Position> {

  Page<Position> findByUserAndStatusIn(User user, List<PositionStatus> statuses, Pageable pageable);

  List<Position> findByUserAndStatusIn(User user, List<PositionStatus> statuses);

  @Query(
      "SELECT p FROM Position p WHERE p.user = :user AND p.optionSeries = :optionSeries "
          + "AND p.direction = :direction AND p.status IN ('OPEN', 'PARTIAL')")
  Optional<Position> findOpenPositionByUserAndOptionSeriesAndDirection(
      @Param("user") User user,
      @Param("optionSeries") OptionSerie optionSeries,
      @Param("direction") TransactionType direction);

  @Query("SELECT p FROM Position p JOIN p.operations po WHERE po.operation.id = :operationId")
  Optional<Position> findByOperationId(@Param("operationId") UUID operationId);

  @Query("SELECT DISTINCT p FROM Position p JOIN p.operations po WHERE po.operation.id IN :operationIds")
  List<Position> findByOperationIds(@Param("operationIds") List<UUID> operationIds);
}
