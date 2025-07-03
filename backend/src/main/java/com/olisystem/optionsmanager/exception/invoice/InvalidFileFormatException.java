package com.olisystem.optionsmanager.exception.invoice;

/**
 * Exceção lançada quando o arquivo não é um PDF válido ou tem formato inválido
 */
public class InvalidFileFormatException extends RuntimeException {

    private final String fileName;
    private final String expectedFormat;
    private final String actualFormat;

    public InvalidFileFormatException(String message, String fileName, String expectedFormat, String actualFormat) {
        super(message);
        this.fileName = fileName;
        this.expectedFormat = expectedFormat;
        this.actualFormat = actualFormat;
    }

    public String getFileName() {
        return fileName;
    }

    public String getExpectedFormat() {
        return expectedFormat;
    }

    public String getActualFormat() {
        return actualFormat;
    }
}
