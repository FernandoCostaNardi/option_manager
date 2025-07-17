package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.model.transaction.TransactionType;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OperationRepository
    extends JpaRepository<Operation, UUID>, JpaSpecificationExecutor<Operation> {
  // Método corrigido - a ordem dos parâmetros deve ser: statusList, user, pageable
  Page<Operation> findByStatusInAndUser(
      List<OperationStatus> statuses, User user, Pageable pageable);

  // Adicione este método se precisar buscar apenas por status sem usuário
  Page<Operation> findByStatusIn(List<OperationStatus> statuses, Pageable pageable);

  Page<Operation> findByUserAndStatusIn(
      User user, List<OperationStatus> statuses, Pageable pageableWithoutSort);

  // Método para buscar todas as operações (sem paginação) por usuário e status
  List<Operation> findByUserAndStatusIn(User user, List<OperationStatus> statuses);

  // Método para buscar uma operação por optionSeries, user e status
  Operation findByOptionSeriesAndUserAndStatus(OptionSerie optionSerie, User currentUser, OperationStatus active);
  
  // ✅ NOVO MÉTODO: Busca operação por optionSeries, user, status E transactionType
  Operation findByOptionSeriesAndUserAndStatusAndTransactionType(
      OptionSerie optionSerie, User currentUser, OperationStatus status, TransactionType transactionType);
  
  // ✅ NOVO MÉTODO: Busca operações por optionSeries, user E transactionType (sem filtro de status)
  List<Operation> findByOptionSeriesAndUserAndTransactionType(
      OptionSerie optionSerie, User currentUser, TransactionType transactionType);
  
  // ✅ NOVO MÉTODO: Busca operações por optionSeries e user (sem filtros)
  List<Operation> findByOptionSeriesAndUser(OptionSerie optionSerie, User currentUser);
  
  // === MÉTODOS PARA VALIDAÇÃO DE DUPLICATAS (FASE 2) ===
  
  /**
   * Busca operações por data de entrada e código da série (contendo)
   */
  @Query("SELECT o FROM Operation o WHERE o.entryDate = :entryDate AND UPPER(o.optionSeries.code) LIKE UPPER(CONCAT('%', :assetCode, '%'))")
  List<Operation> findByEntryDateAndOptionSeriesCodeContaining(
      @Param("entryDate") java.time.LocalDate entryDate, 
      @Param("assetCode") String assetCode);
  
  /**
   * Busca operações por usuário, data e código do ativo
   */
  @Query("SELECT o FROM Operation o WHERE o.user.id = :userId AND o.entryDate = :entryDate AND UPPER(o.optionSeries.code) LIKE UPPER(CONCAT('%', :assetCode, '%'))")
  List<Operation> findByUserAndEntryDateAndAssetCode(
      @Param("userId") UUID userId,
      @Param("entryDate") java.time.LocalDate entryDate, 
      @Param("assetCode") String assetCode);
}
