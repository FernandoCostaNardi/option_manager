package com.olisystem.optionsmanager.service.operation.detector;

import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.Position;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço responsável por detectar e classificar cenários complexos de operações.
 */
@Slf4j
@Service
public class ComplexScenarioDetector {

    public enum ScenarioType {
        SIMPLE_SINGLE_LOT,           // Cenário 1: Um lote apenas
        SIMPLE_MULTIPLE_LOTS,        // Cenário 2: Múltiplos lotes, uma saída total
        PARTIAL_SINGLE_SOURCE,       // Cenário 3.1: Saídas parciais de um lote
        COMPLEX_MULTIPLE_SOURCES     // Cenário 3.2: Múltiplas entradas/saídas intercaladas
    }

    /**
     * Detecta o tipo de cenário baseado na posição e quantidade solicitada
     */
    public ScenarioType detectScenario(Position position, int requestedQuantity) {
        log.debug("Detectando cenário para posição {} com {} lotes, quantidade solicitada: {}", 
                position.getId(), position.getEntryLots().size(), requestedQuantity);

        List<EntryLot> activeLots = getActiveLots(position);
        
        boolean isPartialExit = requestedQuantity < position.getRemainingQuantity();
        boolean hasMultipleLots = activeLots.size() > 1;
        boolean hasPartiallyConsumedLots = hasPartiallyConsumedLots(activeLots);
        boolean hasMultipleEntriesInHistory = hasMultipleEntriesInHistory(position);

        // Determinar cenário
        ScenarioType scenario = determineScenarioType(
            hasMultipleLots, isPartialExit, hasPartiallyConsumedLots, hasMultipleEntriesInHistory
        );

        log.info("Cenário detectado: {} para posição {}", scenario, position.getId());
        return scenario;
    }

    /**
     * Verifica se a posição representa um cenário complexo
     */
    public boolean isComplexScenario(Position position, int requestedQuantity) {
        ScenarioType scenario = detectScenario(position, requestedQuantity);
        return scenario == ScenarioType.COMPLEX_MULTIPLE_SOURCES;
    }

    /**
     * Fornece detalhes sobre o cenário detectado
     */
    public String getScenarioDescription(ScenarioType scenario) {
        return switch (scenario) {
            case SIMPLE_SINGLE_LOT -> "Cenário simples: um único lote de entrada";
            case SIMPLE_MULTIPLE_LOTS -> "Cenário simples: múltiplos lotes, saída total";
            case PARTIAL_SINGLE_SOURCE -> "Cenário parcial: saídas graduais de poucos lotes";
            case COMPLEX_MULTIPLE_SOURCES -> "Cenário complexo: múltiplas entradas/saídas intercaladas";
        };
    }

    // ======================================================================================
    // MÉTODOS PRIVADOS DE APOIO
    // ======================================================================================

    private List<EntryLot> getActiveLots(Position position) {
        return position.getEntryLots().stream()
            .filter(lot -> lot.getRemainingQuantity() > 0)
            .collect(Collectors.toList());
    }

    private boolean hasPartiallyConsumedLots(List<EntryLot> activeLots) {
        return activeLots.stream()
            .anyMatch(lot -> lot.getRemainingQuantity() < lot.getQuantity());
    }

    private boolean hasMultipleEntriesInHistory(Position position) {
        return position.getEntryLots().size() > 1;
    }

    private ScenarioType determineScenarioType(boolean hasMultipleLots, 
                                             boolean isPartialExit, 
                                             boolean hasPartiallyConsumedLots, 
                                             boolean hasMultipleEntriesInHistory) {
        
        // Cenário 3.2: Complexo - múltiplas entradas + saídas parciais
        if (hasMultipleEntriesInHistory && hasPartiallyConsumedLots) {
            return ScenarioType.COMPLEX_MULTIPLE_SOURCES;
        }
        
        // Cenário 3.1: Saídas parciais mas de fonte simples
        if (isPartialExit && !hasMultipleEntriesInHistory) {
            return ScenarioType.PARTIAL_SINGLE_SOURCE;
        }
        
        // Cenário 2: Múltiplos lotes mas saída total simples
        if (hasMultipleLots && !isPartialExit) {
            return ScenarioType.SIMPLE_MULTIPLE_LOTS;
        }
        
        // Cenário 1: Caso mais simples
        return ScenarioType.SIMPLE_SINGLE_LOT;
    }
}
