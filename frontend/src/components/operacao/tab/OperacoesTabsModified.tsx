import React, { useEffect, useState } from 'react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../ui/tabs';
import { LoadingSpinner } from '../../LoadingSpinner';
import { ErrorMessage } from '../../ErrorMessage';
import { OperacoesAtivasTable } from '../tables/OperacoesAtivasTable';
import { OperacoesFinalizadasTable } from '../tables/OperacoesFinalizadasTable';
import { Paginacao } from '../../Paginacao';
import { OperacaoAtiva, OperacaoFinalizada, SortField, SortDirection } from '../../../types/operacao/operacoes.types';
import { useNavigate } from 'react-router-dom';
import { useFiltros } from '../../../hooks/operacao/useFiltros';
import { OperacaoService } from '../../../services/operacaoService';
import { Download } from 'lucide-react';
import { jwtDecode } from 'jwt-decode';
import { FinalizarOperacaoModal } from '../modals/FinalizarOperacaoModal';
import { Posicao } from '../../../types/posicao/posicoes.types';
import { PosicoesTab } from '../../posicao/tab/PosicoesTab';
import { PosicoesDashboard } from '../../../services/posicaoService';

interface OperacoesTabsProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  
  // Props para operações ativas
  operacoesAtivas: OperacaoAtiva[];
  loadingAtivas: boolean;
  
  // Props para operações finalizadas
  operacoesFinalizadas: OperacaoFinalizada[];
  loadingFinalizadas: boolean;
  
  // Props para posições
  posicoes?: Posicao[];
  posicoesDashboardData?: PosicoesDashboard;
  loadingPosicoes?: boolean;
  onRefreshPosicoes?: () => void;
  
  // Props comuns
  error: string | null;
  sortField: SortField;
  sortDirection: SortDirection;
  onSort: (field: SortField) => void;
  currentPage: number;
  totalPages: number;
  totalItems: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  
  // Handlers
  onEdit: (id: string) => void;
  onFinalize: (id: string) => void;
  onRemove: (id: string) => void;
  onView: (id: string) => void;
  onViewTargets: (id: string) => void;
  
  // Renderização customizada
  renderAtivasCards?: (isLoading: boolean) => React.ReactNode;
  renderFinalizadasCards?: (isLoading: boolean) => React.ReactNode;
  renderPosicoesCards?: (isLoading: boolean) => React.ReactNode;
}

export const OperacoesTabs: React.FC<OperacoesTabsProps> = ({
  activeTab,
  setActiveTab,
  operacoesAtivas,
  operacoesFinalizadas,
  posicoes = [],
  posicoesDashboardData = {
    totalPositions: 0,
    totalLongPositions: 0,
    totalShortPositions: 0,
    totalInvested: 0,
    totalUnrealizedProfitLoss: 0,
    totalUnrealizedProfitLossPercentage: 0
  },
  loadingAtivas,
  loadingFinalizadas,
  loadingPosicoes = false,
  onRefreshPosicoes = () => {},
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
  renderFinalizadasCards,
  renderPosicoesCards
}) => {
  const navigate = useNavigate();
  const { filtros } = useFiltros();
  const [exportLoading, setExportLoading] = useState(false);
  const [modalFinalizarOperacaoAberto, setModalFinalizarOperacaoAberto] = useState(false);
  const [operacaoParaFinalizar, setOperacaoParaFinalizar] = useState<OperacaoAtiva | null>(null);
  
  // Estados para o gerenciamento de posições
  const [expandedPositionId, setExpandedPositionId] = useState<string | null>(null);
  
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
    let tokenExpirado = false;
    if (!token) {
      tokenExpirado = true;
    } else {
      try {
        const decoded: any = jwtDecode(token)
        if (decoded.exp && Date.now() >= decoded.exp * 1000) {
          tokenExpirado = true;
        }
      } catch (e) {
        tokenExpirado = true;
      }
    }
    if (tokenExpirado) {
      localStorage.removeItem('token');
      navigate('/login', {
        state: {
          from: window.location.pathname,
          message: 'Sua sessão expirou. Por favor, faça login novamente.'
        }
      });
      return;
    }
    // Se o token existir e estiver válido, mudar a aba
    setActiveTab(value);
    // Resetar para a primeira página
    onPageChange(0);
  };

  // Função para exportar operações
  const handleExport = async (formato: 'excel' | 'pdf', status: string[]) => {
    try {
      setExportLoading(true);
      const blob = await OperacaoService.exportarOperacoes(filtros, status, formato);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = formato === 'excel' ? 'operacoes.xlsx' : 'operacoes.pdf';
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      alert('Erro ao exportar operações.');
    } finally {
      setExportLoading(false);
    }
  };

  // Funções para exportar diferentes tipos de operações
  const handleExportAtivas = async (formato: 'excel' | 'pdf') => {
    await handleExport(formato, ['ACTIVE']);
  };
  
  const handleExportFinalizadas = async (formato: 'excel' | 'pdf') => {
    await handleExport(formato, ['WINNER', 'LOSER']);
  };

  // Handler para abrir modal de finalização com a operação selecionada
  const handleFinalizar = (operacao: OperacaoAtiva) => {
    setOperacaoParaFinalizar(operacao);
    setModalFinalizarOperacaoAberto(true);
  };
  
  const fecharModalFinalizarOperacao = () => {
    setModalFinalizarOperacaoAberto(false);
    setOperacaoParaFinalizar(null);
  };
  
  const handleFinalizarOperacaoSucesso = () => {
    // Fechar o modal primeiro
    fecharModalFinalizarOperacao();
    
    // Usar setTimeout para garantir que o modal tenha tempo de fechar
    setTimeout(() => {
      // Mudar para a aba de operações finalizadas
      setActiveTab('finalizadas');
      
      // Recarregar as operações
      onPageChange(currentPage);
    }, 500);
  };

  // Handlers para posições
  const handleViewPosition = (id: string) => {
    setExpandedPositionId(id === expandedPositionId ? null : id);
  };

  return (
    <>
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
          <TabsTrigger 
            value="posicoes" 
            className={`px-4 py-2 ${activeTab === 'posicoes' ? 'bg-purple-100' : ''}`}
          >
            Posições
          </TabsTrigger>
        </TabsList>
        
        {/* Tab de Operações Ativas */}
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
                <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
                  <h2 className="font-semibold text-gray-800">Operações Ativas</h2>
                  <div className="relative">
                    <button
                      className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg shadow hover:bg-purple-700 transition"
                      disabled={exportLoading}
                      onClick={e => {
                        const menu = document.getElementById('export-menu-ativas');
                        if (menu) menu.classList.toggle('hidden');
                      }}
                    >
                      <Download className="w-4 h-4" /> Exportar
                    </button>
                    <div
                      id="export-menu-ativas"
                      className="hidden absolute right-0 mt-2 w-32 bg-white border rounded shadow z-10"
                    >
                      <button
                        className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                        onClick={() => {
                          handleExportAtivas('excel');
                          document.getElementById('export-menu-ativas')?.classList.add('hidden');
                        }}
                      >Exportar Excel</button>
                      <button
                        className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                        onClick={() => {
                          handleExportAtivas('pdf');
                          document.getElementById('export-menu-ativas')?.classList.add('hidden');
                        }}
                      >Exportar PDF</button>
                    </div>
                  </div>
                </div>
                <OperacoesAtivasTable
                  operacoes={operacoesAtivas}
                  sortField={sortField}
                  sortDirection={sortDirection}
                  onSort={onSort}
                  onEdit={onEdit}
                  onFinalize={handleFinalizar}
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
        
        {/* Tab de Operações Finalizadas */}
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
                <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
                  <h2 className="font-semibold text-gray-800">Operações Finalizadas</h2>
                  <div className="relative">
                    <button
                      className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg shadow hover:bg-purple-700 transition"
                      disabled={exportLoading}
                      onClick={e => {
                        const menu = document.getElementById('export-menu-finalizadas');
                        if (menu) menu.classList.toggle('hidden');
                      }}
                    >
                      <Download className="w-4 h-4" /> Exportar
                    </button>
                    <div
                      id="export-menu-finalizadas"
                      className="hidden absolute right-0 mt-2 w-32 bg-white border rounded shadow z-10"
                    >
                      <button
                        className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                        onClick={() => {
                          handleExportFinalizadas('excel');
                          document.getElementById('export-menu-finalizadas')?.classList.add('hidden');
                        }}
                      >Exportar Excel</button>
                      <button
                        className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                        onClick={() => {
                          handleExportFinalizadas('pdf');
                          document.getElementById('export-menu-finalizadas')?.classList.add('hidden');
                        }}
                      >Exportar PDF</button>
                    </div>
                  </div>
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
        
        {/* Tab de Posições */}
        <TabsContent value="posicoes">
          <PosicoesTab
            posicoes={posicoes}
            dashboardData={posicoesDashboardData}
            loading={loadingPosicoes}
            currentPage={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            onPageChange={onPageChange}
            onRefresh={onRefreshPosicoes}
          />
        </TabsContent>
      </Tabs>
      
      {/* Modais */}
      <FinalizarOperacaoModal
        isOpen={modalFinalizarOperacaoAberto}
        operacao={operacaoParaFinalizar}
        onClose={fecharModalFinalizarOperacao}
        onSuccess={handleFinalizarOperacaoSucesso}
      />
    </>
  );
};