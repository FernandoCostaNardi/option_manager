package com.olisystem.optionsmanager.service.operation.search;

import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.record.operation.OperationSearchRequest;
import com.olisystem.optionsmanager.service.operation.OperationService;
import com.olisystem.optionsmanager.service.operation.pagination.PaginationService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class OperationSearchServiceImpl implements OperationSearchService {

    private final OperationService operationService;
    private final PaginationService paginationService;

    public OperationSearchServiceImpl(
            OperationService operationService,
            PaginationService paginationService) {
        this.operationService = operationService;
        this.paginationService = paginationService;
    }

    @Override
    public OperationSummaryResponseDto searchOperations(OperationSearchRequest request, Pageable pageable) {
        // Aplicar valores padrão
        OperationSearchRequest searchRequest = request.withDefaults();

        // Corrigir problemas de paginação
        Pageable resolvedPageable = paginationService.resolvePageable(pageable);

        // Escolher a estratégia de busca apropriada
        if (searchRequest.hasAdditionalCriteria()) {
            return operationService.findByFilters(searchRequest.toFilterCriteria(), resolvedPageable);
        } else {
            return operationService.findByStatuses(searchRequest.status(), resolvedPageable);
        }
    }
}
