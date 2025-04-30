import React from 'react';

interface PaginacaoProps {
  currentPage: number;
  totalPages: number;
  totalItems: number;
  pageSize: number;
  onPageChange: (page: number) => void;
}

export const Paginacao: React.FC<PaginacaoProps> = ({ 
  currentPage, 
  totalPages, 
  totalItems, 
  pageSize, 
  onPageChange 
}) => {
  return (
    <div className="px-6 py-3 flex items-center justify-between border-t border-gray-200">
      <div className="text-sm text-gray-500">
        Mostrando {totalItems === 0 ? 0 : currentPage * pageSize + 1} a {Math.min((currentPage + 1) * pageSize, totalItems)} de {totalItems} resultados
      </div>
      <div className="flex gap-2">
        <button 
          className="px-3 py-1 border border-gray-300 rounded-md text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-50"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 0}
        >
          Anterior
        </button>
        
        {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
          // Lógica para mostrar páginas ao redor da página atual
          let pageToShow;
          if (totalPages <= 5) {
            pageToShow = i;
          } else if (currentPage < 3) {
            pageToShow = i;
          } else if (currentPage > totalPages - 3) {
            pageToShow = totalPages - 5 + i;
          } else {
            pageToShow = currentPage - 2 + i;
          }
          
          return (
            <button
              key={pageToShow}
              onClick={() => onPageChange(pageToShow)}
              className={`px-3 py-1 border rounded-md text-sm ${
                currentPage === pageToShow
                  ? 'bg-purple-50 text-purple-600 border-purple-200'
                  : 'border-gray-300 text-gray-600 hover:bg-gray-50'
              }`}
            >
              {pageToShow + 1}
            </button>
          );
        })}
        
        <button 
          className="px-3 py-1 border border-gray-300 rounded-md text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-50"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
        >
          Próximo
        </button>
      </div>
    </div>
  );
};