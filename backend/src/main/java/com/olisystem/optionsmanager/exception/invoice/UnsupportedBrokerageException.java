package com.olisystem.optionsmanager.exception.invoice;

/**
 * Exceção lançada quando não há parser disponível para uma corretora específica
 */
public class UnsupportedBrokerageException extends RuntimeException {

    private final String brokerageName;
    private final String brokerageId;

    public UnsupportedBrokerageException(String message, String brokerageName, String brokerageId) {
        super(message);
        this.brokerageName = brokerageName;
        this.brokerageId = brokerageId;
    }

    public String getBrokerageName() {
        return brokerageName;
    }

    public String getBrokerageId() {
        return brokerageId;
    }
}
