package com.olisystem.optionsmanager.exception.invoice;

/**
 * Exceção lançada quando um arquivo já foi importado anteriormente
 */
public class DuplicateInvoiceException extends RuntimeException {

    private final String fileHash;
    private final String fileName;

    public DuplicateInvoiceException(String message, String fileHash, String fileName) {
        super(message);
        this.fileHash = fileHash;
        this.fileName = fileName;
    }

    public String getFileHash() {
        return fileHash;
    }

    public String getFileName() {
        return fileName;
    }
}
