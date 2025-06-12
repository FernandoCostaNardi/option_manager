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

  @Query(
      "SELECT e FROM EntryLot e " +
      "JOIN e.position p " +
      "JOIN p.operations po " +
      "JOIN po.operation o " +
      "WHERE p.user = :user " +
      "AND (:statuses IS NULL OR o.status IN :statuses) " +
      "AND (:entryDateStart IS NULL OR e.entryDate >= :entryDateStart) " +
      "AND (:entryDateEnd IS NULL OR e.entryDate <= :entryDateEnd) " +
      "AND (:brokerageName IS NULL OR LOWER(p.brokerage.name) LIKE LOWER(CONCAT('%', :brokerageName, '%'))) " +
      "AND (:analysisHouseName IS NULL OR LOWER(o.analysisHouse.name) LIKE LOWER(CONCAT('%', :analysisHouseName, '%'))) " +
      "AND (:transactionType IS NULL OR p.direction = :transactionType) " +
      "AND (:optionType IS NULL OR p.optionSeries.type = :optionType) " +
      "AND (:optionSeriesCode IS NULL OR LOWER(p.optionSeries.code) LIKE LOWER(CONCAT('%', :optionSeriesCode, '%')))")
  List<EntryLot> findEntryLotsByOperationCriteria(
      @Param("user") com.olisystem.optionsmanager.model.auth.User user,
      @Param("statuses") List<com.olisystem.optionsmanager.model.operation.OperationStatus> statuses,
      @Param("entryDateStart") LocalDate entryDateStart,
      @Param("entryDateEnd") LocalDate entryDateEnd,
      @Param("brokerageName") String brokerageName,
      @Param("analysisHouseName") String analysisHouseName,
      @Param("transactionType") com.olisystem.optionsmanager.model.transaction.TransactionType transactionType,
      @Param("optionType") com.olisystem.optionsmanager.model.option_serie.OptionType optionType,
      @Param("optionSeriesCode") String optionSeriesCode);
}
