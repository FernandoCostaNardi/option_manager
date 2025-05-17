package com.olisystem.optionsmanager.repository.position;

import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.Position;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EntryLotRepository extends JpaRepository<EntryLot, UUID> {

  List<EntryLot> findByPositionOrderByEntryDateAsc(Position position);

  List<EntryLot> findByPositionOrderByEntryDateDesc(Position position);

  @Query(
      "SELECT e FROM EntryLot e WHERE e.position = :position AND e.remainingQuantity > 0 "
          + "AND e.entryDate = :date ORDER BY e.sequenceNumber DESC")
  List<EntryLot> findAvailableLotsSameDayLIFO(
      @Param("position") Position position, @Param("date") LocalDate date);

  @Query(
      "SELECT e FROM EntryLot e WHERE e.position = :position AND e.remainingQuantity > 0 "
          + "AND e.entryDate < :date ORDER BY e.entryDate ASC, e.sequenceNumber ASC")
  List<EntryLot> findAvailableLotsPreviousDaysFIFO(
      @Param("position") Position position, @Param("date") LocalDate date);

  @Query(
      "SELECT e FROM EntryLot e WHERE e.position = :position AND e.remainingQuantity > 0 "
          + "ORDER BY e.entryDate ASC, e.sequenceNumber ASC")
  List<EntryLot> findAvailableLotsFIFO(@Param("position") Position position);

  @Query(
      "SELECT e FROM EntryLot e WHERE e.position = :position AND e.remainingQuantity > 0 "
          + "ORDER BY e.entryDate DESC, e.sequenceNumber DESC")
  List<EntryLot> findAvailableLotsLIFO(@Param("position") Position position);

  @Query(
      "SELECT e FROM EntryLot e WHERE e.position = :position AND e.remainingQuantity =:quantity "
          + "ORDER BY e.entryDate DESC, e.sequenceNumber DESC")
  List<EntryLot> findByPositionAndRemainingQuantityGreaterThan(Position position, int quantity);
}
