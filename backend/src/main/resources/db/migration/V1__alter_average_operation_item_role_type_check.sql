-- Remover a restrição antiga
ALTER TABLE average_operation_item DROP CONSTRAINT IF EXISTS average_operation_item_role_type_check;

-- Adicionar a nova restrição com o valor NEW_ENTRY
ALTER TABLE average_operation_item ADD CONSTRAINT average_operation_item_role_type_check 
CHECK (role_type IN ('ORIGINAL', 'NEW_ENTRY', 'PARTIAL_EXIT', 'TOTAL_EXIT', 'CONSOLIDATED_ENTRY', 'CONSOLIDATED_RESULT')); 