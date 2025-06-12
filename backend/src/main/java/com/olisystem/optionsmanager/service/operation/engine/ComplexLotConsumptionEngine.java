package com.olisystem.optionsmanager.service.operation.engine;

import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.record.consumption.ComplexConsumptionPlan;
import com.olisystem.optionsmanager.record.consumption.ComplexConsumptionResult;
import com.olisystem.optionsmanager.record.consumption.LotConsumption;
import com.olisystem.optionsmanager.record.consumption.LotConsumptionResult;
import com.olisystem.optionsmanager.service.operation.profit.ProfitCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplexLotConsumptionEngine {

    private final ProfitCalculationService profitCalculationService;

    public ComplexConsumptionPlan createConsumptionPlan(Position position, 
                                                      OperationFinalizationRequest request) {
        
        log.info("=== CRIANDO PLANO DE CONSUMO COMPLEXO ===");
        log.info("Posição: {}, Quantidade: {}", position.getId(), request.getQuantity());

        List<EntryLot> availableLots = getAvailableLots(position);
        List<EntryLot> sameDayLots = filterSameDayLots(availableLots, request.getExitDate());
        List<EntryLot> previousDayLots = filterPreviousDayLots(availableLots, request.getExitDate());

        applySortingStrategies(sameDayLots, previousDayLots);

        ComplexConsumptionPlan plan = planComplexConsumption(
            sameDayLots, previousDayLots, request.getQuantity(), request.getExitDate()
        );

        log.info("Plano criado: {} consumos", plan.consumptions().size());
        return plan;
    }

    /**
     * Executa o plano de consumo e calcula todos os resultados
     */
    public ComplexConsumptionResult executeConsumption(ComplexConsumptionPlan plan, 
                                                     BigDecimal exitUnitPrice) {
        
        log.info("=== EXECUTANDO PLANO DE CONSUMO ===");
        
        List<LotConsumptionResult> results = new ArrayList<>();
        BigDecimal totalProfitLoss = BigDecimal.ZERO;
        BigDecimal totalDayTradeProfitLoss = BigDecimal.ZERO;
        BigDecimal totalSwingTradeProfitLoss = BigDecimal.ZERO;
        
        for (LotConsumption consumption : plan.consumptions()) {
            LotConsumptionResult result = processLotConsumption(consumption, exitUnitPrice);
            results.add(result);
            
            totalProfitLoss = totalProfitLoss.add(result.profitLoss());
            
            if (result.tradeType() == TradeType.DAY) {
                totalDayTradeProfitLoss = totalDayTradeProfitLoss.add(result.profitLoss());
            } else {
                totalSwingTradeProfitLoss = totalSwingTradeProfitLoss.add(result.profitLoss());
            }
        }
        
        BigDecimal averageEntryPrice = calculateWeightedAverageEntryPrice(results);
        
        return new ComplexConsumptionResult(
            results, totalProfitLoss, totalDayTradeProfitLoss, totalSwingTradeProfitLoss,
            plan.totalQuantity(), plan.getDayTradeQuantity(), plan.getSwingTradeQuantity(),
            plan.exitDate(), averageEntryPrice, exitUnitPrice
        );
    }

    private LotConsumptionResult processLotConsumption(LotConsumption consumption, 
                                                     BigDecimal exitUnitPrice) {
        
        EntryLot lot = consumption.lot();
        int quantity = consumption.quantityToConsume();

        BigDecimal entryUnitPrice = lot.getUnitPrice();
        BigDecimal entryTotalValue = lot.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal exitTotalValue = exitUnitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal profitLoss = profitCalculationService.calculateProfitLoss(
                entryUnitPrice, exitUnitPrice, quantity
        );
        BigDecimal profitLossPercentage = profitCalculationService.calculateProfitLossPercentage(
            profitLoss, entryTotalValue
        );
        
        return new LotConsumptionResult(
            lot, quantity, consumption.tradeType(), lot.getUnitPrice(), exitUnitPrice,
            profitLoss, profitLossPercentage, entryTotalValue, exitTotalValue
        );
    }

    // ======================================================================================
    // MÉTODOS PRIVADOS DE APOIO
    // ======================================================================================

    private List<EntryLot> getAvailableLots(Position position) {
        return position.getEntryLots().stream()
            .filter(lot -> lot.getRemainingQuantity() > 0)
            .collect(Collectors.toList());
    }

    private List<EntryLot> filterSameDayLots(List<EntryLot> lots, LocalDate exitDate) {
        return lots.stream()
            .filter(lot -> lot.getEntryDate().equals(exitDate))
            .collect(Collectors.toList());
    }

    private List<EntryLot> filterPreviousDayLots(List<EntryLot> lots, LocalDate exitDate) {
        return lots.stream()
            .filter(lot -> lot.getEntryDate().isBefore(exitDate))
            .collect(Collectors.toList());
    }

    private void applySortingStrategies(List<EntryLot> sameDayLots, List<EntryLot> previousDayLots) {
        // LIFO para mesmo dia (mais recente primeiro)
        sameDayLots.sort(Comparator.comparing(EntryLot::getSequenceNumber).reversed());
        
        // FIFO para dias anteriores (mais antigo primeiro)
        previousDayLots.sort(Comparator.comparing(EntryLot::getSequenceNumber));
        
        log.debug("Lotes ordenados: {} mesmo dia (LIFO), {} dias anteriores (FIFO)", 
                sameDayLots.size(), previousDayLots.size());
    }

    private ComplexConsumptionPlan planComplexConsumption(List<EntryLot> sameDayLots,
                                                        List<EntryLot> previousDayLots,
                                                        int totalQuantityNeeded,
                                                        LocalDate exitDate) {
        
        List<LotConsumption> consumptions = new ArrayList<>();
        int remainingToConsume = totalQuantityNeeded;

        // Primeiro: consumir lotes do mesmo dia (LIFO)
        for (EntryLot lot : sameDayLots) {
            if (remainingToConsume <= 0) break;
            
            int toConsume = Math.min(lot.getRemainingQuantity(), remainingToConsume);
            consumptions.add(new LotConsumption(lot, toConsume, TradeType.DAY));
            remainingToConsume -= toConsume;
            
            log.debug("Planejado consumo Day Trade: Lote {} - {} unidades", 
                    lot.getSequenceNumber(), toConsume);
        }

        // Segundo: consumir lotes de dias anteriores (FIFO)
        for (EntryLot lot : previousDayLots) {
            if (remainingToConsume <= 0) break;
            
            int toConsume = Math.min(lot.getRemainingQuantity(), remainingToConsume);
            consumptions.add(new LotConsumption(lot, toConsume, TradeType.SWING));
            remainingToConsume -= toConsume;
            
            log.debug("Planejado consumo Swing Trade: Lote {} - {} unidades", 
                    lot.getSequenceNumber(), toConsume);
        }

        return new ComplexConsumptionPlan(consumptions, totalQuantityNeeded, exitDate, "AUTO");
    }

    private BigDecimal calculateWeightedAverageEntryPrice(List<LotConsumptionResult> results) {
        BigDecimal totalValue = BigDecimal.ZERO;
        int totalQuantity = 0;
        
        for (LotConsumptionResult result : results) {
            totalValue = totalValue.add(result.entryTotalValue());
            totalQuantity += result.quantityConsumed();
        }
        
        if (totalQuantity == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalValue.divide(BigDecimal.valueOf(totalQuantity), 4, java.math.RoundingMode.HALF_UP);
    }
}
