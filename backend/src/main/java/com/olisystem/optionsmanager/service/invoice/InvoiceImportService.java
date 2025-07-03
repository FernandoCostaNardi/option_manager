package com.olisystem.optionsmanager.service.invoice;

import com.olisystem.optionsmanager.dto.invoice.InvoiceImportRequest;
import com.olisystem.optionsmanager.dto.invoice.InvoiceImportResponse;
import com.olisystem.optionsmanager.model.invoice.Invoice;
import com.olisystem.optionsmanager.model.auth.User;

/**
 * Interface para serviço de importação de notas de corretagem
 */
public interface InvoiceImportService {

    /**
     * Importa múltiplas notas de corretagem
     * 
     * @param request Dados da requisição com arquivos
     * @param user Usuário que está importando
     * @return Response com resultado da importação
     */
    InvoiceImportResponse importInvoices(InvoiceImportRequest request, User user);

    /**
     * Verifica se arquivo já foi importado pelo hash
     * 
     * @param fileHash Hash do arquivo
     * @return true se já existe
     */
    boolean isFileAlreadyImported(String fileHash);

    /**
     * Processa um único arquivo de nota
     * 
     * @param fileData Dados do arquivo
     * @param request Request original
     * @param user Usuário
     * @return Resultado da importação
     */
    InvoiceImportResponse.ImportResult processSingleFile(
        InvoiceImportRequest.InvoiceFileData fileData,
        InvoiceImportRequest request,
        User user
    );
}
