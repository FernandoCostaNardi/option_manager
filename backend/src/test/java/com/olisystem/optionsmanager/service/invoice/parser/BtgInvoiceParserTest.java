package com.olisystem.optionsmanager.service.invoice.parser;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste do BtgInvoiceParser com dados reais da nota BTG
 */
public class BtgInvoiceParserTest {

    private BtgInvoiceParser parser;
    
    // Conteúdo simulado da nota BTG 72345997
    private String btgNoteContent;

    @BeforeEach
    void setUp() {
        parser = new BtgInvoiceParser();
        setupBtgNoteContent();
    }

    private void setupBtgNoteContent() {
        btgNoteContent = """
            BTG Pactual CTVM S.A.
            NOTA DE CORRETAGEM
            
            Nr. nota 72345997
            Data pregão 02/06/2025
            
            Cliente FERNANDO COSTA NARDI
            333.444.555-66
            Código cliente 12345-6 789
            
            Negócios realizados
            Q Negociação C/V Tipo mercado Vencimento Especificação do título Obs.(*) Quantidade Preço / Ajuste Valor Operação / Ajuste D/C
            
            1-BOVESPA C OPCAO DE VENDA 06/25 BOVAR136 D 500 2,70 1.350,00 D
            2-BOVESPA C OPCAO DE VENDA 06/25 BOVAR136 D 500 2,76 1.380,00 D
            3-BOVESPA C OPCAO DE VENDA 06/25 BOVAR136 D 500 2,90 1.450,00 D
            4-BOVESPA V OPCAO DE VENDA 06/25 BOVAR136 D 1000 2,76 2.760,00 C
            5-BOVESPA V OPCAO DE VENDA 06/25 BOVAR136 D 500 2,85 1.425,00 C
            
            Resumo dos Negócios
            Debêntures 0,00
            Vendas à vista 4.185,00
            Compras à vista 4.180,00
            Opções - compras 0,00
            Opções - vendas 0,00
            
            Valor líquido das operações 5,00 C
            
            Taxa de liquidação 1,50 D
            Taxa de Registro 1,17 D
            Emolumentos 1,08 D
            Taxa A.N.A. 0,00
            
            Day Trade: Base R$ 4.185,00
            IRRF Projeção R$ 0,00
            
            Líquido para 03/06/2025 1,25 C
            """;
    }

    @Test
    void testCanParseDetectsBtgNote() {
        assertTrue(parser.canParse(btgNoteContent, "nota_btg.pdf"));
        assertFalse(parser.canParse("Conteúdo de outra corretora", "nota_outra.pdf"));
    }

    @Test
    void testParseBasicInfo() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "nota_72345997.pdf", "application/pdf", "dummy content".getBytes()
        );
        
        Invoice invoice = parser.parseInvoice(btgNoteContent, mockFile);
        
        // Verificar dados básicos
        assertEquals("72345997", invoice.getInvoiceNumber());
        assertEquals(LocalDate.of(2025, 6, 2), invoice.getTradingDate());
        assertEquals("FERNANDO COSTA NARDI", invoice.getClientName());
        assertEquals("333.444.555-66", invoice.getCpfCnpj());
        assertEquals("12345-6 789", invoice.getClientCode());
    }

    @Test
    void testParseOperations() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "nota_72345997.pdf", "application/pdf", "dummy content".getBytes()
        );
        
        Invoice invoice = parser.parseInvoice(btgNoteContent, mockFile);
        List<InvoiceItem> items = invoice.getItems();
        
        // Verificar quantidade de operações
        assertEquals(5, items.size(), "Deveria ter extraído 5 operações");
        
        // Verificar primeira operação (Compra)
        InvoiceItem firstOp = items.get(0);
        assertEquals("C", firstOp.getOperationType());
        assertEquals("OPCAO DE VENDA", firstOp.getMarketType());
        assertEquals("BOVAR136", firstOp.getAssetCode());
        assertEquals(500, firstOp.getQuantity());
        assertEquals(new BigDecimal("2.70"), firstOp.getUnitPrice());
        assertEquals(new BigDecimal("1350.00"), firstOp.getTotalValue());
        assertTrue(firstOp.getIsDayTrade(), "Deveria detectar Day Trade");
        assertEquals(LocalDate.of(2025, 6, 30), firstOp.getExpirationDate());
        
        // Verificar quarta operação (Venda)
        InvoiceItem fourthOp = items.get(3);
        assertEquals("V", fourthOp.getOperationType());
        assertEquals(1000, fourthOp.getQuantity());
        assertEquals(new BigDecimal("2.76"), fourthOp.getUnitPrice());
        assertEquals(new BigDecimal("2760.00"), fourthOp.getTotalValue());
        assertTrue(fourthOp.getIsDayTrade());
    }

    @Test
    void testParseFinancialData() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "nota_72345997.pdf", "application/pdf", "dummy content".getBytes()
        );
        
        Invoice invoice = parser.parseInvoice(btgNoteContent, mockFile);
        
        // Verificar valores financeiros
        assertEquals(new BigDecimal("5.00"), invoice.getNetOperationsValue());
        assertEquals(new BigDecimal("-1.50"), invoice.getLiquidationTax());
        assertEquals(new BigDecimal("-1.17"), invoice.getRegistrationTax());
        assertEquals(new BigDecimal("-1.08"), invoice.getEmoluments());
        assertEquals(new BigDecimal("4185.00"), invoice.getIrrfDayTradeBasis());
        assertEquals(new BigDecimal("1.25"), invoice.getNetSettlementValue());
        
        // Verificar cálculos
        assertEquals(new BigDecimal("3.75"), invoice.getTotalCosts());
        assertNotNull(invoice.getSettlementDate());
    }

    @Test 
    void testNoFakeDataGenerated() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "nota_72345997.pdf", "application/pdf", "dummy content".getBytes()
        );
        
        Invoice invoice = parser.parseInvoice(btgNoteContent, mockFile);
        
        // Verificar que não há dados FAKE
        assertNotNull(invoice.getInvoiceNumber(), "Número da nota não deve ser null");
        assertNotEquals("", invoice.getInvoiceNumber(), "Número da nota não deve estar vazio");
        
        // Verificar que não há itens FAKE
        for (InvoiceItem item : invoice.getItems()) {
            assertNotEquals("EXEMPLO", item.getAssetCode(), "Não deve conter asset code EXEMPLO");
            assertNotEquals(100, item.getQuantity(), "Não deve conter quantidade fake 100");
            assertNotEquals(new BigDecimal("10.00"), item.getUnitPrice(), "Não deve conter preço fake 10.00");
        }
        
        // Verificar valores não zerados
        assertNotEquals(BigDecimal.ZERO, invoice.getNetOperationsValue(), "Valor líquido não deve ser zero");
        assertTrue(invoice.getTotalCosts().compareTo(BigDecimal.ZERO) > 0, "Custos devem ser maiores que zero");
    }

    @Test
    void testDayTradeDetection() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "nota_72345997.pdf", "application/pdf", "dummy content".getBytes()
        );
        
        Invoice invoice = parser.parseInvoice(btgNoteContent, mockFile);
        
        // Todas as operações desta nota são Day Trade (marcador "D")
        for (InvoiceItem item : invoice.getItems()) {
            assertTrue(item.getIsDayTrade(), 
                "Operação " + item.getSequenceNumber() + " deveria ser Day Trade");
            assertTrue(item.getObservations().contains("D"), 
                "Observações deveriam conter marcador D");
        }
    }

    @Test
    void testRawContentStoredCorrectly() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "nota_72345997.pdf", "application/pdf", "dummy content".getBytes()
        );
        
        Invoice invoice = parser.parseInvoice(btgNoteContent, mockFile);
        
        assertEquals(btgNoteContent, invoice.getRawContent());
        assertTrue(invoice.getRawContent().contains("Nr. nota 72345997"));
        assertTrue(invoice.getRawContent().contains("FERNANDO COSTA NARDI"));
    }
}