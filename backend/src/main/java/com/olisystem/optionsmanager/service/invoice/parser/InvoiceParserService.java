package com.olisystem.optionsmanager.service.invoice.parser;

import com.olisystem.optionsmanager.dto.invoice.InvoiceImportRequest;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.brokerage.Brokerage;
import com.olisystem.optionsmanager.model.auth.User;

/**
 * Interface para serviços de parsing de notas de corretagem
 */
public interface InvoiceParserService {

    /**
     * Extrai dados de uma nota de corretagem do conteúdo do arquivo
     * 
     * @param fileData Dados do arquivo (conteúdo, nome, hash)
     * @param brokerage Corretora associada
     * @param user Usuário que está importando
     * @return Invoice com dados extraídos (sem ID, ainda não salva)
     */
    Invoice parseInvoice(InvoiceImportRequest.InvoiceFileData fileData, 
                        Brokerage brokerage, 
                        User user);

    /**
     * Verifica se o parser suporta a corretora especificada
     * 
     * @param brokerage Corretora a verificar
     * @return true se suportada
     */
    boolean supportsBrokerage(Brokerage brokerage);

    /**
     * Determina o tipo de corretora baseado no conteúdo da nota
     * 
     * @param content Conteúdo da nota
     * @return Nome/tipo da corretora detectada
     */
    String detectBrokerageType(String content);
}
