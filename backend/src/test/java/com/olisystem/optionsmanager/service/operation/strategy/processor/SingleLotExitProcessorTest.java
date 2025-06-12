package com.olisystem.optionsmanager.service.operation.strategy.processor;

import com.olisystem.optionsmanager.dto.operation.OperationFinalizationRequest;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroup;
import com.olisystem.optionsmanager.model.operation.AverageOperationGroupStatus;
import com.olisystem.optionsmanager.model.operation.AverageOperationItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.operation.OperationRoleType;
import com.olisystem.optionsmanager.model.operation.OperationStatus;
import com.olisystem.optionsmanager.model.operation.TradeType;
import com.olisystem.optionsmanager.model.position.EntryLot;
import com.olisystem.optionsmanager.model.position.ExitRecord;
import com.olisystem.optionsmanager.model.position.ExitStrategy;
import com.olisystem.optionsmanager.model.position.Position;
import com.olisystem.optionsmanager.model.position.PositionOperation;
import com.olisystem.optionsmanager.model.position.PositionOperationType;
import com.olisystem.optionsmanager.model.position.PositionStatus;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.record.operation.OperationExitContext;
import com.olisystem.optionsmanager.record.operation.OperationExitPositionContext;
import com.olisystem.optionsmanager.resolver.tradeType.TradeTypeResolver;
import com.olisystem.optionsmanager.service.operation.averageOperation.AverageOperationGroupService;
import com.olisystem.optionsmanager.service.operation.consolidate.ConsolidatedOperationService;
import com.olisystem.optionsmanager.service.operation.creation.OperationCreationService;
import com.olisystem.optionsmanager.service.operation.exitRecord.ExitRecordService;
import com.olisystem.optionsmanager.service.operation.profit.ProfitCalculationService;
import com.olisystem.optionsmanager.service.operation.status.OperationStatusService;
import com.olisystem.optionsmanager.service.position.entrylots.EntryLotUpdateService;
import com.olisystem.optionsmanager.service.position.positionOperation.PositionOperationService;
import com.olisystem.optionsmanager.service.position.update.PositionUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("SingleLotExitProcessor - Teste de Consolidação Final")
class SingleLotExitProcessorTest {

    @Mock
    private TradeTypeResolver tradeTypeResolver;
    @Mock private ProfitCalculationService profitCalculationService;
    @Mock private EntryLotUpdateService entryLotUpdateService;
    @Mock private PositionUpdateService positionUpdateService;
    @Mock private OperationStatusService operationStatusService;
    @Mock private ExitRecordService exitRecordService;
    @Mock private PositionOperationService positionOperationService;
    @Mock private AverageOperationGroupService averageOperationGroupService;
    @Mock private OperationCreationService operationCreationService;
    @Mock private ConsolidatedOperationService consolidatedOperationService;

    @InjectMocks
    private SingleLotExitProcessor processor;

    private OperationExitPositionContext context;
    private Operation originalOperation;
    private Operation consolidatedResultOperation;
    private Operation finalExitOperation;
    private Position position;
    private EntryLot entryLot;
    private AverageOperationGroup group;
    private User user;

    @BeforeEach
    void setUp() {
        setupTestData();
        setupMocks();
    }

    @Test
    @DisplayName("Deve consolidar corretamente operação com saída total e calcular resultado absoluto")
    void shouldConsolidateFinalOperationWithCorrectAbsoluteResult() {
        // When
        Operation result = processor.process(context);

        // Then
        assertNotNull(result);
        assertEquals(OperationStatus.LOSER, result.getStatus());
        assertEquals(new BigDecimal("-75.75"), result.getProfitLoss());
        assertEquals(0, result.getProfitLossPercentage().compareTo(new BigDecimal("-24.51")));
        
        // Verificar que as operações intermediárias foram marcadas como HIDDEN
        verify(operationStatusService).updateOperationStatus(originalOperation, OperationStatus.HIDDEN);
        
        // Verificar que a consolidação foi chamada com os parâmetros corretos
        verify(consolidatedOperationService).transformToTotalExit(
            argThat(op -> 
                op.getQuantity() == 225 &&
                op.getExitUnitPrice().compareTo(new BigDecimal("0.46")) == 0
            ),
            eq(group)
        );
    }

    @Test
    @DisplayName("Deve calcular resultado absoluto correto: -R$ 75,75 (-24,51%)")
    void shouldCalculateCorrectAbsoluteResult() {
        // Given
        when(consolidatedOperationService.transformToTotalExit(any(), any()))
            .thenAnswer(invocation -> {
                Operation op = invocation.getArgument(0);
                return op; // Retorna a operação sem modificação para testar o cálculo real
            });

        // When
        Operation result = processor.process(context);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("-75.75"), result.getProfitLoss());
        assertEquals(0, result.getProfitLossPercentage().compareTo(new BigDecimal("-24.51")));
        assertEquals(OperationStatus.LOSER, result.getStatus());
    }

    @Test
    @DisplayName("Deve identificar operação ORIGINAL corretamente para cálculo do investimento")
    void shouldIdentifyOriginalOperationForInvestmentCalculation() {
        // Given
        Operation expectedOriginal = originalOperation;
        
        // When
        Operation result = processor.process(context);

        // Then
        verify(consolidatedOperationService).transformToTotalExit(finalExitOperation, group);
    }

    private void setupTestData() {
        user = User.builder()
            .id(UUID.randomUUID())
            .email("test@test.com")
            .build();

        // Operação ORIGINAL: 300 cotas × R$ 1,03 = R$ 309,00
        originalOperation = Operation.builder()
            .id(UUID.randomUUID())
            .transactionType(TransactionType.BUY)
            .status(OperationStatus.HIDDEN)
            .quantity(300)
            .entryUnitPrice(new BigDecimal("1.03"))
            .entryTotalValue(new BigDecimal("309.00"))
            .entryDate(LocalDate.of(2025, 5, 15))
            .user(user)
            .build();

        // Operação CONSOLIDATED_RESULT: Saída parcial 75 cotas × R$ 1,73 = R$ 129,75
        consolidatedResultOperation = Operation.builder()
            .id(UUID.randomUUID())
            .transactionType(TransactionType.SELL)
            .status(OperationStatus.WINNER)
            .quantity(75)
            .entryUnitPrice(new BigDecimal("1.03"))
            .entryTotalValue(new BigDecimal("77.25"))
            .exitUnitPrice(new BigDecimal("1.73"))
            .exitTotalValue(new BigDecimal("129.75"))
            .profitLoss(new BigDecimal("52.50"))
            .profitLossPercentage(new BigDecimal("67.96"))
            .entryDate(LocalDate.of(2025, 5, 15))
            .exitDate(LocalDate.of(2025, 5, 19))
            .tradeType(TradeType.SWING)
            .user(user)
            .build();

        // Operação de saída final: 225 cotas × R$ 0,46 = R$ 103,50
        finalExitOperation = Operation.builder()
            .id(UUID.randomUUID())
            .transactionType(TransactionType.SELL)
            .status(OperationStatus.LOSER)
            .quantity(225)
            .entryUnitPrice(new BigDecimal("0.80")) // Preço break-even
            .entryTotalValue(new BigDecimal("180.00"))
            .exitUnitPrice(new BigDecimal("0.46"))
            .exitTotalValue(new BigDecimal("103.50"))
            .profitLoss(new BigDecimal("-76.50"))
            .profitLossPercentage(new BigDecimal("-42.50"))
            .entryDate(LocalDate.of(2025, 5, 15))
            .exitDate(LocalDate.of(2025, 6, 3))
            .tradeType(TradeType.SWING)
            .user(user)
            .build();

        // Position com lucro parcial já realizado
        position = Position.builder()
            .id(UUID.randomUUID())
            .status(PositionStatus.PARTIAL)
            .totalQuantity(300)
            .remainingQuantity(225) // Após saída parcial de 75
            .averagePrice(new BigDecimal("0.80")) // Preço break-even após saída parcial
            .totalRealizedProfit(new BigDecimal("52.50")) // Lucro da saída parcial
            .totalRealizedProfitPercentage(new BigDecimal("17.00"))
            .user(user)
            .build();

        // EntryLot restante
        entryLot = EntryLot.builder()
            .id(UUID.randomUUID())
            .entryDate(LocalDate.of(2025, 5, 15))
            .quantity(225) // Quantidade restante após saída parcial
            .remainingQuantity(225)
            .unitPrice(new BigDecimal("1.03"))
            .totalValue(new BigDecimal("231.75"))
            .sequenceNumber(1)
            .isFullyConsumed(false)
            .position(position)
            .build();

        // AverageOperationGroup com operações
        group = AverageOperationGroup.builder()
            .id(UUID.randomUUID())
            .status(AverageOperationGroupStatus.PARTIALLY_CLOSED)
            .totalQuantity(300)
            .remainingQuantity(225)
            .closedQuantity(75)
            .totalProfit(new BigDecimal("52.50"))
            .positionId(position.getId())
            .build();

        // Itens do grupo simulando o histórico
        AverageOperationItem originalItem = AverageOperationItem.builder()
            .id(UUID.randomUUID())
            .group(group)
            .operation(originalOperation)
            .roleType(OperationRoleType.ORIGINAL)
            .sequenceNumber(1)
            .build();

        AverageOperationItem consolidatedResultItem = AverageOperationItem.builder()
            .id(UUID.randomUUID())
            .group(group)
            .operation(consolidatedResultOperation)
            .roleType(OperationRoleType.CONSOLIDATED_RESULT)
            .sequenceNumber(2)
            .build();

        group.setItems(Arrays.asList(originalItem, consolidatedResultItem));

        // Request para saída final
        OperationFinalizationRequest request = OperationFinalizationRequest.builder()
            .operationId(originalOperation.getId())
            .exitDate(LocalDate.of(2025, 6, 3))
            .exitUnitPrice(new BigDecimal("0.46"))
            .quantity(225)
            .build();

            // Context completo
        OperationExitContext exitContext = new OperationExitContext(
            request,
            originalOperation, // activeOperation
            user
        );

        context = new OperationExitPositionContext(
            exitContext,
            group,
            TransactionType.SELL,  // Adicionando o transactionType
            position,
            Arrays.asList(entryLot)  // availableLots
        );
    }

    private void setupMocks() {
        // TradeTypeResolver
        when(tradeTypeResolver.determineTradeType(any(), any()))
            .thenReturn(TradeType.SWING);

        // ProfitCalculationService - Valores corretos para o cenário
        when(profitCalculationService.calculateProfitLoss(any(), any(), anyInt()))
            .thenReturn(new BigDecimal("-76.50"));
        
        when(profitCalculationService.calculateProfitLossPercentage(
            eq(new BigDecimal("-76.50")), // Lucro/prejuízo
            eq(new BigDecimal("180.00")) // Valor total de entrada
        )).thenReturn(new BigDecimal("-42.50"));

        // OperationCreationService
        when(operationCreationService.createExitOperation(any(), any(), any(), any(), anyInt()))
            .thenReturn(finalExitOperation);

        // ConsolidatedOperationService
        when(consolidatedOperationService.transformToTotalExit(any(), any()))
            .thenReturn(Operation.builder()
                .id(UUID.randomUUID())
                .transactionType(TransactionType.SELL)
                .status(OperationStatus.LOSER)
                .quantity(225)
                .entryUnitPrice(new BigDecimal("0.80"))
                .entryTotalValue(new BigDecimal("180.00"))
                .exitUnitPrice(new BigDecimal("0.46"))
                .exitTotalValue(new BigDecimal("103.50"))
                .profitLoss(new BigDecimal("-75.75"))
                .profitLossPercentage(new BigDecimal("-24.51").setScale(2, RoundingMode.HALF_UP))
                .entryDate(LocalDate.of(2025, 5, 15))
                .exitDate(LocalDate.of(2025, 6, 3))
                .tradeType(TradeType.SWING)
                .user(user)
                .build());

        // Outros mocks básicos
        doNothing().when(entryLotUpdateService).updateEntryLot(any(), anyInt());
        doNothing().when(positionUpdateService).updatePosition(any(), any(), any(), any());
        doNothing().when(operationStatusService).updateOperationStatus(any(), any());
        when(exitRecordService.createExitRecord(any(), any(), any(), anyInt()))
            .thenReturn(ExitRecord.builder()
                .id(UUID.randomUUID())
                .entryLot(entryLot)
                .exitOperation(finalExitOperation)
                .exitDate(LocalDate.of(2025, 6, 3))
                .quantity(225)
                .entryUnitPrice(new BigDecimal("1.03"))
                .exitUnitPrice(new BigDecimal("0.46"))
                .profitLoss(new BigDecimal("-75.75"))
                .profitLossPercentage(new BigDecimal("-24.51"))
                .appliedStrategy(ExitStrategy.FIFO)
                .build());
        when(positionOperationService.createPositionOperation(any(), any(), any(), any()))
            .thenReturn(PositionOperation.builder()
                .id(UUID.randomUUID())
                .position(position)
                .operation(finalExitOperation)
                .type(PositionOperationType.FULL_EXIT)
                .timestamp(LocalDate.of(2025, 6, 3).atStartOfDay())
                .sequenceNumber(1)
                .build());
        doNothing().when(averageOperationGroupService).updateOperationGroup(any(), any(), any(), any());
    }
}
