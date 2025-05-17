-- Criar uma coluna temporária para armazenar os valores ordinais
ALTER TABLE average_operation_item ADD COLUMN role_type_ordinal INTEGER;

-- Converter os valores existentes para ordinais
UPDATE average_operation_item SET role_type_ordinal = 0 WHERE role_type = 'ORIGINAL';
UPDATE average_operation_item SET role_type_ordinal = 1 WHERE role_type = 'NEW_ENTRY';
UPDATE average_operation_item SET role_type_ordinal = 2 WHERE role_type = 'PARTIAL_EXIT';
UPDATE average_operation_item SET role_type_ordinal = 3 WHERE role_type = 'TOTAL_EXIT';
UPDATE average_operation_item SET role_type_ordinal = 4 WHERE role_type = 'CONSOLIDATED_ENTRY';
UPDATE average_operation_item SET role_type_ordinal = 5 WHERE role_type = 'CONSOLIDATED_RESULT';

-- Remover a coluna antiga
ALTER TABLE average_operation_item DROP COLUMN role_type;

-- Renomear a nova coluna
ALTER TABLE average_operation_item RENAME COLUMN role_type_ordinal TO role_type; 