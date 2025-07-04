-- Migration para mapeamento entre Operations e InvoiceItems
-- Criada em: 2025-07-03
-- Objetivo: Rastrear qual InvoiceItem originou qual Operation

CREATE TABLE operation_source_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Relacionamentos principais
    operation_id UUID NOT NULL REFERENCES operations(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    invoice_item_id UUID NOT NULL REFERENCES invoice_items(id) ON DELETE CASCADE,
    
    -- Tipo de mapeamento
    mapping_type VARCHAR(50) NOT NULL,
    
    -- Metadados adicionais
    processing_sequence INTEGER,
    notes TEXT,
    
    -- Auditoria
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para performance e integridade
CREATE INDEX idx_operation_source_mapping_operation_id ON operation_source_mapping(operation_id);
CREATE INDEX idx_operation_source_mapping_invoice_id ON operation_source_mapping(invoice_id);
CREATE INDEX idx_operation_source_mapping_invoice_item_id ON operation_source_mapping(invoice_item_id);
CREATE INDEX idx_operation_source_mapping_type ON operation_source_mapping(mapping_type);

-- Constraint para evitar duplicatas
CREATE UNIQUE INDEX idx_operation_source_mapping_unique 
ON operation_source_mapping(operation_id, invoice_item_id);

-- Comentários
COMMENT ON TABLE operation_source_mapping IS 'Mapeamento entre Operations criadas e InvoiceItems que as originaram';
COMMENT ON COLUMN operation_source_mapping.mapping_type IS 'Tipo: NEW_OPERATION, EXISTING_OPERATION_EXIT, DAY_TRADE_ENTRY, DAY_TRADE_EXIT';
COMMENT ON COLUMN operation_source_mapping.processing_sequence IS 'Ordem de processamento dentro do batch';
COMMENT ON COLUMN operation_source_mapping.notes IS 'Observações sobre o mapeamento';
