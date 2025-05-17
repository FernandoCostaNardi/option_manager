package com.olisystem.optionsmanager.service.operation.search;

import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.record.operation.OperationSearchRequest;
import org.springframework.data.domain.Pageable;

public interface OperationSearchService {
    /**
     * Realiza a busca de operações com base nos critérios e paginação fornecidos
     */
    OperationSummaryResponseDto searchOperations(OperationSearchRequest searchRequest, Pageable pageable);
}
