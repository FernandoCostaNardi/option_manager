-- Migration para adicionar campo de contagem de reprocessamento
-- Criada em: 2025-07-17
-- Objetivo: Rastrear quantas vezes uma invoice foi reprocessada

ALTER TABLE invoice_processing_log 
ADD COLUMN reprocessed_count INTEGER DEFAULT 0 NOT NULL;

-- Comentário
COMMENT ON COLUMN invoice_processing_log.reprocessed_count IS 'Número de vezes que esta invoice foi reprocessada';

-- Índice para consultas por reprocessamento
CREATE INDEX idx_invoice_processing_log_reprocessed_count ON invoice_processing_log(reprocessed_count); 