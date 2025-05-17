package com.olisystem.optionsmanager.service.operation.filter;

import com.olisystem.optionsmanager.dto.operation.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import org.springframework.data.domain.Pageable;

public interface OperationFilterService {

    public OperationSummaryResponseDto findByFilters(
            OperationFilterCriteria criteria, Pageable pageable);
}
