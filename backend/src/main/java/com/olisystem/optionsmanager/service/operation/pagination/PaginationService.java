package com.olisystem.optionsmanager.service.operation.pagination;

import org.springframework.data.domain.Pageable;

public interface PaginationService {
    public Pageable resolvePageable(Pageable pageable);
}
