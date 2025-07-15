package com.olisystem.optionsmanager.service.invoice.processing.detection;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.repository.InvoiceItemRepository;
import com.olisystem.optionsmanager.service.option_series.OptionSerieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para OperationDetectionEngine
 * Cada teste valida uma funcionalidade crítica do motor de detecção.
 */
class OperationDetectionEngineTest {

    private OperationDetectionEngine detectionEngine;
    private OperationPatternDetector patternDetector;
    private OperationTypeClassifier typeClassifier;
    private OperationConsolidator consolidator;
    private InvoiceItemRepository invoiceItemRepository;
    private User user;

    @BeforeEach
    void setUp() {
        patternDetector = mock(OperationPatternDetector.class);
        typeClassifier = mock(OperationTypeClassifier.class);
        consolidator = mock(OperationConsolidator.class);
        invoiceItemRepository = mock(InvoiceItemRepository.class);
        
        detectionEngine = new OperationDetectionEngine(
            invoiceItemRepository, patternDetector, typeClassifier, consolidator);
        
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("usuario_teste@teste.com");
    }

    @Test
    @DisplayName("Deve detectar operações com sucesso")
    void testDetectOperationsSuccess() {
        // Explicação: Garante que o engine consegue detectar operações em invoices válidas.
        
        // Arrange
        List<Invoice> invoices = createTestInvoices();
        List<DetectedOperation> detectedOps = createTestDetectedOperations();
        List<ClassifiedOperation> classifiedOps = createTestClassifiedOperations();
        List<ConsolidatedOperation> consolidatedOps = createTestConsolidatedOperations();
        
        when(patternDetector.detectPatterns(any(), any())).thenReturn(detectedOps);
        when(typeClassifier.classifyOperations(detectedOps)).thenReturn(classifiedOps);
        when(consolidator.consolidateOperations(classifiedOps)).thenReturn(consolidatedOps);
        
        // Act
        DetectionResult result = detectionEngine.detectOperations(invoices, user);
        
        // Assert
        assertTrue(result.isSuccessful());
        assertEquals(2, result.getTotalInvoices());
        assertEquals(4, result.getTotalItems());
        assertEquals(2, result.getTotalCandidateOperations());
        assertEquals(2, result.getTotalFinalOperations());
        assertTrue(result.getDetectionRate() > 0);
        assertTrue(result.getConsolidationRate() > 0);
        
        // Verify
        verify(patternDetector).detectPatterns(any(), eq(user));
        verify(typeClassifier).classifyOperations(detectedOps);
        verify(consolidator).consolidateOperations(classifiedOps);
    }

    @Test
    @DisplayName("Deve lidar com lista vazia de invoices")
    void testDetectOperationsEmptyList() {
        // Explicação: Garante que o engine funciona corretamente com lista vazia.
        
        // Arrange
        List<Invoice> invoices = new ArrayList<>();
        when(patternDetector.detectPatterns(any(), any())).thenReturn(new ArrayList<>());
        when(typeClassifier.classifyOperations(any())).thenReturn(new ArrayList<>());
        when(consolidator.consolidateOperations(any())).thenReturn(new ArrayList<>());
        
        // Act
        DetectionResult result = detectionEngine.detectOperations(invoices, user);
        
        // Assert
        assertTrue(result.isSuccessful());
        assertEquals(0, result.getTotalInvoices());
        assertEquals(0, result.getTotalItems());
        assertEquals(0, result.getTotalCandidateOperations());
        assertEquals(0, result.getTotalFinalOperations());
    }

    @Test
    @DisplayName("Deve lidar com erro durante detecção")
    void testDetectOperationsError() {
        // Explicação: Garante que o engine trata erros graciosamente.
        
        // Arrange
        List<Invoice> invoices = createTestInvoices();
        when(patternDetector.detectPatterns(any(), any()))
            .thenThrow(new RuntimeException("Erro de teste"));
        
        // Act
        DetectionResult result = detectionEngine.detectOperations(invoices, user);
        
        // Assert
        assertFalse(result.isSuccessful());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Erro na detecção"));
    }

    @Test
    @DisplayName("Deve validar item que pode gerar operação")
    void testCanGenerateOperation() {
        // Explicação: Garante que a validação de itens funciona corretamente.
        
        // Arrange
        InvoiceItem validItem = createTestInvoiceItem("PETR4OPCAO", "C", 100, BigDecimal.valueOf(2.50));
        InvoiceItem invalidItem = createTestInvoiceItem("PETR4", "C", 100, BigDecimal.valueOf(50.00));
        
        // Act & Assert
        assertTrue(detectionEngine.canGenerateOperation(validItem));
        assertFalse(detectionEngine.canGenerateOperation(invalidItem));
        assertFalse(detectionEngine.canGenerateOperation(null));
    }

    @Test
    @DisplayName("Deve detectar operações para invoice única")
    void testDetectOperationsForInvoice() {
        // Explicação: Garante que o engine funciona com invoice única.
        
        // Arrange
        Invoice invoice = createTestInvoice();
        List<DetectedOperation> detectedOps = createTestDetectedOperations();
        List<ClassifiedOperation> classifiedOps = createTestClassifiedOperations();
        List<ConsolidatedOperation> consolidatedOps = createTestConsolidatedOperations();
        
        when(patternDetector.detectPatterns(any(), any())).thenReturn(detectedOps);
        when(typeClassifier.classifyOperations(detectedOps)).thenReturn(classifiedOps);
        when(consolidator.consolidateOperations(classifiedOps)).thenReturn(consolidatedOps);
        
        // Act
        DetectionResult result = detectionEngine.detectOperationsForInvoice(invoice, user);
        
        // Assert
        assertTrue(result.isSuccessful());
        assertEquals(1, result.getTotalInvoices());
        assertEquals(2, result.getTotalItems());
    }

    // === MÉTODOS AUXILIARES ===

    private List<Invoice> createTestInvoices() {
        List<Invoice> invoices = new ArrayList<>();
        
        Invoice invoice1 = createTestInvoice();
        Invoice invoice2 = createTestInvoice();
        invoice2.setId(UUID.randomUUID());
        invoice2.setInvoiceNumber("NOTA002");
        
        invoices.add(invoice1);
        invoices.add(invoice2);
        
        return invoices;
    }

    private Invoice createTestInvoice() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("NOTA001");
        invoice.setTradingDate(LocalDate.now());
        
        List<InvoiceItem> items = new ArrayList<>();
        items.add(createTestInvoiceItem("PETR4OPCAO", "C", 100, BigDecimal.valueOf(2.50)));
        items.add(createTestInvoiceItem("VALE3OPCAO", "V", 50, BigDecimal.valueOf(1.80)));
        
        invoice.setItems(items);
        return invoice;
    }

    private InvoiceItem createTestInvoiceItem(String assetCode, String operationType, 
                                           Integer quantity, BigDecimal unitPrice) {
        InvoiceItem item = new InvoiceItem();
        item.setId(UUID.randomUUID());
        item.setAssetCode(assetCode);
        item.setOperationType(operationType);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setTotalValue(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        item.setIsDayTrade(true);
        item.setObservations("Day Trade");
        
        return item;
    }

    private List<DetectedOperation> createTestDetectedOperations() {
        List<DetectedOperation> operations = new ArrayList<>();
        
        DetectedOperation op1 = DetectedOperation.builder()
            .id(UUID.randomUUID())
            .detectionId("DET_001")
            .assetCode("PETR4OPCAO")
            .transactionType(TransactionType.BUY)
            .quantity(100)
            .unitPrice(BigDecimal.valueOf(2.50))
            .totalValue(BigDecimal.valueOf(250.00))
            .confidenceScore(0.9)
            .build();
        
        DetectedOperation op2 = DetectedOperation.builder()
            .id(UUID.randomUUID())
            .detectionId("DET_002")
            .assetCode("VALE3OPCAO")
            .transactionType(TransactionType.SELL)
            .quantity(50)
            .unitPrice(BigDecimal.valueOf(1.80))
            .totalValue(BigDecimal.valueOf(90.00))
            .confidenceScore(0.8)
            .build();
        
        operations.add(op1);
        operations.add(op2);
        
        return operations;
    }

    private List<ClassifiedOperation> createTestClassifiedOperations() {
        List<ClassifiedOperation> operations = new ArrayList<>();
        
        ClassifiedOperation op1 = ClassifiedOperation.builder()
            .id(UUID.randomUUID())
            .detectionId("DET_001")
            .assetCode("PETR4OPCAO")
            .transactionType(TransactionType.BUY)
            .quantity(100)
            .unitPrice(BigDecimal.valueOf(2.50))
            .totalValue(BigDecimal.valueOf(250.00))
            .classificationConfidence(0.9)
            .build();
        
        ClassifiedOperation op2 = ClassifiedOperation.builder()
            .id(UUID.randomUUID())
            .detectionId("DET_002")
            .assetCode("VALE3OPCAO")
            .transactionType(TransactionType.SELL)
            .quantity(50)
            .unitPrice(BigDecimal.valueOf(1.80))
            .totalValue(BigDecimal.valueOf(90.00))
            .classificationConfidence(0.8)
            .build();
        
        operations.add(op1);
        operations.add(op2);
        
        return operations;
    }

    private List<ConsolidatedOperation> createTestConsolidatedOperations() {
        List<ConsolidatedOperation> operations = new ArrayList<>();
        
        ConsolidatedOperation op1 = ConsolidatedOperation.builder()
            .id(UUID.randomUUID())
            .consolidationId("CONS_001")
            .assetCode("PETR4OPCAO")
            .transactionType(TransactionType.BUY)
            .quantity(100)
            .unitPrice(BigDecimal.valueOf(2.50))
            .totalValue(BigDecimal.valueOf(250.00))
            .consolidationConfidence(0.9)
            .isReadyForCreation(true)
            .build();
        
        ConsolidatedOperation op2 = ConsolidatedOperation.builder()
            .id(UUID.randomUUID())
            .consolidationId("CONS_002")
            .assetCode("VALE3OPCAO")
            .transactionType(TransactionType.SELL)
            .quantity(50)
            .unitPrice(BigDecimal.valueOf(1.80))
            .totalValue(BigDecimal.valueOf(90.00))
            .consolidationConfidence(0.8)
            .isReadyForCreation(true)
            .build();
        
        operations.add(op1);
        operations.add(op2);
        
        return operations;
    }
} 