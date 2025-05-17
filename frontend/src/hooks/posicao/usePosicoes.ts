import { useState, useEffect, useCallback } from 'react';
import { PosicaoService, PosicoesDashboard } from '../../services/posicaoService';
import { Posicao } from '../../types/posicao/posicoes.types';

interface UsePosicoesPrpps {
  currentPage: number;
  pageSize: number;
  setTotalPages: (totalPages: number) => void;
  setTotalItems: (totalItems: number) => void;
}

export const usePosicoes = ({
  currentPage,
  pageSize,
  setTotalPages,
  setTotalItems
}: UsePosicoesPrpps) => {
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
    } catch (error) {
      console.error('Erro ao carregar posições:', error);
      setError('Não foi possível carregar as posições. Tente novamente mais tarde.');
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, setTotalItems, setTotalPages]);

  useEffect(() => {
    carregarPosicoes();
  }, [carregarPosicoes]);

  return {
    posicoes,
    loading,
    error,
    carregarPosicoes,
    dashboardData
  };
};