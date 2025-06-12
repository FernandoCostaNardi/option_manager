package com.olisystem.optionsmanager.service.operation.average;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
public class AveragePriceCalculator {

    private static final int PRECISION = 6;

    /**
     * Calcula o novo preço médio ponderado após uma saída parcial
     *
     * @param currentValue Valor atual restante na operação
     * @param exitTotalValue Valor TOTAL recebido na saída (não apenas lucro)
     * @param remainingQuantity Quantidade que resta na operação
     * @return Novo preço médio ponderado
     */
    public BigDecimal calculateNewAveragePrice(BigDecimal currentValue,
                                               BigDecimal exitTotalValue,
                                               Integer remainingQuantity) {

        log.debug("Calculando novo preço médio: valor_atual={}, valor_recebido={}, quantidade_restante={}",
                currentValue, exitTotalValue, remainingQuantity);

        // Validações
        if (currentValue == null || exitTotalValue == null || remainingQuantity == null) {
            throw new IllegalArgumentException("Parâmetros não podem ser nulos");
        }

        if (remainingQuantity < 0) {
            throw new IllegalArgumentException("Quantidade restante não pode ser negativa");
        }

        if (remainingQuantity == 0) {
            log.debug("Quantidade restante é zero - operação fechada completamente");
            return BigDecimal.ZERO;
        }

        // ✅ FÓRMULA CORRETA: Descontar valor total recebido, não apenas lucro
        BigDecimal remainingValue = currentValue.subtract(exitTotalValue);

        // Validar se valor restante não ficou negativo (situação anômala)
        if (remainingValue.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("ATENÇÃO: Valor restante ficou negativo ({}) - possível inconsistência nos dados", remainingValue);
            // Em caso de valor negativo, retornar zero para evitar preço negativo
            remainingValue = BigDecimal.ZERO;
        }

        BigDecimal newAveragePrice = remainingValue.divide(
                BigDecimal.valueOf(remainingQuantity),
                PRECISION,
                RoundingMode.HALF_UP);

        log.info("Novo preço médio calculado: {} (valor_restante={}, quantidade={})",
                newAveragePrice, remainingValue, remainingQuantity);

        return newAveragePrice;
    }

    /**
     * Calcula o valor total restante após uma saída
     *
     * @param currentValue Valor atual na operação
     * @param exitTotalValue Valor total recebido na saída
     * @return Valor restante na operação
     */
    public BigDecimal calculateRemainingValue(BigDecimal currentValue, BigDecimal exitTotalValue) {

        if (currentValue == null || exitTotalValue == null) {
            throw new IllegalArgumentException("Valores não podem ser nulos");
        }

        BigDecimal remainingValue = currentValue.subtract(exitTotalValue);

        log.debug("Valor restante calculado: {} - {} = {}", currentValue, exitTotalValue, remainingValue);

        return remainingValue.max(BigDecimal.ZERO); // Garantir que não fique negativo
    }

    /**
     * Valida se o novo preço médio calculado faz sentido
     *
     * @param originalPrice Preço original da operação
     * @param newAveragePrice Novo preço médio calculado
     * @param profitLoss Lucro/prejuízo da última saída
     * @return true se o preço médio faz sentido
     */
    public boolean validateNewAveragePrice(BigDecimal originalPrice,
                                           BigDecimal newAveragePrice,
                                           BigDecimal profitLoss) {

        if (originalPrice == null || newAveragePrice == null || profitLoss == null) {
            return false;
        }

        // Se houve lucro, o preço médio deve tender a diminuir
        // Se houve prejuízo, o preço médio deve tender a aumentar
        boolean isValid = true;

        if (profitLoss.compareTo(BigDecimal.ZERO) > 0) {
            // Lucro: preço médio deveria ser menor ou igual ao original
            if (newAveragePrice.compareTo(originalPrice) > 0) {
                log.warn("Preço médio após lucro ({}) é maior que o original ({})", newAveragePrice, originalPrice);
                isValid = false;
            }
        } else if (profitLoss.compareTo(BigDecimal.ZERO) < 0) {
            // Prejuízo: preço médio deveria ser maior ou igual ao original
            if (newAveragePrice.compareTo(originalPrice) < 0) {
                log.warn("Preço médio após prejuízo ({}) é menor que o original ({})", newAveragePrice, originalPrice);
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Calcula o preço médio de saída ponderado para múltiplas saídas
     *
     * @param totalValueReceived Valor total recebido em todas as saídas
     * @param totalQuantitySold Quantidade total vendida
     * @return Preço médio de saída
     */
    public BigDecimal calculateAverageExitPrice(BigDecimal totalValueReceived, Integer totalQuantitySold) {

        if (totalValueReceived == null || totalQuantitySold == null || totalQuantitySold == 0) {
            return BigDecimal.ZERO;
        }

        return totalValueReceived.divide(
                BigDecimal.valueOf(totalQuantitySold),
                PRECISION,
                RoundingMode.HALF_UP);
    }

    /**
     * Método auxiliar para logging detalhado
     */
    public void logCalculationDetails(String step, BigDecimal currentValue, BigDecimal exitValue,
                                      BigDecimal newValue, Integer quantity, BigDecimal avgPrice) {

        log.debug("=== CÁLCULO DE PREÇO MÉDIO - {} ===", step);
        log.debug("Valor atual: {}", currentValue);
        log.debug("Valor saída: {}", exitValue);
        log.debug("Valor restante: {}", newValue);
        log.debug("Quantidade restante: {}", quantity);
        log.debug("Novo preço médio: {}", avgPrice);
        log.debug("=====================================");
    }
}
