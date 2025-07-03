-- ===================================================================
-- Scripts SQL úteis para consultas no sistema de importação de invoices
-- ===================================================================

-- 1. CONSULTAS BÁSICAS DE MONITORAMENTO

-- Total de notas importadas por usuário
SELECT 
    u.username,
    u.email,
    COUNT(i.id) as total_invoices,
    MIN(i.trading_date) as primeira_nota,
    MAX(i.trading_date) as ultima_nota,
    COUNT(DISTINCT i.brokerage_id) as corretoras_diferentes
FROM invoices i
JOIN users u ON i.user_id = u.id
GROUP BY u.id, u.username, u.email
ORDER BY total_invoices DESC;

-- Notas importadas por corretora
SELECT 
    b.name as corretora,
    COUNT(i.id) as total_notas,
    SUM(i.gross_operations_value) as valor_total_operacoes,
    AVG(i.gross_operations_value) as valor_medio_por_nota
FROM invoices i
JOIN brokerages b ON i.brokerage_id = b.id
GROUP BY b.id, b.name
ORDER BY total_notas DESC;

-- Importações por data
SELECT 
    DATE(i.imported_at) as data_importacao,
    COUNT(i.id) as notas_importadas,
    COUNT(DISTINCT i.user_id) as usuarios_diferentes
FROM invoices i
WHERE i.imported_at >= NOW() - INTERVAL '30 days'
GROUP BY DATE(i.imported_at)
ORDER BY data_importacao DESC;

-- ===================================================================
-- 2. CONSULTAS DE ANÁLISE DE DADOS

-- Estatísticas de operações por tipo
SELECT 
    ii.operation_type,
    ii.market_type,
    COUNT(ii.id) as total_operacoes,
    SUM(ii.quantity) as quantidade_total,
    SUM(ii.total_value) as valor_total,
    AVG(ii.unit_price) as preco_medio
FROM invoice_items ii
JOIN invoices i ON ii.invoice_id = i.id
GROUP BY ii.operation_type, ii.market_type
ORDER BY total_operacoes DESC;

-- Análise de Day Trade
SELECT 
    DATE(i.trading_date) as data_pregao,
    COUNT(CASE WHEN ii.is_day_trade = true THEN 1 END) as operacoes_day_trade,
    COUNT(CASE WHEN ii.is_day_trade = false THEN 1 END) as operacoes_swing,
    ROUND(
        COUNT(CASE WHEN ii.is_day_trade = true THEN 1 END) * 100.0 / COUNT(ii.id), 
        2
    ) as percentual_day_trade
FROM invoices i
JOIN invoice_items ii ON i.id = ii.invoice_id
WHERE i.trading_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(i.trading_date)
ORDER BY data_pregao DESC;

-- Assets mais negociados
SELECT 
    ii.asset_code,
    COUNT(ii.id) as vezes_negociado,
    SUM(ii.quantity) as quantidade_total,
    SUM(ii.total_value) as volume_financeiro,
    COUNT(DISTINCT i.user_id) as usuarios_diferentes
FROM invoice_items ii
JOIN invoices i ON ii.invoice_id = i.id
WHERE ii.asset_code IS NOT NULL
GROUP BY ii.asset_code
HAVING COUNT(ii.id) >= 5  -- Pelo menos 5 operações
ORDER BY volume_financeiro DESC
LIMIT 20;

-- ===================================================================
-- 3. CONSULTAS DE VALIDAÇÃO E QUALIDADE

-- Notas sem itens (possível problema de parsing)
SELECT 
    i.id,
    i.invoice_number,
    i.trading_date,
    b.name as corretora,
    u.username
FROM invoices i
LEFT JOIN invoice_items ii ON i.id = ii.invoice_id
JOIN brokerages b ON i.brokerage_id = b.id
JOIN users u ON i.user_id = u.id
WHERE ii.id IS NULL;

-- Itens com dados inconsistentes
SELECT 
    ii.id,
    i.invoice_number,
    ii.asset_specification,
    ii.quantity,
    ii.unit_price,
    ii.total_value,
    (ii.quantity * ii.unit_price) as valor_calculado,
    ABS(ii.total_value - (ii.quantity * ii.unit_price)) as diferenca
FROM invoice_items ii
JOIN invoices i ON ii.invoice_id = i.id
WHERE ABS(ii.total_value - (ii.quantity * ii.unit_price)) > 0.01  -- Diferença > 1 centavo
ORDER BY diferenca DESC;

-- Hashes duplicados (não deveria existir devido à constraint)
SELECT 
    file_hash,
    COUNT(*) as duplicatas,
    STRING_AGG(invoice_number, ', ') as numeros_nota
FROM invoices
WHERE file_hash IS NOT NULL
GROUP BY file_hash
HAVING COUNT(*) > 1;


-- ===================================================================
-- 4. CONSULTAS DE PERFORMANCE

-- Notas com mais itens (possível problema de parsing)
SELECT 
    i.id,
    i.invoice_number,
    i.trading_date,
    b.name as corretora,
    COUNT(ii.id) as total_itens
FROM invoices i
JOIN invoice_items ii ON i.id = ii.invoice_id
JOIN brokerages b ON i.brokerage_id = b.id
GROUP BY i.id, i.invoice_number, i.trading_date, b.name
ORDER BY total_itens DESC
LIMIT 10;

-- ===================================================================
-- 5. RELATÓRIOS PARA USUÁRIOS

-- Resumo financeiro por usuário e período
CREATE OR REPLACE VIEW v_user_financial_summary AS
SELECT 
    u.username,
    u.email,
    DATE_TRUNC('month', i.trading_date) as mes_ano,
    COUNT(i.id) as total_notas,
    SUM(i.gross_operations_value) as valor_bruto_operacoes,
    SUM(i.total_costs) as total_custos,
    SUM(i.total_taxes) as total_impostos,
    SUM(i.net_settlement_value) as valor_liquido_liquidacao
FROM invoices i
JOIN users u ON i.user_id = u.id
GROUP BY u.id, u.username, u.email, DATE_TRUNC('month', i.trading_date)
ORDER BY u.username, mes_ano DESC;

-- Resumo de operações por ativo
CREATE OR REPLACE VIEW v_asset_operations_summary AS
SELECT 
    ii.asset_code,
    ii.market_type,
    COUNT(CASE WHEN ii.operation_type = 'C' THEN 1 END) as total_compras,
    COUNT(CASE WHEN ii.operation_type = 'V' THEN 1 END) as total_vendas,
    SUM(CASE WHEN ii.operation_type = 'C' THEN ii.quantity ELSE 0 END) as quantidade_comprada,
    SUM(CASE WHEN ii.operation_type = 'V' THEN ii.quantity ELSE 0 END) as quantidade_vendida,
    SUM(CASE WHEN ii.operation_type = 'C' THEN ii.total_value ELSE 0 END) as valor_compras,
    SUM(CASE WHEN ii.operation_type = 'V' THEN ii.total_value ELSE 0 END) as valor_vendas
FROM invoice_items ii
WHERE ii.asset_code IS NOT NULL
GROUP BY ii.asset_code, ii.market_type
ORDER BY ii.asset_code;

-- ===================================================================
-- COMENTÁRIOS FINAIS:
-- 
-- Estes scripts são úteis para:
-- 1. Monitorar a saúde do sistema de importação
-- 2. Validar qualidade dos dados importados
-- 3. Gerar relatórios para usuários
-- 4. Identificar problemas de parsing
-- 5. Análise de performance
-- 
-- Use com cuidado em ambiente de produção e sempre teste em ambiente de desenvolvimento primeiro.
