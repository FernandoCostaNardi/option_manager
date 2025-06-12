package com.olisystem.optionsmanager.service.position.status;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.position.PositionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por gerenciar e determinar o status correto das posições
 * em cenários complexos com múltiplas entradas e saídas.
 */
@Slf4j
@Service
public class PositionStatusManager {

    /**
     * Determina o novo status após uma entrada na posição
     */
    public PositionStatus determineStatusAfterEntry(Position position, Operation newEntry) {
        log.debug("Determinando status após entrada para posição {}", position.getId());

        // Após nova entrada, se há quantidade restante > 0, sempre fica OPEN
        if (position.getRemainingQuantity() > 0) {
            log.debug("Posição {} tem quantidade restante {}, status será OPEN", 
                    position.getId(), position.getRemainingQuantity());
            return PositionStatus.OPEN;
        }

        // Se por algum motivo não há quantidade restante, fica CLOSED
        log.warn("Posição {} sem quantidade restante após entrada - status CLOSED", position.getId());
        return PositionStatus.CLOSED;
    }

    /**
     * Determina o status após uma saída da posição
     */
    public PositionStatus determineStatusAfterExit(Position position, int quantityBeingExited) {
        log.debug("Determinando status após saída de {} unidades para posição {}", 
                quantityBeingExited, position.getId());

        int remainingAfterExit = position.getRemainingQuantity() - quantityBeingExited;
        
        log.debug("Quantidade restante após saída: {} - {} = {}", 
                position.getRemainingQuantity(), quantityBeingExited, remainingAfterExit);

        if (remainingAfterExit == 0) {
            log.info("Posição {} será fechada - quantidade restante zero", position.getId());
            return PositionStatus.CLOSED;
        } else if (remainingAfterExit == position.getTotalQuantity()) {
            log.info("Posição {} primeira saída - status OPEN", position.getId());
            return PositionStatus.OPEN; // Primeira saída mas ainda sobra tudo
        } else {
            log.info("Posição {} com saída parcial - status PARTIAL", position.getId());
            return PositionStatus.PARTIAL;
        }
    }

    /**
     * Verifica se o status atual da posição está correto
     */
    public boolean isStatusConsistent(Position position) {
        log.debug("Verificando consistência do status para posição {}", position.getId());

        PositionStatus currentStatus = position.getStatus();
        int remaining = position.getRemainingQuantity();
        int total = position.getTotalQuantity();

        boolean consistent = switch (currentStatus) {
            case OPEN -> remaining > 0 && remaining == total;
            case PARTIAL -> remaining > 0 && remaining < total;
            case CLOSED -> remaining == 0;
        };

        if (!consistent) {
            log.warn("INCONSISTÊNCIA: Posição {} status={}, remaining={}, total={}", 
                    position.getId(), currentStatus, remaining, total);
        }

        return consistent;
    }

    /**
     * Corrige o status da posição baseado nas quantidades atuais
     */
    public PositionStatus correctStatus(Position position) {
        log.info("Corrigindo status da posição {}", position.getId());

        int remaining = position.getRemainingQuantity();
        int total = position.getTotalQuantity();

        if (remaining == 0) {
            return PositionStatus.CLOSED;
        } else if (remaining == total) {
            return PositionStatus.OPEN;
        } else {
            return PositionStatus.PARTIAL;
        }
    }
}
