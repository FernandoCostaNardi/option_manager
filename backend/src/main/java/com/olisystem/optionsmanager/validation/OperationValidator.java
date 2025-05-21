package com.olisystem.optionsmanager.validation;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.exception.InvalidOperationException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class OperationValidator {

  public void validateCreate(OperationDataRequest request) {
    validateRequiredFields(request);
    validateDates(request);
    validateQuantities(request);
  }

  public void validateUpdate(OperationDataRequest request) {
    validateRequiredFields(request);
    validateDates(request);
    validateQuantities(request);
  }

  private void validateRequiredFields(OperationDataRequest request) {
    if (request.getBaseAssetCode() == null || request.getBaseAssetCode().isEmpty()) {
      throw new InvalidOperationException("Código do ativo base é obrigatório");
    }
    if (request.getOptionSeriesCode() == null || request.getOptionSeriesCode().isEmpty()) {
      throw new InvalidOperationException("Código da série de opções é obrigatório");
    }
    if (request.getEntryDate() == null) {
      throw new InvalidOperationException("Data de entrada é obrigatória");
    }
    if (request.getQuantity() <= 0) {
      throw new InvalidOperationException("Quantidade deve ser maior que zero");
    }
    if (request.getEntryUnitPrice() == null
        || request.getEntryUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidOperationException("Preço unitário de entrada deve ser maior que zero");
    }
  }

  private void validateDates(OperationDataRequest request) {
    if (request.getExitDate() != null && request.getExitDate().isBefore(request.getEntryDate())) {
      throw new InvalidOperationException("Data de saída não pode ser anterior à data de entrada");
    }
  }

  private void validateQuantities(OperationDataRequest request) {
    if (request.getQuantity() <= 0) {
      throw new InvalidOperationException("Quantidade deve ser maior que zero");
    }
    if (request.getEntryUnitPrice() == null
        || request.getEntryUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidOperationException("Preço unitário de entrada deve ser maior que zero");
    }
  }

    public void validateCreateExitOperation(OperationFinalizationRequest request) {
      if (request.getOperationId() == null ) {
        throw new InvalidOperationException("ID da operação é obrigatório");
      }
      if (request.getExitDate() == null) {
        throw new InvalidOperationException("Data de saída é obrigatória");
      }
      if (request.getExitUnitPrice() == null
          || request.getExitUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
        throw new InvalidOperationException("Preço unitário de saída deve ser maior que zero");
      }
      if (request.getQuantity() <= 0) {
        throw new InvalidOperationException("Quantidade deve ser maior que zero");
      }
      
    }
}
