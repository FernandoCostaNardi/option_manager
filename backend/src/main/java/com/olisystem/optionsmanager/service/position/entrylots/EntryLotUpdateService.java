package com.olisystem.optionsmanager.service.position.entrylots;

import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.repository.position.EntryLotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntryLotUpdateService {

    private final EntryLotRepository entryLotRepository;

    /**
     * Atualiza o lote de entrada, reduzindo a quantidade disponível
     */
    @Transactional
    public void updateEntryLot(EntryLot lot, int quantityToReduce) {
        log.debug("Atualizando lote de entrada ID: {}", lot.getId());

        int newRemainingQuantity = lot.getRemainingQuantity() - quantityToReduce;
        lot.setRemainingQuantity(newRemainingQuantity);
        lot.setIsFullyConsumed(newRemainingQuantity == 0);

        entryLotRepository.save(lot);

        log.debug("Lote de entrada atualizado. Quantidade restante: {}", newRemainingQuantity);
    }
}
