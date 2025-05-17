import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  totalItems?: number;
  pageSize?: number;
  onPageChange: (page: number) => void;
  className?: string;
  variant?: 'default' | 'minimal' | 'compact';
  showStats?: boolean;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  totalItems,
  pageSize = 10,
  onPageChange,
  className = '',
  variant = 'default',
  showStats = true,
}) => {
  // Cálculo dos itens mostrados (se totalItems for fornecido)
  const startItem = totalItems ? (totalItems === 0 ? 0 : currentPage * pageSize + 1) : null;
  const endItem = totalItems ? Math.min((currentPage + 1) * pageSize, totalItems) : null;

  // Não mostrar paginação se houver apenas uma página
  if (totalPages <= 1) {
    if (showStats && totalItems !== undefined) {
      return (
        <div className={`flex items-center justify-between ${className}`}>
          <div className="text-sm text-gray-500">
            Mostrando {totalItems === 0 ? '0' : `1 a ${totalItems}`} de {totalItems} resultados
          </div>
          <div className="flex items-center space-x-2">
            <span className="px-3 py-1 border border-gray-200 bg-gray-50 rounded-md text-sm text-gray-700">
              1
            </span>
          </div>
        </div>
      );
    }
    return null;
  }

  // Gerar array de páginas com base no variant selecionado
  const getPageNumbers = (): Array<number | 'ellipsis'> => {
    const result: Array<number | 'ellipsis'> = [];
    
    // Versão compacta (apenas prev/next)
    if (variant === 'compact') {
      return [];
    }
    
    // Versão mínima (primeira, atual, última)
    if (variant === 'minimal') {
      result.push(0);
      
      if (currentPage > 1) {
        result.push('ellipsis');
      }
      
      if (currentPage > 0 && currentPage < totalPages - 1) {
        result.push(currentPage);
      }
      
      if (currentPage < totalPages - 2) {
        result.push('ellipsis');
      }
      
      if (totalPages > 1) {
        result.push(totalPages - 1);
      }
      
      return result;
    }
    
    // Versão padrão com mais páginas visíveis
    const maxPagesToShow = 7;

    if (totalPages <= maxPagesToShow) {
      // Mostrar todas as páginas se o total for pequeno
      for (let i = 0; i < totalPages; i++) {
        result.push(i);
      }
    } else {
      // Sempre mostrar a primeira página
      result.push(0);
      
      // Lógica para mostrar páginas ao redor da atual
      const leftSide = Math.floor((maxPagesToShow - 3) / 2);
      const rightSide = maxPagesToShow - leftSide - 3;
      
      // Se a página atual estiver próxima do início
      if (currentPage < leftSide + 1) {
        // Mostrar páginas do início até um certo ponto
        for (let i = 1; i < Math.min(maxPagesToShow - 2, totalPages - 1); i++) {
          result.push(i);
        }
        
        // Adicionar elipse se necessário
        if (totalPages > maxPagesToShow - 1) {
          result.push('ellipsis');
        }
      } 
      // Se a página atual estiver próxima do fim
      else if (currentPage >= totalPages - (rightSide + 1)) {
        // Adicionar elipse no início
        result.push('ellipsis');
        
        // Mostrar últimas páginas
        const startPage = Math.max(1, totalPages - (maxPagesToShow - 2));
        for (let i = startPage; i < totalPages - 1; i++) {
          result.push(i);
        }
      } 
      // Se a página atual estiver no meio
      else {
        // Elipse no início
        result.push('ellipsis');
        
        // Páginas ao redor da atual
        const startPage = Math.max(1, currentPage - leftSide);
        const endPage = Math.min(currentPage + rightSide, totalPages - 2);
        
        for (let i = startPage; i <= endPage; i++) {
          result.push(i);
        }
        
        // Elipse no fim
        if (endPage < totalPages - 2) {
          result.push('ellipsis');
        }
      }
      
      // Sempre mostrar a última página
      result.push(totalPages - 1);
    }
    
    return result;
  };

  // Renderização com base no variant
  return (
    <div className={`flex items-center ${showStats && totalItems ? 'justify-between' : 'justify-center'} ${className}`}>
      {/* Contador de itens */}
      {showStats && totalItems !== undefined && (
        <div className="text-sm text-gray-500">
          Mostrando {startItem} a {endItem} de {totalItems} resultados
        </div>
      )}

      {/* Paginação */}
      <div className="flex items-center space-x-2">
        {/* Botão anterior */}
        <button
          onClick={() => currentPage > 0 ? onPageChange(currentPage - 1) : undefined}
          disabled={currentPage === 0}
          className="flex items-center justify-center h-8 px-2 rounded-md border border-gray-300 text-gray-600 disabled:opacity-50 disabled:pointer-events-none hover:bg-gray-50"
        >
          <ChevronLeft className="h-4 w-4" />
          {variant !== 'compact' && <span className="ml-1">Anterior</span>}
        </button>

        {/* Números das páginas */}
        {variant !== 'compact' && getPageNumbers().map((pageNumber, index) => {
          if (pageNumber === 'ellipsis') {
            return (
              <span key={`ellipsis-${index}`} className="flex items-center justify-center w-8 h-8 text-gray-400">
                ...
              </span>
            );
          }

          return (
            <button
              key={`page-${pageNumber}-${index}`}
              onClick={() => onPageChange(pageNumber)}
              className={`flex items-center justify-center w-8 h-8 rounded-md ${
                currentPage === pageNumber
                  ? 'bg-indigo-600 text-white border border-indigo-600'
                  : 'text-gray-700 border border-gray-300 hover:bg-gray-50'
              }`}
            >
              {pageNumber + 1}
            </button>
          );
        })}

        {/* Botão próximo */}
        <button
          onClick={() => currentPage < totalPages - 1 ? onPageChange(currentPage + 1) : undefined}
          disabled={currentPage >= totalPages - 1}
          className="flex items-center justify-center h-8 px-2 rounded-md border border-gray-300 text-gray-600 disabled:opacity-50 disabled:pointer-events-none hover:bg-gray-50"
        >
          {variant !== 'compact' && <span className="mr-1">Próximo</span>}
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
};