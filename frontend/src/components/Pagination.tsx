import React from 'react';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
}) => {
  // Não mostrar paginação se tivermos apenas uma página
  if (totalPages <= 1) return (
    <div className="flex items-center justify-center space-x-2">
      <button
        disabled={true}
        className="flex items-center justify-center w-8 h-8 rounded-full text-gray-300 cursor-not-allowed"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          viewBox="0 0 20 20"
          fill="currentColor"
        >
          <path
            fillRule="evenodd"
            d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z"
            clipRule="evenodd"
          />
        </svg>
      </button>
      
      <button
        className="flex items-center justify-center w-8 h-8 rounded-full bg-indigo-600 text-white font-medium"
      >
        1
      </button>
      
      <button
        disabled={true}
        className="flex items-center justify-center w-8 h-8 rounded-full text-gray-300 cursor-not-allowed"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          viewBox="0 0 20 20"
          fill="currentColor"
        >
          <path
            fillRule="evenodd"
            d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z"
            clipRule="evenodd"
          />
        </svg>
      </button>
    </div>
  );

  // Gerar array de páginas
  const getPageNumbers = (): Array<number> => {
    const pageNumbers: number[] = [];
    const maxPagesToShow = 7; // Ajuste conforme necessário

    if (totalPages <= maxPagesToShow) {
      // Mostrar todas as páginas se o total for pequeno
      for (let i = 0; i < totalPages; i++) {
        pageNumbers.push(i);
      }
    } else {
      // Mostrar páginas com elipses
      const leftSide = Math.floor(maxPagesToShow / 2);
      const rightSide = maxPagesToShow - leftSide;

      // Página atual está próxima do início
      if (currentPage < leftSide) {
        for (let i = 0; i < maxPagesToShow - 1; i++) {
          pageNumbers.push(i);
        }
        pageNumbers.push(-1); // Elipse
        pageNumbers.push(totalPages - 1); // Última página
      } 
      // Página atual está próxima do fim
      else if (currentPage >= totalPages - rightSide) {
        pageNumbers.push(0); // Primeira página
        pageNumbers.push(-1); // Elipse
        for (let i = totalPages - (maxPagesToShow - 1); i < totalPages; i++) {
          pageNumbers.push(i);
        }
      } 
      // Página atual está no meio
      else {
        pageNumbers.push(0); // Primeira página
        pageNumbers.push(-1); // Elipse
        for (let i = currentPage - 1; i <= currentPage + 1; i++) {
          pageNumbers.push(i);
        }
        pageNumbers.push(-1); // Elipse
        pageNumbers.push(totalPages - 1); // Última página
      }
    }

    return pageNumbers;
  };

  return (
    <div className="flex items-center justify-center space-x-2">
      {/* Botão anterior */}
      <button
        onClick={() => currentPage > 0 ? onPageChange(currentPage - 1) : undefined}
        disabled={currentPage === 0}
        className="flex items-center justify-center w-8 h-8 rounded-full text-gray-500 disabled:opacity-50"
        aria-label="Página anterior"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          viewBox="0 0 20 20"
          fill="currentColor"
        >
          <path
            fillRule="evenodd"
            d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z"
            clipRule="evenodd"
          />
        </svg>
      </button>

      {/* Números de página */}
      {getPageNumbers().map((pageNumber, index) => {
        if (pageNumber === -1) {
          // Renderiza elipse
          return (
            <span key={`ellipsis-${index}`} className="px-2">
              ...
            </span>
          );
        }

        return (
          <button
            key={`page-${pageNumber}-${index}`}
            onClick={() => onPageChange(pageNumber)}
            className={`flex items-center justify-center w-8 h-8 rounded-full ${
              currentPage === pageNumber
                ? 'bg-indigo-600 text-white font-medium'
                : 'text-gray-700 hover:bg-gray-100'
            }`}
            aria-label={`Ir para página ${pageNumber + 1}`}
            aria-current={currentPage === pageNumber ? 'page' : undefined}
          >
            {pageNumber + 1}
          </button>
        );
      })}

      {/* Botão próximo */}
      <button
        onClick={() => currentPage < totalPages - 1 ? onPageChange(currentPage + 1) : undefined}
        disabled={currentPage >= totalPages - 1}
        className="flex items-center justify-center w-8 h-8 rounded-full text-gray-500 disabled:opacity-50"
        aria-label="Próxima página"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          viewBox="0 0 20 20"
          fill="currentColor"
        >
          <path
            fillRule="evenodd"
            d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z"
            clipRule="evenodd"
          />
        </svg>
      </button>
    </div>
  );
};