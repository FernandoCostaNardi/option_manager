package com.olisystem.optionsmanager.service.invoice;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 📊 Resultado do processamento de invoices para operations
 */
@Data
@Builder
public class InvoiceProcessingResult {
    
    /**
     * Número de invoices processadas
     */
    private final int processedInvoices;
    
    /**
     * Número de operações criadas com sucesso
     */
    private final int createdOperations;
    
    /**
     * Número de operações ignoradas (duplicatas, inválidas, etc.)
     */
    private final int skippedOperations;
    
    /**
     * Lista de erros encontrados durante o processamento
     */
    private final List<String> errors;
    
    /**
     * Indica se o processamento foi executado com sucesso total
     */
    public boolean isFullSuccess() {
        return errors.isEmpty() && createdOperations > 0;
    }
    
    /**
     * Indica se houve ao menos sucesso parcial
     */
    public boolean hasPartialSuccess() {
        return createdOperations > 0;
    }
    
    /**
     * Total de itens processados (criados + ignorados)
     */
    public int getTotalProcessedItems() {
        return createdOperations + skippedOperations;
    }
    
    /**
     * Retorna um resumo textual do processamento
     */
    public String getSummary() {
        if (processedInvoices == 0) {
            return "Nenhuma invoice processada";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Processadas: %d invoices", processedInvoices));
        summary.append(String.format(" | Criadas: %d operações", createdOperations));
        
        if (skippedOperations > 0) {
            summary.append(String.format(" | Ignoradas: %d", skippedOperations));
        }
        
        if (!errors.isEmpty()) {
            summary.append(String.format(" | Erros: %d", errors.size()));
        }
        
        return summary.toString();
    }
}