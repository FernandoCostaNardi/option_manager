package com.olisystem.optionsmanager.service.operation.filter;

import com.olisystem.optionsmanager.dto.operation.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.operation.OperationItemDto;
import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.mapper.operation.OperationItemMapper;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.util.OperationSummaryCalculator;
import com.olisystem.optionsmanager.util.SecurityUtil;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OperationFilterServiceImpl implements OperationFilterService {

    private final OperationRepository operationRepository;
    private final OperationItemMapper operationItemMapper;

    public OperationFilterServiceImpl(
            OperationRepository operationRepository,
            OperationItemMapper operationItemMapper
    ) {
        this.operationRepository = operationRepository;
        this.operationItemMapper = operationItemMapper;
    }

    @Override
    public OperationSummaryResponseDto findByFilters(
            OperationFilterCriteria criteria, Pageable pageable) {
        Page<Operation> page = operationRepository.findAll(createSpecification(criteria), pageable);

        // Buscar todas as operações para calcular totais (sem paginação)
        Specification<Operation> spec = createSpecification(criteria);
        List<Operation> allOperations = operationRepository.findAll(spec);
        List<OperationItemDto> allDtos =
                allOperations.stream().map(OperationItemMapper::mapToDto).collect(Collectors.toList());

        List<OperationItemDto> dtos =
                page.getContent().stream().map(OperationItemMapper::mapToDto).collect(Collectors.toList());

        // Calcular totalizadores usando a classe utilitária com informações de paginação e todos os
        // totais
        OperationSummaryResponseDto summary =
                OperationSummaryCalculator.calculateSummaryWithTotals(
                        dtos,
                        allDtos,
                        page.getNumber(),
                        page.getTotalPages(),
                        page.getTotalElements(),
                        page.getSize());
        return summary;
    }

    private Specification<Operation> createSpecification(OperationFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtro por usuário
            predicates.add(cb.equal(root.get("user"), SecurityUtil.getLoggedUser()));

            // Filtro por status
            if (criteria.getStatus() != null && !criteria.getStatus().isEmpty()) {
                predicates.add(root.get("status").in(criteria.getStatus()));
            }

            // Filtro por datas
            if (criteria.getEntryDateStart() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("entryDate"), criteria.getEntryDateStart()));
            }
            if (criteria.getEntryDateEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("entryDate"), criteria.getEntryDateEnd()));
            }
            if (criteria.getExitDateStart() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("exitDate"), criteria.getExitDateStart()));
            }
            if (criteria.getExitDateEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("exitDate"), criteria.getExitDateEnd()));
            }

            // Filtro por casa de análise
            if (criteria.getAnalysisHouseName() != null && !criteria.getAnalysisHouseName().isEmpty()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("analysisHouse").get("name")),
                                "%" + criteria.getAnalysisHouseName().toLowerCase() + "%"));
            }

            // Filtro por corretora
            if (criteria.getBrokerageName() != null && !criteria.getBrokerageName().isEmpty()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("brokerage").get("name")),
                                "%" + criteria.getBrokerageName().toLowerCase() + "%"));
            }

            // Filtro por tipo de transação
            if (criteria.getTransactionType() != null) {
                predicates.add(cb.equal(root.get("transactionType"), criteria.getTransactionType()));
            }

            // Filtro por tipo de trade
            if (criteria.getTradeType() != null) {
                predicates.add(cb.equal(root.get("tradeType"), criteria.getTradeType()));
            }

            // Filtro por tipo de opção
            if (criteria.getOptionType() != null) {
                predicates.add(cb.equal(root.get("optionSeries").get("type"), criteria.getOptionType()));
            }

            // Filtro por código da série
            if (criteria.getOptionSeriesCode() != null && !criteria.getOptionSeriesCode().isEmpty()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("optionSeries").get("code")),
                                "%" + criteria.getOptionSeriesCode().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
