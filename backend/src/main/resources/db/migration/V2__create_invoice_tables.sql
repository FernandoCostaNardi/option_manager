-- Migration V2: Criar tabelas para importação de notas de corretagem (invoices)

-- Criar tabela principal de invoices
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brokerage_id UUID REFERENCES brokerages(id),
    user_id UUID REFERENCES users(id),
    
    -- Metadados da Nota
    invoice_number VARCHAR(50),
    trading_date DATE,
    settlement_date DATE,
    
    -- Dados do Cliente
    client_name VARCHAR(255),
    client_cpf VARCHAR(20),
    client_code VARCHAR(50),
    
    -- Resumo Financeiro
    gross_operations_value DECIMAL(15,4),
    net_operations_value DECIMAL(15,4),
    total_costs DECIMAL(15,4),
    total_taxes DECIMAL(15,4),
    net_settlement_value DECIMAL(15,4),
    
    -- Impostos e Taxas
    liquidation_tax DECIMAL(15,4),
    registration_tax DECIMAL(15,4),
    emoluments DECIMAL(15,4),
    ana_tax DECIMAL(15,4),
    term_options_tax DECIMAL(15,4),
    brokerage_fee DECIMAL(15,4),
    iss DECIMAL(15,4),
    pis DECIMAL(15,4),
    cofins DECIMAL(15,4),
    
    -- IRRF
    irrf_day_trade_basis DECIMAL(15,4),
    irrf_day_trade_value DECIMAL(15,4),
    irrf_common_basis DECIMAL(15,4),
    irrf_common_value DECIMAL(15,4),
    
    -- Dados Brutos
    raw_content TEXT,
    file_hash VARCHAR(255),
    
    -- Campos Legados (compatibilidade)
    trade_date DATE,
    total_value DOUBLE PRECISION,
    total_fees DOUBLE PRECISION,
    
    -- Controle
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Criar tabela de itens da invoice
CREATE TABLE invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    
    -- Dados da Operação
    sequence_number INTEGER,
    operation_type VARCHAR(10), -- 'C' (Compra) ou 'V' (Venda)
    market_type VARCHAR(50), -- 'VISTA', 'OPCAO DE COMPRA', etc.
    asset_specification VARCHAR(255), -- Código completo do ativo/opção
    asset_code VARCHAR(20), -- Código extraído (ex: PETR4, CSANE165)
    expiration_date DATE, -- Para opções
    strike_price DECIMAL(15,4), -- Para opções
    
    -- Quantidades e Preços
    quantity INTEGER,
    unit_price DECIMAL(15,4),
    total_value DECIMAL(15,4),
    
    -- Observações
    observations VARCHAR(255), -- 'D' para Day Trade, '#' negócio direto, etc.
    is_day_trade BOOLEAN DEFAULT FALSE,
    is_direct_deal BOOLEAN DEFAULT FALSE,
    
    -- Campos Legados (compatibilidade)
    asset VARCHAR(255),
    price DOUBLE PRECISION,
    operation_value DOUBLE PRECISION,
    
    -- Controle
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Criar índices para performance
CREATE INDEX idx_invoices_brokerage_id ON invoices(brokerage_id);
CREATE INDEX idx_invoices_user_id ON invoices(user_id);
CREATE INDEX idx_invoices_trading_date ON invoices(trading_date);
CREATE INDEX idx_invoices_file_hash ON invoices(file_hash);
CREATE INDEX idx_invoices_invoice_number ON invoices(invoice_number);

CREATE INDEX idx_invoice_items_invoice_id ON invoice_items(invoice_id);
CREATE INDEX idx_invoice_items_asset_code ON invoice_items(asset_code);
CREATE INDEX idx_invoice_items_operation_type ON invoice_items(operation_type);
CREATE INDEX idx_invoice_items_is_day_trade ON invoice_items(is_day_trade);

-- Trigger para atualizar updated_at automaticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_invoices_updated_at 
    BEFORE UPDATE ON invoices 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_invoice_items_updated_at 
    BEFORE UPDATE ON invoice_items 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comentários nas tabelas
COMMENT ON TABLE invoices IS 'Tabela principal para armazenar notas de corretagem importadas';
COMMENT ON TABLE invoice_items IS 'Itens/operações individuais dentro de cada nota de corretagem';

COMMENT ON COLUMN invoices.file_hash IS 'Hash MD5/SHA256 do arquivo PDF para evitar duplicatas';
COMMENT ON COLUMN invoices.raw_content IS 'Conteúdo completo extraído do PDF para auditoria';
COMMENT ON COLUMN invoice_items.sequence_number IS 'Ordem da operação dentro da nota';
COMMENT ON COLUMN invoice_items.is_day_trade IS 'Calculado automaticamente baseado em datas e observações';
