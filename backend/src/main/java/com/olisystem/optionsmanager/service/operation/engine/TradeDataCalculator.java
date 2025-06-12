package com.olisystem.optionsmanager.service.operation.engine;

import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.record.consumption.ComplexConsumptionResult;
import com.olisystem.optionsmanager.record.consumption.LotConsumptionResult;
import com.olisystem.optionsmanager.record.consumption.TradeOperationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calculadora para separar dados por tipo de trade.
 */
@Slf4j
@Service
public class TradeDataCalculator {

    /**
     * Calcula dados específicos para Day Trade
     */
    public TradeOperationData calculateDayTradeData(ComplexConsumptionResult result) {
        List<LotConsumptionResult> dayTradeResults = result.results().stream()
            .filter(r -> r.tradeType() == TradeType.DAY)
            .toList();

        if (dayTradeResults.isEmpty()) {
            return createEmptyTradeData(TradeType.DAY);
        }

        return calculateTradeData(TradeType.DAY, dayTradeResults, result.averageExitPrice());
    }

    /**
     * Calcula dados específicos para Swing Trade
     */
    public TradeOperationData calculateSwingTradeData(ComplexConsumptionResult result) {
        List<LotConsumptionResult> swingTradeResults = result.results().stream()
            .filter(r -> r.tradeType() == TradeType.SWING)
            .toList();

        if (swingTradeResults.isEmpty()) {
            return createEmptyTradeData(TradeType.SWING);
        }

        return calculateTradeData(TradeType.SWING, swingTradeResults, result.averageExitPrice());
    }

    private TradeOperationData calculateTradeData(TradeType tradeType, 
                                                List<LotConsumptionResult> results,
                                                BigDecimal exitPrice) {
        
        int totalQuantity = results.stream().mapToInt(LotConsumptionResult::quantityConsumed).sum();
        BigDecimal totalProfitLoss = results.stream()
            .map(LotConsumptionResult::profitLoss)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEntryValue = results.stream()
            .map(LotConsumptionResult::entryTotalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExitValue = results.stream()
            .map(LotConsumptionResult::exitTotalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weightedAverageEntryPrice = totalEntryValue.divide(
            BigDecimal.valueOf(totalQuantity), 4, RoundingMode.HALF_UP
        );

        BigDecimal profitLossPercentage = totalEntryValue.compareTo(BigDecimal.ZERO) == 0 ? 
            BigDecimal.ZERO :
            totalProfitLoss.divide(totalEntryValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return new TradeOperationData(
            tradeType, totalQuantity, totalProfitLoss, profitLossPercentage,
            weightedAverageEntryPrice, totalEntryValue, totalExitValue
        );
    }

    private TradeOperationData createEmptyTradeData(TradeType tradeType) {
        return new TradeOperationData(
            tradeType, 0, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
    }
}
