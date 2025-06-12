package com.olisystem.optionsmanager.resolver.operation;

import com.olisystem.optionsmanager.record.operation.OperationExitContext;
import com.olisystem.optionsmanager.service.operation.strategy.ExitOperationStrategy;
import com.olisystem.optionsmanager.service.operation.strategy.TotalExitOperationStrategy;
import com.olisystem.optionsmanager.service.operation.strategy.processor.PartialExitOperationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExitOperationStrategyResolver {

    private final PartialExitOperationStrategy partialExitStrategy;
    private final TotalExitOperationStrategy totalExitStrategy;

    /**
     * Resolve a estratégia adequada baseada no contexto da operação
     */
    public ExitOperationStrategy resolveStrategy(OperationExitContext context) {

        // Validar contexto
        if (context == null || context.activeOperation() == null || context.request() == null) {
            throw new IllegalArgumentException("Contexto de saída inválido");
        }

        Integer totalQuantity = context.activeOperation().getQuantity();
        Integer requestedQuantity = context.request().getQuantity();

        log.debug("Resolvendo estratégia: quantidade total={}, quantidade solicitada={}",
                totalQuantity, requestedQuantity);

        // ✅ CORREÇÃO: Determinar se é saída parcial ou total
        boolean isPartialExit = requestedQuantity < totalQuantity;

        if (isPartialExit) {
            log.info("Saída parcial detectada - usando PartialExitOperationStrategy");
            return partialExitStrategy;
        } else {
            log.info("Saída total detectada - usando TotalExitOperationStrategy");
            return totalExitStrategy;
        }
    }

    /**
     * Método auxiliar para logging da decisão
     */
    private void logStrategyDecision(OperationExitContext context, String selectedStrategy) {
        log.debug("=== RESOLUÇÃO DE ESTRATÉGIA ===");
        log.debug("Operação ID: {}", context.activeOperation().getId());
        log.debug("Quantidade total: {}", context.activeOperation().getQuantity());
        log.debug("Quantidade solicitada: {}", context.request().getQuantity());
        log.debug("Estratégia selecionada: {}", selectedStrategy);
        log.debug("==============================");
    }

    /**
     * Método público para teste - retorna nome da estratégia sem processar
     */
    public String getSelectedStrategyName(OperationExitContext context) {

        if (context == null || context.activeOperation() == null || context.request() == null) {
            return "ERROR - Contexto inválido";
        }

        Integer totalQuantity = context.activeOperation().getQuantity();
        Integer requestedQuantity = context.request().getQuantity();

        boolean isPartialExit = requestedQuantity < totalQuantity;

        return isPartialExit ? "PartialExitOperationStrategy" : "TotalExitOperationStrategy";
    }
}
