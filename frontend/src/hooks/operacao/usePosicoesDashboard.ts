import { useState, useEffect, useCallback } from 'react';
import { PosicaoService, PosicoesDashboard } from '../../services/posicaoService';
import { Posicao } from '../../types/posicao/posicoes.types';
import { FiltrosOperacao } from '../../types/operacao/operacoes.types';

interface UsePosicoesDashboardProps {
  currentPage: number;
  pageSize: number;
  filtros: FiltrosOperacao;
  setTotalPages: (totalPages: number) => void;
  setTotalItems: (totalItems: number) => void;
}

export const usePosicoesDashboard = ({
  currentPage,
  pageSize,
  filtros,
  setTotalPages,
  setTotalItems
}: UsePosicoesDashboardProps) => {
  const [posicoes, setPosicoes] = useState<Posicao[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dashboardData, setDashboardData] = useState<PosicoesDashboard>({
    totalPositions: 0,
    totalLongPositions: 0,
    totalShortPositions: 0,
    totalInvested: 0,
    totalUnrealizedProfitLoss: 0,
    totalUnrealizedProfitLossPercentage: 0
  });

  const carregarPosicoes = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      // Converter filtros de operações para filtros de posições, se necessário
      // No momento, estamos apenas passando a página e o tamanho
      const response = await PosicaoService.buscarPosicoesAtivas(currentPage, pageSize);
      
      setPosicoes(response.positions || []);
      setDashboardData({
        totalPositions: response.dashboard?.totalPositions || 0,
        totalLongPositions: response.dashboard?.totalLongPositions || 0,
        totalShortPositions: response.dashboard?.totalShortPositions || 0,
        totalInvested: response.dashboard?.totalInvested || 0,
        totalUnrealizedProfitLoss: response.dashboard?.totalUnrealizedProfitLoss || 0,
        totalUnrealizedProfitLossPercentage: response.dashboard?.totalUnrealizedProfitLossPercentage || 0
      });
      
      setTotalPages(response.totalPages || 1);
      setTotalItems(response.totalElements || 0);
    } catch (err) {
      console.error('Erro ao carregar posições:', err);
      
      // Traduzir o erro ou usar uma mensagem padrão
      let errorMessage = 'Não foi possível carregar as posições. Tente novamente mais tarde.';
      
      if (err instanceof Error) {
        // Se for um erro de rede ou API
        if (err.message.includes('401') || err.message.includes('unauthorized')) {
          errorMessage = 'Sessão expirada. Por favor, faça login novamente.';
        } else if (err.message.includes('404')) {
          errorMessage = 'Recurso não encontrado. Verifique se a API está disponível.';
        } else if (err.message.includes('500')) {
          errorMessage = 'Erro interno do servidor. Tente novamente mais tarde.';
        } else if (err.message.includes('timeout')) {
          errorMessage = 'Tempo limite de conexão excedido. Verifique sua conexão com a internet.';
        }
      }
      
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, setTotalItems, setTotalPages]);

  // Carregar posições quando os parâmetros mudarem
  useEffect(() => {
    carregarPosicoes();
  }, [carregarPosicoes]);

  return {
    posicoes,
    loading,
    error,
    dashboardData,
    carregarPosicoes
  };
};