package com.olisystem.optionsmanager.dto.invoice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request para importação de múltiplas notas de corretagem
 * Máximo de 5 notas por requisição
 */
public record InvoiceImportRequest(
    
    @NotNull(message = "ID da corretora é obrigatório")
    UUID brokerageId,
    
    @NotNull(message = "Lista de arquivos é obrigatória")
    @Size(min = 1, max = 5, message = "Deve enviar entre 1 e 5 arquivos por vez")
    List<InvoiceFileData> files
) {
    
    /**
     * Dados de um arquivo de nota de corretagem
     */
    public record InvoiceFileData(
        
        @NotNull(message = "Nome do arquivo é obrigatório")
        String fileName,
        
        @NotNull(message = "Conteúdo do arquivo é obrigatório")
        @Size(min = 1, message = "Arquivo não pode estar vazio")
        String fileContent,
        
        @NotNull(message = "Hash do arquivo é obrigatório")
        String fileHash
    ) {
        
        /**
         * Valida se o arquivo tem extensão PDF
         */
        public boolean isPdfFile() {
            return fileName != null && fileName.toLowerCase().endsWith(".pdf");
        }
        
        /**
         * Retorna o nome sem extensão
         */
        public String getFileNameWithoutExtension() {
            if (fileName == null) return null;
            int lastDot = fileName.lastIndexOf('.');
            return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
        }
    }
}
