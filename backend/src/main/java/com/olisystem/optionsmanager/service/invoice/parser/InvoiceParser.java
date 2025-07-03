package com.olisystem.optionsmanager.service.invoice.parser;

import com.olisystem.optionsmanager.model.invoice.Invoice;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Interface para parsers específicos de notas de corretagem por corretora
 */
public interface InvoiceParser {
    
    /**
     * Verifica se este parser pode processar o arquivo
     */
    boolean canParse(String pdfText, String filename);
    
    /**
     * Extrai dados da nota de corretagem e cria objeto Invoice
     */
    Invoice parseInvoice(String pdfText, MultipartFile originalFile) throws IOException;
    
    /**
     * Nome da corretora suportada
     */
    String getBrokerageName();
    
    /**
     * Prioridade do parser (maior = mais específico)
     */
    int getPriority();
}
