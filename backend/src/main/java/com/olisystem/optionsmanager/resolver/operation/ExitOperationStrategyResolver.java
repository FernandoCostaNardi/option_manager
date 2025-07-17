package com.olisystem.optionsmanager.resolver.operation;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.record.operation.OperationExitContext;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.service.operation.detector.PartialExitDetector;
import com.olisystem.optionsmanager.service.operation.factory.ExitContextFactory;
import com.olisystem.optionsmanager.service.operation.strategy.ExitOperationStrategy;
import com.olisystem.optionsmanager.service.operation.strategy.processor.ExitProcessorSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExitOperationStrategyResolver {

    private final ExitProcessorSelector processorSelector;
    private final ExitContextFactory contextFactory;
    private final PartialExitDetector partialExitDetector;

    /**
     * Resolve a estratégia adequada baseada no contexto da operação
     * CORRIGIDO: Usar ExitProcessorSelector diretamente, sem depender de Position
     */
    public ExitOperationStrategy resolveStrategy(OperationExitContext context) {

        // Validar contexto
        if (context == null || context.activeOperation() == null || context.request() == null) {
            throw new IllegalArgumentException("Contexto de saída inválido");
        }

        Operation activeOperation = context.activeOperation();
        Integer requestedQuantity = context.request().getQuantity();

        log.info("🔍 Resolvendo estratégia para operação: {}, quantidade solicitada: {}", 
                activeOperation.getId(), requestedQuantity);

        try {
            // ✅ CORREÇÃO: Usar ExitProcessorSelector diretamente
            // Criar contexto de posição para o seletor
            OperationExitPositionContext positionContext = contextFactory.createPositionContext(context);
            
            // Usar o seletor para determinar o processador correto
            String selectedProcessor = processorSelector.getSelectedProcessorName(positionContext);
            log.info("🎯 Processador selecionado pelo ExitProcessorSelector: {}", selectedProcessor);
            
            // ✅ ESTRATÉGIA UNIFICADA: Criar uma estratégia que usa o ExitProcessorSelector
            return new UnifiedExitOperationStrategy(processorSelector, contextFactory);

        } catch (Exception e) {
            log.error("❌ Erro ao resolver estratégia para operação {}: {}", activeOperation.getId(), e.getMessage());
            throw new RuntimeException("Falha ao resolver estratégia de saída", e);
        }
    }

    /**
     * Estratégia unificada que usa o ExitProcessorSelector
     */
    private static class UnifiedExitOperationStrategy implements ExitOperationStrategy {
        private final ExitProcessorSelector processorSelector;
        private final ExitContextFactory contextFactory;
        
        public UnifiedExitOperationStrategy(ExitProcessorSelector processorSelector, ExitContextFactory contextFactory) {
            this.processorSelector = processorSelector;
            this.contextFactory = contextFactory;
        }
        
        @Override
        public com.olisystem.optionsmanager.model.operation.Operation process(OperationExitContext context) {
            log.info("🔄 Usando estratégia unificada com ExitProcessorSelector");
            
            // Preparar contexto de posição
            OperationExitPositionContext positionContext = contextFactory.createPositionContext(context);
            
            // Usar o seletor para processar
            return processorSelector.selectAndProcess(positionContext);
        }
    }
}
