package com.olisystem.optionsmanager.service.operation.averageOperation.finder;

import com.olisystem.optionsmanager.exception.ResourceNotFoundException;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationGroupFinder {

    private final AverageOperationService averageOperationService;

    /**
     * Localiza o grupo de operações associado à operação
     */
    public AverageOperationGroup findGroupByOperation(Operation operation) {
        log.debug("Buscando grupo para operação: {}", operation.getId());

        AverageOperationGroup group = averageOperationService.getGroupByOperation(operation);
        if (group == null) {
            throw new ResourceNotFoundException("Grupo de operação não encontrado para a operação: " + operation.getId());
        }

        return group;
    }
}
