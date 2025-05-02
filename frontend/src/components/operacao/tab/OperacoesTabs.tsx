import React, { useEffect } from 'react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../ui/tabs';
import { LoadingSpinner } from '../../LoadingSpinner';
import { ErrorMessage } from '../../ErrorMessage';
import { OperacoesAtivasTable } from '../tables/OperacoesAtivasTable';
import { OperacoesFinalizadasTable } from '../tables/OperacoesFinalizadasTable';
import { Paginacao } from '../../Paginacao';
import { OperacaoAtiva, OperacaoFinalizada, SortField, SortDirection } from '../../../types/operacao/operacoes.types';
import { useNavigate } from 'react-router-dom';

interface OperacoesTabsProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  operacoesAtivas: OperacaoAtiva[];
  operacoesFinalizadas: OperacaoFinalizada[];
  loadingAtivas: boolean;
  loadingFinalizadas: boolean;
  error: string | null;
  sortField: SortField;
  sortDirection: SortDirection;
  onSort: (field: SortField) => void;
  currentPage: number;
  totalPages: number;
  totalItems: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  onEdit: (id: string) => void;
  onFinalize: (id: string) => void;
  onRemove: (id: string) => void;
  onView: (id: string) => void;
  onViewTargets: (id: string) => void;
  renderAtivasCards?: (isLoading: boolean) => React.ReactNode;
  renderFinalizadasCards?: (isLoading: boolean) => React.ReactNode;
}

export const OperacoesTabs: React.FC<OperacoesTabsProps> = ({
  activeTab,
  setActiveTab,
  operacoesAtivas,
  operacoesFinalizadas,
  loadingAtivas,
  loadingFinalizadas,
  error,
  sortField,
  sortDirection,
  onSort,
  currentPage,
  totalPages,
  totalItems,
  pageSize,
  onPageChange,
  onEdit,
  onFinalize,
  onRemove,
  onView,
  onViewTargets,
  renderAtivasCards,
  renderFinalizadasCards
}) => {
  const navigate = useNavigate();
  
  // Verificar se o erro está relacionado a token expirado
  useEffect(() => {
    if (error && (
      error.includes('token expirado') || 
      error.includes('token inválido') || 
      error.includes('não autorizado') ||
      error.includes('unauthorized') ||
      error.includes('401')
    )) {
      // Limpar o token do localStorage
      localStorage.removeItem('token');
      
      // Redirecionar para a página de login
      navigate('/login', { 
        state: { 
          from: window.location.pathname,
          message: 'Sua sessão expirou. Por favor, faça login novamente.' 
        } 
      });
    }
  }, [error, navigate]);

  // Verificar token ao mudar de aba
  const handleTabChange = (value: string) => {
    // Verificar se o token existe
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login', { 
        state: { 
          from: window.location.pathname,
          message: 'Você precisa estar logado para acessar esta página.' 
        } 
      });
      return;
    }
    
    // Se o token existir, mudar a aba
    setActiveTab(value);
  };

  return (
    <Tabs value={activeTab} onValueChange={handleTabChange} className="w-full">
      <TabsList className="mb-4">
        <TabsTrigger 
          value="ativas" 
          className={`px-4 py-2 ${activeTab === 'ativas' ? 'bg-purple-100' : ''}`}
        >
          Operações Ativas
        </TabsTrigger>
        <TabsTrigger 
          value="finalizadas" 
          className={`px-4 py-2 ${activeTab === 'finalizadas' ? 'bg-purple-100' : ''}`}
        >
          Operações Finalizadas
        </TabsTrigger>
      </TabsList>
      
      <TabsContent value="ativas">
        {loadingAtivas ? (
          <LoadingSpinner message="Carregando operações ativas..." />
        ) : error && !error.includes('token') && !error.includes('unauthorized') && !error.includes('401') ? (
          <ErrorMessage message={error} />
        ) : (
          <>
            {/* Renderizar os cards de dashboard para operações ativas */}
            {renderAtivasCards && renderAtivasCards(loadingAtivas)}
            
            <div className="bg-white rounded-xl shadow-sm overflow-hidden border border-gray-100">
              <div className="px-6 py-4 border-b border-gray-100">
                <h2 className="font-semibold text-gray-800">Operações Ativas</h2>
              </div>
              <OperacoesAtivasTable
                operacoes={operacoesAtivas}
                sortField={sortField}
                sortDirection={sortDirection}
                onSort={onSort}
                onEdit={onEdit}
                onFinalize={onFinalize}
                onRemove={onRemove}
                onViewTargets={onViewTargets}
              />
              <Paginacao
                currentPage={currentPage}
                totalPages={totalPages}
                totalItems={totalItems}
                pageSize={pageSize}
                onPageChange={onPageChange}
              />
            </div>
          </>
        )}
      </TabsContent>
      
      <TabsContent value="finalizadas">
        {loadingFinalizadas ? (
          <LoadingSpinner message="Carregando operações finalizadas..." />
        ) : error && !error.includes('token') && !error.includes('unauthorized') && !error.includes('401') ? (
          <ErrorMessage message={error} />
        ) : (
          <>
            {/* Renderizar os cards de dashboard para operações finalizadas */}
            {renderFinalizadasCards && renderFinalizadasCards(loadingFinalizadas)}
            
            <div className="bg-white rounded-xl shadow-sm overflow-hidden border border-gray-100">
              <div className="px-6 py-4 border-b border-gray-100">
                <h2 className="font-semibold text-gray-800">Operações Finalizadas</h2>
              </div>
              <OperacoesFinalizadasTable
                operacoes={operacoesFinalizadas}
                loading={loadingFinalizadas}
                sortField={sortField}
                sortDirection={sortDirection}
                onSort={onSort}
                onView={onView}
                onRemove={onRemove}
                onViewTargets={onViewTargets}
              />
              <Paginacao
                currentPage={currentPage}
                totalPages={totalPages}
                totalItems={totalItems}
                pageSize={pageSize}
                onPageChange={onPageChange}
              />
            </div>
          </>
        )}
      </TabsContent>
    </Tabs>
  );
};