package com.olisystem.optionsmanager.service.operation.pagination;


import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PaginationServiceImpl implements PaginationService{
    /**
     * Corrige problemas de mapeamento de propriedades na ordenação
     */
    public Pageable resolvePageable(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                if (order.getProperty().equals("optionSerieCode")) {
                    // Criar um novo Sort com o nome correto da propriedade
                    Sort newSort = Sort.by(order.getDirection(), "optionSeriesCode");
                    // Criar um novo Pageable com o Sort corrigido
                    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), newSort);
                }
            }
        }
        return pageable;
    }
}
