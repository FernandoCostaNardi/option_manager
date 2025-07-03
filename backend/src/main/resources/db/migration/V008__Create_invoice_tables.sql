-- ===================================================================
-- Migration V008: Criação das tabelas para importação de notas de corretagem
-- Data: 2025-06-30
-- ===================================================================

-- Tabela principal de notas de corretagem (invoices)
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brokerage_id UUID NOT NULL,
    user_id UUID NOT NULL,
    
    -- Metadados da Nota
    invoice_number VARCHAR(50) NOT NULL,
    trading_date DATE NOT NULL,
    settlement_date DATE,
    
    -- Cliente
    client_name VARCHAR(255),
    client_cpf VARCHAR(20),
    client_code VARCHAR(50),
    
    -- Resumo Financeiro
    gross_operations_value DECIMAL(15,4) DEFAULT 0.00,
    net_operations_value DECIMAL(15,4) DEFAULT 0.00,
    total_costs DECIMAL(15,4) DEFAULT 0.00,
    total_taxes DECIMAL(15,4) DEFAULT 0.00,
    net_settlement_value DECIMAL(15,4) DEFAULT 0.00,
    
    -- Impostos e Taxas
    liquidation_tax DECIMAL(15,4) DEFAULT 0.00,
    registration_tax DECIMAL(15,4) DEFAULT 0.00,
    emoluments DECIMAL(15,4) DEFAULT 0.00,
    ana_tax DECIMAL(15,4) DEFAULT 0.00,
    term_options_tax DECIMAL(15,4) DEFAULT 0.00,
    brokerage_fee DECIMAL(15,4) DEFAULT 0.00,
    iss DECIMAL(15,4) DEFAULT 0.00,
    pis DECIMAL(15,4) DEFAULT 0.00,
    cofins DECIMAL(15,4) DEFAULT 0.00
);    
    -- IRRF
    irrf_day_trade_basis DECIMAL(15,4) DEFAULT 0.00,
    irrf_day_trade_value DECIMAL(15,4) DEFAULT 0.00,
    irrf_common_basis DECIMAL(15,4) DEFAULT 0.00,
    irrf_common_value DECIMAL(15,4) DEFAULT 0.00,
    
    -- Dados Brutos
    raw_content TEXT,
    file_hash VARCHAR(255) UNIQUE,
    
    -- Controle
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    FOREIGN KEY (brokerage_id) REFERENCES brokerages(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    
    -- Índices para consultas frequentes
    CONSTRAINT unique_invoice_per_brokerage UNIQUE (brokerage_id, invoice_number, trading_date)
);

-- Tabela de itens da nota de corretagem (operações individuais)
CREATE TABLE invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL,
    
    -- Dados da Operação
    sequence_number INTEGER NOT NULL,
    operation_type VARCHAR(10) NOT NULL, -- 'C' (Compra) ou 'V' (Venda)
    market_type VARCHAR(50), -- 'VISTA', 'OPCAO DE COMPRA', 'OPCAO DE VENDA', etc.
    asset_specification VARCHAR(255) NOT NULL, -- Código completo do ativo/opção
    asset_code VARCHAR(20), -- Código extraído (ex: PETR4, CSANE165)
    expiration_date DATE, -- Para opções
    strike_price DECIMAL(15,4), -- Para opções
    
    -- Quantidades e Preços
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(15,4) NOT NULL,
    total_value DECIMAL(15,4) NOT NULL,
    
    -- Observações
    observations VARCHAR(255), -- 'D' para Day Trade, '#' negócio direto, etc.
    is_day_trade BOOLEAN DEFAULT FALSE,
    is_direct_deal BOOLEAN DEFAULT FALSE,
    
    -- Controle
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

-- Índices para performance
CREATE INDEX idx_invoices_brokerage_user ON invoices(brokerage_id, user_id);
CREATE INDEX idx_invoices_trading_date ON invoices(trading_date);
CREATE INDEX idx_invoices_file_hash ON invoices(file_hash);
CREATE INDEX idx_invoice_items_invoice_id ON invoice_items(invoice_id);
CREATE INDEX idx_invoice_items_asset_code ON invoice_items(asset_code);
CREATE INDEX idx_invoice_items_sequence ON invoice_items(invoice_id, sequence_number);

-- Comentários para documentação
COMMENT ON TABLE invoices IS 'Tabela principal para armazenar notas de corretagem importadas';
COMMENT ON TABLE invoice_items IS 'Itens/operações individuais dentro de cada nota de corretagem';
COMMENT ON COLUMN invoices.file_hash IS 'Hash MD5/SHA256 do arquivo original para evitar duplicatas';
COMMENT ON COLUMN invoice_items.observations IS 'Campo para observações da corretora (D=Day Trade, #=Negócio Direto)';
