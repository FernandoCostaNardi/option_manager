import React, { useState } from 'react';
import { Posicao } from '../../../types/posicao/posicoes.types';
import { PosicoesDashboard } from '../../../services/posicaoService';
import { PosicoesDashboardCards } from '../cards/PosicoesDashboard';
import { PosicoesTable } from '../tables/PosicoesTable';
import { NovaEntradaModal } from '../modals/NovaEntradaModal';
import { SaidaPosicaoModal } from '../modals/SaidaPosicaoModal';
import { Pagination } from '../../ui/Pagination';

interface PosicoesTabProps {
  posicoes: Posicao[];
  dashboardData: PosicoesDashboard;
  loading: boolean;
  currentPage: number;
  totalPages: number;
  totalItems: number;
  onPageChange: (page: number) => void;
  onRefresh: () => void;
}

export function PosicoesTab({
  posicoes,
  dashboardData,
  loading,
  currentPage,
  totalPages,
  totalItems,
  onPageChange,
  onRefresh
}: PosicoesTabProps) {
  // Estados para expandir detalhes e controlar modais
  const [expandedPositionId, setExpandedPositionId] = useState<string | null>(null);
  const [novaEntradaModal, setNovaEntradaModal] = useState(false);
  const [saidaPosicaoModal, setSaidaPosicaoModal] = useState(false);
  const [posicaoSelecionada, setPosicaoSelecionada] = useState<Posicao | null>(null);

  // Handler para visualizar detalhes da posição
  const handleViewDetails = (id: string) => {
    setExpandedPositionId(expandedPositionId === id ? null : id);
  };

  // Handler para adicionar entrada
  const handleAddEntry = (posicao: Posicao) => {
    setPosicaoSelecionada(posicao);
    setNovaEntradaModal(true);
  };

  // Handler para realizar saída
  const handleExit = (posicao: Posicao) => {
    setPosicaoSelecionada(posicao);
    setSaidaPosicaoModal(true);
  };

  // Handler para sucesso nas operações
  const handleOperationSuccess = () => {
    setNovaEntradaModal(false);
    setSaidaPosicaoModal(false);
    onRefresh();
  };

  return (
    <div className="space-y-6">
      {/* Dashboard de posições */}
      <PosicoesDashboardCards 
        dashboardData={dashboardData} 
        loading={loading} 
      />

      {/* Tabela de posições */}
      <PosicoesTable
        posicoes={posicoes}
        loading={loading}
        expandedPositionId={expandedPositionId}
        onViewDetails={handleViewDetails}
        onAddEntry={handleAddEntry}
        onExit={handleExit}
      />

      {/* Paginação */}
      {!loading && posicoes.length > 0 && (
        <div className="mt-4">
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            pageSize={10}
            onPageChange={onPageChange}
            variant="default"
            showStats={true}
          />
        </div>
      )}

      {/* Modais */}
      <NovaEntradaModal
        isOpen={novaEntradaModal}
        posicao={posicaoSelecionada}
        onClose={() => setNovaEntradaModal(false)}
        onSuccess={handleOperationSuccess}
      />

      <SaidaPosicaoModal
        isOpen={saidaPosicaoModal}
        posicao={posicaoSelecionada}
        onClose={() => setSaidaPosicaoModal(false)}
        onSuccess={handleOperationSuccess}
      />
    </div>
  );
}