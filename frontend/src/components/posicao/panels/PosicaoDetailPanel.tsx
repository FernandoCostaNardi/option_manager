import React, { useEffect, useState } from 'react';
import { Loader2, ArrowUp, ArrowDown, ArrowLeftRight, DollarSign, Calendar } from 'lucide-react';
import { Posicao, EntryLot, PositionOperation } from '../../../types/posicao/posicoes.types';
import { PosicaoService } from '../../../services/posicaoService';

interface PosicaoDetailPanelProps {
  posicao: Posicao;
  onClose: () => void;
}

export function PosicaoDetailPanel({ posicao, onClose }: PosicaoDetailPanelProps) {
  // Estado para lotes e histórico
  const [lotes, setLotes] = useState<EntryLot[]>([]);
  const [historico, setHistorico] = useState<PositionOperation[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'lotes' | 'historico'>('lotes');

  // Carregar dados
  useEffect(() => {
    const carregarDados = async () => {
      try {
        setLoading(true);
        const [lotesData, historicoData] = await Promise.all([
          PosicaoService.buscarLotesPorPosicao(posicao.id),
          PosicaoService.buscarHistoricoPorPosicao(posicao.id)
        ]);
        setLotes(lotesData);
        setHistorico(historicoData);
      } catch (error) {
        console.error('Erro ao carregar detalhes da posição', error);
      } finally {
        setLoading(false);
      }
    };
    
    carregarDados();
  }, [posicao.id]);

  // Função para formatar data
  const formatarData = (dataString: string) => {
    const data = new Date(dataString);
    return data.toLocaleDateString('pt-BR');
  };

  // Função para formatar moeda
  const formatarMoeda = (valor: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(valor);
  };

  // Função para renderizar lotes
  const renderizarLotes = () => {
    if (lotes.length === 0) {
      return (
        <div className="text-center py-4 text-gray-500">
          Nenhum lote de entrada encontrado.
        </div>
      );
    }

    return (
      <div className="overflow-x-auto">
        <table className="min-w-full bg-white">
          <thead>
            <tr className="bg-gray-50 text-left text-xs leading-4 font-medium text-gray-500 uppercase tracking-wider">
              <th className="px-6 py-3">Data</th>
              <th className="px-6 py-3">Preço Unitário</th>
              <th className="px-6 py-3">Quantidade</th>
              <th className="px-6 py-3">Qtd. Restante</th>
              <th className="px-6 py-3">Valor Total</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {lotes.map((lote) => (
              <tr key={lote.id}>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {formatarData(lote.entryDate)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {formatarMoeda(lote.unitPrice)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {lote.originalQuantity}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {lote.remainingQuantity}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {formatarMoeda(lote.totalValue)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  // Função para renderizar histórico
  const renderizarHistorico = () => {
    if (historico.length === 0) {
      return (
        <div className="text-center py-4 text-gray-500">
          Nenhuma operação encontrada no histórico.
        </div>
      );
    }

    return (
      <div className="overflow-x-auto">
        <table className="min-w-full bg-white">
          <thead>
            <tr className="bg-gray-50 text-left text-xs leading-4 font-medium text-gray-500 uppercase tracking-wider">
              <th className="px-6 py-3">Seq.</th>
              <th className="px-6 py-3">Tipo</th>
              <th className="px-6 py-3">Data</th>
              <th className="px-6 py-3">Quantidade</th>
              <th className="px-6 py-3">Preço</th>
              <th className="px-6 py-3">Valor Total</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {historico.map((operacao) => (
              <tr key={operacao.id}>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {operacao.sequence}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${
                    operacao.type === 'ENTRY' 
                      ? 'bg-green-100 text-green-800' 
                      : 'bg-red-100 text-red-800'
                  }`}>
                    {operacao.type === 'ENTRY' ? 'Entrada' : 'Saída'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {formatarData(operacao.date)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {operacao.quantity}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {formatarMoeda(operacao.unitPrice)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {formatarMoeda(operacao.totalAmount)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };
  
  return (
    <div className="bg-white border border-gray-200 rounded-lg shadow-sm p-4 mb-4">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-bold text-gray-800">
          Detalhes da Posição: {posicao.optionSeriesCode}
        </h3>
        <button 
          onClick={onClose}
          className="text-gray-400 hover:text-gray-600"
        >
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd"></path>
          </svg>
        </button>
      </div>

      {/* Sumário da posição */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-gray-50 p-3 rounded-lg">
          <div className="flex items-center mb-1">
            <Calendar className="h-4 w-4 text-indigo-500 mr-1" />
            <span className="text-xs text-gray-500">Criação</span>
          </div>
          <div className="text-sm font-semibold">{formatarData(posicao.creationDate)}</div>
        </div>

        <div className="bg-gray-50 p-3 rounded-lg">
          <div className="flex items-center mb-1">
            <DollarSign className="h-4 w-4 text-indigo-500 mr-1" />
            <span className="text-xs text-gray-500">Preço Médio</span>
          </div>
          <div className="text-sm font-semibold">{formatarMoeda(posicao.averageEntryPrice)}</div>
        </div>

        <div className="bg-gray-50 p-3 rounded-lg">
          <div className="flex items-center mb-1">
            <ArrowLeftRight className="h-4 w-4 text-indigo-500 mr-1" />
            <span className="text-xs text-gray-500">Direção</span>
          </div>
          <div className="flex items-center">
            {posicao.direction === 'LONG' ? (
              <>
                <ArrowUp className="h-4 w-4 text-green-500 mr-1" />
                <span className="text-sm font-semibold text-green-600">LONG</span>
              </>
            ) : (
              <>
                <ArrowDown className="h-4 w-4 text-red-500 mr-1" />
                <span className="text-sm font-semibold text-red-600">SHORT</span>
              </>
            )}
          </div>
        </div>

        <div className="bg-gray-50 p-3 rounded-lg">
          <div className="flex items-center mb-1">
            <span className="text-xs text-gray-500">P&L Não Realizado</span>
          </div>
          <div className={`text-sm font-semibold ${
            posicao.unrealizedProfitLoss >= 0 ? 'text-green-600' : 'text-red-600'
          }`}>
            {formatarMoeda(posicao.unrealizedProfitLoss)}
          </div>
        </div>
      </div>

      {/* Tabs para alternar entre lotes e histórico */}
      <div className="border-b border-gray-200 mb-4">
        <nav className="-mb-px flex space-x-6">
          <button
            className={`py-2 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'lotes' 
                ? 'border-indigo-500 text-indigo-600' 
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
            onClick={() => setActiveTab('lotes')}
          >
            Lotes de Entrada
          </button>
          <button
            className={`py-2 px-1 border-b-2 font-medium text-sm ${
              activeTab === 'historico' 
                ? 'border-indigo-500 text-indigo-600' 
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
            onClick={() => setActiveTab('historico')}
          >
            Histórico de Operações
          </button>
        </nav>
      </div>

      {/* Conteúdo da tab ativa */}
      {loading ? (
        <div className="flex justify-center items-center p-8">
          <Loader2 className="h-6 w-6 animate-spin text-indigo-500" />
          <span className="ml-2 text-gray-600">Carregando...</span>
        </div>
      ) : (
        <div className="bg-gray-50 rounded-lg p-4">
          {activeTab === 'lotes' ? renderizarLotes() : renderizarHistorico()}
        </div>
      )}
    </div>
  );
}