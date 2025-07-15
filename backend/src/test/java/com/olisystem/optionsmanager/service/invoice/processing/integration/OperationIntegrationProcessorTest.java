package com.olisystem.optionsmanager.service.invoice.processing.integration;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.service.invoice.processing.detection.ConsolidatedOperation;
import com.olisystem.optionsmanager.service.operation.OperationService;
import com.olisystem.optionsmanager.service.option_series.OptionSerieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para OperationIntegrationProcessor
 * Valida o funcionamento do sistema de integração de operações
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
class OperationIntegrationProcessorTest {

    private OperationIntegrationProcessor integrationProcessor;
    private OperationService operationService;
    private OptionSerieService optionSerieService;
    private OperationMappingService mappingService;
    private OperationValidationService validationService;
    private User user;

    @BeforeEach
    void setUp() {
        operationService = mock(OperationService.class);
        optionSerieService = mock(OptionSerieService.class);
        mappingService = mock(OperationMappingService.class);
        validationService = mock(OperationValidationService.class);
        
        integrationProcessor = new OperationIntegrationProcessor(
            operationService, optionSerieService, mappingService, validationService);
        
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("usuario_teste@teste.com");
    }

    @Test
    @DisplayName("Deve processar integração com sucesso")
    void testProcessIntegrationSuccess() {
        // Explicação: Garante que o processador consegue integrar operações válidas.
        
        // Arrange
        List<ConsolidatedOperation> consolidatedOps = createTestConsolidatedOperations();
        List<Invoice> sourceInvoices = createTestInvoices();
        
        when(validationService.validateOperation(any(), any()))
            .thenReturn(ValidationResult.builder().valid(true).build());
        when(mappingService.createMappings(any(), any(), any()))
            .thenReturn(new ArrayList<>());
        
        // Act
        IntegrationResult result = integrationProcessor.processIntegration(consolidatedOps, sourceInvoices, user);
        
        // Assert
        assertTrue(result.isSuccessful());
        assertEquals(2, result.getTotalOperations());
        assertEquals(2, result.getTotalSuccessfulOperations());
        assertEquals(0, result.getTotalFailedOperations());
        assertTrue(result.getSuccessRate() > 0);
        
        // Verify
        verify(validationService, times(2)).validateOperation(any(), eq(user));
        verify(mappingService, times(2)).createMappings(any(), any(), any());
    }

    @Test
    @DisplayName("Deve lidar com operações inválidas")
    void testProcessIntegrationWithInvalidOperations() {
        // Explicação: Garante que o processador trata operações inválidas corretamente.
        
        // Arrange
        List<ConsolidatedOperation> consolidatedOps = createTestConsolidatedOperations();
        List<Invoice> sourceInvoices = createTestInvoices();
        
        when(validationService.validateOperation(any(), any()))
            .thenReturn(ValidationResult.builder()
                .valid(false)
                .errorMessage("Operação inválida")
                .errorCount(1)
                .build());
        
        // Act
        IntegrationResult result = integrationProcessor.processIntegration(consolidatedOps, sourceInvoices, user);
        
        // Assert
        assertTrue(result.isSuccessful()); // Processo em si foi bem-sucedido
        assertEquals(2, result.getTotalOperations());
        assertEquals(0, result.getTotalSuccessfulOperations());
        assertEquals(2, result.getTotalFailedOperations());
        assertEquals(0.0, result.getSuccessRate());
    }

    @Test
    @DisplayName("Deve lidar com lista vazia de operações")
    void testProcessIntegrationEmptyList() {
        // Explicação: Garante que o processador funciona corretamente com lista vazia.
        
        // Arrange
        List<ConsolidatedOperation> consolidatedOps = new ArrayList<>();
        List<Invoice> sourceInvoices = createTestInvoices();
        
        // Act
        IntegrationResult result = integrationProcessor.processIntegration(consolidatedOps, sourceInvoices, user);
        
        // Assert
        assertTrue(result.isSuccessful());
        assertEquals(0, result.getTotalOperations());
        assertEquals(0, result.getTotalSuccessfulOperations());
        assertEquals(0, result.getTotalFailedOperations());
    }

    @Test
    @DisplayName("Deve lidar com erro durante processamento")
    void testProcessIntegrationError() {
        // Explicação: Garante que o processador trata erros graciosamente.
        
        // Arrange
        List<ConsolidatedOperation> consolidatedOps = createTestConsolidatedOperations();
        List<Invoice> sourceInvoices = createTestInvoices();
        
        when(validationService.validateOperation(any(), any()))
            .thenThrow(new RuntimeException("Erro de teste"));
        
        // Act
        IntegrationResult result = integrationProcessor.processIntegration(consolidatedOps, sourceInvoices, user);
        
        // Assert
        assertTrue(result.isSuccessful()); // Processo em si foi bem-sucedido
        assertEquals(2, result.getTotalOperations());
        assertEquals(0, result.getTotalSuccessfulOperations());
        assertEquals(2, result.getTotalFailedOperations());
    }

    @Test
    @DisplayName("Deve validar se operação pode ser integrada")
    void testCanIntegrateOperation() {
        // Explicação: Garante que a validação de integração funciona corretamente.
        
        // Arrange
        ConsolidatedOperation validOp = createTestConsolidatedOperation(true, 0.9);
        ConsolidatedOperation invalidOp = createTestConsolidatedOperation(false, 0.3);
        
        // Act & Assert
        assertTrue(integrationProcessor.canIntegrateOperation(validOp));
        assertFalse(integrationProcessor.canIntegrateOperation(invalidOp));
    }

    @Test
    @DisplayName("Deve processar mapeamentos corretamente")
    void testProcessMappings() {
        // Explicação: Garante que os mapeamentos são processados corretamente.
        
        // Arrange
        List<ConsolidatedOperation> consolidatedOps = createTestConsolidatedOperations();
        List<Invoice> sourceInvoices = createTestInvoices();
        List<InvoiceOperationMapping> testMappings = createTestMappings();
        
        when(validationService.validateOperation(any(), any()))
            .thenReturn(ValidationResult.builder().valid(true).build());
        when(mappingService.createMappings(any(), any(), any()))
            .thenReturn(testMappings);
        
        // Act
        IntegrationResult result = integrationProcessor.processIntegration(consolidatedOps, sourceInvoices, user);
        
        // Assert
        assertTrue(result.isSuccessful());
        assertTrue(result.getTotalMappings() > 0);
        
        // Verify
        verify(mappingService, times(2)).saveMappings(any());
    }

    @Test
    @DisplayName("Deve validar operações para integração")
    void testValidateOperationsForIntegration() {
        // Explicação: Garante que a validação em lote funciona corretamente.
        
        // Arrange
        List<ConsolidatedOperation> consolidatedOps = createTestConsolidatedOperations();
        
        when(validationService.validateOperations(any(), any()))
            .thenReturn(ValidationSummary.builder()
                .totalOperations(2)
                .successRate(100.0)
                .validOperations(consolidatedOps)
                .invalidOperations(new ArrayList<>())
                .build());
        
        // Act
        ValidationSummary summary = integrationProcessor.validateOperationsForIntegration(consolidatedOps, user);
        
        // Assert
        assertNotNull(summary);
        assertEquals(2, summary.getTotalOperations());
        assertEquals(100.0, summary.getSuccessRate());
        assertEquals(2, summary.getValidCount());
        assertEquals(0, summary.getInvalidCount());
        assertTrue(summary.isAllValid());
        
        // Verify
        verify(validationService, times(1)).validateOperations(any(), eq(user));
    }

    @Test
    @DisplayName("Deve calcular tempo de processamento")
    void testProcessingTimeCalculation() {
        // Explicação: Garante que o tempo de processamento é calculado corretamente.
        
        // Arrange
        List<ConsolidatedOperation> consolidatedOps = createTestConsolidatedOperations();
        List<Invoice> sourceInvoices = createTestInvoices();
        
        when(validationService.validateOperation(any(), any()))
            .thenReturn(ValidationResult.builder().valid(true).build());
        when(mappingService.createMappings(any(), any(), any()))
            .thenReturn(new ArrayList<>());
        
        // Act
        IntegrationResult result = integrationProcessor.processIntegration(consolidatedOps, sourceInvoices, user);
        
        // Assert
        assertTrue(result.getProcessingTimeMs() >= 0); // Pode ser 0 se muito rápido
        assertTrue(result.getProcessingTimeMs() < 10000); // Deve ser rápido
        
        System.out.println("⏱️ Tempo de processamento: " + result.getProcessingTimeMs() + "ms");
    }

    // === MÉTODOS AUXILIARES ===

    private List<ConsolidatedOperation> createTestConsolidatedOperations() {
        List<ConsolidatedOperation> operations = new ArrayList<>();
        
        ConsolidatedOperation op1 = createTestConsolidatedOperation(true, 0.9);
        ConsolidatedOperation op2 = createTestConsolidatedOperation(true, 0.8);
        
        operations.add(op1);
        operations.add(op2);
        
        return operations;
    }

    private ConsolidatedOperation createTestConsolidatedOperation(boolean readyForCreation, double confidence) {
        return ConsolidatedOperation.builder()
            .id(UUID.randomUUID())
            .consolidationId("CONS_001")
            .assetCode("PETR4OPCAO")
            .transactionType(TransactionType.BUY)
            .quantity(100)
            .unitPrice(BigDecimal.valueOf(2.50))
            .totalValue(BigDecimal.valueOf(250.00))
            .tradeDate(LocalDate.now())
            .consolidationConfidence(confidence)
            .isReadyForCreation(readyForCreation)
            .sourceOperations(new ArrayList<>())
            .build();
    }

    private List<Invoice> createTestInvoices() {
        List<Invoice> invoices = new ArrayList<>();
        
        Invoice invoice1 = new Invoice();
        invoice1.setId(UUID.randomUUID());
        invoice1.setInvoiceNumber("NOTA001");
        invoice1.setTradingDate(LocalDate.now());
        
        Invoice invoice2 = new Invoice();
        invoice2.setId(UUID.randomUUID());
        invoice2.setInvoiceNumber("NOTA002");
        invoice2.setTradingDate(LocalDate.now());
        
        invoices.add(invoice1);
        invoices.add(invoice2);
        
        return invoices;
    }

    private List<InvoiceOperationMapping> createTestMappings() {
        List<InvoiceOperationMapping> mappings = new ArrayList<>();
        
        InvoiceOperationMapping mapping1 = InvoiceOperationMapping.builder()
            .id(UUID.randomUUID())
            .mappingId("MAP_001")
            .mappingType("NEW_OPERATION")
            .notes("Teste de mapeamento")
            .build();
        
        InvoiceOperationMapping mapping2 = InvoiceOperationMapping.builder()
            .id(UUID.randomUUID())
            .mappingId("MAP_002")
            .mappingType("DAY_TRADE_ENTRY")
            .notes("Day Trade")
            .build();
        
        mappings.add(mapping1);
        mappings.add(mapping2);
        
        return mappings;
    }
} 