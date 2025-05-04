package com.olisystem.optionsmanager.report;

import com.olisystem.optionsmanager.dto.operation.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import org.springframework.data.domain.Pageable;

public interface OperationReportData {
  OperationSummaryResponseDto findByFilters(OperationFilterCriteria criteria, Pageable pageable);
}
