package com.olisystem.optionsmanager.service.position.update;

import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.position.PositionStatus;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionUpdateService {

    private final PositionRepository positionRepository;

    /**
     * Atualiza a posição com os dados da operação de saída
     */
    @Transactional
    public void updatePosition(Position position, OperationFinalizationRequest request,
                               BigDecimal profitLoss, BigDecimal profitLossPercentage) {
        log.info("Atualizando posição ID: {}", position.getId());

        position.setTotalRealizedProfit(profitLoss);
        position.setTotalRealizedProfitPercentage(profitLossPercentage);
        position.setRemainingQuantity(position.getRemainingQuantity() - request.getQuantity());
        position.setStatus(PositionStatus.CLOSED);
        position.setCloseDate(request.getExitDate());

        positionRepository.save(position);

        log.info("Posição {} fechada. Todas as entradas consumidas.", position.getId());
    }
}
