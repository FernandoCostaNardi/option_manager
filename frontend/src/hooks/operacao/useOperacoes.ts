import { useState, useEffect, useCallback } from 'react';
import api from '../../services/api';
import { OperacaoAtiva, OperacaoFinalizada, SortField, SortDirection, FiltrosOperacao } from '../../types/operacao/operacoes.types';

interface UseOperacoesProps {
  currentPage: number;
  pageSize: number;
  sortField: SortField;
  sortDirection: SortDirection;
  filtros: FiltrosOperacao;
  setTotalPages: (totalPages: number) => void;
  setTotalItems: (totalItems: number) => void;
}

export const useOperacoes = ({
  currentPage,
  pageSize,
  sortField,
  sortDirection,
  filtros,
  setTotalPages,
  setTotalItems
}: UseOperacoesProps) => {
  const [operacoesAtivas, setOperacoesAtivas] = useState<OperacaoAtiva[]>([]);
  const [operacoesFinalizadas, setOperacoesFinalizadas] = useState<OperacaoFinalizada[]>([]);
  const [loadingAtivas, setLoadingAtivas] = useState(false);
  const [loadingFinalizadas, setLoadingFinalizadas] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState("ativas");
  const [dashboardData, setDashboardData] = useState<any>({});

  const carregarOperacoesAtivas = useCallback(async () => {
    if (loadingAtivas) return;
    setLoadingAtivas(true);
    setError(null);
    try {
      const token = localStorage.getItem('token');
      const response = await api.get('/operations', {
        params: { 
          status: 'ACTIVE',
          page: currentPage,
          size: pageSize,
          sort: sortField ? `${sortField},${sortDirection}` : undefined,
          entryDateStart: filtros.entryDateStart,
          entryDateEnd: filtros.entryDateEnd,
          analysisHouseName: filtros.analysisHouseName,
          brokerageName: filtros.brokerageName,
          transactionType: filtros.transactionType,
          optionType: filtros.optionType,
          tradeType: filtros.tradeType
        },
        headers: token ? { Authorization: `Bearer ${token}` } : undefined
      });
      const data = response.data;
      let operations: any[] = [];
      let dashboard: any = {};
      if (data) {
        if (data.content && data.content.length > 0) {
          // Formato antigo
          const firstContent = data.content[0];
          operations = firstContent.operations || [];
          dashboard = firstContent;
        } else if (data.operations && Array.isArray(data.operations)) {
          // Formato novo (direto)
          operations = data.operations;
          dashboard = data;
        }
      }
      if (operations.length > 0) {
        const processedOperations = operations.map(op => ({
          ...op,
          entryDate: op.entryDate,
          exitDate: op.exitDate,
          optionType: op.optionType || '',
          tradeType: op.tradeType || '',
          status: op.status || '',
          transactionType: op.transactionType || '',
          optionSeriesCode: op.optionSeriesCode || '',
          brokerageName: op.brokerageName || '',
          analysisHouseName: op.analysisHouseName || '',
          baseAssetLogoUrl: op.baseAssetLogoUrl || '',
          quantity: op.quantity || 0,
          entryUnitPrice: op.entryUnitPrice || 0,
          entryTotalValue: op.entryTotalValue || 0,
          exitUnitPrice: op.exitUnitPrice || 0,
          exitTotalValue: op.exitTotalValue || 0,
          profitLoss: op.profitLoss || 0,
          profitLossPercentage: op.profitLossPercentage || 0,
          groupId: op.groupId || null,
        }));
        setOperacoesAtivas(processedOperations);
        setDashboardData({
          totalActiveOperations: dashboard.totalActiveOperations || 0,
          totalPutOperations: dashboard.totalPutOperations || 0,
          totalCallOperations: dashboard.totalCallOperations || 0,
          totalEntryValue: dashboard.totalEntryValue || 0
        });
        setTotalPages(data.totalPages || 1);
        setTotalItems(data.totalElements || 0);
      } else {
        setOperacoesAtivas([]);
        setDashboardData({});
        setTotalPages(1);
        setTotalItems(0);
      }
    } catch (error) {
      console.error('Erro ao carregar operações ativas:', error);
      setError('Não foi possível carregar as operações ativas. Tente novamente mais tarde.');
    } finally {
      setLoadingAtivas(false);
    }
  }, [currentPage, pageSize, sortField, sortDirection, filtros, setTotalPages, setTotalItems]);

  const carregarOperacoesFinalizadas = useCallback(async () => {
    if (loadingFinalizadas) return;
    setLoadingFinalizadas(true);
    setError(null);
    try {
      const token = localStorage.getItem('token');
      const statusParam = filtros.status || 'WINNER,LOSER';
      const response = await api.get('/operations', {
        params: { 
          status: statusParam,
          page: currentPage,
          size: pageSize,
          sort: sortField ? `${sortField},${sortDirection}` : undefined,
          entryDateStart: filtros.entryDateStart,
          entryDateEnd: filtros.entryDateEnd,
          exitDateStart: filtros.exitDateStart,
          exitDateEnd: filtros.exitDateEnd,
          analysisHouseName: filtros.analysisHouseName,
          brokerageName: filtros.brokerageName,
          transactionType: filtros.transactionType,
          optionType: filtros.optionType,
          tradeType: filtros.tradeType
        },
        headers: token ? { Authorization: `Bearer ${token}` } : undefined
      });
      const data = response.data;
      let operations: any[] = [];
      let dashboard: any = {};
      if (data) {
        if (data.content && data.content.length > 0) {
          const firstContent = data.content[0];
          operations = firstContent.operations || [];
          dashboard = firstContent;
        } else if (data.operations && Array.isArray(data.operations)) {
          operations = data.operations;
          dashboard = data;
        }
      }
      if (operations.length > 0) {
        const processedOperations = operations.map(op => ({
          ...op,
          entryDate: op.entryDate,
          exitDate: op.exitDate,
          optionType: op.optionType || '',
          tradeType: op.tradeType || '',
          status: op.status || '',
          transactionType: op.transactionType || '',
          optionSeriesCode: op.optionSeriesCode || '',
          brokerageName: op.brokerageName || '',
          analysisHouseName: op.analysisHouseName || '',
          baseAssetLogoUrl: op.baseAssetLogoUrl || '',
          quantity: op.quantity || 0,
          entryUnitPrice: op.entryUnitPrice || 0,
          entryTotalValue: op.entryTotalValue || 0,
          exitUnitPrice: op.exitUnitPrice || 0,
          exitTotalValue: op.exitTotalValue || 0,
          profitLoss: op.profitLoss || 0,
          profitLossPercentage: op.profitLossPercentage || 0,
          groupId: op.groupId || null,
        }));
        setOperacoesFinalizadas(processedOperations);
        setDashboardData({
          totalWinningOperations: dashboard.totalWinningOperations || 0,
          totalLosingOperations: dashboard.totalLosingOperations || 0,
          totalSwingTradeOperations: dashboard.totalSwingTradeOperations || 0,
          totalDayTradeOperations: dashboard.totalDayTradeOperations || 0,
          totalProfitLoss: dashboard.totalProfitLoss || 0,
          totalProfitLossPercentage: dashboard.totalProfitLossPercentage || 0,
          totalEntryValue: dashboard.totalEntryValue || 0
        });
        setTotalPages(data.totalPages || 1);
        setTotalItems(data.totalElements || 0);
      } else {
        setOperacoesFinalizadas([]);
        setDashboardData({});
        setTotalPages(1);
        setTotalItems(0);
      }
    } catch (error) {
      console.error('Erro ao carregar operações finalizadas:', error);
      setError('Erro ao carregar operações finalizadas');
    } finally {
      setLoadingFinalizadas(false);
    }
  }, [currentPage, pageSize, sortField, sortDirection, filtros, setTotalPages, setTotalItems]);

  useEffect(() => {
    if (activeTab === "ativas") {
      carregarOperacoesAtivas();
    } else if (activeTab === "finalizadas") {
      carregarOperacoesFinalizadas();
    }
  }, [activeTab, currentPage, pageSize, sortField, sortDirection, carregarOperacoesAtivas, carregarOperacoesFinalizadas]);

  return {
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
  };
};