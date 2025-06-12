package com.olisystem.optionsmanager.service.position.update;

import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.exception.BusinessException;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.position.PositionStatus;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionUpdateService {

    private final PositionRepository positionRepository;
    private static final int PRECISION = 4;

    /**
     * Atualiza a posição com os dados da operação de saída TOTAL
     * CORREÇÃO: Agora usa percentual consolidado correto
     */
    @Transactional
    public void updatePosition(Position position, OperationFinalizationRequest request,
                               BigDecimal profitLoss, BigDecimal profitLossPercentage) {
        log.info("Iniciando atualização da posição ID: {} - Saída TOTAL", position.getId());

        // CORREÇÃO: Validações antes da atualização
        validatePositionUpdate(position, request, profitLoss, profitLossPercentage);

        // Capturar valores originais para log
        Integer originalRemainingQuantity = position.getRemainingQuantity();
        PositionStatus originalStatus = position.getStatus();
        BigDecimal originalProfit = position.getTotalRealizedProfit();

        // ✅ CORREÇÃO: Calcular percentual consolidado baseado no valor total original
        BigDecimal totalOriginalInvestment = position.getAveragePrice()
                .multiply(BigDecimal.valueOf(position.getTotalQuantity()));
        
        // Se a position já tem lucro acumulado (de saídas parciais), somar ao profitLoss atual
        BigDecimal totalConsolidatedProfit = profitLoss;
        if (originalProfit != null) {
            totalConsolidatedProfit = originalProfit.add(profitLoss);
        }
        
        BigDecimal consolidatedPercentage = totalConsolidatedProfit
                .divide(totalOriginalInvestment, PRECISION, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Atualizar campos da posição
        position.setTotalRealizedProfit(totalConsolidatedProfit);
        position.setTotalRealizedProfitPercentage(consolidatedPercentage); // ✅ CORREÇÃO: Percentual consolidado
        position.setRemainingQuantity(0); // CORREÇÃO: Saída total = 0
        position.setStatus(PositionStatus.CLOSED);
        position.setCloseDate(request.getExitDate());

        // Salvar posição
        Position savedPosition = positionRepository.save(position);

        log.info("Posição {} atualizada com sucesso: " +
                        "Quantidade: {} → {}, " +
                        "Status: {} → {}, " +
                        "Lucro/Prejuízo: {} → {}, " +
                        "Percentual Consolidado: {}%",
                savedPosition.getId(),
                originalRemainingQuantity, savedPosition.getRemainingQuantity(),
                originalStatus, savedPosition.getStatus(),
                originalProfit, savedPosition.getTotalRealizedProfit(),
                consolidatedPercentage);
    }

    /**
     * NOVO MÉTODO: Atualiza posição para saída PARCIAL
     * CORREÇÃO: Agora inclui atualização do preço médio break-even
     */
    @Transactional
    public void updatePositionPartial(Position position, OperationFinalizationRequest request,
                                      BigDecimal profitLoss, BigDecimal profitLossPercentage,
                                      Integer quantityExited) {
        log.info("Iniciando atualização da posição ID: {} - Saída PARCIAL de {} unidades",
                position.getId(), quantityExited);

        // Validações específicas para saída parcial
        validatePartialPositionUpdate(position, request, quantityExited);

        // Capturar valores originais
        Integer originalRemainingQuantity = position.getRemainingQuantity();
        BigDecimal originalProfit = position.getTotalRealizedProfit() != null ?
                position.getTotalRealizedProfit() : BigDecimal.ZERO;
        BigDecimal originalAveragePrice = position.getAveragePrice();

        // Calcular nova quantidade restante
        Integer newRemainingQuantity = originalRemainingQuantity - quantityExited;

        // ✅ CORREÇÃO PRINCIPAL: Calcular e atualizar preço médio break-even
        BigDecimal newBreakEvenPrice = calculateBreakEvenPrice(position, request, quantityExited);
        
        // Acumular lucro realizado
        BigDecimal newTotalRealizedProfit = originalProfit.add(profitLoss);

        // ✅ CORREÇÃO: Calcular percentual consolidado baseado no valor total original investido
        BigDecimal totalOriginalInvestment = originalAveragePrice.multiply(BigDecimal.valueOf(position.getTotalQuantity()));
        BigDecimal consolidatedPercentage = newTotalRealizedProfit
                .divide(totalOriginalInvestment, PRECISION, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Atualizar campos
        position.setAveragePrice(newBreakEvenPrice); // ✅ NOVO: Atualizar preço médio break-even
        position.setTotalRealizedProfit(newTotalRealizedProfit);
        position.setTotalRealizedProfitPercentage(consolidatedPercentage); // ✅ CORREÇÃO: Percentual consolidado
        position.setRemainingQuantity(newRemainingQuantity);

        // Determinar status baseado na quantidade restante
        if (newRemainingQuantity == 0) {
            position.setStatus(PositionStatus.CLOSED);
            position.setCloseDate(request.getExitDate());
        } else {
            position.setStatus(PositionStatus.PARTIAL);
        }

        Position savedPosition = positionRepository.save(position);

        log.info("Posição {} atualizada (PARCIAL): " +
                        "Quantidade: {} → {}, " +
                        "Preço Médio: {} → {} (break-even), " +
                        "Status: {}, " +
                        "Lucro Acumulado: {} → {}, " +
                        "Percentual Consolidado: {}%",
                savedPosition.getId(),
                originalRemainingQuantity, newRemainingQuantity,
                originalAveragePrice, newBreakEvenPrice,
                savedPosition.getStatus(),
                originalProfit, newTotalRealizedProfit,
                consolidatedPercentage);
    }

    /**
     * NOVO MÉTODO: Calcula preço médio break-even após saída parcial
     */
    private BigDecimal calculateBreakEvenPrice(Position position, OperationFinalizationRequest request, 
                                               Integer quantityExited) {
        // Valor total original investido
        BigDecimal totalOriginalInvestment = position.getAveragePrice()
                .multiply(BigDecimal.valueOf(position.getTotalQuantity()));
        
        // Valor recebido na saída
        BigDecimal exitTotalValue = request.getExitUnitPrice()
                .multiply(BigDecimal.valueOf(quantityExited));
        
        // Valor líquido restante (para break-even)
        BigDecimal remainingValue = totalOriginalInvestment.subtract(exitTotalValue);
        
        // Nova quantidade restante
        Integer newRemainingQuantity = position.getRemainingQuantity() - quantityExited;
        
        // Se não há quantidade restante, preço médio é zero
        if (newRemainingQuantity == 0) {
            return BigDecimal.ZERO;
        }
        
        // Calcular novo preço médio break-even
        BigDecimal breakEvenPrice = remainingValue.divide(
                BigDecimal.valueOf(newRemainingQuantity), 
                PRECISION, 
                RoundingMode.HALF_UP);
        
        log.debug("Cálculo break-even: total_original={}, valor_recebido={}, " +
                  "valor_restante={}, quantidade_restante={}, preço_break_even={}",
                  totalOriginalInvestment, exitTotalValue, remainingValue, 
                  newRemainingQuantity, breakEvenPrice);
        
        return breakEvenPrice;
    }

    /**
     * NOVO MÉTODO: Validações para atualização de posição total
     */
    private void validatePositionUpdate(Position position, OperationFinalizationRequest request,
                                        BigDecimal profitLoss, BigDecimal profitLossPercentage) {
        // Validar posição
        if (position == null) {
            throw new IllegalArgumentException("Posição não pode ser nula");
        }

        if (position.getStatus() == PositionStatus.CLOSED) {
            throw new BusinessException(
                    String.format("Posição %s já está fechada e não pode ser atualizada", position.getId())
            );
        }

        // Validar request
        if (request == null) {
            throw new IllegalArgumentException("Request de finalização não pode ser nulo");
        }

        if (request.getExitDate() == null) {
            throw new IllegalArgumentException("Data de saída é obrigatória");
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantidade de saída deve ser maior que zero");
        }

        // CORREÇÃO: Validar se quantidade de saída não excede disponível
        if (request.getQuantity() > position.getRemainingQuantity()) {
            throw new BusinessException(
                    String.format("Quantidade de saída (%d) excede quantidade disponível na posição (%d). Posição ID: %s",
                            request.getQuantity(), position.getRemainingQuantity(), position.getId())
            );
        }

        // Validar valores financeiros
        if (profitLoss == null) {
            throw new IllegalArgumentException("Valor de lucro/prejuízo não pode ser nulo");
        }

        if (profitLossPercentage == null) {
            throw new IllegalArgumentException("Percentual de lucro/prejuízo não pode ser nulo");
        }

        log.debug("Validações da posição {} aprovadas", position.getId());
    }

    /**
     * NOVO MÉTODO: Validações específicas para atualização parcial
     */
    private void validatePartialPositionUpdate(Position position, OperationFinalizationRequest request,
                                               Integer quantityExited) {
        validatePositionUpdate(position, request, BigDecimal.ZERO, BigDecimal.ZERO);

        if (quantityExited == null || quantityExited <= 0) {
            throw new IllegalArgumentException("Quantidade de saída deve ser maior que zero");
        }

        if (quantityExited > position.getRemainingQuantity()) {
            throw new BusinessException(
                    String.format("Quantidade de saída (%d) excede quantidade disponível (%d) na posição %s",
                            quantityExited, position.getRemainingQuantity(), position.getId())
            );
        }

        // CORREÇÃO: Validar que não resultará em quantidade negativa
        Integer resultingQuantity = position.getRemainingQuantity() - quantityExited;
        if (resultingQuantity < 0) {
            throw new BusinessException(
                    String.format("Operação resultaria em quantidade negativa (%d) para posição %s",
                            resultingQuantity, position.getId())
            );
        }
    }

    /**
     * NOVO MÉTODO: Reabrir posição (útil para correções)
     */
    @Transactional
    public void reopenPosition(Position position, String reason) {
        log.warn("Reabrindo posição {}: {}", position.getId(), reason);

        position.setStatus(PositionStatus.OPEN);
        position.setCloseDate(null);
        position.setTotalRealizedProfit(BigDecimal.ZERO);
        position.setTotalRealizedProfitPercentage(BigDecimal.ZERO);

        positionRepository.save(position);

        log.info("Posição {} reaberta com sucesso", position.getId());
    }

    /**
     * NOVO MÉTODO: Verificar se posição pode ser fechada totalmente
     */
    public boolean canClosePositionCompletely(Position position, Integer requestedQuantity) {
        return position.getRemainingQuantity().equals(requestedQuantity);
    }

    /**
     * NOVO MÉTODO: Calcular quantidade máxima que pode ser retirada da posição
     */
    public Integer getMaxWithdrawableQuantity(Position position) {
        return position.getRemainingQuantity();
    }
}
