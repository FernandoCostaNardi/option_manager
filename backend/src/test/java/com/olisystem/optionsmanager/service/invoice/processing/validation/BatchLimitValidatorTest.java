package com.olisystem.optionsmanager.service.invoice.processing.validation;

import com.olisystem.optionsmanager.model.auth.User;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.repository.InvoiceItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para BatchLimitValidator
 * Cada teste valida uma regra de negócio crítica para o processamento em lote.
 */
class BatchLimitValidatorTest {

    private InvoiceItemRepository invoiceItemRepository;
    private BatchLimitValidator validator;
    private User user;

    @BeforeEach
    void setUp() {
        invoiceItemRepository = mock(InvoiceItemRepository.class);
        validator = new BatchLimitValidator(invoiceItemRepository);
        user = new User();
        user.setId(java.util.UUID.randomUUID());
        user.setEmail("usuario_teste@teste.com");
    }

    @Test
    @DisplayName("Deve permitir processamento quando todos os limites são respeitados")
    void testBatchLimitsOk() {
        // Explicação: Garante que o processamento é permitido quando todos os limites são respeitados.
        List<Invoice> invoices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Invoice inv = new Invoice();
            inv.setId(java.util.UUID.randomUUID());
            inv.setInvoiceNumber("NOTA_" + i);
            invoices.add(inv);
        }
        
        List<InvoiceItem> mockItems = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            InvoiceItem item = new InvoiceItem();
            item.setTotalValue(java.math.BigDecimal.valueOf(1000));
            mockItems.add(item);
        }
        
        when(invoiceItemRepository.findByInvoiceIdWithAllRelations(any()))
            .thenReturn(mockItems);

        var result = validator.validateBatchLimits(invoices);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Deve rejeitar quando excede limite de invoices por lote")
    void testExceedInvoiceBatchLimit() {
        // Explicação: Garante que o sistema bloqueia lotes com mais de 10 invoices.
        List<Invoice> invoices = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            Invoice inv = new Invoice();
            inv.setId(java.util.UUID.randomUUID());
            inv.setInvoiceNumber("NOTA_" + i);
            invoices.add(inv);
        }
        
        List<InvoiceItem> mockItems = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            InvoiceItem item = new InvoiceItem();
            item.setTotalValue(java.math.BigDecimal.valueOf(1000));
            mockItems.add(item);
        }
        
        when(invoiceItemRepository.findByInvoiceIdWithAllRelations(any()))
            .thenReturn(mockItems);
            
        var result = validator.validateBatchLimits(invoices);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("excede limite de 10 invoices")));
    }

    @Test
    @DisplayName("Deve rejeitar quando invoice tem mais de 50 itens")
    void testExceedItemsPerInvoiceLimit() {
        // Explicação: Garante que invoices com mais de 50 itens são bloqueadas.
        Invoice inv = new Invoice();
        inv.setId(java.util.UUID.randomUUID());
        inv.setInvoiceNumber("NOTA_GRANDE");
        
        List<Invoice> invoices = List.of(inv);
        
        List<InvoiceItem> mockItems = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            InvoiceItem item = new InvoiceItem();
            item.setTotalValue(java.math.BigDecimal.valueOf(1000));
            mockItems.add(item);
        }
        
        when(invoiceItemRepository.findByInvoiceIdWithAllRelations(any()))
            .thenReturn(mockItems);
            
        var result = validator.validateBatchLimits(invoices);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("excede limite de 50 itens")));
    }

    @Test
    @DisplayName("Deve rejeitar quando lote excede valor total máximo")
    void testExceedTotalBatchValue() {
        // Explicação: Garante que o lote não pode exceder o valor total máximo.
        List<Invoice> invoices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Invoice inv = new Invoice();
            inv.setId(java.util.UUID.randomUUID());
            inv.setInvoiceNumber("NOTA_" + i);
            invoices.add(inv);
        }
        
        List<InvoiceItem> mockItems = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            InvoiceItem item = new InvoiceItem();
            item.setTotalValue(java.math.BigDecimal.valueOf(200000)); // Valor alto
            mockItems.add(item);
        }
        
        when(invoiceItemRepository.findByInvoiceIdWithAllRelations(any()))
            .thenReturn(mockItems);
            
        var result = validator.validateBatchLimits(invoices);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("Valor total do lote excede limite")));
    }

    // Outros testes podem ser adicionados para concorrência e estatísticas, se necessário.
} 