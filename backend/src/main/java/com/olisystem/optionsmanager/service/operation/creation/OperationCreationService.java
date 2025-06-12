package com.olisystem.optionsmanager.service.operation.creation;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.option_serie.OptionSerie;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    /**
     * Cria uma nova operação de saída
     */
    Operation createExitOperation(OperationExitPositionContext exitPositionContext, TradeType tradeType, BigDecimal profitLoss, TransactionType type, Integer totalQuantity);

    /**
     * Cria uma operação de saída com dados específicos calculados
     * Usado para cenários complexos onde os dados já foram calculados previamente
     */
    Operation createExitOperationWithSpecificData(
        Operation originalOperation,
        OptionSerie optionSerie,
        User currentUser,
        int quantity,
        BigDecimal entryUnitPrice,
        BigDecimal exitUnitPrice,
        BigDecimal profitLoss,
        BigDecimal profitLossPercentage,
        TradeType tradeType,
        LocalDate exitDate
    );
}
