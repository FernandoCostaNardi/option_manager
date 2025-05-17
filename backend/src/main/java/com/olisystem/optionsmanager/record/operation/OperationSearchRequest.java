package com.olisystem.optionsmanager.record.operation;

import com.olisystem.optionsmanager.dto.operation.OperationFilterCriteria;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.option_serie.OptionType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public record OperationSearchRequest(
        List<OperationStatus> status,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate entryDateStart,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate entryDateEnd,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate exitDateStart,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate exitDateEnd,

        String analysisHouseName,
        String brokerageName,
        TransactionType transactionType,
        TradeType tradeType,
        OptionType optionType,
        String optionSeriesCode
) {
    /**
     * Verifica se existem critérios de filtro além do status
     */
    public boolean hasAdditionalCriteria() {
        return entryDateStart != null || entryDateEnd != null ||
                exitDateStart != null || exitDateEnd != null ||
                analysisHouseName != null || brokerageName != null ||
                transactionType != null || tradeType != null ||
                optionType != null || optionSeriesCode != null;
    }

    /**
     * Retorna uma cópia com valores padrão aplicados quando necessário
     */
    public OperationSearchRequest withDefaults() {
        if (status == null || status.isEmpty()) {
            return new OperationSearchRequest(
                    Collections.singletonList(OperationStatus.ACTIVE),
                    entryDateStart, entryDateEnd, exitDateStart, exitDateEnd,
                    analysisHouseName, brokerageName, transactionType, tradeType,
                    optionType, optionSeriesCode
            );
        }
        return this;
    }

    /**
     * Converte para critérios de filtro
     */
    public OperationFilterCriteria toFilterCriteria() {
        return OperationFilterCriteria.builder()
                .status(status)
                .entryDateStart(entryDateStart)
                .entryDateEnd(entryDateEnd)
                .exitDateStart(exitDateStart)
                .exitDateEnd(exitDateEnd)
                .analysisHouseName(analysisHouseName)
                .brokerageName(brokerageName)
                .transactionType(transactionType)
                .tradeType(tradeType)
                .optionType(optionType)
                .optionSeriesCode(optionSeriesCode)
                .build();
    }
}
