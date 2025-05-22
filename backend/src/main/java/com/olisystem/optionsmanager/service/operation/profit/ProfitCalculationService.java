package com.olisystem.optionsmanager.service.operation.profit;

import com.olisystem.optionsmanager.service.position.PositionCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ProfitCalculationService {

    private final PositionCalculator positionCalculator;

    /**
     * Calcula o lucro/prejuízo da operação
     */
    public BigDecimal calculateProfitLoss(BigDecimal entryUnitPrice, BigDecimal exitUnitPrice, int quantity) {
        return positionCalculator.calculateProfitLoss(entryUnitPrice, exitUnitPrice, quantity);
    }

    /**
     * Calcula o percentual de lucro/prejuízo
     */
    public BigDecimal calculateProfitLossPercentage(BigDecimal profitLoss, BigDecimal entryTotalValue) {
        return profitLoss.divide(entryTotalValue, 2, RoundingMode.HALF_UP);
    }
}
