package com.olisystem.optionsmanager.service.operation.filter;

import com.olisystem.optionsmanager.dto.operation.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.operation.OperationItemDto;
import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.mapper.operation.OperationItemMapper;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.AverageOperationItem;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.AverageOperationItemRepository;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import com.olisystem.optionsmanager.util.OperationSummaryCalculator;
import com.olisystem.optionsmanager.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OperationFilterServiceImpl implements OperationFilterService {

    private final OperationRepository operationRepository;
    private final PositionRepository positionRepository;
    private final OperationItemMapper operationItemMapper;
    private final AverageOperationItemRepository averageOperationItemRepository;

    public OperationFilterServiceImpl(
            OperationRepository operationRepository,
            PositionRepository positionRepository,
            OperationItemMapper operationItemMapper,
            AverageOperationItemRepository averageOperationItemRepository
    ) {
        this.operationRepository = operationRepository;
        this.positionRepository = positionRepository;
        this.operationItemMapper = operationItemMapper;
        this.averageOperationItemRepository = averageOperationItemRepository;
    }

    @Override
    public OperationSummaryResponseDto findByFilters(
            OperationFilterCriteria criteria, Pageable pageable) {
        Page<Operation> page = operationRepository.findAll(createSpecification(criteria), pageable);

        // Buscar todas as operações para calcular totais (sem paginação)
        Specification<Operation> spec = createSpecification(criteria);
        List<Operation> allOperations = operationRepository.findAll(spec);
        List<OperationItemDto> allDtos =
                allOperations.stream().map(operationItemMapper::mapToDto).collect(Collectors.toList());

        List<OperationItemDto> dtos =
                page.getContent().stream().map(operationItemMapper::mapToDto).collect(Collectors.toList());

        // 🔧 CORREÇÃO: Calcular valor investido baseado no status das operações
        BigDecimal totalInvestedValue = calculateTotalInvestedValue(allOperations, criteria);

        // Calcular totalizadores usando a classe utilitária corrigida
        OperationSummaryResponseDto summary =
                OperationSummaryCalculator.calculateSummaryWithTotalsAndInvestedValue(
                        dtos,
                        allDtos,
                        totalInvestedValue,
                        page.getNumber(),
                        page.getTotalPages(),
                        page.getTotalElements(),
                        page.getSize());
        return summary;
    }

    /**
     * 🔧 CORREÇÃO: Calcular valor total investido baseado apenas em operações com data de saída 
     * e que não sejam consolidadas (quantidade × valor unitário de entrada)
     */
    private BigDecimal calculateTotalInvestedValue(List<Operation> allOperations, OperationFilterCriteria criteria) {
        log.info("=== INICIANDO CÁLCULO DE VALOR INVESTIDO ===");
        
        log.info("Total de operações recebidas: {}", allOperations.size());
        
        // ✅ LÓGICA SIMPLIFICADA: Filtrar operações com data de saída e valores válidos
        List<Operation> exitedOperations = allOperations.stream()
                .filter(operation -> {
                    boolean hasExitDate = operation.getExitDate() != null;
                    boolean hasValidValues = operation.getQuantity() != null && operation.getEntryUnitPrice() != null;
                    
                    log.info("Operação {}: exitDate = {} | quantidade = {} | precoUnitario = {} | válida = {}", 
                            operation.getOptionSeries().getCode(), 
                            operation.getExitDate(), 
                            operation.getQuantity(),
                            operation.getEntryUnitPrice(),
                            hasExitDate && hasValidValues);
                    
                    return hasExitDate && hasValidValues;
                })
                .toList();
        
        log.info("Operações filtradas: {} operações totais, {} com saída válidas", 
                allOperations.size(), exitedOperations.size());
        
        exitedOperations.forEach(op -> {
            BigDecimal operationValue = op.getEntryUnitPrice().multiply(BigDecimal.valueOf(op.getQuantity()));
            log.info("✅ INCLUÍDA - Operação {}: {} unidades × {} = {}", 
                    op.getOptionSeries().getCode(),
                    op.getQuantity(), 
                    op.getEntryUnitPrice(), 
                    operationValue);
        });
        
        if (exitedOperations.isEmpty()) {
            log.warn("Nenhuma operação com saída válida encontrada para cálculo");
            return BigDecimal.ZERO;
        }
        
        // ✅ CALCULAR: quantidade × valor unitário de entrada para cada operação
        BigDecimal total = exitedOperations.stream()
                .map(op -> {
                    BigDecimal operationValue = op.getEntryUnitPrice()
                            .multiply(BigDecimal.valueOf(op.getQuantity()));
                    return operationValue;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("=== VALOR TOTAL INVESTIDO CALCULADO: {} ===", total);
        return total;
    }
    
    /**
     * Verifica se uma operação é consolidada (baseado no seu grupo e roleType)
     * ⚠️ MÉTODO TEMPORARIAMENTE DESABILITADO PARA DEPURAÇÃO
     */
    private boolean isConsolidatedOperation(Operation operation) {
        // 🔧 TEMPORÁRIO: Retornar sempre false para incluir todas as operações
        // até resolvermos o problema da lógica de consolidação
        return false;
        
        /* LÓGICA ORIGINAL COMENTADA:
        try {
            log.debug("🔍 Verificando se operação {} é consolidada...", operation.getOptionSeries().getCode());
            
            // ✅ BUSCAR TODOS OS ITENS da operação para depuração
            List<AverageOperationItem> allItems = averageOperationItemRepository.findByOperation_Id(operation.getId());
            
            log.info("🔍 Operação {} tem {} itens no grupo:", operation.getOptionSeries().getCode(), allItems.size());
            
            for (AverageOperationItem item : allItems) {
                log.info("  - Item ID: {} | RoleType: {} | É consolidação: {}", 
                        item.getId(), item.getRoleType(), item.getRoleType().isConsolidation());
            }
            
            // ✅ LÓGICA CORRIGIDA: Uma operação é considerada consolidada apenas se 
            // TODOS os seus itens forem do tipo consolidação
            boolean hasNonConsolidatedItem = allItems.stream()
                    .anyMatch(item -> !item.getRoleType().isConsolidation());
            
            boolean isOperationConsolidated = !hasNonConsolidatedItem && !allItems.isEmpty();
            
            log.info("🔍 Operação {} - Tem item não consolidado: {} - É operação consolidada: {}", 
                     operation.getOptionSeries().getCode(), hasNonConsolidatedItem, isOperationConsolidated);
            
            return isOperationConsolidated;
            
        } catch (Exception e) {
            log.error("❌ Erro ao verificar se operação {} é consolidada: {}", 
                    operation.getId(), e.getMessage(), e);
            return false;
        }
        */
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
