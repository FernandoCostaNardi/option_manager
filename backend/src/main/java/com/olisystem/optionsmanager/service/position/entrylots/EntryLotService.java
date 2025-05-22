package com.olisystem.optionsmanager.service.position.entrylots;

import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.repository.position.EntryLotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntryLotService {

    private final EntryLotRepository entryLotRepository;

    /**
     * Busca lotes de entrada disponíveis para a posição
     */
    public List<EntryLot> findAvailableLotsByPosition(Position position) {
        log.debug("Buscando lotes disponíveis para posição: {}", position.getId());

        List<EntryLot> availableLots = entryLotRepository
                .findByPositionOrderByEntryDateAsc(position).stream()
                .filter(lot -> lot.getRemainingQuantity() > 0)
                .collect(Collectors.toList());

        log.debug("Encontrados {} lotes disponíveis", availableLots.size());
        return availableLots;
    }
}
