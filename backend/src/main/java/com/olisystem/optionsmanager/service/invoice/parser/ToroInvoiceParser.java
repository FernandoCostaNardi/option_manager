package com.olisystem.optionsmanager.service.invoice.parser;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser específico para notas de corretagem da Toro
 */
@Slf4j
@Component
public class ToroInvoiceParser extends AbstractInvoiceParser {
    
    // Padrões específicos do Toro
    private static final Pattern CLIENTE_PATTERN = Pattern.compile("FERNANDO COSTA NARDI\\s+.*?Conta:\\s*(\\d+)");
    private static final Pattern DATA_PATTERN = Pattern.compile("Data de referência\\s+Comprovante\\s+(\\d{2}/\\d{2}/\\d{4})\\s+(\\d+)");
    private static final Pattern OPERACAO_PATTERN = Pattern.compile(
        "(COMPRA|VENDA)\\s+(BOVESPA \\d+)\\s+(\\d+)\\s+R\\$([\\d,]+)\\s+R\\$([\\d,]+)\\s+(VISTA)"
    );
    
    // Valores financeiros
    private static final Pattern VALOR_OPERACOES_PATTERN = Pattern.compile("Valor das Operações\\s+-?R\\$([\\d,]+\\.\\d{2})");
    private static final Pattern LIQUIDACAO_PATTERN = Pattern.compile("Taxa de Liquidação\\s+-?R\\$([\\d,]+\\.\\d{2})");
    private static final Pattern EMOLUMENTOS_PATTERN = Pattern.compile("Emolumentos\\s+-?R\\$([\\d,]+\\.\\d{2})");
    private static final Pattern LIQUIDO_PATTERN = Pattern.compile("LÍQUIDO DA NOTA\\s+-?R\\$([\\d,]+\\.\\d{2})");
    
    @Override
    public boolean canParse(String pdfText, String filename) {
        return pdfText.contains("FERNANDO COSTA NARDI") &&
               pdfText.contains("Conta: 743507") &&
               pdfText.contains("COMPROVANTE BOVESPA AÇÕES");
    }
    
    @Override
    public Invoice parseInvoice(String pdfText, MultipartFile originalFile) throws IOException {
        log.info("=== INICIANDO PARSING TORO ===");
        
        Invoice invoice = Invoice.builder().build();
        
        try {
            // Dados básicos
            extractBasicInfo(pdfText, invoice);
            
            // Operações
            List<InvoiceItem> items = extractOperations(pdfText, invoice);
            invoice.setItems(items);
            
            // Dados financeiros
            extractFinancialData(pdfText, invoice);
            
            // Metadados
            invoice.setRawContent(pdfText);
            
            log.info("=== PARSING TORO CONCLUÍDO ===");
            log.info("Invoice: {} | Itens: {} | Valor: {}", 
                    invoice.getInvoiceNumber(), 
                    items.size(), 
                    invoice.getNetOperationsValue());
            
            return invoice;
            
        } catch (Exception e) {
            log.error("Erro no parsing Toro: {}", e.getMessage(), e);
            throw new IOException("Erro ao processar nota Toro: " + e.getMessage(), e);
        }
    }
    
    private void extractBasicInfo(String text, Invoice invoice) {
        logParsingInfo("Dados básicos", "iniciando");
        
        // Cliente e conta
        Matcher clienteMatcher = CLIENTE_PATTERN.matcher(text);
        if (clienteMatcher.find()) {
            invoice.setClientName("FERNANDO COSTA NARDI");
            invoice.setClientCode(clienteMatcher.group(1));
            logParsingInfo("Cliente", invoice.getClientName());
            logParsingInfo("Conta", invoice.getClientCode());
        }
        
        // Data e número da nota
        Matcher dataMatcher = DATA_PATTERN.matcher(text);
        if (dataMatcher.find()) {
            String dateStr = dataMatcher.group(1);
            String numeroNota = dataMatcher.group(2);
            
            invoice.setTradingDate(parseDate(dateStr));
            invoice.setInvoiceNumber(numeroNota);
            
            logParsingInfo("Data pregão", dateStr);
            logParsingInfo("Número nota", numeroNota);
        }
        
        // CPF (fixo para Toro do Fernando)
        invoice.setCpfCnpj("292.983.738-13");
    }
    
    private List<InvoiceItem> extractOperations(String text, Invoice invoice) {
        logParsingInfo("Operações", "iniciando extração");
        
        List<InvoiceItem> items = new ArrayList<>();
        
        // Buscar seção de detalhes
        String detailsSection = extractBetween(text, "Detalhes:", "OBS [LEGENDA]");
        if (detailsSection.isEmpty()) {
            detailsSection = extractBetween(text, "Detalhes:", "Especificações diversas");
        }
        
        if (detailsSection.isEmpty()) {
            log.warn("Seção de detalhes não encontrada");
            return items;
        }
        
        // Extrair informações da linha de resumo
        // Exemplo: "VGIA11 - FIAGRO VGIA CI"
        // "COMPRA BOVESPA 1 29 R$9,04 R$262,16 VISTA"
        
        Pattern assetPattern = Pattern.compile("([A-Z0-9]+)\\s+-\\s+([^\\n]+)");
        Pattern operationPattern = Pattern.compile(
            "(COMPRA|VENDA)\\s+(BOVESPA \\d+)\\s+(\\d+)\\s+R\\$([\\d,]+)\\s+R\\$([\\d,]+)\\s+(VISTA)"
        );
        
        Matcher assetMatcher = assetPattern.matcher(detailsSection);
        Matcher opMatcher = operationPattern.matcher(detailsSection);
        
        if (assetMatcher.find() && opMatcher.find()) {
            InvoiceItem item = InvoiceItem.builder().build();
            
            // Dados do ativo
            String assetCode = assetMatcher.group(1);
            String assetName = assetMatcher.group(2);
            
            // Dados da operação
            String operationType = opMatcher.group(1);
            String quantity = opMatcher.group(3);
            String unitPrice = opMatcher.group(4);
            String totalValue = opMatcher.group(5);
            String marketType = opMatcher.group(6);
            
            // Preencher item
            item.setAssetCode(assetCode);
            item.setAssetSpecification(assetCode + " - " + assetName);
            item.setOperationType(operationType.equals("COMPRA") ? "C" : "V");
            item.setMarketType(marketType);
            item.setQuantity(parseInteger(quantity));
            item.setUnitPrice(parseMoney(unitPrice));
            item.setTotalValue(parseMoney(totalValue));
            item.setSequenceNumber(1);
            
            setupInvoiceItem(item, invoice, 1);
            items.add(item);
            
            logParsingInfo("Operação encontrada", 
                String.format("%s %s %s x R$%s = R$%s", 
                    operationType, assetCode, quantity, unitPrice, totalValue));
        } else {
            log.warn("Padrão de operação não encontrado na seção: {}", detailsSection.substring(0, Math.min(200, detailsSection.length())));
        }
        
        logParsingInfo("Total operações", items.size());
        return items;
    }
    
    private void extractFinancialData(String text, Invoice invoice) {
        logParsingInfo("Dados financeiros", "iniciando");
        
        // Valor das operações
        String valorOp = extractWithRegex(text, "Valor das Operações\\s+-?R\\$([\\d,]+\\.\\d{2})");
        if (!valorOp.isEmpty()) {
            invoice.setGrossOperationsValue(parseMoney(valorOp));
            logParsingInfo("Valor operações", valorOp);
        }
        
        // Taxa de liquidação
        String taxaLiq = extractWithRegex(text, "Taxa de Liquidação\\s+-?R\\$([\\d,]+\\.\\d{2})");
        if (!taxaLiq.isEmpty()) {
            invoice.setLiquidationTax(parseMoney(taxaLiq));
            logParsingInfo("Taxa liquidação", taxaLiq);
        }
        
        // Emolumentos
        String emol = extractWithRegex(text, "Emolumentos\\s+-?R\\$([\\d,]+\\.\\d{2})");
        if (!emol.isEmpty()) {
            invoice.setEmoluments(parseMoney(emol));
            logParsingInfo("Emolumentos", emol);
        }
        
        // Líquido da nota
        String liquido = extractWithRegex(text, "LÍQUIDO DA NOTA\\s+-?R\\$([\\d,]+\\.\\d{2})");
        if (!liquido.isEmpty()) {
            invoice.setNetSettlementValue(parseMoney(liquido));
            logParsingInfo("Líquido nota", liquido);
        }
        
        // Data de liquidação
        String dataLiq = extractWithRegex(text, "LÍQUIDAÇÃO\\s+(\\d{2}/\\d{2}/\\d{4})");
        if (!dataLiq.isEmpty()) {
            invoice.setSettlementDate(parseDate(dataLiq));
            logParsingInfo("Data liquidação", dataLiq);
        }
        
        // Calcular totais
        calculateTotals(invoice);
    }
    
    private void calculateTotals(Invoice invoice) {
        if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
            BigDecimal totalOperations = invoice.getItems().stream()
                    .map(item -> item.getTotalValue() != null ? item.getTotalValue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (invoice.getGrossOperationsValue() == null) {
                invoice.setGrossOperationsValue(totalOperations);
            }
        }
        
        // Calcular custos totais
        BigDecimal totalCosts = BigDecimal.ZERO;
        if (invoice.getLiquidationTax() != null) totalCosts = totalCosts.add(invoice.getLiquidationTax());
        if (invoice.getEmoluments() != null) totalCosts = totalCosts.add(invoice.getEmoluments());
        
        invoice.setTotalCosts(totalCosts);
        
        // Valor líquido
        if (invoice.getNetSettlementValue() == null && invoice.getGrossOperationsValue() != null) {
            invoice.setNetOperationsValue(invoice.getGrossOperationsValue().subtract(totalCosts));
        }
    }
    
    @Override
    public String getBrokerageName() {
        return "TORO";
    }
    
    @Override
    public int getPriority() {
        return 100; // Alta especificidade para Toro
    }
}
