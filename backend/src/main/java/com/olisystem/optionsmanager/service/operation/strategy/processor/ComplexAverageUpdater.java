package com.olisystem.optionsmanager.service.operation.strategy.processor;

import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroupStatus;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.position.PositionStatus;
import com.olisystem.optionsmanager.record.consumption.ComplexConsumptionResult;
import com.olisystem.optionsmanager.service.position.status.PositionStatusManager;
import com.olisystem.optionsmanager.service.position.update.PositionUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplexAverageUpdater {

    private final PositionUpdateService positionUpdateService;
    private final PositionStatusManager positionStatusManager;

    @Transactional
    public void updateAfterComplexExit(Position position, 
                                     AverageOperationGroup group,
                                     ComplexConsumptionResult result) {
        
        log.info("=== ATUALIZANDO ESTRUTURAS AGREGADAS ===");
        log.info("Position: {}, Quantidade consumida: {}, P&L total: {}", 
                position.getId(), result.totalQuantity(), result.totalProfitLoss());

        updatePositionAfterComplexExit(position, result);
        updateGroupAfterComplexExit(group, position, result);
        
        log.info("Estruturas agregadas atualizadas com sucesso");
    }

    private void updatePositionAfterComplexExit(Position position, ComplexConsumptionResult result) {
        log.debug("Atualizando Position após saída complexa");
        
        int newRemainingQuantity = position.getRemainingQuantity() - result.totalQuantity();
        position.setRemainingQuantity(newRemainingQuantity);
        
        BigDecimal currentProfit = position.getTotalRealizedProfit() != null ? 
            position.getTotalRealizedProfit() : BigDecimal.ZERO;
        BigDecimal newTotalProfit = currentProfit.add(result.totalProfitLoss());
        position.setTotalRealizedProfit(newTotalProfit);

        // Calcular percentual total acumulado
        BigDecimal totalInvestment = calculateTotalInvestment(position);
        BigDecimal totalProfitPercentage = totalInvestment.compareTo(BigDecimal.ZERO) > 0 ?
            newTotalProfit.divide(totalInvestment, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
            BigDecimal.ZERO;
        position.setTotalRealizedProfitPercentage(totalProfitPercentage);

        // Determinar e atualizar status
        PositionStatus newStatus = positionStatusManager.determineStatusAfterExit(
            position, result.totalQuantity()
        );
        position.setStatus(newStatus);

        // Se fechou completamente, definir data de fechamento
        if (newStatus == PositionStatus.CLOSED) {
            position.setCloseDate(result.exitDate());
        }

        log.debug("Position atualizada: quantidade restante={}, P&L total={} ({}%), status={}", 
                newRemainingQuantity, newTotalProfit, totalProfitPercentage, newStatus);
    }

    private void updateGroupAfterComplexExit(AverageOperationGroup group, 
                                           Position position,
                                           ComplexConsumptionResult result) {
        log.debug("Atualizando AverageOperationGroup após saída complexa");

        // Atualizar quantidades
        group.setRemainingQuantity(position.getRemainingQuantity());
        group.setClosedQuantity(group.getTotalQuantity() - position.getRemainingQuantity());

        // Atualizar lucro total
        group.setTotalProfit(position.getTotalRealizedProfit());

        // Calcular preço médio de saída ponderado
        BigDecimal weightedExitPrice = calculateWeightedAverageExitPrice(group, result);
        group.setAvgExitPrice(weightedExitPrice);

        // Atualizar status do grupo
        AverageOperationGroupStatus groupStatus = position.getStatus() == PositionStatus.CLOSED ?
            AverageOperationGroupStatus.CLOSED : AverageOperationGroupStatus.PARTIALLY_CLOSED;
        group.setStatus(groupStatus);

        log.debug("Grupo atualizado: quantidade restante={}, fechada={}, P&L total={}, status={}", 
                group.getRemainingQuantity(), group.getClosedQuantity(), 
                group.getTotalProfit(), groupStatus);
    }

    private BigDecimal calculateTotalInvestment(Position position) {
        return position.getEntryLots().stream()
            .map(lot -> lot.getUnitPrice().multiply(BigDecimal.valueOf(lot.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateWeightedAverageExitPrice(AverageOperationGroup group, 
                                                       ComplexConsumptionResult result) {
        // Por ora, usar o preço de saída do resultado
        return result.averageExitPrice();
    }
}
