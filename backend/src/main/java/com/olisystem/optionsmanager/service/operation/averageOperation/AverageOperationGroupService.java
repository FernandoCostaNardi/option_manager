package com.olisystem.optionsmanager.service.operation.averageOperation;

import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroupStatus;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.repository.AverageOperationGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AverageOperationGroupService {

    private final AverageOperationGroupRepository groupRepository;
    private final AverageOperationService averageOperationService;

    /**
     * Atualiza o grupo de operações com os dados da operação de saída
     */
    @Transactional
    public void updateOperationGroup(AverageOperationGroup group, Position position,
                                     Operation exitOperation, BigDecimal profitLoss) {
        log.debug("Atualizando grupo de operações ID: {}", group.getId());

        // Adicionar novo item ao grupo
        averageOperationService.addNewItemGroup(
                group, exitOperation, OperationRoleType.TOTAL_EXIT);

        // Atualizar grupo
        group.setRemainingQuantity(position.getRemainingQuantity());
        group.setAvgExitPrice(exitOperation.getExitUnitPrice());
        group.setClosedQuantity(exitOperation.getQuantity());
        group.setTotalProfit(profitLoss);
        group.setStatus(AverageOperationGroupStatus.CLOSED);

        groupRepository.save(group);

        log.debug("Grupo de operações atualizado com sucesso");
    }
}
