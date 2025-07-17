package com.olisystem.optionsmanager.service.operation.strategy.processor;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.service.operation.consolidate.ConsolidatedOperationService;
import com.olisystem.optionsmanager.service.operation.detector.PartialExitDetector;
import com.olisystem.optionsmanager.service.operation.detector.ComplexScenarioDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExitProcessorSelector {

    private final SingleLotExitProcessor singleLotProcessor;
    private final MultipleLotExitProcessor multipleLotProcessor;
    private final PartialExitProcessor partialExitProcessor;
    private final ComplexScenarioProcessor complexScenarioProcessor;
    private final PartialExitDetector partialExitDetector;
    private final ComplexScenarioDetector complexScenarioDetector;
    private final ConsolidatedOperationService consolidatedOperationService;

    /**
     * Seleciona o processador apropriado baseado no contexto da operação
     * CORRIGIDO: Usando lógica completa de detecção de cenários
     */
    public Operation selectAndProcess(OperationExitPositionContext context) {

        validateContext(context);

        int lotCount = context.availableLots().size();
        Integer requestedQuantity = context.context().request().getQuantity();

        log.info("Selecionando processador: {} lotes, quantidade solicitada: {}, quantidade restante: {}",
                lotCount, requestedQuantity, context.position().getRemainingQuantity());

        // 🔧 CORREÇÃO: Verificar cenário complexo primeiro
        if (complexScenarioDetector.isComplexScenario(context.position(), requestedQuantity)) {
            ComplexScenarioDetector.ScenarioType scenario = complexScenarioDetector.detectScenario(
                    context.position(), requestedQuantity);
            log.info("Cenário complexo detectado: {} - usando ComplexScenarioProcessor", scenario);
            return complexScenarioProcessor.process(context);
        }

        // 🔧 CORREÇÃO: Verificar se é uma posição que já teve saídas parciais
        boolean hasConsolidatedOperations = consolidatedOperationService.hasConsolidatedOperations(
                context.position().getUser(), 
                context.position().getOptionSeries(), 
                context.position().getBrokerage()
        );
        
        log.info("Posição tem operações consolidadas: {}", hasConsolidatedOperations);
        
        // 🔧 CORREÇÃO: Se já tem operações consolidadas, usar PartialExitProcessor
        if (hasConsolidatedOperations) {
            log.info("✅ Posição com operações consolidadas - usando PartialExitProcessor");
            return partialExitProcessor.process(context);
        }
        
        // 🔧 CORREÇÃO: Para posições sem consolidadas, usar lógica baseada em lotes
        if (lotCount == 1) {
            PartialExitDetector.ExitType exitType = partialExitDetector.determineExitType(
                    context.position(), requestedQuantity);

            log.info("Lote único - Tipo de saída detectado: {}", exitType);

            switch (exitType) {
                case SINGLE_TOTAL_EXIT:
                    log.info("✅ Usando SingleLotExitProcessor para saída total única");
                    return singleLotProcessor.process(context);
                    
                case FIRST_PARTIAL_EXIT:
                case SUBSEQUENT_PARTIAL_EXIT:
                case FINAL_PARTIAL_EXIT:
                    log.info("✅ Usando PartialExitProcessor para saída parcial tipo: {}", exitType);
                    return partialExitProcessor.process(context);
                    
                default:
                    log.warn("❌ Tipo de saída não reconhecido: {} - usando SingleLotExitProcessor como fallback", exitType);
                    return singleLotProcessor.process(context);
            }
        }
        else if (lotCount > 1) {
            // Múltiplos lotes simples - usar MultipleLotExitProcessor
            log.info("Múltiplos lotes detectados - usando MultipleLotExitProcessor");
            return multipleLotProcessor.process(context);
        }
        else {
            // Nenhum lote disponível - erro
            throw new IllegalStateException("Nenhum lote disponível para processamento");
        }
    }

    /**
     * Método auxiliar para logging detalhado da seleção
     */
    private void logProcessorSelection(OperationExitPositionContext context, String selectedProcessor) {

        log.debug("=== SELEÇÃO DE PROCESSADOR ===");
        log.debug("Operação ID: {}", context.context().activeOperation().getId());
        log.debug("Posição ID: {}", context.position().getId());
        log.debug("Status da posição: {}", context.position().getStatus());
        log.debug("Lotes disponíveis: {}", context.availableLots().size());
        log.debug("Quantidade restante: {}", context.position().getRemainingQuantity());
        log.debug("Quantidade solicitada: {}", context.context().request().getQuantity());
        log.debug("Processador selecionado: {}", selectedProcessor);
        log.debug("=============================");
    }

    /**
     * Verifica se o contexto é válido para processamento
     */
    private void validateContext(OperationExitPositionContext context) {

        if (context == null) {
            throw new IllegalArgumentException("Contexto de saída não pode ser nulo");
        }

        if (context.availableLots() == null) {
            throw new IllegalArgumentException("Lista de lotes disponíveis não pode ser nula");
        }

        if (context.position() == null) {
            throw new IllegalArgumentException("Posição não pode ser nula");
        }

        if (context.context() == null || context.context().request() == null) {
            throw new IllegalArgumentException("Request de finalização não pode ser nulo");
        }
    }

    /**
     * Método público para testar a seleção sem processar
     * Útil para debugging e testes
     */
    public String getSelectedProcessorName(OperationExitPositionContext context) {

        validateContext(context);

        int lotCount = context.availableLots().size();
        Integer requestedQuantity = context.context().request().getQuantity();

        // Verificar cenário complexo primeiro
        if (complexScenarioDetector.isComplexScenario(context.position(), requestedQuantity)) {
            ComplexScenarioDetector.ScenarioType scenario = complexScenarioDetector.detectScenario(
                context.position(), requestedQuantity);
            return "ComplexScenarioProcessor (" + scenario + ")";
        }

        if (lotCount == 1) {
            PartialExitDetector.ExitType exitType = partialExitDetector.determineExitType(
                    context.position(), requestedQuantity);

            switch (exitType) {
                case SINGLE_TOTAL_EXIT:
                    return "SingleLotExitProcessor";
                case FIRST_PARTIAL_EXIT:
                case SUBSEQUENT_PARTIAL_EXIT:
                case FINAL_PARTIAL_EXIT:
                    return "PartialExitProcessor";
                default:
                    return "SingleLotExitProcessor (fallback)";
            }
        } else if (lotCount > 1) {
            return "MultipleLotExitProcessor";
        } else {
            return "ERROR - Nenhum lote disponível";
        }
    }
}
