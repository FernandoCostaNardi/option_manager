package com.olisystem.optionsmanager.resolver.operation;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.record.operation.OperationExitContext;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import com.olisystem.optionsmanager.service.operation.detector.PartialExitDetector;
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
    private final PartialExitDetector partialExitDetector;
    private final PositionRepository positionRepository;

    /**
     * Resolve a estratégia adequada baseada no contexto da operação
     */
    public ExitOperationStrategy resolveStrategy(OperationExitContext context) {

        // Validar contexto
        if (context == null || context.activeOperation() == null || context.request() == null) {
            throw new IllegalArgumentException("Contexto de saída inválido");
        }

        Operation activeOperation = context.activeOperation();
        Integer requestedQuantity = context.request().getQuantity();

        log.debug("Resolvendo estratégia para operação: {}, quantidade solicitada: {}", 
                activeOperation.getId(), requestedQuantity);

        try {
            // 🎯 NOVA LÓGICA: Buscar posição através da operação
            var positionOpt = positionRepository.findByOperationId(activeOperation.getId());
            
            if (positionOpt.isEmpty()) {
                throw new RuntimeException("Position não encontrada para operação: " + activeOperation.getId());
            }
            
            var position = positionOpt.get();
            
            log.debug("Position encontrada - Status: {}, Quantidade restante: {}, Quantidade total: {}", 
                    position.getStatus(), position.getRemainingQuantity(), position.getTotalQuantity());

            // 🔍 Usar PartialExitDetector para classificação precisa
            boolean isFirstPartial = partialExitDetector.isFirstPartialExit(position);
            boolean isSubsequentPartial = partialExitDetector.isSubsequentPartialExit(position);
            boolean isFinalFromPartial = partialExitDetector.isFinalExit(position, requestedQuantity) 
                    && position.getStatus() == com.olisystem.optionsmanager.model.position.PositionStatus.PARTIAL;
            boolean isPartialNormal = partialExitDetector.isPartialExit(position, requestedQuantity);

            log.debug("Análise detalhada: isFirstPartial={}, isSubsequentPartial={}, isFinalFromPartial={}, isPartialNormal={}", 
                    isFirstPartial, isSubsequentPartial, isFinalFromPartial, isPartialNormal);

            // 📋 Qualquer tipo de saída parcial → PartialExitOperationStrategy
            if (isFirstPartial || isSubsequentPartial || isFinalFromPartial || isPartialNormal) {
                if (isFinalFromPartial) {
                    log.info("🎯 FINAL_PARTIAL_EXIT detectada - usando PartialExitOperationStrategy");
                } else if (isFirstPartial) {
                    log.info("🔄 FIRST_PARTIAL_EXIT detectada - usando PartialExitOperationStrategy");
                } else if (isSubsequentPartial) {
                    log.info("➡️ SUBSEQUENT_PARTIAL_EXIT detectada - usando PartialExitOperationStrategy");
                } else {
                    log.info("📝 PARTIAL_EXIT detectada - usando PartialExitOperationStrategy");
                }
                return partialExitStrategy;
            }

            // 📊 Saída total simples (position OPEN + quantidade = total)
            log.info("✅ TOTAL_EXIT detectada - usando TotalExitOperationStrategy");
            return totalExitStrategy;

        } catch (Exception e) {
            log.error("Erro ao buscar posição para operação {}: {}", activeOperation.getId(), e.getMessage());
            
            // 🔄 Fallback para lógica anterior em caso de erro
            boolean isPartialExit = requestedQuantity < activeOperation.getQuantity();
            
            if (isPartialExit) {
                log.warn("⚠️ Fallback: Saída parcial detectada - usando PartialExitOperationStrategy");
                return partialExitStrategy;
            } else {
                log.warn("⚠️ Fallback: Saída total detectada - usando TotalExitOperationStrategy");
                return totalExitStrategy;
            }
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
