package com.olisystem.optionsmanager.repository.position;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.ExitRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExitRecordRepository extends JpaRepository<ExitRecord, UUID> {

  List<ExitRecord> findByEntryLot(EntryLot entryLot);

  List<ExitRecord> findByExitOperation(Operation exitOperation);

  @Query("SELECT e FROM ExitRecord e WHERE e.entryLot.position.id = :positionId")
  List<ExitRecord> findByPositionId(@Param("positionId") UUID positionId);
}
