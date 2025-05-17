package com.olisystem.optionsmanager.service.operation.consolidate;

import com.olisystem.optionsmanager.model.operation.*;
import com.olisystem.optionsmanager.model.position.Position;

import com.olisystem.optionsmanager.repository.OperationRepository;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Serviço especializado no gerenciamento de grupos de operações médias. Responsável por criar e
 * atualizar os grupos que representam operações parciais e seus resultados.
 */
@Service
@Transactional
@Slf4j
public class OperationConsolidateServiceImpl implements OperationConsolidateService {

    @Autowired
    private OperationRepository operationRepository;
   
    @Override
    public Operation consolidateOperationEntryValues(Operation consolidatedEntry, Position position) {
        
        consolidatedEntry.setQuantity(position.getRemainingQuantity());
        consolidatedEntry.setEntryUnitPrice(position.getAveragePrice());
        consolidatedEntry.setEntryTotalValue(position.getAveragePrice().multiply(new BigDecimal(position.getRemainingQuantity())));

        return operationRepository.save(consolidatedEntry);
    }
}
