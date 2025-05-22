package com.olisystem.optionsmanager.service.operation.status;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationStatusService {

    private final OperationRepository operationRepository;

    /**
     * Atualiza o status da operação ativa para oculta
     */
    @Transactional
    public void updateOperationStatus(Operation operation, OperationStatus status) {
        log.debug("Atualizando status da operação ID: {} para {}", operation.getId(), status);

        operation.setStatus(status);
        operationRepository.save(operation);
    }
}
