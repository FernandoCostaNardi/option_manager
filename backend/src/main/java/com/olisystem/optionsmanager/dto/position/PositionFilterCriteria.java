package com.olisystem.optionsmanager.dto.position;

import com.olisystem.optionsmanager.model.option_serie.OptionType;
import com.olisystem.optionsmanager.model.position.PositionStatus;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionFilterCriteria {
  private List<PositionStatus> status;
  private LocalDate openDateStart;
  private LocalDate openDateEnd;
  private LocalDate closeDateStart;
  private LocalDate closeDateEnd;
  private String brokerageName;
  private TransactionType direction;
  private OptionType optionType;
  private String optionSeriesCode;
  private String baseAssetCode;
  private Boolean hasMultipleEntries;
  private Boolean hasPartialExits;
}
