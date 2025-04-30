import React, { useState } from 'react';
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
  };
  
  // Handlers para ações da tabela
  const handleEditar = (id: string) => {
    alert(`Editando operação ${id}`);
  };
  
  const handleRemover = (id: string) => {
    if (window.confirm('Tem certeza que deseja remover esta operação?')) {
      alert(`Removendo operação ${id}`);
    }
  };
  
  const handleFinalizar = (id: string) => {
    setOperacaoParaFinalizar(id);
    setModalFinalizarOperacaoAberto(true);
  };
  
  const handleVisualizar = (id: string) => {
    alert(`Visualizando detalhes da operação ${id}`);
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