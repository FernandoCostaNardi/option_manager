package com.olisystem.optionsmanager.service.position.finder;

import com.olisystem.optionsmanager.exception.ResourceNotFoundException;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.repository.position.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionFinder {

    private final PositionRepository positionRepository;

    /**
     * Localiza a posição pelo ID
     */
    public Position findPositionById(UUID positionId) {
        log.debug("Buscando posição ID: {}", positionId);

        return positionRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Posição não encontrada: " + positionId));
    }
}
