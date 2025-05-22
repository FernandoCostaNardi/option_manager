package com.olisystem.optionsmanager.resolver.transactionType;

import com.olisystem.optionsmanager.model.transaction.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TransactionTypeResolver {

    /**
     * Determina o tipo de transação inverso
     */
    public TransactionType resolveInverseTransactionType(TransactionType originalType) {
        log.debug("Determinando tipo de transação inverso para: {}", originalType);

        TransactionType inverseType = originalType == TransactionType.BUY
                ? TransactionType.SELL
                : TransactionType.BUY;

        log.debug("Tipo de transação inverso determinado: {}", inverseType);
        return inverseType;
    }
}
