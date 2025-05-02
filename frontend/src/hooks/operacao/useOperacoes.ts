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
          exitDateStart: filtros.exitDateStart,
          exitDateEnd: filtros.exitDateEnd,
          analysisHouseName: filtros.analysisHouseName,
          brokerageName: filtros.brokerageName,
          transactionType: filtros.transactionType,
          optionType: filtros.optionType,
          tradeType: filtros.tradeType
          // Não incluímos status aqui pois estamos buscando apenas operações ativas
        },
        headers: token ? { Authorization: `Bearer ${token}` } : undefined
      });
      
      setOperacoesAtivas(response.data.content || response.data);
      setTotalPages(response.data.totalPages || 1);
      setTotalItems(response.data.totalElements || response.data.length);
    } catch (error) {
      console.error('Erro ao carregar operações ativas:', error);
      setError('Não foi possível carregar as operações ativas. Tente novamente mais tarde.');
    } finally {
      setLoadingAtivas(false);
    }
  }, [currentPage, pageSize, sortField, sortDirection, filtros, setTotalPages, setTotalItems]);

  // Certifique-se de que a função de carregamento das operações finalizadas está mapeando corretamente os dados
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
      
      // Certifique-se de que os dados estão sendo mapeados corretamente
      const data = response.data;
      
      // Verifique se a resposta tem o formato esperado
      if (data && data.content) {
        setOperacoesFinalizadas(data.content);
        setTotalPages(data.totalPages);
        setTotalItems(data.totalElements);
      } else {
        console.error('Formato de resposta inesperado:', data);
        setError('Erro ao carregar operações finalizadas: formato de resposta inválido');
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
  }, [activeTab, currentPage, sortField, sortDirection, filtros, carregarOperacoesAtivas, carregarOperacoesFinalizadas]);

  return {
    operacoesAtivas,
    operacoesFinalizadas,
    loadingAtivas,
    loadingFinalizadas,
    error,
    activeTab,
    setActiveTab,
    carregarOperacoesAtivas,
    carregarOperacoesFinalizadas
  };
};