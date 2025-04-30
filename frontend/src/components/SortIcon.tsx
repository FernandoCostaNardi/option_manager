import React from 'react';
import { ArrowUp, ArrowDown } from 'lucide-react';
import { SortField, SortDirection } from '../types/operacao/operacoes.types';

interface SortIconProps {
  currentField: SortField;
  field: SortField;
  direction: SortDirection;
}

export const SortIcon: React.FC<SortIconProps> = ({ currentField, field, direction }) => {
  if (currentField !== field) return null;
  
  return direction === 'asc' 
    ? <ArrowUp className="h-3 w-3 ml-1 inline" /> 
    : <ArrowDown className="h-3 w-3 ml-1 inline" />;
};