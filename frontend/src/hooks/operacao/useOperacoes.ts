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
      console.log('Enviando filtros para operações ativas:', filtros); // Log para debug
      
      const response = await api.get('/operations', {
        params: { 
          status: 'ACTIVE',  // Apenas o status ACTIVE
          page: currentPage,
          size: pageSize,
          sort: sortField ? `${sortField},${sortDirection}` : undefined,
          // Filtros adicionais que podem ser aplicados
          entryDateStart: filtros.entryDateStart,
          entryDateEnd: filtros.entryDateEnd,
          analysisHouseName: filtros.analysisHouseName,
          brokerageName: filtros.brokerageName,
          transactionType: filtros.transactionType,
          optionType: filtros.optionType, // Garantir que este campo seja enviado
          tradeType: filtros.tradeType
          // Removido o optionSeriesCode dos parâmetros
        },
        headers: token ? { Authorization: `Bearer ${token}` } : undefined
      });
      
      // Novo formato de resposta
      const data = response.data;
      
      if (data && data.content && data.content.length > 0) {
        // Extrair operações do novo formato
        const firstContent = data.content[0];
        
        // Garantir que todas as propriedades existam antes de usar
        const operations = firstContent.operations || [];
        
        // Processar cada operação para garantir que todos os campos estejam presentes
        const processedOperations = operations.map(op => ({
          ...op,
          // Garantir que campos de string existam para evitar erros de toUpperCase()
          optionType: op.optionType || '',
          tradeType: op.tradeType || '',
          status: op.status || '',
          transactionType: op.transactionType || '',
          optionSeriesCode: op.optionSeriesCode || '',
          brokerageName: op.brokerageName || '',
          analysisHouseName: op.analysisHouseName || ''
        }));
        
        setOperacoesAtivas(processedOperations);
        
        // Extrair dados do dashboard
        setDashboardData({
          totalActiveOperations: firstContent.totalActiveOperations || 0,
          totalPutOperations: firstContent.totalPutOperations || 0,
          totalCallOperations: firstContent.totalCallOperations || 0,
          totalEntryValue: firstContent.totalEntryValue || 0
        });
        
        // Configurar paginação
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
      // Determina o status a ser enviado na requisição
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
      
      // Novo formato de resposta
      const data = response.data;
      
      if (data && data.content && data.content.length > 0) {
        // Extrair operações do novo formato
        const firstContent = data.content[0];
        
        // Garantir que todas as propriedades existam antes de usar
        const operations = firstContent.operations || [];
        
        // Processar cada operação para garantir que todos os campos estejam presentes
        const processedOperations = operations.map(op => ({
          ...op,
          // Garantir que campos de string existam para evitar erros de toUpperCase()
          optionType: op.optionType || '',
          tradeType: op.tradeType || '',
          status: op.status || '',
          transactionType: op.transactionType || '',
          optionSeriesCode: op.optionSeriesCode || '',
          brokerageName: op.brokerageName || '',
          analysisHouseName: op.analysisHouseName || ''
        }));
        
        setOperacoesFinalizadas(processedOperations);
        
        // Extrair dados do dashboard
        setDashboardData({
          totalWinningOperations: firstContent.totalWinningOperations || 0,
          totalLosingOperations: firstContent.totalLosingOperations || 0,
          totalSwingTradeOperations: firstContent.totalSwingTradeOperations || 0,
          totalDayTradeOperations: firstContent.totalDayTradeOperations || 0,
          totalProfitLoss: firstContent.totalProfitLoss || 0,
          totalProfitLossPercentage: firstContent.totalProfitLossPercentage || 0,
          totalEntryValue: firstContent.totalEntryValue || 0
        });
        
        // Configurar paginação
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
// Removido 'filtros' da lista de dependências

  // quando os filtros são alterados, pois já estamos chamando aplicarFiltros()
  // explicitamente quando os filtros são aplicados
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