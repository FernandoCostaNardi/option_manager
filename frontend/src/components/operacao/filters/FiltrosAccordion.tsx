import React, { useState, useEffect } from 'react';
import { Filter, ChevronDown, ChevronUp, X, Loader2 } from 'lucide-react';
import { FiltrosOperacao } from '../../../types/operacao/operacoes.types';
import api from '../../../services/api';
import DatePicker, { registerLocale } from 'react-datepicker';
import {ptBR} from 'date-fns/locale/pt-BR';
import "react-datepicker/dist/react-datepicker.css";
import "../../../styles/datepicker-custom.css"; // Importe os estilos personalizados

interface FiltrosAccordionProps {
  acordeonAberto: boolean;
  setAcordeonAberto: (aberto: boolean) => void;
  filtros: FiltrosOperacao;
  setFiltros: React.Dispatch<React.SetStateAction<FiltrosOperacao>>;
  temFiltrosAtivos: () => boolean;
  obterTextoFiltrosAtivos: () => string;
  limparFiltros: () => void;
  aplicarFiltros: () => void;
}

export const FiltrosAccordion: React.FC<FiltrosAccordionProps> = ({
  acordeonAberto,
  setAcordeonAberto,
  filtros,
  setFiltros,
  temFiltrosAtivos,
  obterTextoFiltrosAtivos,
  limparFiltros,
  aplicarFiltros
}) => {
  // Estado local para armazenar os filtros temporários
  const [filtrosTemp, setFiltrosTemp] = useState<FiltrosOperacao>(filtros);

  // Estados para as datas
  const [entryDateRange, setEntryDateRange] = useState<[Date | null, Date | null]>([
    filtrosTemp.entryDateStart ? new Date(filtrosTemp.entryDateStart) : null,
    filtrosTemp.entryDateEnd ? new Date(filtrosTemp.entryDateEnd) : null
  ]);
  
  const [exitDateRange, setExitDateRange] = useState<[Date | null, Date | null]>([
    filtrosTemp.exitDateStart ? new Date(filtrosTemp.exitDateStart) : null,
    filtrosTemp.exitDateEnd ? new Date(filtrosTemp.exitDateEnd) : null
  ]);

  // Estados para as listas de corretoras e casas de análise
  const [corretoras, setCorretoras] = useState<{id: string, name: string}[]>([]);
  const [casasAnalise, setCasasAnalise] = useState<{id: string, name: string}[]>([]);
  const [carregandoCorretoras, setCarregandoCorretoras] = useState(false);
  const [carregandoCasasAnalise, setCarregandoCasasAnalise] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Função para formatar data para string no formato YYYY-MM-DD
  const formatDateToString = (date: Date | null): string | null => {
    if (!date) return null;
    return date.toISOString().split('T')[0];
  };

  // Função para aplicar os filtros e fechar o acordeon
  const handleAplicarFiltros = () => {
    // Atualiza os filtros temporários com as datas do range
    const updatedFiltros = {
      ...filtrosTemp,
      entryDateStart: entryDateRange[0] ? formatDateToString(entryDateRange[0]) : null,
      entryDateEnd: entryDateRange[1] ? formatDateToString(entryDateRange[1]) : null,
      exitDateStart: exitDateRange[0] ? formatDateToString(exitDateRange[0]) : null,
      exitDateEnd: exitDateRange[1] ? formatDateToString(exitDateRange[1]) : null
    };
    
    setFiltrosTemp(updatedFiltros);
    setFiltros(updatedFiltros); // Atualiza os filtros reais com os temporários
    aplicarFiltros(); // Aplica os filtros (atualiza a tabela)
    setAcordeonAberto(false); // Fecha o acordeon
  };

  // Função para limpar os filtros temporários e aplicar
  const handleLimparFiltros = (e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    
    // Limpa os filtros temporários
    setFiltrosTemp({
      entryDateStart: null,
      entryDateEnd: null,
      exitDateStart: null,
      exitDateEnd: null,
      analysisHouseName: null,
      brokerageName: null
    });
    
    // Limpa os ranges de data
    setEntryDateRange([null, null]);
    setExitDateRange([null, null]);
    
    // Limpa os filtros reais
    limparFiltros();
    
    // Aplica os filtros (atualiza a tabela)
    aplicarFiltros();
  };

  // Função para carregar corretoras
  const carregarCorretoras = async () => {
    setCarregandoCorretoras(true);
    
    try {
      const response = await api.get('/brokerages?page=0&size=100');
      const lista = response.data.content || response.data;
      setCorretoras(Array.isArray(lista) ? lista.map((c: any) => ({ id: c.id, name: c.name })) : []);
    } catch (error) {
      console.error('Erro ao carregar corretoras:', error);
      setError('Não foi possível carregar a lista de corretoras.');
    } finally {
      setCarregandoCorretoras(false);
    }
  };

  // Função para carregar casas de análise
  const carregarCasasAnalise = async () => {
    setCarregandoCasasAnalise(true);
    
    try {
      const response = await api.get('/analysis-houses?page=0&size=100');
      const lista = response.data.content || response.data;
      setCasasAnalise(Array.isArray(lista) ? lista.map((c: any) => ({ id: c.id, name: c.name })) : []);
    } catch (error) {
      console.error('Erro ao carregar casas de análise:', error);
      setError('Não foi possível carregar a lista de casas de análise.');
    } finally {
      setCarregandoCasasAnalise(false);
    }
  };

  // Atualiza os filtros temporários quando os filtros reais mudam
  React.useEffect(() => {
    setFiltrosTemp(filtros);
    
    // Atualiza os ranges de data quando os filtros mudam
    setEntryDateRange([
      filtros.entryDateStart ? new Date(filtros.entryDateStart) : null,
      filtros.entryDateEnd ? new Date(filtros.entryDateEnd) : null
    ]);
    
    setExitDateRange([
      filtros.exitDateStart ? new Date(filtros.exitDateStart) : null,
      filtros.exitDateEnd ? new Date(filtros.exitDateEnd) : null
    ]);
  }, [filtros]);

  // Carregar corretoras e casas de análise quando o acordeon é aberto
  useEffect(() => {
    if (acordeonAberto) {
      if (corretoras.length === 0) {
        carregarCorretoras();
      }
      if (casasAnalise.length === 0) {
        carregarCasasAnalise();
      }
    }
  }, [acordeonAberto]);

  return (
    <div className="mb-6 bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
      <div 
        className="px-6 py-4 flex justify-between items-center cursor-pointer"
        onClick={() => setAcordeonAberto(!acordeonAberto)}
      >
        <div className="flex items-center gap-2">
          <Filter className="w-5 h-5 text-purple-600" />
          <h3 className="font-medium text-gray-800">Filtros</h3>
          {temFiltrosAtivos() && !acordeonAberto && (
            <span className="ml-2 text-sm text-gray-600 truncate max-w-md">
              {obterTextoFiltrosAtivos()}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          {temFiltrosAtivos() && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleLimparFiltros(e);
              }}
              className="text-xs text-gray-500 hover:text-gray-700 flex items-center gap-1"
            >
              <X className="w-4 h-4" /> Limpar filtros
            </button>
          )}
          {acordeonAberto ? (
            <ChevronUp className="w-5 h-5 text-gray-500" />
          ) : (
            <ChevronDown className="w-5 h-5 text-gray-500" />
          )}
        </div>
      </div>
      
      {acordeonAberto && (
        <div className="px-6 py-4 border-t border-gray-100">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Período de Entrada
              </label>
              <DatePicker
                selectsRange={true}
                startDate={entryDateRange[0]}
                endDate={entryDateRange[1]}
                onChange={(update) => {
                  setEntryDateRange(update);
                }}
                isClearable={true}
                locale="pt-BR"
                dateFormat="dd/MM/yyyy"
                placeholderText="Selecione o período de entrada"
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-purple-500"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Período de Saída
              </label>
              <DatePicker
                selectsRange={true}
                startDate={exitDateRange[0]}
                endDate={exitDateRange[1]}
                onChange={(update) => {
                  setExitDateRange(update);
                }}
                isClearable={true}
                locale="pt-BR"
                dateFormat="dd/MM/yyyy"
                placeholderText="Selecione o período de saída"
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-purple-500"
              />
            </div>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Casa de Análise
              </label>
              <div className="relative">
                {carregandoCasasAnalise ? (
                  <div className="w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50 flex items-center">
                    <Loader2 className="w-4 h-4 text-gray-400 animate-spin mr-2" />
                    <span className="text-sm text-gray-500">Carregando...</span>
                  </div>
                ) : (
                  <select
                    value={filtrosTemp.analysisHouseName || ''}
                    onChange={(e) => setFiltrosTemp({...filtrosTemp, analysisHouseName: e.target.value || null})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-purple-500 appearance-none"
                  >
                    <option value="">Selecione uma casa</option>
                    {casasAnalise.map((casa) => (
                      <option key={casa.id} value={casa.name}>
                        {casa.name}
                      </option>
                    ))}
                  </select>
                )}
              </div>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Corretora
              </label>
              <div className="relative">
                {carregandoCorretoras ? (
                  <div className="w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50 flex items-center">
                    <Loader2 className="w-4 h-4 text-gray-400 animate-spin mr-2" />
                    <span className="text-sm text-gray-500">Carregando...</span>
                  </div>
                ) : (
                  <select
                    value={filtrosTemp.brokerageName || ''}
                    onChange={(e) => setFiltrosTemp({...filtrosTemp, brokerageName: e.target.value || null})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-purple-500 appearance-none"
                  >
                    <option value="">Selecione uma corretora</option>
                    {corretoras.map((corretora) => (
                      <option key={corretora.id} value={corretora.name}>
                        {corretora.name}
                      </option>
                    ))}
                  </select>
                )}
              </div>
            </div>
          </div>
          
          {error && (
            <div className="mt-4 p-3 bg-red-50 text-red-700 rounded-lg text-sm flex items-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
              </svg>
              {error}
            </div>
          )}
          
          <div className="mt-4 flex justify-end">
            <button
              onClick={() => handleLimparFiltros()}
              className="px-4 py-2 text-gray-600 bg-gray-100 rounded-md hover:bg-gray-200 mr-2"
            >
              Limpar
            </button>
            <button
              onClick={handleAplicarFiltros}
              className="px-4 py-2 bg-purple-600 text-white rounded-md hover:bg-purple-700"
            >
              Aplicar Filtros
            </button>
          </div>
        </div>
      )}
    </div>
  );
};