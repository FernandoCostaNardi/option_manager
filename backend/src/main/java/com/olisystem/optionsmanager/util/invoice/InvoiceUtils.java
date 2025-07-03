package com.olisystem.optionsmanager.util.invoice;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Utilitários para processamento de notas de corretagem
 */
@Slf4j
public class InvoiceUtils {

    private static final Pattern PDF_SIGNATURE = Pattern.compile("^%PDF-");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{2})[/-](\\d{2})[/-](\\d{4})");
    private static final Pattern MONEY_PATTERN = Pattern.compile("R?\\$?[\\s]*(\\d{1,3}(?:\\.\\d{3})*(?:,\\d{2})?)", Pattern.CASE_INSENSITIVE);
    
    // Formatadores de data
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yy"),
        DateTimeFormatter.ofPattern("dd-MM-yy")
    };

    /**
     * Gera hash SHA-256 do conteúdo do arquivo
     */
    public static String generateFileHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            log.error("Erro ao gerar hash: {}", e.getMessage());
            throw new RuntimeException("Falha ao gerar hash do arquivo", e);
        }
    }

    /**
     * Verifica se o conteúdo parece ser um PDF válido OU texto extraído de nota de corretagem
     */
    public static boolean isPdfContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Se for Base64, decodificar primeiro
            String actualContent = content.trim();
            
            // Verificar se é Base64 (tentativa de decode)
            if (isBase64(actualContent)) {
                byte[] decoded = java.util.Base64.getDecoder().decode(actualContent);
                actualContent = new String(decoded, 0, Math.min(decoded.length, 100)); // Só os primeiros 100 bytes
            }
            
            // Verifica assinatura PDF
            if (PDF_SIGNATURE.matcher(actualContent).find()) {
                return true;
            }
            
            // 🔧 NOVA VALIDAÇÃO: Aceita texto extraído de notas de corretagem
            return isExtractedInvoiceText(content);
            
        } catch (Exception e) {
            // Se não conseguir decodificar Base64, verificar diretamente
            if (PDF_SIGNATURE.matcher(content.trim()).find()) {
                return true;
            }
            
            // Verificar se é texto extraído válido
            return isExtractedInvoiceText(content);
        }
    }
    
    /**
     * Verifica se o conteúdo parece ser texto extraído de uma nota de corretagem
     */
    private static boolean isExtractedInvoiceText(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String upperContent = content.toUpperCase();
        
        // Deve conter pelo menos 2 dos indicadores principais
        int indicators = 0;
        
        if (upperContent.contains("NOTA DE CORRETAGEM")) indicators++;
        if (upperContent.contains("BTG PACTUAL") || upperContent.contains("CLEAR") || 
            upperContent.contains("RICO") || upperContent.contains("TORO")) indicators++;
        if (upperContent.contains("NR. NOTA") || upperContent.contains("NÚMERO") ||
            upperContent.contains("DATA PREGÃO")) indicators++;
        if (upperContent.contains("C.N.P.J") || upperContent.contains("CNPJ")) indicators++;
        if (upperContent.contains("CLIENTE") || upperContent.contains("CPF")) indicators++;
        
        return indicators >= 2;
    }
    
    /**
     * Verifica se uma string é Base64 válida
     */
    private static boolean isBase64(String str) {
        try {
            // Base64 deve ter comprimento múltiplo de 4 e usar apenas caracteres válidos
            if (str.length() % 4 != 0) return false;
            java.util.Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Valida se o nome do arquivo tem extensão PDF
     */
    public static boolean hasPdfExtension(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }

    /**
     * Extrai todas as datas encontradas no texto
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        String cleanDate = dateStr.trim();
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleanDate, formatter);
            } catch (DateTimeParseException e) {
                // Tenta próximo formato
            }
        }
        
        log.warn("Não foi possível converter data: {}", dateStr);
        return null;
    }

    /**
     * Limpa e padroniza texto para busca
     */
    public static String cleanText(String text) {
        if (text == null) {
            return "";
        }
        
        return text.trim()
                  .replaceAll("\\s+", " ")  // Múltiplos espaços para um
                  .replaceAll("[\\r\\n]+", " ");  // Quebras de linha para espaço
    }

    /**
     * Extrai valor monetário de uma string
     */
    public static String extractMoneyValue(String text) {
        if (text == null) {
            return "0,00";
        }
        
        var matcher = MONEY_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "0,00";
    }

    /**
     * Converte valor monetário brasileiro para BigDecimal
     */
    public static java.math.BigDecimal parseBrazilianMoney(String value) {
        if (value == null || value.trim().isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        
        try {
            // Remove R$, espaços e símbolos
            String cleaned = value.replaceAll("[R$\\s]", "")
                                 .replace(".", "")  // Remove pontos de milhares
                                 .replace(",", "."); // Vírgula decimal vira ponto
            
            return new java.math.BigDecimal(cleaned);
            
        } catch (NumberFormatException e) {
            log.warn("Erro ao converter valor monetário '{}': {}", value, e.getMessage());
            return java.math.BigDecimal.ZERO;
        }
    }

    /**
     * Verifica se uma string contém indicadores de Day Trade
     */
    public static boolean isDayTradeIndicator(String observation) {
        if (observation == null) {
            return false;
        }
        
        String obs = observation.toUpperCase().trim();
        return obs.contains("D") || 
               obs.contains("DAY") || 
               obs.contains("TRADE") ||
               obs.contains("DT");
    }

    /**
     * Verifica se uma string contém indicadores de negócio direto
     */
    public static boolean isDirectDealIndicator(String observation) {
        if (observation == null) {
            return false;
        }
        
        return observation.contains("#") || 
               observation.toUpperCase().contains("DIRETO");
    }

    /**
     * Extrai código do ativo de uma especificação completa
     */
    public static String extractAssetCode(String specification) {
        if (specification == null || specification.trim().isEmpty()) {
            return null;
        }
        
        // Remove espaços e pega primeira parte antes de espaço
        String[] parts = specification.trim().split("\\s+");
        if (parts.length > 0) {
            // Remove caracteres especiais exceto letras e números
            return parts[0].replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        }
        
        return null;
    }

    /**
     * Valida se um número de nota parece válido
     */
    public static boolean isValidInvoiceNumber(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            return false;
        }
        
        // Deve ter pelo menos 3 dígitos e no máximo 20 caracteres
        String cleaned = invoiceNumber.trim();
        return cleaned.matches(".*\\d{3,}.*") && cleaned.length() <= 20;
    }

    /**
     * Normaliza nome de cliente removendo caracteres especiais
     */
    public static String normalizeClientName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        return name.trim()
                   .replaceAll("\\d", "") // Remove números
                   .replaceAll("[^a-zA-ZÀ-ÿ\\s]", "") // Remove caracteres especiais exceto acentos
                   .replaceAll("\\s+", " ") // Normaliza espaços
                   .trim();
    }

    /**
     * Gera nome de arquivo único baseado em timestamp
     */
    public static String generateUniqueFileName(String originalName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = "";
        
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        
        return "invoice_" + timestamp + extension;
    }
}
