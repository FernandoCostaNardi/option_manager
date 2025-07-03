package com.olisystem.optionsmanager.validator.invoice;

import com.olisystem.optionsmanager.dto.invoice.InvoiceImportRequest;
import com.olisystem.optionsmanager.exception.invoice.InvalidFileFormatException;
import com.olisystem.optionsmanager.util.invoice.InvoiceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validador para arquivos de importação de notas de corretagem
 */
@Component
@Slf4j
public class InvoiceFileValidator {

    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB em bytes (aproximado)
    private static final int MIN_CONTENT_LENGTH = 1000; // Mínimo de caracteres para um PDF válido
    private static final Set<String> REQUIRED_PDF_KEYWORDS = Set.of("PDF", "CORRETAGEM", "NOTA");

    /**
     * Valida uma requisição completa de importação
     */
    public void validateImportRequest(InvoiceImportRequest request) {
        log.debug("Validando requisição de importação com {} arquivos", request.files().size());

        // Validações globais
        validateFileCount(request.files().size());
        validateNoDuplicateHashes(request.files());

        // Validações individuais
        for (InvoiceImportRequest.InvoiceFileData fileData : request.files()) {
            validateSingleFile(fileData);
        }

        log.debug("✅ Validação da requisição concluída com sucesso");
    }

    /**
     * Valida um único arquivo
     */
    public void validateSingleFile(InvoiceImportRequest.InvoiceFileData fileData) {
        log.debug("Validando arquivo: {}", fileData.fileName());

        // Validações básicas
        validateFileName(fileData.fileName());
        validateFileContent(fileData.fileContent());
        validateFileHash(fileData.fileHash(), fileData.fileContent());

        // Validações de formato
        validatePdfFormat(fileData);
        validateContentStructure(fileData);

        log.debug("✅ Arquivo {} validado com sucesso", fileData.fileName());
    }

    /**
     * Valida quantidade de arquivos na requisição
     */
    private void validateFileCount(int fileCount) {
        if (fileCount < 1) {
            throw new IllegalArgumentException("Deve enviar pelo menos 1 arquivo");
        }

        if (fileCount > 5) {
            throw new IllegalArgumentException("Máximo de 5 arquivos por importação");
        }
    }

    /**
     * Valida se não há hashes duplicados na mesma requisição
     */
    private void validateNoDuplicateHashes(List<InvoiceImportRequest.InvoiceFileData> files) {
        Set<String> hashes = new HashSet<>();
        List<String> duplicates = new ArrayList<>();

        for (InvoiceImportRequest.InvoiceFileData file : files) {
            if (!hashes.add(file.fileHash())) {
                duplicates.add(file.fileName());
            }
        }

        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException("Arquivos duplicados na mesma requisição: " + duplicates);
        }
    }

    /**
     * Valida nome do arquivo
     */
    private void validateFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do arquivo é obrigatório");
        }

        if (!InvoiceUtils.hasPdfExtension(fileName)) {
            throw new InvalidFileFormatException(
                "Arquivo deve ter extensão .pdf",
                fileName,
                "PDF",
                getFileExtension(fileName)
            );
        }

        if (fileName.length() > 255) {
            throw new IllegalArgumentException("Nome do arquivo muito longo (máximo 255 caracteres)");
        }
    }

    /**
     * Valida conteúdo do arquivo
     */
    private void validateFileContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Conteúdo do arquivo é obrigatório");
        }

        if (content.length() < MIN_CONTENT_LENGTH) {
            throw new InvalidFileFormatException(
                "Arquivo muito pequeno para ser uma nota de corretagem válida",
                "N/A",
                "PDF com conteúdo mínimo",
                "Arquivo com " + content.length() + " caracteres"
            );
        }

        if (content.length() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Arquivo muito grande (máximo 10MB)");
        }
    }

    /**
     * Valida hash do arquivo
     */
    private void validateFileHash(String providedHash, String content) {
        if (providedHash == null || providedHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Hash do arquivo é obrigatório");
        }

        // Verifica se o hash tem formato válido (SHA-256 = 64 caracteres hex)
        if (!providedHash.matches("^[a-fA-F0-9]{64}$")) {
            throw new IllegalArgumentException("Hash deve ter formato SHA-256 válido (64 caracteres hexadecimais)");
        }

        // Verifica se o hash confere com o conteúdo
        String calculatedHash = InvoiceUtils.generateFileHash(content);
        if (!providedHash.equalsIgnoreCase(calculatedHash)) {
            throw new IllegalArgumentException("Hash do arquivo não confere com o conteúdo");
        }
    }

    /**
     * Valida formato PDF
     */
    private void validatePdfFormat(InvoiceImportRequest.InvoiceFileData fileData) {
        if (!InvoiceUtils.isPdfContent(fileData.fileContent())) {
            throw new InvalidFileFormatException(
                "Conteúdo não parece ser um PDF válido",
                fileData.fileName(),
                "PDF",
                "Formato não reconhecido"
            );
        }
    }

    /**
     * Valida estrutura básica do conteúdo
     */
    private void validateContentStructure(InvoiceImportRequest.InvoiceFileData fileData) {
        String content = fileData.fileContent().toUpperCase();
        List<String> missingKeywords = new ArrayList<>();

        // Verifica se contém palavras-chave essenciais
        for (String keyword : REQUIRED_PDF_KEYWORDS) {
            if (!content.contains(keyword)) {
                missingKeywords.add(keyword);
            }
        }

        if (!missingKeywords.isEmpty()) {
            log.warn("Arquivo {} não contém palavras-chave essenciais: {}", 
                     fileData.fileName(), missingKeywords);
            // Por enquanto só logga warning, não bloqueia
            // Futuramente pode ser mais rigoroso baseado no parser específico
        }
    }

    /**
     * Extrai extensão do arquivo
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "sem extensão";
        }
        
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
