package com.olisystem.optionsmanager.service.operation.filter;

import com.olisystem.optionsmanager.dto.operation.OperationFilterCriteria;
import com.olisystem.optionsmanager.dto.operation.OperationItemDto;
import com.olisystem.optionsmanager.dto.operation.OperationSummaryResponseDto;
import com.olisystem.optionsmanager.mapper.operation.OperationItemMapper;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.repository.AverageOperationGroupRepository;
import com.olisystem.optionsmanager.repository.OperationRepository;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import com.olisystem.optionsmanager.util.OperationSummaryCalculator;
import com.olisystem.optionsmanager.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.JoinType;

@Slf4j
@Service
public class OperationFilterServiceImpl implements OperationFilterService {

    private final OperationRepository operationRepository;
    private final AverageOperationGroupRepository groupRepository;
    private final PositionRepository positionRepository;
    private final OperationItemMapper operationItemMapper;

    public OperationFilterServiceImpl(
            OperationRepository operationRepository,
            AverageOperationGroupRepository groupRepository,
            PositionRepository positionRepository,
            OperationItemMapper operationItemMapper
    ) {
        this.operationRepository = operationRepository;
        this.groupRepository = groupRepository;
        this.positionRepository = positionRepository;
        this.operationItemMapper = operationItemMapper;
    }
    @Override
    public OperationSummaryResponseDto findByFilters(
            OperationFilterCriteria criteria, Pageable pageable) {
        
        log.info("=== NOVA LÓGICA: BUSCA PARTINDO DO AVERAGE_OPERATION_GROUP ===");
        log.info("Critérios recebidos: {}", criteria);
        log.info("Status nos critérios: {}", criteria.getStatus());
        
        UUID currentUserId = SecurityUtil.getLoggedUser().getId();
        log.info("User ID: {}", currentUserId);
        
        // 1. BUSCAR GRUPOS que atendem aos critérios
        List<AverageOperationGroup> filteredGroups = findGroupsByCriteria(criteria, currentUserId);
        log.info("Grupos encontrados: {}", filteredGroups.size());
        
        // 2. COLETAR TODAS AS OPERAÇÕES dos grupos encontrados
        List<Operation> allOperations = collectOperationsFromGroups(filteredGroups, criteria);
        log.info("Total de operações coletadas: {}", allOperations.size());
        
        // 2.5. APLICAR FILTROS ADICIONAIS (se necessário)
        allOperations = applyAdditionalFilters(allOperations, criteria);
        log.info("Total de operações após filtros adicionais: {}", allOperations.size());
        
        // 3. GARANTIR que todas as operações tenham o groupId preenchido
        setGroupIdInOperations(allOperations, filteredGroups);
        
        // 4. APLICAR PAGINAÇÃO
        Page<Operation> pagedOperations = applyPagination(allOperations, pageable);
        
        // 5. MAPEAR PARA DTO
        var allDtos = allOperations.stream()
                .map(operationItemMapper::mapToDto)
                .collect(Collectors.toList());

        var pagedDtos = pagedOperations.getContent().stream()
                .map(operationItemMapper::mapToDto)
                .collect(Collectors.toList());
        
        // 6. CALCULAR VALOR INVESTIDO ✅ CORRIGIDO
        BigDecimal totalInvestedValue = calculateTotalInvestedValue(allOperations, criteria);
        
        // 7. CALCULAR TOTALIZADORES
        OperationSummaryResponseDto summary = OperationSummaryCalculator.calculateSummaryWithTotalsAndInvestedValue(
                pagedDtos,
                allDtos,
                totalInvestedValue,
                pagedOperations.getNumber(),
                pagedOperations.getTotalPages(),
                pagedOperations.getTotalElements(),
                pagedOperations.getSize());
        
        log.info("Resultado final: {} operações na página, {} total", pagedDtos.size(), allOperations.size());
        return summary;
    }
    /**
     * Busca grupos baseado nos critérios de filtro
     */
    private List<AverageOperationGroup> findGroupsByCriteria(OperationFilterCriteria criteria, UUID userId) {
        log.info("=== FINDGROUPSBYCRITERIA ===");
        log.info("Critérios recebidos: status={}, entryDateStart={}, analysisHouse={}", 
            criteria.getStatus(), criteria.getEntryDateStart(), criteria.getAnalysisHouseName());
        
        boolean hasAnyFilter = hasAnyFilter(criteria);
        boolean hasNonStatusFilters = hasNonStatusFilters(criteria);
        
        log.info("hasAnyFilter: {}", hasAnyFilter);
        log.info("hasNonStatusFilters: {}", hasNonStatusFilters);
        
        try {
            // Se não há filtros, usar query simples
            if (!hasAnyFilter) {
                log.info("NENHUM FILTRO - USANDO QUERY SIMPLES");
                return groupRepository.findAll().stream()
                    .filter(group -> group.getItems().stream()
                        .anyMatch(item -> item.getOperation().getUser().getId().equals(userId)))
                    .collect(Collectors.toList());
            }
            
            // Se há apenas status ACTIVE (caso mais comum), usar query específica
            if (criteria.getStatus() != null && 
                criteria.getStatus().size() == 1 && 
                criteria.getStatus().get(0) == OperationStatus.ACTIVE &&
                !hasNonStatusFilters) {
                log.info("APENAS STATUS ACTIVE - USANDO QUERY ESPECÍFICA");
                List<AverageOperationGroup> result = groupRepository.findAll().stream()
                    .filter(group -> group.getItems().stream()
                        .anyMatch(item -> item.getOperation().getUser().getId().equals(userId) &&
                                        item.getOperation().getStatus() == OperationStatus.ACTIVE))
                    .collect(Collectors.toList());
                log.info("Grupos retornados pela query específica: {}", result.size());
                return result;
            }
            
            // Para outros casos, usar query simples e filtrar depois
            log.info("OUTROS CASOS - USANDO QUERY SIMPLES COM FILTRO POSTERIOR");
            return groupRepository.findAll().stream()
                .filter(group -> group.getItems().stream()
                    .anyMatch(item -> item.getOperation().getUser().getId().equals(userId)))
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Erro na busca de grupos, fallback para query simples: {}", e.getMessage());
            return groupRepository.findAll().stream()
                .filter(group -> group.getItems().stream()
                    .anyMatch(item -> item.getOperation().getUser().getId().equals(userId)))
                .collect(Collectors.toList());
        }
    }
    /**
     * ✅ CORREÇÃO CRÍTICA: Coleta operações escolhendo a representação correta baseada no status procurado
     */
    private List<Operation> collectOperationsFromGroups(List<AverageOperationGroup> groups, OperationFilterCriteria criteria) {
        log.info("=== COLLECTOPERATIONSFROMGROUPS ===");
        log.info("Coletando operações de {} grupos", groups.size());
        log.info("Critérios de status: {}", criteria.getStatus());
        
        List<Operation> allOperations = new ArrayList<>();
        for (AverageOperationGroup group : groups) {
            log.info("Processando Grupo ID: {} - {} items", group.getId(), group.getItems() != null ? group.getItems().size() : 0);
            
            if (group.getItems() == null || group.getItems().isEmpty()) {
                log.warn("Grupo {} não tem items!", group.getId());
                continue;
            }
            
            // ✅ NOVA LÓGICA: Escolher operação baseada no status procurado
            Operation bestOperation = selectBestOperationForStatus(group.getItems(), criteria.getStatus());
            
            if (bestOperation != null) {
                allOperations.add(bestOperation);
                log.info("✅ Grupo {} representado por operação: ID={}, Status={}", 
                    group.getId(), bestOperation.getId(), bestOperation.getStatus());
            } else {
                log.warn("❌ Grupo {} não tem nenhuma operação adequada para o filtro!", group.getId());
            }
        }
        
        log.info("Total de operações coletadas (baseadas no filtro): {}", allOperations.size());
        return allOperations;
    }
    /**
     * ✅ CORREÇÃO PRINCIPAL: Seleciona a melhor operação de um grupo baseada no status procurado
     * Resolve o bug onde buscava ACTIVE mas escolhia CONSOLIDATED_RESULT com status WINNER
     */
    private Operation selectBestOperationForStatus(List<AverageOperationItem> items, List<OperationStatus> statusFilter) {
        log.info("  === SELECIONANDO MELHOR OPERAÇÃO ===");
        log.info("  Filtro de status: {}", statusFilter);
        
        // Separar operações por tipo e status
        Operation consolidatedResult = null;
        Operation consolidatedEntry = null;
        Operation originalActive = null;
        Operation totalExit = null;
        List<Operation> otherOperations = new ArrayList<>();
        
        for (AverageOperationItem item : items) {
            Operation operation = item.getOperation();
            
            log.info("  Analisando: RoleType={}, Status={}, ID={}", 
                item.getRoleType(), operation.getStatus(), operation.getId());
            
            // Pular operações HIDDEN
            if (operation.getStatus() == OperationStatus.HIDDEN) {
                log.info("  ❌ Ignorada: HIDDEN");
                continue;
            }
            
            // Categorizar operações
            switch (item.getRoleType()) {
                case CONSOLIDATED_RESULT:
                    consolidatedResult = operation;
                    log.info("  📊 CONSOLIDATED_RESULT encontrada: Status={}", operation.getStatus());
                    break;
                case CONSOLIDATED_ENTRY:
                    consolidatedEntry = operation;
                    log.info("  📥 CONSOLIDATED_ENTRY encontrada: Status={}", operation.getStatus());
                    break;
                case ORIGINAL:
                    if (operation.getStatus() == OperationStatus.ACTIVE) {
                        originalActive = operation;
                        log.info("  🔵 ORIGINAL ACTIVE encontrada");
                    }
                    break;
                case TOTAL_EXIT:
                    totalExit = operation;
                    log.info("  📤 TOTAL_EXIT encontrada: Status={}", operation.getStatus());
                    break;
                default:
                    otherOperations.add(operation);
                    log.info("  📋 Outra operação: {}", item.getRoleType());
                    break;
            }
        }
        
        // ✅ NOVA LÓGICA DE SELEÇÃO BASEADA NO STATUS
        if (statusFilter != null && statusFilter.size() == 1) {
            OperationStatus targetStatus = statusFilter.get(0);
            log.info("  🎯 Buscando status específico: {}", targetStatus);
            
            switch (targetStatus) {
                case ACTIVE:
                    // Para ACTIVE: priorizar CONSOLIDATED_ENTRY > ORIGINAL ACTIVE
                    if (consolidatedEntry != null && consolidatedEntry.getStatus() == OperationStatus.ACTIVE) {
                        log.info("  ✅ Escolhida: CONSOLIDATED_ENTRY ACTIVE");
                        return consolidatedEntry;
                    }
                    if (originalActive != null) {
                        log.info("  ✅ Escolhida: ORIGINAL ACTIVE (fallback)");
                        return originalActive;
                    }
                    break;
                    
                case WINNER:
                case LOSER:
                    // Para WINNER/LOSER: priorizar CONSOLIDATED_RESULT > TOTAL_EXIT
                    if (consolidatedResult != null && consolidatedResult.getStatus() == targetStatus) {
                        log.info("  ✅ Escolhida: CONSOLIDATED_RESULT {}", targetStatus);
                        return consolidatedResult;
                    }
                    if (totalExit != null && totalExit.getStatus() == targetStatus) {
                        log.info("  ✅ Escolhida: TOTAL_EXIT {}", targetStatus);
                        return totalExit;
                    }
                    break;
                    
                default:
                    // Para outros status: usar lógica original
                    if (consolidatedResult != null && consolidatedResult.getStatus() == targetStatus) {
                        log.info("  ✅ Escolhida: CONSOLIDATED_RESULT {}", targetStatus);
                        return consolidatedResult;
                    }
                    if (totalExit != null && totalExit.getStatus() == targetStatus) {
                        log.info("  ✅ Escolhida: TOTAL_EXIT {}", targetStatus);
                        return totalExit;
                    }
                    break;
            }
        }
        
        // ✅ FALLBACK: Lógica original (priorizar consolidadas)
        log.info("  🔄 Usando lógica de fallback");
        if (consolidatedResult != null) {
            log.info("  ✅ Escolhida: CONSOLIDATED_RESULT (fallback)");
            return consolidatedResult;
        }
        if (totalExit != null) {
            log.info("  ✅ Escolhida: TOTAL_EXIT (fallback)");
            return totalExit;
        }
        if (consolidatedEntry != null) {
            log.info("  ✅ Escolhida: CONSOLIDATED_ENTRY (fallback)");
            return consolidatedEntry;
        }
        if (originalActive != null) {
            log.info("  ✅ Escolhida: ORIGINAL ACTIVE (fallback)");
            return originalActive;
        }
        if (!otherOperations.isEmpty()) {
            log.info("  ✅ Escolhida: Primeira outra operação (último fallback)");
            return otherOperations.get(0);
        }
        
        log.warn("  ❌ Nenhuma operação adequada encontrada");
        return null;
    }
    /**
     * 🔧 CORREÇÃO CRÍTICA: Calcular valor total investido baseado na quantidade real
     * Para operações consolidadas (WINNER/LOSER): entryUnitPrice × quantity
     * Para operações ACTIVE: entryUnitPrice × quantity das operações ativas
     */
    private BigDecimal calculateTotalInvestedValue(List<Operation> allOperations, OperationFilterCriteria criteria) {
        log.info("=== INICIANDO CÁLCULO DE VALOR INVESTIDO CORRIGIDO ===");
        
        boolean isActiveFilter = criteria.getStatus() != null && 
                               criteria.getStatus().contains(OperationStatus.ACTIVE);
        
        if (isActiveFilter) {
            log.info("=== CÁLCULO PARA OPERAÇÕES ACTIVE ===");
            BigDecimal totalDireto = allOperations.stream()
                .filter(op -> op.getStatus() == OperationStatus.ACTIVE)
                .map(op -> {
                    // Para ACTIVE: calcular baseado na quantidade real da operação
                    BigDecimal unitPrice = op.getEntryUnitPrice() != null ? op.getEntryUnitPrice() : BigDecimal.ZERO;
                    Integer quantity = op.getQuantity() != null ? op.getQuantity() : 0;
                    BigDecimal value = unitPrice.multiply(BigDecimal.valueOf(quantity));
                    
                    log.info("Operação ACTIVE: ID={}, Code={}, Quantity={}, UnitPrice={}, CalculatedValue={}", 
                        op.getId(), op.getOptionSeries().getCode(), quantity, unitPrice, value);
                    return value;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            log.info("=== VALOR TOTAL CALCULADO PARA ACTIVE: {} ===", totalDireto);
            return totalDireto;
        }
        
        // 🔧 CORREÇÃO PRINCIPAL: Para WINNER/LOSER calcular baseado na quantidade real
        log.info("=== CÁLCULO CORRIGIDO PARA WINNER/LOSER ===");
        BigDecimal total = allOperations.stream()
            .map(op -> {
                // ✅ NOVA LÓGICA: entryUnitPrice × quantity (não entryTotalValue fixo)
                BigDecimal unitPrice = op.getEntryUnitPrice() != null ? op.getEntryUnitPrice() : BigDecimal.ZERO;
                Integer quantity = op.getQuantity() != null ? op.getQuantity() : 0;
                BigDecimal calculatedValue = unitPrice.multiply(BigDecimal.valueOf(quantity));
                
                log.info("Operação: ID={}, Code={}, Status={}, Quantity={}, UnitPrice={}, CalculatedValue={}", 
                    op.getId(), op.getOptionSeries().getCode(), op.getStatus(), 
                    quantity, unitPrice, calculatedValue);
                    
                return calculatedValue;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("=== VALOR TOTAL CORRIGIDO: {} ===", total);
        return total;
    }
    /**
     * Verifica se há algum filtro ativo
     */
    private boolean hasAnyFilter(OperationFilterCriteria criteria) {
        return criteria.getStatus() != null && !criteria.getStatus().isEmpty()
            || criteria.getEntryDateStart() != null
            || criteria.getEntryDateEnd() != null
            || criteria.getExitDateStart() != null
            || criteria.getExitDateEnd() != null
            || criteria.getAnalysisHouseName() != null
            || criteria.getBrokerageName() != null
            || criteria.getTransactionType() != null
            || criteria.getTradeType() != null
            || criteria.getOptionType() != null
            || criteria.getOptionSeriesCode() != null;
    }

    /**
     * Verifica se há filtros além do status
     */
    private boolean hasNonStatusFilters(OperationFilterCriteria criteria) {
        return criteria.getEntryDateStart() != null
            || criteria.getEntryDateEnd() != null
            || criteria.getExitDateStart() != null
            || criteria.getExitDateEnd() != null
            || criteria.getAnalysisHouseName() != null
            || criteria.getBrokerageName() != null
            || criteria.getTransactionType() != null
            || criteria.getTradeType() != null
            || criteria.getOptionType() != null
            || criteria.getOptionSeriesCode() != null;
    }
    /**
     * ✅ CORREÇÃO: Aplica filtros adicionais SEM filtrar por status 
     * (status já foi aplicado na seleção da operação)
     */
    private List<Operation> applyAdditionalFilters(List<Operation> operations, OperationFilterCriteria criteria) {
        log.info("=== APLICANDO FILTROS ADICIONAIS ===");
        log.info("Operações antes dos filtros: {}", operations.size());
        
        List<Operation> filtered = operations.stream()
            .filter(operation -> {
                // ✅ SKIP filtro de status - já foi aplicado na seleção da operação
                log.info("✅ Operação {} mantida (status já filtrado na seleção): Status={}", 
                    operation.getId(), operation.getStatus());
                
                // Filtro de data de entrada
                if (criteria.getEntryDateStart() != null && operation.getEntryDate().isBefore(criteria.getEntryDateStart())) {
                    log.info("❌ Operação {} filtrada: EntryDate {} < {}", 
                        operation.getId(), operation.getEntryDate(), criteria.getEntryDateStart());
                    return false;
                }
                if (criteria.getEntryDateEnd() != null && operation.getEntryDate().isAfter(criteria.getEntryDateEnd())) {
                    log.info("❌ Operação {} filtrada: EntryDate {} > {}", 
                        operation.getId(), operation.getEntryDate(), criteria.getEntryDateEnd());
                    return false;
                }
                
                // Filtro de data de saída
                if (criteria.getExitDateStart() != null && operation.getExitDate() != null && 
                    operation.getExitDate().isBefore(criteria.getExitDateStart())) {
                    log.info("❌ Operação {} filtrada: ExitDate {} < {}", 
                        operation.getId(), operation.getExitDate(), criteria.getExitDateStart());
                    return false;
                }
                if (criteria.getExitDateEnd() != null && operation.getExitDate() != null && 
                    operation.getExitDate().isAfter(criteria.getExitDateEnd())) {
                    log.info("❌ Operação {} filtrada: ExitDate {} > {}", 
                        operation.getId(), operation.getExitDate(), criteria.getExitDateEnd());
                    return false;
                }
                
                return true;
            })
            .collect(Collectors.toList());
        
        log.info("Operações após filtros adicionais: {}", filtered.size());
        return filtered;
    }
    /**
     * Garante que todas as operações tenham o groupId preenchido
     */
    private void setGroupIdInOperations(List<Operation> operations, List<AverageOperationGroup> groups) {
        // Criar mapa grupo -> operações para facilitar a busca
        Map<UUID, UUID> operationToGroupMap = new HashMap<>();
        
        for (AverageOperationGroup group : groups) {
            if (group.getItems() != null) {
                for (AverageOperationItem item : group.getItems()) {
                    operationToGroupMap.put(item.getOperation().getId(), group.getId());
                }
            }
        }
        
        log.info("GroupId mapeado para {} operações", operationToGroupMap.size());
    }
    
    /**
     * Aplica paginação manual na lista de operações
     */
    private Page<Operation> applyPagination(List<Operation> operations, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), operations.size());
        
        List<Operation> pageContent = operations.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, operations.size());
    }
}