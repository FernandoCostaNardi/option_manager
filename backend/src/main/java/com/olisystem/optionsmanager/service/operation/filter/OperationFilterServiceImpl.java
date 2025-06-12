package com.olisystem.optionsmanager.service.operation.filter;

import com.olisystem.optionsmanager.dto.operation.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.operation.OperationItemDto;
import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.mapper.operation.OperationItemMapper;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.repository.OperationRepository;
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

    public OperationFilterServiceImpl(
            OperationRepository operationRepository,
            PositionRepository positionRepository
    ) {
        this.operationRepository = operationRepository;
        this.positionRepository = positionRepository;
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
     * 🔧 CORREÇÃO: Calcular valor total investido baseado nos EntryLots das Positions
     * relacionadas às operações filtradas, aplicando lógica específica por status
     */
    private BigDecimal calculateTotalInvestedValue(List<Operation> allOperations, OperationFilterCriteria criteria) {
        log.info("=== INICIANDO CÁLCULO DE VALOR INVESTIDO ===");
        
        // Extrair IDs das operações filtradas
        List<UUID> operationIds = allOperations.stream()
                .map(Operation::getId)
                .toList();
        
        log.info("Operações filtradas: {} operações", operationIds.size());
        allOperations.forEach(op -> log.info("- Operation ID: {} | Código: {} | Status: {}", 
                                           op.getId(), 
                                           op.getOptionSeries().getCode(), 
                                           op.getStatus()));
        
        if (operationIds.isEmpty()) {
            log.warn("Nenhuma operação encontrada para cálculo");
            return BigDecimal.ZERO;
        }
        
        // Buscar Positions relacionadas às operações filtradas
        List<Position> relatedPositions = positionRepository.findByOperationIds(operationIds);
        log.info("Positions relacionadas encontradas: {}", relatedPositions.size());
        
        // Verificar se é filtro por status ACTIVE
        boolean isActiveFilter = criteria.getStatus() != null && 
                               criteria.getStatus().contains(OperationStatus.ACTIVE);
        
        log.info("Filtro ACTIVE detectado: {}", isActiveFilter);
        log.info("Status no critério: {}", criteria.getStatus());
        
        // 🔧 CORREÇÃO: Para operações ACTIVE, buscar Positions por série de opção caso não encontre associação
        if (isActiveFilter && relatedPositions.size() < allOperations.size()) {
            log.info("Nem todas as operações ACTIVE têm Position associada. Buscando por série de opção...");
            
            // Buscar Positions ativas por série de opção para operações não associadas
            for (Operation operation : allOperations) {
                boolean positionFound = relatedPositions.stream()
                        .anyMatch(p -> p.getOperations().stream()
                                .anyMatch(po -> po.getOperation().getId().equals(operation.getId())));
                
                if (!positionFound) {
                    log.info("Buscando Position para operação {} ({})", 
                            operation.getOptionSeries().getCode(), operation.getId());
                    
                    Optional<Position> position = positionRepository.findOpenPositionByUserAndOptionSeriesAndDirection(
                            SecurityUtil.getLoggedUser(),
                            operation.getOptionSeries(),
                            operation.getTransactionType()
                    );
                    
                    if (position.isPresent()) {
                        relatedPositions.add(position.get());
                        log.info("Position alternativa encontrada: {} para {}", 
                                position.get().getId(), operation.getOptionSeries().getCode());
                    } else {
                        log.warn("Position não encontrada nem por associação nem por série: {}", 
                                operation.getOptionSeries().getCode());
                    }
                }
            }
        }
        
        // 🔍 DEBUG: Verificar cada operação individualmente
        for (Operation operation : allOperations) {
            Optional<Position> position = positionRepository.findByOperationId(operation.getId());
            log.info("Operação {} ({}): Position encontrada = {}", 
                    operation.getOptionSeries().getCode(),
                    operation.getId(),
                    position.isPresent() ? position.get().getId() : "NÃO ENCONTRADA");
        }
        
        log.info("Total de Positions após busca alternativa: {}", relatedPositions.size());
        
        relatedPositions.forEach(pos -> {
            log.info("- Position ID: {} | Série: {} | Quantidade total: {} | Restante: {}", 
                    pos.getId(), 
                    pos.getOptionSeries().getCode(),
                    pos.getTotalQuantity(),
                    pos.getRemainingQuantity());
            log.info("  EntryLots desta Position: {}", pos.getEntryLots().size());
        });
        
        // Coletar todos os EntryLots
        List<EntryLot> allEntryLots = relatedPositions.stream()
                .flatMap(position -> position.getEntryLots().stream())
                .toList();
        
        log.info("Total de EntryLots encontrados: {}", allEntryLots.size());
        
        BigDecimal total = BigDecimal.ZERO;
        
        if (isActiveFilter) {
            // 🔧 CORREÇÃO: Para ACTIVE usar preço médio da Position
            log.info("Calculando valor usando preço médio das Positions (ACTIVE)");
            for (Position position : relatedPositions) {
                BigDecimal positionValue = calculatePositionValue(position, isActiveFilter);
                total = total.add(positionValue);
                
                log.info("Position ID: {} | Série: {} | Quantidade restante: {} | Preço médio: {} | Valor calculado: {}", 
                        position.getId(),
                        position.getOptionSeries().getCode(),
                        position.getRemainingQuantity(), 
                        position.getAveragePrice(), 
                        positionValue);
            }
        } else {
            // Para outros status: usar EntryLots individuais  
            log.info("Calculando valor usando EntryLots individuais (NÃO-ACTIVE)");
            for (EntryLot entryLot : allEntryLots) {
                BigDecimal lotValue = calculateEntryLotValue(entryLot, isActiveFilter);
                total = total.add(lotValue);
                
                log.info("EntryLot ID: {} | Quantidade: {} | Restante: {} | Preço: {} | Valor calculado: {}", 
                        entryLot.getId(), 
                        entryLot.getQuantity(), 
                        entryLot.getRemainingQuantity(), 
                        entryLot.getUnitPrice(), 
                        lotValue);
            }
        }
        
        log.info("=== VALOR TOTAL INVESTIDO CALCULADO: {} ===", total);
        return total;
    }
    
    /**
     * Calcula o valor da Position baseado no preço médio:
     * - ACTIVE: remainingQuantity × averagePrice (valor ainda em aberto usando preço médio)
     */
    private BigDecimal calculatePositionValue(Position position, boolean isActiveFilter) {
        if (position.getAveragePrice() == null) {
            log.warn("Position {} tem averagePrice nulo", position.getId());
            return BigDecimal.ZERO;
        }
        
        int quantityToUse;
        String logicDescription;
        if (isActiveFilter) {
            // Para ACTIVE: usar quantidade restante com preço médio da posição
            quantityToUse = position.getRemainingQuantity();
            logicDescription = "ACTIVE - quantidade restante × preço médio";
        } else {
            // Para outros status: usar quantidade total menos restante
            quantityToUse = position.getTotalQuantity() - position.getRemainingQuantity();
            logicDescription = "NÃO-ACTIVE - quantidade consumida × preço médio";
        }
        
        BigDecimal result = position.getAveragePrice().multiply(BigDecimal.valueOf(quantityToUse));
        log.debug("Position {}: {} | Quantidade usada: {} | Preço médio: {} | Resultado: {}", 
                 position.getOptionSeries().getCode(), logicDescription, quantityToUse, position.getAveragePrice(), result);
        
        return result;
    }

    /**
     * Calcula o valor do EntryLot baseado na regra de negócio:
     * - ACTIVE: remainingQuantity × unitPrice (valor ainda em aberto)
     * - Outros: (quantity - remainingQuantity) × unitPrice (valor já consumido)
     */
    private BigDecimal calculateEntryLotValue(EntryLot entryLot, boolean isActiveFilter) {
        if (entryLot.getUnitPrice() == null) {
            log.warn("EntryLot {} tem unitPrice nulo", entryLot.getId());
            return BigDecimal.ZERO;
        }
        
        int quantityToUse;
        String logicDescription;
        if (isActiveFilter) {
            // Para ACTIVE: usar quantidade restante (ainda em aberto)
            quantityToUse = entryLot.getRemainingQuantity();
            logicDescription = "ACTIVE - quantidade restante";
        } else {
            // Para outros status: usar quantidade consumida
            quantityToUse = entryLot.getQuantity() - entryLot.getRemainingQuantity();
            logicDescription = "NÃO-ACTIVE - quantidade consumida";
        }
        
        BigDecimal result = entryLot.getUnitPrice().multiply(BigDecimal.valueOf(quantityToUse));
        log.debug("Lógica: {} | Quantidade usada: {} | Preço: {} | Resultado: {}", 
                 logicDescription, quantityToUse, entryLot.getUnitPrice(), result);
        
        return result;
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
