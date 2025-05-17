package com.olisystem.optionsmanager.validation;

import com.olisystem.optionsmanager.dto.position.PositionEntryRequest;
import com.olisystem.optionsmanager.dto.position.PositionExitRequest;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import jakarta.validation.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validador para operações relacionadas a posições. Verifica se os dados fornecidos para criação,
 * entrada e saída são válidos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PositionValidator {

  private final PositionRepository positionRepository;

  /** Valida um pedido de entrada em posição. */
  public void validateEntryRequest(PositionEntryRequest request) {
    List<String> errors = new ArrayList<>();

    // Validar dados mínimos necessários
    if (request.getOptionSeriesCode() == null || request.getOptionSeriesCode().isBlank()) {
      errors.add("Código da opção é obrigatório");
    }

    if (request.getOptionSeriesType() == null) {
      errors.add("Tipo da opção é obrigatório");
    }

    if (request.getOptionSeriesStrikePrice() == null
        || request.getOptionSeriesStrikePrice().compareTo(BigDecimal.ZERO) <= 0) {
      errors.add("Preço de exercício deve ser maior que zero");
    }

    if (request.getOptionSeriesExpirationDate() == null) {
      errors.add("Data de expiração é obrigatória");
    } else if (request.getOptionSeriesExpirationDate().isBefore(LocalDate.now())) {
      errors.add("Data de expiração não pode ser no passado");
    }

    if (request.getBaseAssetCode() == null || request.getBaseAssetCode().isBlank()) {
      errors.add("Código do ativo base é obrigatório");
    }

    if (request.getBrokerageId() == null) {
      errors.add("Corretora é obrigatória");
    }

    if (request.getEntryDate() == null) {
      errors.add("Data de entrada é obrigatória");
    } else if (request.getEntryDate().isAfter(LocalDate.now())) {
      errors.add("Data de entrada não pode ser no futuro");
    }

    if (request.getQuantity() == null || request.getQuantity() <= 0) {
      errors.add("Quantidade deve ser maior que zero");
    }

    if (request.getUnitPrice() == null || request.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
      errors.add("Preço unitário deve ser maior que zero");
    }

    // Validar posição existente, se especificada
    if (request.getPositionId() != null) {
      positionRepository
          .findById(request.getPositionId())
          .orElseThrow(() -> new ValidationException("Posição informada não existe"));
    }

    if (!errors.isEmpty()) {
      throw new ValidationException("Erros de validação: " + String.join(", ", errors));
    }
  }

  /** Valida um pedido de saída de posição. */
  public void validateExitRequest(PositionExitRequest request, Position position) {
    List<String> errors = new ArrayList<>();

    // Validar dados mínimos necessários
    if (request.getPositionId() == null) {
      errors.add("ID da posição é obrigatório");
    }

    if (request.getExitDate() == null) {
      errors.add("Data de saída é obrigatória");
    } else {
      if (request.getExitDate().isBefore(position.getOpenDate())) {
        errors.add("Data de saída não pode ser anterior à data de abertura da posição");
      }
      if (request.getExitDate().isAfter(LocalDate.now())) {
        errors.add("Data de saída não pode ser no futuro");
      }
    }

    if (request.getQuantity() == null || request.getQuantity() <= 0) {
      errors.add("Quantidade deve ser maior que zero");
    } else if (request.getQuantity() > position.getRemainingQuantity()) {
      errors.add("Quantidade de saída não pode ser maior que a quantidade restante da posição");
    }

    if (request.getExitUnitPrice() == null
        || request.getExitUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
      errors.add("Preço unitário de saída deve ser maior que zero");
    }

    if (request.getExitStrategy() == null) {
      errors.add("Estratégia de saída é obrigatória");
    }

    if (!errors.isEmpty()) {
      throw new ValidationException("Erros de validação: " + String.join(", ", errors));
    }
  }
}
