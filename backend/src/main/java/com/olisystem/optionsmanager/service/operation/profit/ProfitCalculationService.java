package com.olisystem.optionsmanager.service.operation.profit;

import com.olisystem.optionsmanager.service.position.PositionCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfitCalculationService {

    private final PositionCalculator positionCalculator;

    private static final int PRECISION = 4;

    /**
     * Calcula o lucro/prejuízo da operação
     */
    public BigDecimal calculateProfitLoss(BigDecimal entryUnitPrice, BigDecimal exitUnitPrice, int quantity) {
        BigDecimal profitLoss = positionCalculator.calculateProfitLoss(entryUnitPrice, exitUnitPrice, quantity);

        log.debug("Calculando lucro/prejuízo: entrada={}, saída={}, quantidade={}, resultado={}",
                entryUnitPrice, exitUnitPrice, quantity, profitLoss);

        return profitLoss;
    }

    /**
     * Calcula o percentual de lucro/prejuízo
     * CORREÇÃO: Agora multiplica por 100 para retornar percentual real
     */
    public BigDecimal calculateProfitLossPercentage(BigDecimal profitLoss, BigDecimal entryTotalValue) {
        if (entryTotalValue.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Valor total de entrada é zero, retornando percentual 0");
            return BigDecimal.ZERO;
        }

        // CORREÇÃO: Multiplicar por 100 para obter percentual real
        BigDecimal percentage = profitLoss
                .divide(entryTotalValue, PRECISION, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        log.debug("Calculando percentual: lucro={}, valor_entrada={}, percentual={}%",
                profitLoss, entryTotalValue, percentage);

        return percentage;
    }

    /**
     * NOVO MÉTODO: Calcula percentual baseado em preços unitários
     */
    public BigDecimal calculateProfitLossPercentageFromPrices(BigDecimal entryUnitPrice, BigDecimal exitUnitPrice) {
        if (entryUnitPrice.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Preço unitário de entrada é zero, retornando percentual 0");
            return BigDecimal.ZERO;
        }

        BigDecimal percentage = exitUnitPrice
                .subtract(entryUnitPrice)
                .divide(entryUnitPrice, PRECISION, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        log.debug("Calculando percentual por preços: entrada={}, saída={}, percentual={}%",
                entryUnitPrice, exitUnitPrice, percentage);

        return percentage;
    }

    /**
     * NOVO MÉTODO: Validação de valores antes do cálculo
     */
    public void validateCalculationInputs(BigDecimal entryUnitPrice, BigDecimal exitUnitPrice, int quantity) {
        if (entryUnitPrice == null || exitUnitPrice == null) {
            throw new IllegalArgumentException("Preços de entrada e saída não podem ser nulos");
        }

        if (entryUnitPrice.compareTo(BigDecimal.ZERO) < 0 || exitUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Preços não podem ser negativos");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
    }
}
