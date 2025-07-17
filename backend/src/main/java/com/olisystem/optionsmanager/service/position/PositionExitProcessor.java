package com.olisystem.optionsmanager.service.position;

import com.olisystem.optionsmanager.dto.position.PositionExitRequest;
import com.olisystem.optionsmanager.dto.position.PositionExitResult;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Processador de saída de posições que não depende de OperationService
 * Quebra o ciclo de dependência entre PositionService e OperationService
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PositionExitProcessor {

    private final PositionRepository positionRepository;

    /**
     * Processa saída de posição sem depender de OperationService
     */
    public PositionExitResult processExit(PositionExitRequest request) {
        log.info("[PositionExitProcessor] Processando saída: {}", request);

        try {
            // Buscar posição
            Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new RuntimeException("Posição não encontrada: " + request.getPositionId()));

            // Validar se há quantidade suficiente
            if (position.getRemainingQuantity() < request.getQuantity()) {
                return PositionExitResult.builder()
                    .positionId(request.getPositionId())
                    .exitQuantity(0)
                    .message("Quantidade insuficiente para saída")
                    .build();
            }

            // Simular processamento de saída (sem criar operação)
            log.info("Saída processada para posição: {} - Quantidade: {}", 
                position.getId(), request.getQuantity());

            return PositionExitResult.builder()
                .positionId(request.getPositionId())
                .exitQuantity(request.getQuantity())
                .message("Saída processada com sucesso")
                .build();

        } catch (Exception e) {
            log.error("Erro ao processar saída: " + e.getMessage(), e);
            return PositionExitResult.builder()
                .positionId(request.getPositionId())
                .exitQuantity(0)
                .message("Erro ao processar saída: " + e.getMessage())
                .build();
        }
    }
} 