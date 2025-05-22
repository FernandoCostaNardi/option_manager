package com.olisystem.optionsmanager.service.position.positionOperation;

import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.position.PositionOperation;
import com.olisystem.optionsmanager.model.position.PositionOperationType;
import com.olisystem.optionsmanager.repository.position.PositionOperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionOperationService {

    private final PositionOperationRepository positionOperationRepository;

    /**
     * Cria um registro de operação de posição
     */
    @Transactional
    public PositionOperation createPositionOperation(Position position, Operation exitOperation,
                                                     OperationFinalizationRequest request,
                                                     PositionOperationType type) {
        log.debug("Criando registro de operação de posição tipo: {}", type);

        PositionOperation operation = PositionOperation.builder()
                .position(position)
                .operation(exitOperation)
                .type(type)
                .timestamp(request.getExitDate().atStartOfDay())
                .sequenceNumber(1)
                .build();

        return positionOperationRepository.save(operation);
    }
}
