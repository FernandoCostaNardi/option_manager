import React, { useState, useMemo } from 'react';
import { OperacoesHeader } from '../../components/operacao/header/OperacoesHeader';
import { FiltrosAccordion } from '../../components/operacao/filters/FiltrosAccordion';
import { OperacoesTabs } from '../../components/operacao/tab/OperacoesTabs';
import { NovaOperacaoModal } from '../../components/operacao/modals/NovaOperacaoModal';
import { FinalizarOperacaoModal } from '../../components/operacao/modals/FinalizarOperacaoModal';
import { VisualizarTargetsModal } from '../../components/operacao/modals/VisualizarTargetsModal';
import { usePaginacao } from '../../hooks/usePaginacao';
import { useOrdenacao } from '../../hooks/operacao/useOrdenacao';
import { useFiltros } from '../../hooks/operacao/useFiltros';
import { useOperacoes } from '../../hooks/operacao/useOperacoes';
import { formatarMoeda } from '../../utils/formatadores';
import { BarChart, DollarSign, ListChecks, Percent } from 'lucide-react';
import { OperacaoAtiva } from '../../types/operacao/operacoes.types';

export function Operacoes() {
  // Estado para modais
  const [modalNovaOperacaoAberto, setModalNovaOperacaoAberto] = useState(false);
  const [modalFinalizarOperacaoAberto, setModalFinalizarOperacaoAberto] = useState(false);
  const [operacaoParaFinalizar, setOperacaoParaFinalizar] = useState<string | null>(null);
  const [modalVisualizarTargetsAberto, setModalVisualizarTargetsAberto] = useState(false);
  const [operacaoParaVisualizarTargets, setOperacaoParaVisualizarTargets] = useState<string | null>(null);
  const [operacaoParaEditar, setOperacaoParaEditar] = useState<OperacaoAtiva | undefined>(undefined);
  
  // Hook de filtros
  const {
    acordeonAberto,
    setAcordeonAberto,
    filtros,
    setFiltros,
    limparFiltros,
    temFiltrosAtivos,
    obterTextoFiltrosAtivos
  } = useFiltros();
  
  // Hook de paginação
  const {
    currentPage,
    totalPages,
    totalItems,
    pageSize,
    setTotalPages,
    setTotalItems,
    resetPage,
    paginate
  } = usePaginacao();
  
  // Hook de ordenação
  const {
    sortField,
    sortDirection,
    handleSort
  } = useOrdenacao(resetPage);
  
  // Hook para carregar operações
  const {
    operacoesAtivas,
    operacoesFinalizadas,
    loadingAtivas,
    loadingFinalizadas,
    error,
    activeTab,
    setActiveTab,
    carregarOperacoesAtivas,
    carregarOperacoesFinalizadas,
    dashboardData
  } = useOperacoes({
    currentPage,
    pageSize,
    sortField,
    sortDirection,
    filtros,
    setTotalPages,
    setTotalItems
  });
  
  // Função para aplicar filtros
  const aplicarFiltros = () => {
    // Resetar para a primeira página
    resetPage();
    
    // Usar setTimeout para garantir que a atualização do estado seja concluída
    setTimeout(() => {
      // Chamar apenas a função correspondente à aba ativa
      if (activeTab === "ativas") {
        carregarOperacoesAtivas();
      } else if (activeTab === "finalizadas") {
        carregarOperacoesFinalizadas();
      }
    }, 0);
  };
  
  // Handlers para ações da tabela
  const handleEditar = (id: string) => {
    // Encontrar a operação pelo ID
    const operacao = operacoesAtivas.find(op => op.id === id);
    if (operacao) {
      setOperacaoParaEditar(operacao);
      setModalNovaOperacaoAberto(true);
    }
  };
  
  const handleRemover = (id: string) => {
    if (window.confirm('Tem certeza que deseja remover esta operação?')) {
      // Implementação da remoção
      console.log(`Removendo operação ${id}`);
      carregarOperacoesAtivas();
      carregarOperacoesFinalizadas();
    }
  };
  
  const handleFinalizar = (id: string) => {
    setOperacaoParaFinalizar(id);
    setModalFinalizarOperacaoAberto(true);
  };
  
  const handleVisualizar = (id: string) => {
    // Implementação da visualização
    console.log(`Visualizando detalhes da operação ${id}`);
  };
  
  const handleVisualizarTargets = (id: string) => {
    setOperacaoParaVisualizarTargets(id);
    setModalVisualizarTargetsAberto(true);
  };
  
  // Handlers para o modal de nova operação
  const abrirModalNovaOperacao = () => {
    setOperacaoParaEditar(undefined);
    setModalNovaOperacaoAberto(true);
  };
  
  const fecharModalNovaOperacao = () => {
    setModalNovaOperacaoAberto(false);
    setOperacaoParaEditar(undefined);
  };
  
  const handleNovaOperacaoSucesso = () => {
    carregarOperacoesAtivas();
    setActiveTab("ativas");
    fecharModalNovaOperacao();
  };
  
  // Handlers para o modal de finalizar operação
  const fecharModalFinalizarOperacao = () => {
    setModalFinalizarOperacaoAberto(false);
    setOperacaoParaFinalizar(null);
  };
  
  const handleFinalizarOperacaoSucesso = () => {
    carregarOperacoesAtivas();
    carregarOperacoesFinalizadas();
    fecharModalFinalizarOperacao();
  };
  
  // Componente de Dashboard Card
  const DashboardCard = ({ icon, title, value, color }: { icon: React.ReactNode, title: string, value: string | number, color: string }) => (
    <div className="bg-white rounded-lg shadow-sm overflow-hidden">
      <div className="p-4 sm:p-6">
        <div className="flex items-center">
          <div className={`p-3 rounded-full ${color} mr-4`}>
            {icon}
          </div>
          <div>
            <h3 className="text-sm font-medium text-gray-500">{title}</h3>
            <p className="text-2xl font-bold text-gray-900 mt-1">{value}</p>
          </div>
        </div>
      </div>
    </div>
  );
  
  // Componente de Dashboard Card com valor secundário
  const DashboardCardWithSecondary = ({ 
    icon, 
    title, 
    primaryValue, 
    secondaryValue, 
    isPrimaryPositive,
    isSecondaryPositive,
    color 
  }: { 
    icon: React.ReactNode, 
    title: string, 
    primaryValue: string | number, 
    secondaryValue: string | number,
    isPrimaryPositive: boolean,
    isSecondaryPositive: boolean,
    color: string 
  }) => (
    <div className="bg-white rounded-lg shadow-sm overflow-hidden">
      <div className="p-4 sm:p-6">
        <div className="flex items-center">
          <div className={`p-3 rounded-full ${color} mr-4`}>
            {icon}
          </div>
          <div>
            <h3 className="text-sm font-medium text-gray-500">{title}</h3>
            <p className={`text-2xl font-bold mt-1 ${isPrimaryPositive ? 'text-green-600' : 'text-red-600'}`}>
              {primaryValue}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
  
  // Renderização dos cards para operações ativas
  const renderAtivasCards = (isLoading: boolean) => (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
      <DashboardCard 
        icon={<ListChecks className="h-6 w-6 text-blue-500" />}
        title="Operações Ativas"
        value={isLoading ? "..." : dashboardData.totalActiveOperations || 0}
        color="bg-blue-100"
      />
      <DashboardCard 
        icon={<BarChart className="h-6 w-6 text-purple-500" />}
        title="CALL / PUT"
        value={isLoading ? "..." : `${dashboardData.totalCallOperations || 0} / ${dashboardData.totalPutOperations || 0}`}
        color="bg-purple-100"
      />
      <DashboardCard 
        icon={<DollarSign className="h-6 w-6 text-green-500" />}
        title="Valor Total"
        value={isLoading ? "..." : formatarMoeda(dashboardData.totalEntryValue || 0)}
        color="bg-green-100"
      />
    </div>
  );
  
  // Renderização dos cards para operações finalizadas
  const renderFinalizadasCards = (isLoading: boolean) => (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
      <DashboardCard 
        icon={<ListChecks className="h-6 w-6 text-blue-500" />}
        title="Ganhadoras / Perdedoras"
        value={isLoading ? "..." : `${dashboardData.totalWinningOperations || 0} / ${dashboardData.totalLosingOperations || 0}`}
        color="bg-blue-100"
      />
      <DashboardCard 
        icon={<BarChart className="h-6 w-6 text-purple-500" />}
        title="SwingTrade / DayTrade"
        value={isLoading ? "..." : `${dashboardData.totalSwingTradeOperations || 0} / ${dashboardData.totalDayTradeOperations || 0}`}
        color="bg-purple-100"
      />
      <DashboardCardWithSecondary 
        icon={<DollarSign className="h-6 w-6 text-green-500" />}
        title="Resultado Total"
        primaryValue={isLoading ? "..." : formatarMoeda(dashboardData.totalProfitLoss || 0)}
        secondaryValue={isLoading ? "..." : (
          (dashboardData.totalProfitLoss || 0) >= 0 
            ? `Ganhos: ${formatarMoeda(dashboardData.totalProfitLoss || 0)}`
            : `Perdas: ${formatarMoeda(Math.abs(dashboardData.totalProfitLoss || 0))}`
        )}
        isPrimaryPositive={!isLoading && (dashboardData.totalProfitLoss || 0) >= 0}
        isSecondaryPositive={!isLoading && (dashboardData.totalProfitLoss || 0) >= 0}
        color="bg-green-100"
      />
      <DashboardCardWithSecondary 
        icon={<Percent className="h-6 w-6 text-blue-500" />}
        title="% Total"
        primaryValue={isLoading ? "..." : `${(dashboardData.totalProfitLossPercentage || 0).toFixed(2)}%`}
        secondaryValue={isLoading ? "..." : (
          (dashboardData.totalProfitLossPercentage || 0) >= 0 
            ? "Rentabilidade positiva" 
            : "Rentabilidade negativa"
        )}
        isPrimaryPositive={!isLoading && (dashboardData.totalProfitLossPercentage || 0) >= 0}
        isSecondaryPositive={!isLoading && (dashboardData.totalProfitLossPercentage || 0) >= 0}
        color="bg-blue-100"
      />
    </div>
  );
  
  return (
    <div className="container mx-auto py-6">
      <OperacoesHeader onNovaOperacao={abrirModalNovaOperacao} />
      
      <FiltrosAccordion
        acordeonAberto={acordeonAberto}
        setAcordeonAberto={setAcordeonAberto}
        filtros={filtros}
        setFiltros={setFiltros}
        temFiltrosAtivos={temFiltrosAtivos}
        obterTextoFiltrosAtivos={obterTextoFiltrosAtivos}
        limparFiltros={limparFiltros}
        aplicarFiltros={aplicarFiltros}
        activeTab={activeTab}
      />
      
      <OperacoesTabs
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        operacoesAtivas={operacoesAtivas}
        operacoesFinalizadas={operacoesFinalizadas}
        loadingAtivas={loadingAtivas}
        loadingFinalizadas={loadingFinalizadas}
        error={error}
        sortField={sortField}
        sortDirection={sortDirection}
        onSort={handleSort}
        currentPage={currentPage}
        totalPages={totalPages}
        totalItems={totalItems}
        pageSize={pageSize}
        onPageChange={paginate}
        onEdit={handleEditar}
        onFinalize={handleFinalizar}
        onRemove={handleRemover}
        onView={handleVisualizar}
        onViewTargets={handleVisualizarTargets}
        renderAtivasCards={renderAtivasCards}
        renderFinalizadasCards={renderFinalizadasCards}
      />
      
      {/* Modal de Nova Operação */}
      <NovaOperacaoModal
        isOpen={modalNovaOperacaoAberto}
        onClose={fecharModalNovaOperacao}
        onSuccess={handleNovaOperacaoSucesso}
        operacaoExistente={operacaoParaEditar}
      />
      
      {/* Modal de Finalizar Operação */}
      <FinalizarOperacaoModal
        isOpen={modalFinalizarOperacaoAberto}
        operacao={operacaoParaFinalizar}
        onClose={fecharModalFinalizarOperacao}
        onSuccess={handleFinalizarOperacaoSucesso}
      />
      
      {/* Modal de Visualizar Targets */}
      <VisualizarTargetsModal
        isOpen={modalVisualizarTargetsAberto}
        operationId={operacaoParaVisualizarTargets}
        onClose={() => setModalVisualizarTargetsAberto(false)}
      />
    </div>
  );
}


