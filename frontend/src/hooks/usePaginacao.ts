import { useState, useCallback } from 'react';

export const usePaginacao = (pageSize = 5) => {
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);

  const resetPage = useCallback(() => {
    setCurrentPage(0);
  }, []);

  const paginate = useCallback((pageNumber: number) => {
    setCurrentPage(pageNumber);
  }, []);

  return {
    currentPage,
    totalPages,
    totalItems,
    pageSize,
    setTotalPages,
    setTotalItems,
    resetPage,
    paginate
  };
};