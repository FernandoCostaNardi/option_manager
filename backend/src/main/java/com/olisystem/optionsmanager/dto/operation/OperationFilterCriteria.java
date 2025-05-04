package com.olisystem.optionsmanager.dto.operation;

import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.option_serie.OptionType;
import com.olisystem.optionsmanager.model.transaction.TransactionType;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperationFilterCriteria {
  private List<OperationStatus> status;
  private LocalDate entryDateStart;
  private LocalDate entryDateEnd;
  private LocalDate exitDateStart;
  private LocalDate exitDateEnd;
  private String analysisHouseName;
  private String brokerageName;
  private TransactionType transactionType;
  private TradeType tradeType;
  private OptionType optionType;
  private String optionSeriesCode; // Certifique-se de que o nome está correto aqui (com 's')
}
