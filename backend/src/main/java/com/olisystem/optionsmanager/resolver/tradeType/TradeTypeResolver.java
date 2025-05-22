package com.olisystem.optionsmanager.resolver.tradeType;

import com.olisystem.optionsmanager.model.operation.TradeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TradeTypeResolver {

    /**
     * Determina o tipo de operação com base nas datas de entrada e saída
     */
    public TradeType determineTradeType(LocalDate entryDate, LocalDate exitDate) {
        return entryDate.equals(exitDate) ? TradeType.DAY : TradeType.SWING;
    }
}
