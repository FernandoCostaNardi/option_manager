-- Migration para tabela de log de processamento de invoices
-- Criada em: 2025-07-03
-- Objetivo: Rastrear processamento de cada invoice e auditoria completa

CREATE TABLE invoice_processing_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    -- Status do processamento
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    
    -- Contadores de operações
    operations_created INTEGER DEFAULT 0 NOT NULL,
    operations_updated INTEGER DEFAULT 0 NOT NULL,
    operations_skipped INTEGER DEFAULT 0 NOT NULL,
    
    -- Detalhes do processamento
    processing_details JSONB,
    error_message TEXT,
    
    -- Controle de tempo
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    processing_duration_ms BIGINT,
    
    -- Auditoria
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para performance
CREATE INDEX idx_invoice_processing_log_invoice_id ON invoice_processing_log(invoice_id);
CREATE INDEX idx_invoice_processing_log_user_id ON invoice_processing_log(user_id);
CREATE INDEX idx_invoice_processing_log_status ON invoice_processing_log(status);
CREATE INDEX idx_invoice_processing_log_created_at ON invoice_processing_log(created_at);

-- Comentários
COMMENT ON TABLE invoice_processing_log IS 'Log de processamento de invoices para criação de operações';
COMMENT ON COLUMN invoice_processing_log.status IS 'Status: PENDING, PROCESSING, SUCCESS, PARTIAL_SUCCESS, ERROR';
COMMENT ON COLUMN invoice_processing_log.operations_created IS 'Número de novas operações criadas';
COMMENT ON COLUMN invoice_processing_log.operations_updated IS 'Número de operações existentes finalizadas';
COMMENT ON COLUMN invoice_processing_log.operations_skipped IS 'Número de operações ignoradas (duplicatas, etc)';
COMMENT ON COLUMN invoice_processing_log.processing_details IS 'Detalhes em JSON do processamento';
