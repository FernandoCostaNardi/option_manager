package com.olisystem.optionsmanager.service.operation.creation;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;

public interface OperationCreationService {
    /**
     * Cria uma nova operação ativa
     */
    Operation createActiveOperation(OperationDataRequest request, OptionSerie optionSerie, User currentUser);

    /**
     * Cria uma nova operação oculta
     */
    Operation createHiddenOperation(OperationDataRequest request, OptionSerie optionSerie, User currentUser);

    /**
     * Cria uma operação consolidada a partir de outra existente
     */
    Operation createConsolidatedOperation(Operation originalOperation, OptionSerie optionSerie, User currentUser);
}
