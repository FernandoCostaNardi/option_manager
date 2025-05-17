package com.olisystem.optionsmanager.repository;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
