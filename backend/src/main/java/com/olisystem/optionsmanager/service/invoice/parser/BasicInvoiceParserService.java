package com.olisystem.optionsmanager.service.invoice.parser;

import com.olisystem.optionsmanager.dto.invoice.InvoiceImportRequest;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import com.olisystem.optionsmanager.model.brokerage.Brokerage;
import com.olisystem.optionsmanager.model.auth.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementação do parser de notas de corretagem
 * Seleciona automaticamente o parser específico por corretora
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BasicInvoiceParserService implements InvoiceParserService {

    // Injeção de parsers específicos
    private final BtgInvoiceParser btgInvoiceParser;
    private final ToroInvoiceParser toroInvoiceParser;

    // Padrões regex básicos para extração de dados
    private static final Pattern INVOICE_NUMBER_PATTERN = 
        Pattern.compile("(?:Nota|Número|N[úo]mero|Nr?).*?(?:Corretagem)?[\\s:]*([0-9]+)", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern DATE_PATTERN = 
        Pattern.compile("(\\d{2})[/-](\\d{2})[/-](\\d{4})");
    
    private static final Pattern MONEY_PATTERN = 
        Pattern.compile("R?\\$?[\\s]*(\\d{1,3}(?:\\.\\d{3})*(?:,\\d{2})?)", Pattern.CASE_INSENSITIVE);

    @Override
    public Invoice parseInvoice(InvoiceImportRequest.InvoiceFileData fileData, 
                               Brokerage brokerage, 
                               User user) {
        
        log.info("🔍 Iniciando parsing da nota: {}", fileData.fileName());
        
        String content = fileData.fileContent();
        
        // 🎯 SELEÇÃO AUTOMÁTICA DO PARSER ESPECÍFICO
        InvoiceParser specificParser = selectSpecificParser(content, fileData.fileName());
        
        if (specificParser != null) {
            log.info("✅ Parser específico selecionado: {}", specificParser.getBrokerageName());
            
            try {
                // Criar MultipartFile mock para compatibilidade
                MultipartFile mockFile = createMockMultipartFile(fileData);
                Invoice parsedInvoice = specificParser.parseInvoice(content, mockFile);
                
                // 🔧 CORREÇÃO: Configurar campos obrigatórios que vêm do serviço
                parsedInvoice.setBrokerage(brokerage);
                parsedInvoice.setUser(user);
                parsedInvoice.setFileHash(fileData.fileHash());
                parsedInvoice.setImportedAt(java.time.LocalDateTime.now());
                
                log.info("✅ Parsing específico concluído com dados configurados");
                return parsedInvoice;
                
            } catch (Exception e) {
                log.error("❌ Erro no parser específico {}: {}", specificParser.getBrokerageName(), e.getMessage());
                log.warn("🔄 Tentando parser genérico como fallback...");
            }
        }
        
        // 🔄 FALLBACK: Parser genérico (básico)
        log.info("⚙️ Usando parser genérico");
        return parseWithGenericParser(fileData, brokerage, user);
    }
    
    /**
     * Seleciona parser específico baseado no conteúdo da nota
     */
    private InvoiceParser selectSpecificParser(String content, String filename) {
        log.info("🔍 Detectando parser específico para: {}", filename);
        
        // Lista de parsers específicos em ordem de prioridade
        InvoiceParser[] parsers = {btgInvoiceParser, toroInvoiceParser};
        
        for (InvoiceParser parser : parsers) {
            log.info("🧪 Testando parser: {}", parser.getBrokerageName());
            
            if (parser.canParse(content, filename)) {
                log.info("🎯 Parser selecionado: {}", parser.getBrokerageName());
                return parser;
            }
        }
        
        log.warn("⚠️ Nenhum parser específico encontrado. Usando genérico.");
        return null; // Nenhum parser específico encontrado
    }
    
    /**
     * Cria um MultipartFile mock para compatibilidade com parsers específicos
     */
    private MultipartFile createMockMultipartFile(InvoiceImportRequest.InvoiceFileData fileData) {
        return new MultipartFile() {
            @Override
            public String getName() { return "file"; }
            
            @Override
            public String getOriginalFilename() { return fileData.fileName(); }
            
            @Override
            public String getContentType() { return "application/pdf"; }
            
            @Override
            public boolean isEmpty() { return false; }
            
            @Override
            public long getSize() { return fileData.fileContent().length(); }
            
            @Override
            public byte[] getBytes() { return fileData.fileContent().getBytes(); }
            
            @Override
            public java.io.InputStream getInputStream() { 
                return new java.io.ByteArrayInputStream(fileData.fileContent().getBytes()); 
            }
            
            @Override
            public void transferTo(java.io.File dest) throws java.io.IOException, IllegalStateException {
                throw new UnsupportedOperationException("Not supported for mock file");
            }
        };
    }
    
    /**
     * Parser genérico (versão básica para desenvolvimento)
     */
    private Invoice parseWithGenericParser(InvoiceImportRequest.InvoiceFileData fileData, 
                                         Brokerage brokerage, 
                                         User user) {
        String content = fileData.fileContent();
        
        // Cria invoice básica
        Invoice invoice = Invoice.builder()
            .brokerage(brokerage)
            .user(user)
            .rawContent(content)
            .fileHash(fileData.fileHash())
            .importedAt(LocalDateTime.now())
            .items(new ArrayList<>())
            .build();

        try {
            // Extrai dados básicos
            extractBasicInvoiceData(invoice, content);
            
            // Extrai itens (versão simplificada)
            extractInvoiceItems(invoice, content);
            
            log.info("✅ Parsing genérico concluído. Nota: {}, Data: {}, Itens: {}", 
                     invoice.getInvoiceNumber(), 
                     invoice.getTradingDate(), 
                     invoice.getItems() != null ? invoice.getItems().size() : 0);
            
        } catch (Exception e) {
            log.error("Erro durante parsing genérico: {}", e.getMessage(), e);
            throw new RuntimeException("Falha no parsing da nota: " + e.getMessage(), e);
        }

        return invoice;
    }

    @Override
    public boolean supportsBrokerage(Brokerage brokerage) {
        // Por enquanto, suporta todas as corretoras de forma básica
        return true;
    }

    @Override
    public String detectBrokerageType(String content) {
        String contentUpper = content.toUpperCase();
        
        if (contentUpper.contains("BTG PACTUAL")) return "BTG";
        if (contentUpper.contains("CLEAR") || contentUpper.contains("XP INVESTIMENTOS")) return "CLEAR/XP";
        if (contentUpper.contains("RICO")) return "RICO";
        if (contentUpper.contains("TORO")) return "TORO";
        
        return "DESCONHECIDA";
    }

    /**
     * Extrai dados básicos da nota
     */
    private void extractBasicInvoiceData(Invoice invoice, String content) {
        
        // Número da nota
        String invoiceNumber = extractInvoiceNumber(content);
        invoice.setInvoiceNumber(invoiceNumber != null ? invoiceNumber : "N/A");
        
        // Data de negociação (pega a primeira data encontrada)
        LocalDate tradingDate = extractFirstDate(content);
        invoice.setTradingDate(tradingDate != null ? tradingDate : LocalDate.now());
        
        // Nome do cliente (simplificado - pega linha que contém CPF)
        String clientName = extractClientName(content);
        if (clientName != null) {
            invoice.setClientName(clientName);
        }
        
        // Valores financeiros básicos (versão simplificada)
        extractBasicFinancialData(invoice, content);
        
        log.debug("Dados básicos extraídos - Nota: {}, Data: {}, Cliente: {}", 
                  invoice.getInvoiceNumber(), invoice.getTradingDate(), invoice.getClientName());
    }

    /**
     * Extrai número da nota
     */
    private String extractInvoiceNumber(String content) {
        Matcher matcher = INVOICE_NUMBER_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extrai primeira data encontrada no documento
     */
    private LocalDate extractFirstDate(String content) {
        Matcher matcher = DATE_PATTERN.matcher(content);
        if (matcher.find()) {
            try {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                log.warn("Erro ao converter data: {}", matcher.group());
            }
        }
        return null;
    }

    /**
     * Extrai nome do cliente (versão simplificada)
     */
    private String extractClientName(String content) {
        // Procura por linha que contém "CPF" e extrai o nome antes
        String[] lines = content.split("\\n");
        for (String line : lines) {
            if (line.toUpperCase().contains("CPF")) {
                // Remove CPF e números, pega o que sobra como nome
                String name = line.replaceAll("CPF.*", "")
                                 .replaceAll("\\d", "")
                                 .replaceAll("[^a-zA-ZÀ-ÿ\\s]", "")
                                 .trim();
                if (name.length() > 3) {
                    // Trunca se for muito longo (máximo 500 caracteres para segurança)
                    if (name.length() > 500) {
                        log.warn("Nome do cliente muito longo ({}), truncando: {}", 
                                name.length(), name.substring(0, 50) + "...");
                        name = name.substring(0, 500).trim();
                    }
                    return name;
                }
            }
        }
        return null;
    }

    /**
     * Extrai dados financeiros básicos
     */
    private void extractBasicFinancialData(Invoice invoice, String content) {
        // Por enquanto, apenas inicializa com zeros
        // Posteriormente implementaremos extração real por corretora
        
        invoice.setGrossOperationsValue(BigDecimal.ZERO);
        invoice.setNetOperationsValue(BigDecimal.ZERO);
        invoice.setTotalCosts(BigDecimal.ZERO);
        invoice.setTotalTaxes(BigDecimal.ZERO);
        invoice.setNetSettlementValue(BigDecimal.ZERO);
        
        // Inicializa todas as taxas com zero
        invoice.setLiquidationTax(BigDecimal.ZERO);
        invoice.setRegistrationTax(BigDecimal.ZERO);
        invoice.setEmoluments(BigDecimal.ZERO);
        invoice.setAnaTax(BigDecimal.ZERO);
        invoice.setTermOptionsTax(BigDecimal.ZERO);
        invoice.setBrokerageFee(BigDecimal.ZERO);
        invoice.setIss(BigDecimal.ZERO);
        invoice.setPis(BigDecimal.ZERO);
        invoice.setCofins(BigDecimal.ZERO);
        
        // IRRF
        invoice.setIrrfDayTradeBasis(BigDecimal.ZERO);
        invoice.setIrrfDayTradeValue(BigDecimal.ZERO);
        invoice.setIrrfCommonBasis(BigDecimal.ZERO);
        invoice.setIrrfCommonValue(BigDecimal.ZERO);
    }

    /**
     * Extrai itens da nota (versão muito simplificada)
     */
    private void extractInvoiceItems(Invoice invoice, String content) {
        // Por enquanto, cria apenas um item de exemplo
        // Posteriormente implementaremos extração real das operações
        
        InvoiceItem sampleItem = InvoiceItem.builder()
            .invoice(invoice)
            .sequenceNumber(1)
            .operationType("C") // Compra por padrão
            .marketType("VISTA")
            .assetSpecification("EXEMPLO")
            .assetCode("EXEMPLO")
            .quantity(100)
            .unitPrice(BigDecimal.valueOf(10.00))
            .totalValue(BigDecimal.valueOf(1000.00))
            .isDayTrade(false)
            .isDirectDeal(false)
            .observations("Dados extraídos automaticamente - versão inicial")
            .build();
        
        // Adiciona item à lista
        if (invoice.getItems() == null) {
            invoice.setItems(new ArrayList<>());
        }
        invoice.getItems().add(sampleItem);
        sampleItem.setInvoice(invoice);
        
        log.debug("Item de exemplo criado para desenvolvimento");
    }
}
