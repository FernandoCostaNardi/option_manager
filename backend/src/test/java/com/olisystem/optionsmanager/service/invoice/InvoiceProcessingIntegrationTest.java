package com.olisystem.optionsmanager.service.invoice;

import com.olisystem.optionsmanager.dto.operation.OperationDataRequest;
import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.operation.Operation;
import com.olisystem.optionsmanager.model.transaction.TransactionType;
import com.olisystem.optionsmanager.service.invoice.processing.InvoiceToOperationMapper;
import com.olisystem.optionsmanager.service.invoice.processing.InvoiceConsolidationProcessor;
import com.olisystem.optionsmanager.service.operation.OperationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de integração para validar correções no processamento de invoice
 * 
 * @author Sistema de Gestão de Opções
 * @since 2025-07-14
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InvoiceProcessingIntegrationTest {

    @Autowired
    private InvoiceToOperationMapper invoiceToOperationMapper;

    @Autowired
    private OperationService operationService;

    @Autowired
    private InvoiceConsolidationProcessor consolidationProcessor;

    private User testUser;
    private Invoice testInvoice;
    private InvoiceItem testItem;

    @BeforeEach
    void setUp() {
        // Criar usuário de teste
        testUser = createTestUser();
        
        // Criar invoice de teste
        testInvoice = createTestInvoice();
        
        // Criar item de teste
        testItem = createTestInvoiceItem();
    }

    @Test
    @DisplayName("✅ Teste: TransactionType deve ser preservado durante mapeamento")
    void testTransactionTypePreservation() {
        // Arrange
        InvoiceItem buyItem = createTestInvoiceItem("C", "PETR4", 100, BigDecimal.valueOf(10.50));
        InvoiceItem sellItem = createTestInvoiceItem("V", "PETR4", 50, BigDecimal.valueOf(11.00));

        // Act & Assert - Compra
        OperationDataRequest buyRequest = invoiceToOperationMapper.mapToOperationRequest(buyItem);
        assertEquals(TransactionType.BUY, buyRequest.getTransactionType(), 
            "TransactionType deve ser BUY para operação de compra");

        // Act & Assert - Venda
        OperationDataRequest sellRequest = invoiceToOperationMapper.mapToOperationRequest(sellItem);
        assertEquals(TransactionType.SELL, sellRequest.getTransactionType(), 
            "TransactionType deve ser SELL para operação de venda");
    }

    @Test
    @DisplayName("✅ Teste: Operation criada deve preservar TransactionType")
    void testOperationCreationPreservesTransactionType() {
        // Arrange
        InvoiceItem buyItem = createTestInvoiceItem("C", "PETR4", 100, BigDecimal.valueOf(10.50));
        OperationDataRequest request = invoiceToOperationMapper.mapToOperationRequest(buyItem);

        // Act
        Operation operation = operationService.createOperation(request, testUser);

        // Assert
        assertNotNull(operation, "Operação deve ser criada");
        assertEquals(TransactionType.BUY, operation.getTransactionType(), 
            "TransactionType deve ser preservado na operação criada");
        assertEquals(request.getTransactionType(), operation.getTransactionType(), 
            "TransactionType da operação deve ser igual ao do request");
    }

    @Test
    @DisplayName("✅ Teste: Validações robustas de mapeamento")
    void testRobustMappingValidations() {
        // Arrange - Casos de teste
        List<TestCase> testCases = List.of(
            new TestCase("C", TransactionType.BUY, "Compra simples"),
            new TestCase("V", TransactionType.SELL, "Venda simples"),
            new TestCase("COMPRA", TransactionType.BUY, "Compra por extenso"),
            new TestCase("VENDA", TransactionType.SELL, "Venda por extenso"),
            new TestCase("BUY", TransactionType.BUY, "Compra em inglês"),
            new TestCase("SELL", TransactionType.SELL, "Venda em inglês"),
            new TestCase(null, TransactionType.BUY, "Valor nulo"),
            new TestCase("", TransactionType.BUY, "String vazia"),
            new TestCase("   ", TransactionType.BUY, "String com espaços"),
            new TestCase("INVALIDO", TransactionType.BUY, "Valor inválido")
        );

        // Act & Assert
        for (TestCase testCase : testCases) {
            InvoiceItem item = createTestInvoiceItem(testCase.operationType, "PETR4", 100, BigDecimal.valueOf(10.50));
            OperationDataRequest request = invoiceToOperationMapper.mapToOperationRequest(item);
            
            assertEquals(testCase.expectedType, request.getTransactionType(), 
                "Caso: " + testCase.description);
        }
    }

    @Test
    @DisplayName("✅ Teste: Sistema de consolidação integrado")
    void testConsolidationSystemIntegration() {
        // Arrange - Criar múltiplas invoices com itens similares
        List<Invoice> invoices = createTestInvoicesWithSimilarItems();
        List<UUID> invoiceIds = invoices.stream()
            .map(Invoice::getId)
            .collect(Collectors.toList());
        
        // Act
        InvoiceConsolidationProcessor.ConsolidationResult result = 
            consolidationProcessor.processInvoicesWithConsolidation(invoiceIds, testUser);

        // Assert
        assertTrue(result.isSuccess(), "Processamento deve ser bem-sucedido");
        assertTrue(result.getConsolidatedOperationsCount() > 0, 
            "Deve criar operações consolidadas");
        assertTrue(result.getErrors().isEmpty(), "Não deve ter erros");
    }

    @Test
    @DisplayName("✅ Teste: Validações de InvoiceItem")
    void testInvoiceItemValidations() {
        // Arrange - Casos inválidos
        List<InvalidTestCase> invalidCases = List.of(
            new InvalidTestCase(null, "InvoiceItem nulo"),
            new InvalidTestCase(createInvalidItem(null, "PETR4", 100, BigDecimal.valueOf(10.50)), "ID nulo"),
            new InvalidTestCase(createInvalidItem(UUID.randomUUID(), null, 100, BigDecimal.valueOf(10.50)), "AssetCode nulo"),
            new InvalidTestCase(createInvalidItem(UUID.randomUUID(), "", 100, BigDecimal.valueOf(10.50)), "AssetCode vazio"),
            new InvalidTestCase(createInvalidItem(UUID.randomUUID(), "PETR4", null, BigDecimal.valueOf(10.50)), "Quantidade nula"),
            new InvalidTestCase(createInvalidItem(UUID.randomUUID(), "PETR4", 0, BigDecimal.valueOf(10.50)), "Quantidade zero"),
            new InvalidTestCase(createInvalidItem(UUID.randomUUID(), "PETR4", 100, null), "Preço nulo"),
            new InvalidTestCase(createInvalidItem(UUID.randomUUID(), "PETR4", 100, BigDecimal.ZERO), "Preço zero")
        );

        // Act & Assert
        for (InvalidTestCase testCase : invalidCases) {
            assertThrows(IllegalArgumentException.class, () -> {
                invoiceToOperationMapper.mapToOperationRequest(testCase.item);
            }, "Caso: " + testCase.description);
        }
    }

    // ======================================================================================
    // MÉTODOS AUXILIARES
    // ======================================================================================

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        return user;
    }

    private Invoice createTestInvoice() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("TEST-001");
        invoice.setTradingDate(LocalDate.now());
        invoice.setUser(testUser);
        invoice.setItems(new ArrayList<>());
        return invoice;
    }

    private InvoiceItem createTestInvoiceItem() {
        return createTestInvoiceItem("C", "PETR4", 100, BigDecimal.valueOf(10.50));
    }

    private InvoiceItem createTestInvoiceItem(String operationType, String assetCode, 
                                           Integer quantity, BigDecimal unitPrice) {
        InvoiceItem item = new InvoiceItem();
        item.setId(UUID.randomUUID());
        item.setSequenceNumber(1);
        item.setOperationType(operationType);
        item.setAssetCode(assetCode);
        item.setAssetSpecification(assetCode + " - Petrobras PN");
        item.setMarketType("VISTA");
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setTotalValue(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        item.setInvoice(testInvoice);
        return item;
    }

    private InvoiceItem createInvalidItem(UUID id, String assetCode, Integer quantity, BigDecimal unitPrice) {
        InvoiceItem item = new InvoiceItem();
        item.setId(id);
        item.setSequenceNumber(1);
        item.setOperationType("C");
        item.setAssetCode(assetCode);
        item.setAssetSpecification("Test Asset");
        item.setMarketType("VISTA");
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setTotalValue(unitPrice != null && quantity != null ? 
            unitPrice.multiply(BigDecimal.valueOf(quantity)) : null);
        item.setInvoice(testInvoice);
        return item;
    }

    private List<Invoice> createTestInvoicesWithSimilarItems() {
        List<Invoice> invoices = new ArrayList<>();
        
        // Invoice 1
        Invoice invoice1 = createTestInvoice();
        invoice1.setInvoiceNumber("TEST-001");
        invoice1.getItems().add(createTestInvoiceItem("C", "PETR4", 100, BigDecimal.valueOf(10.50)));
        invoices.add(invoice1);
        
        // Invoice 2 (mesmo ativo, mesmo dia)
        Invoice invoice2 = createTestInvoice();
        invoice2.setInvoiceNumber("TEST-002");
        invoice2.getItems().add(createTestInvoiceItem("C", "PETR4", 50, BigDecimal.valueOf(10.75)));
        invoices.add(invoice2);
        
        return invoices;
    }

    // ======================================================================================
    // CLASSES AUXILIARES
    // ======================================================================================

    private static class TestCase {
        final String operationType;
        final TransactionType expectedType;
        final String description;

        TestCase(String operationType, TransactionType expectedType, String description) {
            this.operationType = operationType;
            this.expectedType = expectedType;
            this.description = description;
        }
    }

    private static class InvalidTestCase {
        final InvoiceItem item;
        final String description;

        InvalidTestCase(InvoiceItem item, String description) {
            this.item = item;
            this.description = description;
        }
    }
} 