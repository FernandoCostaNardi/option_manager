import { useState, useCallback } from 'react';
import { SortField, SortDirection } from '../../types/operacao/operacoes.types'

export const useOrdenacao = (resetPage: () => void) => {
  const [sortField, setSortField] = useState<SortField>('optionSerieCode');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');

  const handleSort = useCallback((field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
    resetPage();
  }, [sortField, sortDirection, resetPage]);

  return {
    sortField,
    sortDirection,
    handleSort
  };
};