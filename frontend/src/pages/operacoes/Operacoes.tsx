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

export function Operacoes() {
  // Estado para modais
  const [modalNovaOperacaoAberto, setModalNovaOperacaoAberto] = useState(false);
  const [modalFinalizarOperacaoAberto, setModalFinalizarOperacaoAberto] = useState(false);
  const [operacaoParaFinalizar, setOperacaoParaFinalizar] = useState<string | null>(null);
  const [modalVisualizarTargetsAberto, setModalVisualizarTargetsAberto] = useState(false);
  const [operacaoParaVisualizarTargets, setOperacaoParaVisualizarTargets] = useState<string | null>(null);
  
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
    carregarOperacoesFinalizadas
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
    resetPage();
    carregarOperacoesAtivas();
    carregarOperacoesFinalizadas();
  };
  
  // Handlers para ações da tabela
  const handleEditar = (id: string) => {
    // Implementação da edição
    console.log(`Editando operação ${id}`);
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
    setModalNovaOperacaoAberto(true);
  };
  
  const fecharModalNovaOperacao = () => {
    setModalNovaOperacaoAberto(false);
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
  
  // Cálculos para os dashboards
  const dashboardData = useMemo(() => {
    // Dados para operações ativas
    if (activeTab === "ativas" && !loadingAtivas && operacoesAtivas) {
      // Quantidade total de operações ativas
      const totalOperacoes = totalItems;
      
      // Contagem de PUTs e CALLs
      const totalPuts = operacoesAtivas.filter(op => op.optionType === 'PUT').length;
      const totalCalls = operacoesAtivas.filter(op => op.optionType === 'CALL').length;
      
      // Soma do valor total de todas as operações
      const valorTotal = operacoesAtivas.reduce((total, op) => total + (op.entryTotalValue || 0), 0);
      
      return {
        totalOperacoes,
        totalPuts,
        totalCalls,
        valorTotal
      };
    }
    
    // Dados para operações finalizadas
    if (activeTab === "finalizadas" && !loadingFinalizadas && operacoesFinalizadas) {
      // Contagem de operações ganhadoras e perdedoras
      const totalGanhadoras = operacoesFinalizadas.filter(op => op.status === 'WINNER').length;
      const totalPerdedoras = operacoesFinalizadas.filter(op => op.status === 'LOSER').length;
      
      // Contagem de SwingTrade e DayTrade
      const totalSwingTrade = operacoesFinalizadas.filter(op => op.tradeType === 'SWING').length;
      const totalDayTrade = operacoesFinalizadas.filter(op => op.tradeType === 'DAY').length;
      
      // Cálculo de resultados
      const resultadoGanhos = operacoesFinalizadas
        .filter(op => op.profitLoss && op.profitLoss > 0)
        .reduce((total, op) => total + (op.profitLoss || 0), 0);
      
      const resultadoPerdas = operacoesFinalizadas
        .filter(op => op.profitLoss && op.profitLoss < 0)
        .reduce((total, op) => total + (op.profitLoss || 0), 0);
      
      const resultadoTotal = resultadoGanhos + resultadoPerdas;
      
      // Cálculo de percentual
      const totalInvestido = operacoesFinalizadas.reduce((total, op) => total + (op.entryTotalValue || 0), 0);
      const percentualTotal = totalInvestido > 0 ? (resultadoTotal / totalInvestido) * 100 : 0;
      
      return {
        totalGanhadoras,
        totalPerdedoras,
        totalSwingTrade,
        totalDayTrade,
        resultadoGanhos,
        resultadoPerdas,
        resultadoTotal,
        percentualTotal
      };
    }
    
    // Valor padrão
    return {};
  }, [activeTab, operacoesAtivas, operacoesFinalizadas, loadingAtivas, loadingFinalizadas, totalItems]);
  
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
        value={isLoading ? "..." : dashboardData.totalOperacoes || 0}
        color="bg-blue-100"
      />
      <DashboardCard 
        icon={<BarChart className="h-6 w-6 text-purple-500" />}
        title="CALL / PUT"
        value={isLoading ? "..." : `${dashboardData.totalCalls || 0} / ${dashboardData.totalPuts || 0}`}
        color="bg-purple-100"
      />
      <DashboardCard 
        icon={<DollarSign className="h-6 w-6 text-green-500" />}
        title="Valor Total"
        value={isLoading ? "..." : formatarMoeda(dashboardData.valorTotal || 0)}
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
        value={isLoading ? "..." : `${dashboardData.totalGanhadoras || 0} / ${dashboardData.totalPerdedoras || 0}`}
        color="bg-blue-100"
      />
      <DashboardCard 
        icon={<BarChart className="h-6 w-6 text-purple-500" />}
        title="SwingTrade / DayTrade"
        value={isLoading ? "..." : `${dashboardData.totalSwingTrade || 0} / ${dashboardData.totalDayTrade || 0}`}
        color="bg-purple-100"
      />
      <DashboardCardWithSecondary 
        icon={<DollarSign className="h-6 w-6 text-green-500" />}
        title="Resultado Total"
        primaryValue={isLoading ? "..." : formatarMoeda(dashboardData.resultadoTotal || 0)}
        secondaryValue={isLoading ? "..." : (
          (dashboardData.resultadoTotal || 0) >= 0 
            ? `Ganhos: ${formatarMoeda(dashboardData.resultadoGanhos || 0)}`
            : `Perdas: ${formatarMoeda(Math.abs(dashboardData.resultadoPerdas || 0))}`
        )}
        isPrimaryPositive={!isLoading && (dashboardData.resultadoTotal || 0) >= 0}
        isSecondaryPositive={!isLoading && (
          (dashboardData.resultadoTotal || 0) >= 0 
            ? true 
            : false
        )}
        color="bg-green-100"
      />
      <DashboardCardWithSecondary 
        icon={<Percent className="h-6 w-6 text-blue-500" />}
        title="% Total"
        primaryValue={isLoading ? "..." : `${(dashboardData.percentualTotal || 0).toFixed(2)}%`}
        secondaryValue={isLoading ? "..." : (
          (dashboardData.percentualTotal || 0) >= 0 
            ? "Rentabilidade positiva" 
            : "Rentabilidade negativa"
        )}
        isPrimaryPositive={!isLoading && (dashboardData.percentualTotal || 0) >= 0}
        isSecondaryPositive={!isLoading && (dashboardData.percentualTotal || 0) >= 0}
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
      />
      
      {/* Modal de Finalizar Operação */}
      <FinalizarOperacaoModal
        isOpen={modalFinalizarOperacaoAberto}
        operacaoId={operacaoParaFinalizar}
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


