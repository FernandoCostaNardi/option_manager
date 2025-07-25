package com.olisystem.optionsmanager.service.operation;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.dto.operation.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OperationService {

  /**
   * Cria uma nova operação a partir dos dados fornecidos
   */
  Operation createOperation(OperationDataRequest request);

  /**
   * Cria uma nova operação usando um usuário específico
   * (Para processamento de invoices sem contexto de segurança)
   */
  Operation createOperation(OperationDataRequest request, User user);

  /**
   * Faz uma saída total ou parcial de uma operação ativa
   */
  Operation createExitOperation(OperationFinalizationRequest request);

  /**
   * Faz uma saída total ou parcial usando um usuário específico
   * (Para processamento de invoices sem contexto de segurança)
   */
  Operation createExitOperation(OperationFinalizationRequest request, User user);

  /**
   * Atualiza uma operação existente
   */
  void updateOperation(UUID id, OperationDataRequest request);

  /**
   * Busca operações com base nos critérios de filtro
   */
  OperationSummaryResponseDto findByFilters(OperationFilterCriteria criteria, Pageable pageable);

  /**
   * Busca operações por status
   */
  OperationSummaryResponseDto findByStatuses(List<OperationStatus> status, Pageable pageable);

}
