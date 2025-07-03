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
 * Parser específico para notas de corretagem da BTG Pactual
 */
@Slf4j
@Component
public class BtgInvoiceParser extends AbstractInvoiceParser {
    
    // 🔧 CORRIGIDOS: Padrões específicos do BTG Pactual
    private static final Pattern NUMERO_NOTA_PATTERN = Pattern.compile("Nr\\.\\s*nota.*?\\n.*?(\\d{6,})", Pattern.DOTALL);
    private static final Pattern DATA_PREGAO_PATTERN = Pattern.compile("Data pregão.*?\\n.*?(\\d{2}/\\d{2}/\\d{4})", Pattern.DOTALL);
    
    // 🔧 CORRIGIDO: Padrão para extrair cliente e CPF (formato real BTG)
    private static final Pattern CLIENTE_PATTERN = Pattern.compile("(\\d+)\\s+([A-ZÁÊÇÕÃÂÚ\\s]+)\\s+(\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})");
    
    // 🔧 CORRIGIDO: Padrão para código do cliente (formato real BTG)  
    private static final Pattern CODIGO_CLIENTE_PATTERN = Pattern.compile("Cliente.*?\\n.*?(\\d{6,})");
    
    // 🔧 CORRIGIDO: Padrão para operações na tabela (captura sufixos ON/PN e Day Trade flexível)
    private static final Pattern OPERACAO_PATTERN = Pattern.compile(
        "\\d+-BOVESPA\\s+(C|V)\\s+(OPCAO DE (?:COMPRA|VENDA))\\s+(\\d{2}/\\d{2})\\s+([A-Z0-9]+(?:\\s+(?:ON|PN))?)\\s*(D?)\\s+(\\d+)\\s+([\\d,\\.]+)\\s+([\\d,\\.]+)\\s+(D|C)"
    );
    
    // Valores financeiros (CORRIGIDO: adicionado \\. para valores como 1.350,00)
    private static final Pattern VALOR_LIQUIDO_PATTERN = Pattern.compile("Valor líquido das operações\\s+([\\d,\\.]+)\\s+(C|D)");
    private static final Pattern TAXA_LIQUIDACAO_PATTERN = Pattern.compile("Taxa de liquidação\\s+([\\d,\\.]+)\\s+(D|C)");
    private static final Pattern TAXA_REGISTRO_PATTERN = Pattern.compile("Taxa de Registro\\s+([\\d,\\.]+)\\s+(D|C)");
    private static final Pattern EMOLUMENTOS_PATTERN = Pattern.compile("Emolumentos\\s+([\\d,\\.]+)\\s+(D|C)");
    private static final Pattern TAXA_ANA_PATTERN = Pattern.compile("Taxa A\\.N\\.A\\.\\s+([\\d,\\.]+)");
    private static final Pattern LIQUIDO_PARA_PATTERN = Pattern.compile("Líquido para \\d{2}/\\d{2}/\\d{4}\\s+([\\d,\\.]+)\\s+(C|D)");
    
    // Day Trade (CORRIGIDO: adicionado \\. para valores como 4.185,00)
    private static final Pattern DAY_TRADE_BASE_PATTERN = Pattern.compile("Day Trade: Base R\\$\\s*([\\d,\\.]+)");
    private static final Pattern DAY_TRADE_IRRF_PATTERN = Pattern.compile("IRRF Projeção R\\$\\s*([\\d,\\.]+)");
    
    @Override
    public boolean canParse(String pdfText, String filename) {
        boolean canParse = pdfText.contains("BTG Pactual CTVM S.A.") &&
                          pdfText.contains("NOTA DE CORRETAGEM") &&
                          pdfText.contains("Nr. nota");
        
        log.info("🎯 BTG Parser pode processar '{}': {}", filename, canParse ? "✅ SIM" : "❌ NÃO");
        
        return canParse;
    }
    
    @Override
    public Invoice parseInvoice(String pdfText, MultipartFile originalFile) throws IOException {
        log.info("=== INICIANDO PARSING BTG PACTUAL ===");
        
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
            
            // 🔧 VALIDAÇÕES OBRIGATÓRIAS
            validateRequiredFields(invoice);
            
            log.info("=== PARSING BTG CONCLUÍDO ===");
            log.info("Invoice: {} | Data: {} | Cliente: {} | Itens: {} | Valor: {}", 
                    invoice.getInvoiceNumber(), 
                    invoice.getTradingDate(),
                    invoice.getClientName(),
                    items.size(), 
                    invoice.getNetOperationsValue());
            
            return invoice;
            
        } catch (Exception e) {
            log.error("Erro no parsing BTG: {}", e.getMessage(), e);
            throw new IOException("Erro ao processar nota BTG: " + e.getMessage(), e);
        }
    }
    
    /**
     * 🔧 NOVO: Validação de campos obrigatórios
     */
    private void validateRequiredFields(Invoice invoice) throws IOException {
        StringBuilder errors = new StringBuilder();
        
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().trim().isEmpty()) {
            errors.append("- Número da nota não encontrado\n");
        }
        
        if (invoice.getTradingDate() == null) {
            errors.append("- Data de pregão não encontrada\n");
        }
        
        if (invoice.getClientName() == null || invoice.getClientName().trim().isEmpty()) {
            errors.append("- Nome do cliente não encontrado\n");
        }
        
        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            errors.append("- Nenhuma operação encontrada\n");
        }
        
        if (errors.length() > 0) {
            String errorMessage = "❌ Campos obrigatórios não encontrados:\n" + errors.toString();
            log.error(errorMessage);
            throw new IOException(errorMessage);
        }
    }
    
    private void extractBasicInfo(String text, Invoice invoice) {
        logParsingInfo("Dados básicos", "iniciando");
        
        // Número da nota
        Matcher numeroMatcher = NUMERO_NOTA_PATTERN.matcher(text);
        if (numeroMatcher.find()) {
            invoice.setInvoiceNumber(numeroMatcher.group(1));
            logParsingInfo("Número nota", invoice.getInvoiceNumber());
        }
        
        // Data pregão
        Matcher dataMatcher = DATA_PREGAO_PATTERN.matcher(text);
        if (dataMatcher.find()) {
            String dateStr = dataMatcher.group(1);
            invoice.setTradingDate(parseDate(dateStr));
            logParsingInfo("Data pregão", dateStr);
        }
        
        // 🔧 CORRIGIDO: Cliente e CPF (formato: "005594044 FERNANDO COSTA NARDI 292.983.738-13")
        Matcher clienteMatcher = CLIENTE_PATTERN.matcher(text);
        if (clienteMatcher.find()) {
            String clientCode = clienteMatcher.group(1);  // 005594044
            String clientName = clienteMatcher.group(2).trim();  // FERNANDO COSTA NARDI
            String cpf = clienteMatcher.group(3);  // 292.983.738-13
            
            invoice.setClientCode(clientCode);
            invoice.setClientName(clientName);
            invoice.setCpfCnpj(cpf);
            
            logParsingInfo("Código cliente", clientCode);
            logParsingInfo("Cliente", clientName);
            logParsingInfo("CPF", cpf);
        }
    }
    
    private List<InvoiceItem> extractOperations(String text, Invoice invoice) {
        logParsingInfo("Operações", "iniciando extração");
        
        List<InvoiceItem> items = new ArrayList<>();
        
        // 🔍 DEBUG: Verificar conteúdo completo
        log.debug("Conteúdo completo da nota:\n{}", text);
        
        // Buscar seção de negócios realizados com padrões múltiplos
        String operationsSection = extractBetween(text, "Negócios realizados", "Resumo dos Negócios");
        if (operationsSection.isEmpty()) {
            operationsSection = extractBetween(text, "Q Negociação C/V", "Resumo dos Negócios");
        }
        
        // 🔍 NOVO DEBUG: Se ainda não encontrou, buscar por padrões alternativos
        if (operationsSection.isEmpty()) {
            log.warn("Seção de operações não encontrada com padrões padrão. Tentando padrões alternativos...");
            
            // Buscar linha por linha
            String[] lines = text.split("\n");
            boolean inOperationsSection = false;
            StringBuilder sectionBuilder = new StringBuilder();
            
            for (String line : lines) {
                if (line.contains("Negócios realizados") || line.contains("Q Negociação")) {
                    inOperationsSection = true;
                    continue;
                }
                if (line.contains("Resumo dos Negócios") || line.contains("Resumo")) {
                    break;
                }
                if (inOperationsSection) {
                    sectionBuilder.append(line).append("\n");
                }
            }
            
            operationsSection = sectionBuilder.toString();
        }

        // 🔍 DEBUG: Mostrar seção extraída
        log.info("🔍 Seção de operações extraída ({} caracteres):\n{}", 
                 operationsSection.length(), operationsSection);

        if (operationsSection.isEmpty()) {
            log.warn("Seção de operações não encontrada");
            return items;
        }
        
        logParsingInfo("Seção operações", operationsSection.length() + " caracteres");
        
        // 🔍 DEBUG: Verificar se o regex está funcionando
        log.debug("Iniciando busca por operações com regex...");
        
        Matcher operationMatcher = OPERACAO_PATTERN.matcher(operationsSection);
        int sequenceNumber = 1;
        
        while (operationMatcher.find()) {
            log.debug("Operação encontrada: {}", operationMatcher.group());
            
            try {
                InvoiceItem item = InvoiceItem.builder().build();
                
                // Extrair dados da operação
                String operationType = operationMatcher.group(1); // C ou V
                String marketType = operationMatcher.group(2);    // OPCAO DE COMPRA/VENDA  
                String expiration = operationMatcher.group(3);    // 06/25
                String assetCode = operationMatcher.group(4);     // BOVAR136
                String dayTradeMark = operationMatcher.group(5);  // D (opcional)
                String quantity = operationMatcher.group(6);     // quantidade
                String unitPrice = operationMatcher.group(7);    // preço unitário
                String totalValue = operationMatcher.group(8);   // valor total
                String debitCredit = operationMatcher.group(9);  // D ou C
                
                // 🔍 DEBUG: Log dos valores extraídos
                log.info("🔍 Operação extraída - Tipo: {}, Asset: '{}', Qtd: {}, Preço: {}", 
                         operationType, assetCode, quantity, unitPrice);
                
                // Preencher item
                item.setOperationType(operationType);
                item.setMarketType(marketType);
                item.setAssetCode(assetCode);
                item.setAssetSpecification(assetCode + " " + expiration);
                item.setQuantity(parseInteger(quantity));
                item.setUnitPrice(parseMoney(unitPrice));
                item.setTotalValue(parseMoney(totalValue));
                item.setSequenceNumber(sequenceNumber++);
                
                // Observações e características
                String observations = "";
                if ("D".equals(dayTradeMark)) {
                    observations += "D ";
                    item.setIsDayTrade(true);
                } else {
                    item.setIsDayTrade(false);
                }
                item.setObservations(observations.trim());
                item.setIsDirectDeal(false); // BTG não usa # nas operações padrão
                
                // Data de vencimento para opções
                if (expiration != null && !expiration.trim().isEmpty()) {
                    // Converter MM/YY para data completa (último dia do mês)
                    try {
                        String[] parts = expiration.split("/");
                        if (parts.length == 2) {
                            int month = Integer.parseInt(parts[0]);
                            int year = 2000 + Integer.parseInt(parts[1]); // 25 -> 2025
                            LocalDate expirationDate = LocalDate.of(year, month, 1)
                                    .withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth());
                            item.setExpirationDate(expirationDate);
                        }
                    } catch (Exception e) {
                        log.warn("Erro ao converter data de vencimento: {}", expiration);
                    }
                }
                
                setupInvoiceItem(item, invoice, sequenceNumber - 1);
                items.add(item);
                
                logParsingInfo("Operação " + (sequenceNumber - 1), 
                    String.format("%s %s %s %s x R$%s = R$%s", 
                        operationType, marketType, assetCode, quantity, unitPrice, totalValue));
                        
            } catch (Exception e) {
                log.error("Erro ao processar operação: {}", operationMatcher.group(), e);
            }
        }
        
        logParsingInfo("Total operações", items.size());
        return items;
    }
    
    private void extractFinancialData(String text, Invoice invoice) {
        logParsingInfo("Dados financeiros", "iniciando");
        
        // 💰 ANÁLISE DOS VALORES FINANCEIROS BTG:
        // - total_costs = Taxa liquidação + Taxa registro + Emolumentos = 1,50 + 1,17 + 1,08 = 3,75
        // - total_taxes = IRRF Day Trade = 0,01 (único imposto na nota)
        // - irrf_day_trade_basis = 1,25 (base de cálculo do IRRF)
        // - net_settlement_value = 1,25 (valor líquido para liquidação)
        // ⚠️ NORMAL: Alguns valores coincidem (irrf_day_trade_basis = net_settlement_value)
        
        BigDecimal totalCosts = BigDecimal.ZERO;
        BigDecimal totalTaxes = BigDecimal.ZERO;
        
        // Valor líquido das operações
        Matcher valorLiquidoMatcher = VALOR_LIQUIDO_PATTERN.matcher(text);
        if (valorLiquidoMatcher.find()) {
            String valor = valorLiquidoMatcher.group(1);
            String sinal = valorLiquidoMatcher.group(2);
            BigDecimal valorDecimal = parseMoney(valor);
            if ("D".equals(sinal)) {
                valorDecimal = valorDecimal.negate();
            }
            invoice.setNetOperationsValue(valorDecimal);
            logParsingInfo("Valor líquido operações", valor + " " + sinal);
        }
        
        // Taxa de liquidação
        Matcher taxaLiqMatcher = TAXA_LIQUIDACAO_PATTERN.matcher(text);
        if (taxaLiqMatcher.find()) {
            String valor = taxaLiqMatcher.group(1);
            String sinal = taxaLiqMatcher.group(2);
            BigDecimal valorDecimal = parseMoney(valor);
            if ("D".equals(sinal)) {
                valorDecimal = valorDecimal.negate();
            }
            invoice.setLiquidationTax(valorDecimal);
            logParsingInfo("Taxa liquidação", valor + " " + sinal);
        }
        
        // Taxa de registro
        Matcher taxaRegMatcher = TAXA_REGISTRO_PATTERN.matcher(text);
        if (taxaRegMatcher.find()) {
            String valor = taxaRegMatcher.group(1);
            String sinal = taxaRegMatcher.group(2);
            BigDecimal valorDecimal = parseMoney(valor);
            if ("D".equals(sinal)) {
                valorDecimal = valorDecimal.negate();
            }
            invoice.setRegistrationTax(valorDecimal);
            logParsingInfo("Taxa registro", valor + " " + sinal);
        }
        
        // Emolumentos
        Matcher emolMatcher = EMOLUMENTOS_PATTERN.matcher(text);
        if (emolMatcher.find()) {
            String valor = emolMatcher.group(1);
            String sinal = emolMatcher.group(2);
            BigDecimal valorDecimal = parseMoney(valor);
            if ("D".equals(sinal)) {
                valorDecimal = valorDecimal.negate();
            }
            invoice.setEmoluments(valorDecimal);
            logParsingInfo("Emolumentos", valor + " " + sinal);
        }
        
        // Taxa A.N.A.
        Matcher taxaAnaMatcher = TAXA_ANA_PATTERN.matcher(text);
        if (taxaAnaMatcher.find()) {
            String valor = taxaAnaMatcher.group(1);
            invoice.setAnaTax(parseMoney(valor));
            logParsingInfo("Taxa ANA", valor);
        }
        
        // Líquido para liquidação
        Matcher liquidoParaMatcher = LIQUIDO_PARA_PATTERN.matcher(text);
        if (liquidoParaMatcher.find()) {
            String valor = liquidoParaMatcher.group(1);
            String sinal = liquidoParaMatcher.group(2);
            BigDecimal valorDecimal = parseMoney(valor);
            if ("D".equals(sinal)) {
                valorDecimal = valorDecimal.negate();
            }
            invoice.setNetSettlementValue(valorDecimal);
            logParsingInfo("Líquido para liquidação", valor + " " + sinal);
        }
        
        // Day Trade IRRF
        Matcher dayTradeBaseMatcher = DAY_TRADE_BASE_PATTERN.matcher(text);
        if (dayTradeBaseMatcher.find()) {
            String valor = dayTradeBaseMatcher.group(1);
            invoice.setIrrfDayTradeBasis(parseMoney(valor));
            logParsingInfo("Day Trade Base", valor);
        }
        
        Matcher dayTradeIrrfMatcher = DAY_TRADE_IRRF_PATTERN.matcher(text);
        if (dayTradeIrrfMatcher.find()) {
            String valor = dayTradeIrrfMatcher.group(1);
            invoice.setIrrfDayTradeValue(parseMoney(valor));
            logParsingInfo("Day Trade IRRF", valor);
        }
        
        // Calcular totais
        calculateTotals(invoice);
    }
    
    private void calculateTotals(Invoice invoice) {
        // Valor bruto das operações (soma de todas as operações)
        if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
            BigDecimal grossValue = invoice.getItems().stream()
                    .map(item -> item.getTotalValue() != null ? item.getTotalValue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            invoice.setGrossOperationsValue(grossValue);
        }
        
        // Calcular custos totais
        BigDecimal totalCosts = BigDecimal.ZERO;
        
        if (invoice.getLiquidationTax() != null && invoice.getLiquidationTax().compareTo(BigDecimal.ZERO) < 0) {
            totalCosts = totalCosts.add(invoice.getLiquidationTax().abs());
        }
        if (invoice.getRegistrationTax() != null && invoice.getRegistrationTax().compareTo(BigDecimal.ZERO) < 0) {
            totalCosts = totalCosts.add(invoice.getRegistrationTax().abs());
        }
        if (invoice.getEmoluments() != null && invoice.getEmoluments().compareTo(BigDecimal.ZERO) < 0) {
            totalCosts = totalCosts.add(invoice.getEmoluments().abs());
        }
        
        invoice.setTotalCosts(totalCosts);
        
        // Total de taxas
        BigDecimal totalTaxes = BigDecimal.ZERO;
        if (invoice.getIrrfDayTradeValue() != null) {
            totalTaxes = totalTaxes.add(invoice.getIrrfDayTradeValue());
        }
        invoice.setTotalTaxes(totalTaxes);
        
        // Data de liquidação (data pregão + 1 dia útil)
        if (invoice.getTradingDate() != null && invoice.getSettlementDate() == null) {
            invoice.setSettlementDate(invoice.getTradingDate().plusDays(1));
        }
    }
    
    @Override
    public String getBrokerageName() {
        return "BTG PACTUAL";
    }
    
    @Override
    public int getPriority() {
        return 100; // Alta especificidade para BTG
    }
}