package com.olisystem.optionsmanager.service.invoice.parser;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.invoice.InvoiceItem;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe base com funcionalidades comuns para todos os parsers
 */
@Slf4j
public abstract class AbstractInvoiceParser implements InvoiceParser {
    
    // Formatadores de data comuns
    protected static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yy")
    };
    
    // Padrões regex comuns
    protected static final Pattern CPF_PATTERN = Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
    protected static final Pattern CNPJ_PATTERN = Pattern.compile("\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}");
    protected static final Pattern MONEY_PATTERN = Pattern.compile("([\\d,]+\\.\\d{2})");
    protected static final Pattern DATE_PATTERN = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
    
    /**
     * Extrai texto entre duas strings
     */
    protected String extractBetween(String text, String start, String end) {
        int startIdx = text.indexOf(start);
        if (startIdx == -1) return "";
        
        startIdx += start.length();
        int endIdx = text.indexOf(end, startIdx);
        if (endIdx == -1) return "";
        
        return text.substring(startIdx, endIdx).trim();
    }
    
    /**
     * Extrai campo usando regex
     */
    protected String extractWithRegex(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
    
    /**
     * Extrai primeiro campo encontrado por um padrão
     */
    protected String extractFirst(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return "";
    }
    
    /**
     * Converte string para BigDecimal (valores monetários)
     * Corrigido para lidar com formato brasileiro: "1.350,00"
     */
    protected BigDecimal parseMoney(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            // Remove caracteres não numéricos exceto vírgula, ponto e sinal
            String cleaned = value.replaceAll("[^\\d,.-]", "");
            
            // 🔧 CORREÇÃO: Lógica para formato brasileiro
            if (cleaned.contains(",") && cleaned.contains(".")) {
                // Formato brasileiro: "1.350,00" - ponto é milhares, vírgula é decimal
                // Remove pontos (separadores de milhares) e substitui vírgula por ponto
                cleaned = cleaned.replace(".", "").replace(",", ".");
            } else if (cleaned.contains(",") && !cleaned.contains(".")) {
                // Só vírgula: substitui vírgula por ponto para decimal
                cleaned = cleaned.replace(",", ".");
            }
            // Se só tem ponto, mantém como está (formato americano)
            
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Erro ao converter valor monetário: {} -> {}", value, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Converte string para Integer
     */
    protected Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        
        try {
            String cleaned = value.replaceAll("[^\\d-]", "");
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Erro ao converter inteiro: {} -> {}", value, e.getMessage());
            return 0;
        }
    }
    
    /**
     * Converte string para LocalDate
     */
    protected LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        String cleaned = dateStr.trim();
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleaned, formatter);
            } catch (DateTimeParseException ignored) {
                // Tenta o próximo formato
            }
        }
        
        log.warn("Não foi possível converter data: {}", dateStr);
        return null;
    }
    
    /**
     * Detecta se operação é Day Trade baseado em observações
     */
    protected boolean isDayTrade(String observations, LocalDate entryDate, LocalDate exitDate) {
        // Verifica marcador 'D' nas observações
        if (observations != null && observations.toUpperCase().contains("D")) {
            return true;
        }
        
        // Verifica se as datas são iguais (mesmo dia)
        if (entryDate != null && exitDate != null) {
            return entryDate.equals(exitDate);
        }
        
        return false;
    }
    
    /**
     * Detecta se é negócio direto baseado em observações
     */
    protected boolean isDirectDeal(String observations) {
        return observations != null && observations.contains("#");
    }
    
    /**
     * Extrai código do ativo de uma especificação complexa
     * Ex: "PETRF336 PN D" -> "PETRF336"
     */
    protected String extractAssetCode(String specification) {
        if (specification == null || specification.trim().isEmpty()) {
            return "";
        }
        
        // Pega a primeira palavra que parece ser um código de ativo
        String[] parts = specification.trim().split("\\s+");
        if (parts.length > 0) {
            return parts[0];
        }
        
        return specification.trim();
    }
    
    /**
     * Configura campos básicos do InvoiceItem
     */
    protected void setupInvoiceItem(InvoiceItem item, Invoice invoice, Integer sequence) {
        item.setInvoice(invoice);
        item.setSequenceNumber(sequence);
        
        // Detectar características
        if (item.getObservations() != null) {
            item.setIsDayTrade(isDayTrade(item.getObservations(), null, null));
            item.setIsDirectDeal(isDirectDeal(item.getObservations()));
        }
        
        // Extrair código do ativo
        if (item.getAssetSpecification() != null) {
            item.setAssetCode(extractAssetCode(item.getAssetSpecification()));
        }
    }
    
    /**
     * Log de debug com informações do parsing
     */
    protected void logParsingInfo(String step, Object value) {
        log.debug("[{}] Parsing {}: {}", getBrokerageName(), step, value);
    }
    
    /**
     * Limpa texto removendo caracteres especiais
     */
    protected String cleanText(String text) {
        if (text == null) return "";
        
        return text.replaceAll("[\\r\\n\\t]", " ")
                  .replaceAll("\\s+", " ")
                  .trim();
    }
}
