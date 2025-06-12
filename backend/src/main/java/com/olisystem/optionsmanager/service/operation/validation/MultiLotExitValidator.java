package com.olisystem.optionsmanager.service.operation.validation;

import com.olisystem.optionsmanager.exception.BusinessException;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.Position;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço responsável por validar saídas em cenários com múltiplos lotes.
 * Garante a integridade dos dados antes de processar operações complexas.
 */
@Slf4j
@Service
public class MultiLotExitValidator {

    /**
     * Valida se uma saída pode ser processada considerando múltiplos lotes
     */
    public void validateMultiLotExit(Position position, int requestedQuantity) {
        log.debug("Validando saída de {} unidades para posição {}", requestedQuantity, position.getId());

        // Validação 1: Quantidade solicitada deve ser positiva
        if (requestedQuantity <= 0) {
            throw new BusinessException("Quantidade solicitada deve ser maior que zero");
        }

        // Validação 2: Position deve ter lotes disponíveis
        List<EntryLot> availableLots = getAvailableLots(position);
        if (availableLots.isEmpty()) {
            throw new BusinessException("Nenhum lote disponível para saída na posição");
        }

        // Validação 3: Calcular quantidade total disponível
        int totalAvailable = calculateTotalAvailableQuantity(availableLots);
        log.debug("Quantidade total disponível: {}, Solicitada: {}", totalAvailable, requestedQuantity);

        if (requestedQuantity > totalAvailable) {
            throw new BusinessException(String.format(
                "Quantidade solicitada (%d) maior que disponível (%d). " +
                "Lotes disponíveis: %s", 
                requestedQuantity, 
                totalAvailable,
                formatAvailableLots(availableLots)
            ));
        }

        // Validação 4: Verificar consistência da Position
        validatePositionConsistency(position, totalAvailable);

        // Validação 5: Verificar integridade dos lotes
        validateLotsIntegrity(availableLots);

        log.info("Validação de saída múltipla aprovada: {} unidades de {} disponíveis", 
                requestedQuantity, totalAvailable);
    }

    /**
     * Valida se a Position está consistente para processamento
     */
    public void validatePositionForComplexExit(Position position) {
        log.debug("Validando consistência da posição {} para saída complexa", position.getId());

        if (position == null) {
            throw new BusinessException("Position não pode ser nula");
        }

        if (position.getEntryLots() == null || position.getEntryLots().isEmpty()) {
            throw new BusinessException("Position deve ter pelo menos um lote de entrada");
        }

        if (position.getRemainingQuantity() <= 0) {
            throw new BusinessException("Position não possui quantidade disponível para saída");
        }

        // Verificar se quantidade restante bate com soma dos lotes
        int lotsTotalRemaining = position.getEntryLots().stream()
            .mapToInt(EntryLot::getRemainingQuantity)
            .sum();

        if (position.getRemainingQuantity() != lotsTotalRemaining) {
            log.warn("INCONSISTÊNCIA DETECTADA: Position.remainingQuantity={} != soma lotes={}", 
                    position.getRemainingQuantity(), lotsTotalRemaining);
            throw new BusinessException(String.format(
                "Inconsistência na posição: quantidade restante (%d) não confere com soma dos lotes (%d)",
                position.getRemainingQuantity(), lotsTotalRemaining
            ));
        }
    }

    /**
     * Valida se os lotes estão em estado adequado para consumo
     */
    public void validateLotsForConsumption(List<EntryLot> lots, int totalQuantityNeeded) {
        if (lots == null || lots.isEmpty()) {
            throw new BusinessException("Lista de lotes não pode estar vazia");
        }

        int totalAvailableInLots = lots.stream()
            .mapToInt(EntryLot::getRemainingQuantity)
            .sum();

        if (totalQuantityNeeded > totalAvailableInLots) {
            throw new BusinessException(String.format(
                "Quantidade necessária (%d) excede disponível nos lotes (%d)",
                totalQuantityNeeded, totalAvailableInLots
            ));
        }

        // Validar cada lote individualmente
        for (EntryLot lot : lots) {
            validateIndividualLot(lot);
        }
    }

    // ======================================================================================
    // MÉTODOS PRIVADOS DE APOIO
    // ======================================================================================

    private List<EntryLot> getAvailableLots(Position position) {
        return position.getEntryLots().stream()
            .filter(lot -> lot.getRemainingQuantity() > 0)
            .collect(Collectors.toList());
    }

    private int calculateTotalAvailableQuantity(List<EntryLot> availableLots) {
        return availableLots.stream()
            .mapToInt(EntryLot::getRemainingQuantity)
            .sum();
    }

    private String formatAvailableLots(List<EntryLot> lots) {
        return lots.stream()
            .map(lot -> String.format("Lote %d: %d/%d unidades", 
                    lot.getSequenceNumber(), 
                    lot.getRemainingQuantity(), 
                    lot.getQuantity()))
            .collect(Collectors.joining(", "));
    }

    private void validatePositionConsistency(Position position, int calculatedAvailable) {
        if (position.getRemainingQuantity() != calculatedAvailable) {
            log.warn("ATENÇÃO: Position.remainingQuantity={} != calculado={}", 
                    position.getRemainingQuantity(), calculatedAvailable);
            
            // Por ora, apenas log de warning - em produção pode ser erro crítico
            log.warn("Possível inconsistência de dados detectada na posição {}", position.getId());
        }
    }

    private void validateLotsIntegrity(List<EntryLot> lots) {
        for (EntryLot lot : lots) {
            validateIndividualLot(lot);
        }
    }

    private void validateIndividualLot(EntryLot lot) {
        if (lot.getRemainingQuantity() < 0) {
            throw new BusinessException(String.format(
                "Lote %d com quantidade negativa: %d", 
                lot.getSequenceNumber(), lot.getRemainingQuantity()
            ));
        }

        if (lot.getRemainingQuantity() > lot.getQuantity()) {
            throw new BusinessException(String.format(
                "Lote %d com quantidade restante (%d) maior que original (%d)", 
                lot.getSequenceNumber(), lot.getRemainingQuantity(), lot.getQuantity()
            ));
        }

        if (lot.getIsFullyConsumed() && lot.getRemainingQuantity() > 0) {
            log.warn("Lote {} marcado como consumido mas tem quantidade restante: {}", 
                    lot.getSequenceNumber(), lot.getRemainingQuantity());
        }

        if (!lot.getIsFullyConsumed() && lot.getRemainingQuantity() == 0) {
            log.warn("Lote {} não marcado como consumido mas quantidade restante é zero", 
                    lot.getSequenceNumber());
        }
    }
}
