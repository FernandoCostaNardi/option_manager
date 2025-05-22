package com.olisystem.optionsmanager.service.operation.strategy.processor;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExitProcessorSelector {

    private final SingleLotExitProcessor singleLotProcessor;
  //  private final MultipleLotExitProcessor multipleLotProcessor;

    /**
     * Seleciona o processador apropriado com base no número de lotes
     */
    public Operation selectAndProcess(OperationExitPositionContext context) {
        int lotCount = context.availableLots().size();
        log.info("Selecionando processador para {} lotes", lotCount);

        if (lotCount > 1) {
            log.debug("Utilizando processador de múltiplos lotes");
         //   return multipleLotProcessor.process(context);
            return null;
        } else {
            log.debug("Utilizando processador de lote único");
            return singleLotProcessor.process(context);
        }
    }
}
