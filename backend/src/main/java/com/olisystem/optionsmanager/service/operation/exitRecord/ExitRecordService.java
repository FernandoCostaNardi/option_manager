package com.olisystem.optionsmanager.service.operation.exitRecord;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.ExitRecord;
import com.olisystem.optionsmanager.model.position.ExitStrategy;
import com.olisystem.optionsmanager.record.operation.OperationExitContext;
import com.olisystem.optionsmanager.repository.position.ExitRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExitRecordService {

    private final ExitRecordRepository exitRecordRepository;

    /**
     * Cria um registro de saída para a operação
     */
    @Transactional
    public ExitRecord createExitRecord(EntryLot lot, Operation exitOperation,
                                       OperationExitContext context,
                                       BigDecimal profitLoss, BigDecimal profitLossPercentage) {
        log.debug("Criando registro de saída para operação ID: {}", exitOperation.getId());

        ExitRecord exitRecord = ExitRecord.builder()
                .entryLot(lot)
                .exitOperation(exitOperation)
                .exitDate(context.request().getExitDate())
                .quantity(context.request().getQuantity())
                .entryUnitPrice(context.activeOperation().getEntryUnitPrice())
                .exitUnitPrice(context.request().getExitUnitPrice())
                .profitLoss(profitLoss)
                .profitLossPercentage(profitLossPercentage)
                .appliedStrategy(ExitStrategy.LIFO)
                .build();

        return exitRecordRepository.save(exitRecord);
    }
}
