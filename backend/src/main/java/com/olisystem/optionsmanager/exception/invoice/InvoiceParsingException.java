package com.olisystem.optionsmanager.exception.invoice;

/**
 * Exceção lançada quando há erro no parsing de uma nota de corretagem
 */
public class InvoiceParsingException extends RuntimeException {

    private final String fileName;
    private final String brokerageName;

    public InvoiceParsingException(String message, String fileName, String brokerageName) {
        super(message);
        this.fileName = fileName;
        this.brokerageName = brokerageName;
    }

    public InvoiceParsingException(String message, String fileName, String brokerageName, Throwable cause) {
        super(message, cause);
        this.fileName = fileName;
        this.brokerageName = brokerageName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getBrokerageName() {
        return brokerageName;
    }
}
