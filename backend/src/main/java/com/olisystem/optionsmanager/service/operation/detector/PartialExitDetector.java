package com.olisystem.optionsmanager.service.operation.detector;

import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.position.Position;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PartialExitDetector {

    /**
     * Verifica se é a primeira saída parcial de uma posição
     *
     * @param position Posição atual
     * @return true se é a primeira saída parcial
     */
    public boolean isFirstPartialExit(Position position) {

        if (position == null) {
            return false;
        }

        // É primeira saída parcial se:
        // 1. Posição está OPEN (nunca teve saída)
        // 2. Quantidade total ainda é igual à original (sem saídas anteriores)
        boolean isFirst = position.getStatus() == com.olisystem.optionsmanager.model.position.PositionStatus.OPEN
                && position.getRemainingQuantity().equals(position.getTotalQuantity());

        log.debug("Verificando primeira saída parcial - Position ID: {}, Status: {}, " +
                        "Quantidade restante: {}, Quantidade total: {}, É primeira: {}",
                position.getId(), position.getStatus(),
                position.getRemainingQuantity(), position.getTotalQuantity(), isFirst);

        return isFirst;
    }

    /**
     * Verifica se é uma saída parcial subsequente (não é a primeira)
     *
     * @param position Posição atual
     * @return true se é saída parcial subsequente
     */
    public boolean isSubsequentPartialExit(Position position) {

        if (position == null) {
            return false;
        }

        // É saída subsequente se:
        // 1. Posição está PARTIAL (já teve saídas anteriores)
        // 2. Ainda tem quantidade restante
        boolean isSubsequent = position.getStatus() == com.olisystem.optionsmanager.model.position.PositionStatus.PARTIAL
                && position.getRemainingQuantity() > 0;

        log.debug("Verificando saída parcial subsequente - Position ID: {}, Status: {}, " +
                        "Quantidade restante: {}, É subsequente: {}",
                position.getId(), position.getStatus(), position.getRemainingQuantity(), isSubsequent);

        return isSubsequent;
    }

    /**
     * Verifica se é uma saída final (fecha a posição completamente)
     *
     * @param position          Posição atual
     * @param requestedQuantity Quantidade solicitada na saída
     * @return true se é saída final
     */
    public boolean isFinalExit(Position position, Integer requestedQuantity) {

        if (position == null || requestedQuantity == null) {
            return false;
        }

        // É saída final se a quantidade solicitada é igual à quantidade restante
        boolean isFinal = position.getRemainingQuantity().equals(requestedQuantity);

        log.debug("Verificando saída final - Position ID: {}, Quantidade restante: {}, " +
                        "Quantidade solicitada: {}, É final: {}",
                position.getId(), position.getRemainingQuantity(), requestedQuantity, isFinal);

        return isFinal;
    }

    /**
     * Verifica se é uma saída parcial (não fecha a posição)
     *
     * @param position          Posição atual
     * @param requestedQuantity Quantidade solicitada na saída
     * @return true se é saída parcial
     */
    public boolean isPartialExit(Position position, Integer requestedQuantity) {

        if (position == null || requestedQuantity == null) {
            return false;
        }

        // É saída parcial se a quantidade solicitada é menor que a restante
        boolean isPartial = requestedQuantity < position.getRemainingQuantity();

        log.debug("Verificando saída parcial - Position ID: {}, Quantidade restante: {}, " +
                        "Quantidade solicitada: {}, É parcial: {}",
                position.getId(), position.getRemainingQuantity(), requestedQuantity, isPartial);

        return isPartial;
    }

    /**
     * Verifica se já existem operações consolidadoras para esta posição
     *
     * @param group Grupo de operações
     * @return true se já existem consolidadoras
     */
    public boolean hasConsolidatedOperations(AverageOperationGroup group) {

        if (group == null || group.getItems() == null) {
            return false;
        }

        // Verificar se existe algum item com roleType de consolidação
        boolean hasConsolidated = group.getItems().stream()
                .anyMatch(item -> item.getRoleType() != null &&
                        (item.getRoleType().name().equals("CONSOLIDATED_ENTRY") ||
                                item.getRoleType().name().equals("CONSOLIDATED_RESULT")));

        log.debug("Verificando operações consolidadas - Group ID: {}, " +
                        "Total items: {}, Tem consolidadas: {}",
                group.getId(), group.getItems().size(), hasConsolidated);

        return hasConsolidated;
    }

    /**
     * Determina o tipo de saída baseado no contexto
     *
     * @param position          Posição atual
     * @param requestedQuantity Quantidade solicitada
     * @return Tipo de saída detectado
     */
    public ExitType determineExitType(Position position, Integer requestedQuantity) {

        if (isFinalExit(position, requestedQuantity)) {
            if (isFirstPartialExit(position)) {
                return ExitType.SINGLE_TOTAL_EXIT; // Saída única que fecha tudo
            } else {
                return ExitType.FINAL_PARTIAL_EXIT; // Última de uma série de saídas parciais
            }
        } else if (isFirstPartialExit(position)) {
            return ExitType.FIRST_PARTIAL_EXIT;
        } else if (isSubsequentPartialExit(position)) {
            return ExitType.SUBSEQUENT_PARTIAL_EXIT;
        }

        return ExitType.UNKNOWN;
    }

    /**
     * Enum para tipos de saída detectados
     */
    public enum ExitType {
        FIRST_PARTIAL_EXIT,       // Primeira saída parcial - criar consolidadoras
        SUBSEQUENT_PARTIAL_EXIT,  // Saída parcial subsequente - atualizar consolidadoras
        FINAL_PARTIAL_EXIT,       // Última saída parcial - finalizar consolidadoras
        SINGLE_TOTAL_EXIT,        // Saída única total - não precisa consolidadoras
        UNKNOWN                   // Tipo não identificado
    }

    /**
     * Valida se a quantidade solicitada é válida para a posição
     *
     * @param position          Posição atual
     * @param requestedQuantity Quantidade solicitada
     * @return true se é válida
     */
    public boolean validateExitQuantity(Position position, Integer requestedQuantity) {

        if (position == null || requestedQuantity == null) {
            log.error("Position ou quantidade solicitada são nulos");
            return false;
        }

        if (requestedQuantity <= 0) {
            log.error("Quantidade solicitada deve ser maior que zero: {}", requestedQuantity);
            return false;
        }

        if (requestedQuantity > position.getRemainingQuantity()) {
            log.error("Quantidade solicitada ({}) excede quantidade disponível ({})",
                    requestedQuantity, position.getRemainingQuantity());
            return false;
        }

        return true;
    }

    /**
     * Log detalhado do tipo de saída detectado
     */
    public void logExitTypeDetails(ExitType exitType, Position position, Integer requestedQuantity) {

        log.info("=== TIPO DE SAÍDA DETECTADO ===");
        log.info("Tipo: {}", exitType);
        log.info("Position ID: {}", position.getId());
        log.info("Status atual: {}", position.getStatus());
        log.info("Quantidade restante: {}", position.getRemainingQuantity());
        log.info("Quantidade total: {}", position.getTotalQuantity());
        log.info("Quantidade solicitada: {}", requestedQuantity);
        log.info("===============================");
    }
}
