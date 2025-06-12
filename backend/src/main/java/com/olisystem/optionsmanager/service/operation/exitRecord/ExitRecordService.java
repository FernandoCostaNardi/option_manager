package com.olisystem.optionsmanager.service.operation.exitRecord;

import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.ExitRecord;
import com.olisystem.optionsmanager.model.position.ExitStrategy;
import com.olisystem.optionsmanager.record.operation.OperationExitContext;
import com.olisystem.optionsmanager.repository.position.ExitRecordRepository;
import com.olisystem.optionsmanager.service.operation.profit.ProfitCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExitRecordService {

    private final ExitRecordRepository exitRecordRepository;
    private final ProfitCalculationService profitCalculationService;

    /**
     * Cria um registro de saída para a operação
     */
    @Transactional
    public ExitRecord createExitRecord(EntryLot lot, Operation exitOperation, OperationExitContext context) {
        return createExitRecord(lot, exitOperation, context, context.request().getQuantity());
    }
    @Transactional
    public ExitRecord createExitRecord(EntryLot lot, Operation exitOperation,
                                       OperationExitContext context, Integer quantityConsumed) {
        log.debug("Criando registro de saída para operação ID: {}, lote ID: {}, quantidade consumida: {}",
                exitOperation.getId(), lot.getId(), quantityConsumed);

        // CORREÇÃO: Validar quantidade consumida
        validateQuantityConsumed(lot, quantityConsumed);

        // ✅ CORREÇÃO SIMPLICADA: Por enquanto, sempre usar preço original do lote
        // TODO: Implementar lógica de preço break-even quando necessário
        BigDecimal entryPriceToUse = lot.getUnitPrice();

        // CORREÇÃO: Usar quantidade consumida nos cálculos
        BigDecimal lotTotalEntryValue = entryPriceToUse.multiply(BigDecimal.valueOf(quantityConsumed));
        BigDecimal lotTotalExitValue = context.request().getExitUnitPrice().multiply(BigDecimal.valueOf(quantityConsumed));

        // CORREÇÃO: Usar serviço de cálculo para consistência
        BigDecimal lotProfitLoss = profitCalculationService.calculateProfitLoss(
                entryPriceToUse,
                context.request().getExitUnitPrice(),
                quantityConsumed
        );

        BigDecimal lotProfitLossPercentage = profitCalculationService.calculateProfitLossPercentageFromPrices(
                entryPriceToUse,
                context.request().getExitUnitPrice()
        );

        // CORREÇÃO: Determinar estratégia dinamicamente
        ExitStrategy strategy = determineExitStrategy(lot, context);

        ExitRecord exitRecord = ExitRecord.builder()
                .entryLot(lot)
                .exitOperation(exitOperation)
                .exitDate(context.request().getExitDate())
                .quantity(quantityConsumed) // CORREÇÃO: Usar quantidade consumida
                .entryUnitPrice(entryPriceToUse) // ✅ SIMPLIFICADO: Usar preço original por enquanto
                .exitUnitPrice(context.request().getExitUnitPrice())
                .profitLoss(lotProfitLoss)
                .profitLossPercentage(lotProfitLossPercentage)
                .appliedStrategy(strategy) // CORREÇÃO: Estratégia dinâmica
                .build();

        ExitRecord savedRecord = exitRecordRepository.save(exitRecord);

        log.info("Registro de saída criado: ID={}, lucro/prejuízo={}, percentual={}%, estratégia={}",
                savedRecord.getId(), lotProfitLoss, lotProfitLossPercentage, strategy);

        return savedRecord;
    }

    /**
     * NOVO MÉTODO: Determina estratégia de saída baseada no contexto da operação
     */
    private ExitStrategy determineExitStrategy(EntryLot lot, OperationExitContext context) {
        // Se é do mesmo dia = Day Trade = LIFO
        if (lot.getEntryDate().equals(context.request().getExitDate())) {
            log.debug("Day Trade detectado - usando estratégia LIFO");
            return ExitStrategy.LIFO;
        }

        // Se é de dias diferentes = Swing Trade = FIFO
        log.debug("Swing Trade detectado - usando estratégia FIFO");
        return ExitStrategy.FIFO;
    }

    /**
     * NOVO MÉTODO: Validação de quantidade consumida
     */
    private void validateQuantityConsumed(EntryLot lot, Integer quantityConsumed) {
        if (quantityConsumed == null || quantityConsumed <= 0) {
            throw new IllegalArgumentException(
                    String.format("Quantidade consumida deve ser maior que zero. Valor recebido: %s", quantityConsumed)
            );
        }

        if (quantityConsumed > lot.getRemainingQuantity()) {
            throw new IllegalArgumentException(
                    String.format("Quantidade consumida (%d) não pode ser maior que quantidade disponível no lote (%d). Lote ID: %s",
                            quantityConsumed, lot.getRemainingQuantity(), lot.getId())
            );
        }
    }

    /**
     * NOVO MÉTODO: Cria múltiplos registros de saída (para cenários complexos futuros)
     */
    @Transactional
    public ExitRecord createPartialExitRecord(EntryLot lot, Operation exitOperation,
                                              OperationExitContext context, Integer quantityConsumed) {
        log.debug("Criando registro de saída PARCIAL para lote ID: {}, quantidade: {}",
                lot.getId(), quantityConsumed);

        return createExitRecord(lot, exitOperation, context, quantityConsumed);
    }

    /**
     * NOVO MÉTODO: Verificar se lote pode ser totalmente consumido
     */
    public boolean canConsumeEntireLot(EntryLot lot, Integer requestedQuantity) {
        return lot.getRemainingQuantity() <= requestedQuantity;
    }

    /**
     * NOVO MÉTODO: Calcular quantidade máxima que pode ser consumida do lote
     */
    public Integer calculateMaxConsumableQuantity(EntryLot lot, Integer requestedQuantity) {
        return Math.min(lot.getRemainingQuantity(), requestedQuantity);
    }

}
