import React, { useState, useEffect } from 'react';
import { 
  Loader2, 
  BarChart2, 
  Calendar, 
  DollarSign, 
  TrendingUp, 
  TrendingDown,
  ArrowRightLeft,
  CreditCard,
  X
} from 'lucide-react';
import { PosicaoService } from '../../../services/posicaoService';
import { Posicao, EntryLot, PositionOperation, PositionDirection } from '../../../types/posicao/posicoes.types';

interface PosicaoDetailPanelProps {
  posicaoId: string;
  onClose: () => void;
}

export function PosicaoDetailPanel({ posicaoId, onClose }: PosicaoDetailPanelProps) {
  const [posicao, setPosicao] = useState<Posicao | null>(null);
  const [lotes, setLotes] = useState<EntryLot[]>([]);
  const [historico, setHistorico] = useState<PositionOperation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'lotes' | 'historico'>('lotes');

  // Carregar dados da posição
  useEffect(() => {
    const carregarDados = async () => {
      setLoading(true);
      setError(null);
      
      try {
        const [posicaoData, lotesData, historicoData] = await Promise.all([
          PosicaoService.buscarPosicaoPorId(posicaoId),
          PosicaoService.buscarLotesPorPosicao(posicaoId),
          PosicaoService.buscarHistoricoPorPosicao(posicaoId)
        ]);
        
        setPosicao(posicaoData);
        setLotes(lotesData);
        setHistorico(historicoData);
      } catch (err) {
        console.error('Erro ao carregar detalhes da posição:', err);
        setError('Não foi possível carregar os detalhes da posição.');
      } finally {
        setLoading(false);
      }
    };
    
    carregarDados();
  }, [posicaoId]);

  // Helper para formatar data
  const formatarData = (dataString: string) => {
    const data = new Date(dataString);
    return data.toLocaleDateString('pt-BR');
  };

  // Helper para formatar moeda
  const formatarMoeda = (valor: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(valor);
  };

  // Se estiver carregando
  if (loading) {
    return (
      <div className="bg-white border border-gray-200 rounded-lg p-6 mt-2">
        <div className="flex justify-center items-center h-40">
          <Loader2 className="w-8 h-8 text-indigo-600 animate-spin" />
          <span className="ml-2 text-gray-600">Carregando detalhes da posição...</span>
        </div>
      </div>
    );
  }

  // Se ocorreu erro
  if (error || !posicao) {
    return (
      <div className="bg-white border border-gray-200 rounded-lg p-6 mt-2">
        <div className="flex flex-col items-center justify-center h-40">
          <p className="text-red-500 mb-2">
            {error || 'Não foi possível carregar os detalhes da posição.'}
          </p>
          <button
            onClick={onClose}
            className="px-4 py-2 bg-indigo-100 text-indigo-700 rounded-md hover:bg-indigo-200"
          >
            Fechar
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white border border-gray-200 rounded-lg p-6 mt-2">
      <div className="mb-6">
        <div className="flex justify-between items-start">
          <h3 className="text-lg font-bold text-gray-800 mb-4">
            Detalhes da Posição
          </h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Cards de resumo */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          {/* Card: Preço Médio */}
          <div className="p-4 bg-gray-50 rounded-lg border border-gray-100">
            <div className="flex items-center mb-2">
              <DollarSign className="h-5 w-5 text-indigo-500 mr-2" />
              <span className="text-sm font-medium text-gray-600">Preço Médio</span>
            </div>
            <p className="text-lg font-bold text-gray-900">
              {formatarMoeda(posicao.averageEntryPrice)}
            </p>
          </div>

          {/* Card: Quantidade */}
          <div className="p-4 bg-gray-50 rounded-lg border border-gray-100">
            <div className="flex items-center mb-2">
              <BarChart2 className="h-5 w-5 text-indigo-500 mr-2" />
              <span className="text-sm font-medium text-gray-600">Quantidade</span>
            </div>
            <p className="text-lg font-bold text-gray-900">
              {posicao.remainingQuantity} / {posicao.totalQuantity}
            </p>
            <p className="text-xs text-gray-500 mt-1">Restante / Total</p>
          </div>

          {/* Card: Direção */}
          <div className="p-4 bg-gray-50 rounded-lg border border-gray-100">
            <div className="flex items-center mb-2">
              <ArrowRightLeft className="h-5 w-5 text-indigo-500 mr-2" />
              <span className="text-sm font-medium text-gray-600">Direção</span>
            </div>
            <div className="flex items-center">
              {posicao.direction === PositionDirection.LONG ? (
                <>
                  <TrendingUp className="h-5 w-5 text-green-500 mr-1" />
                  <p className="text-lg font-bold text-green-600">LONG</p>
                </>
              ) : (
                <>
                  <TrendingDown className="h-5 w-5 text-red-500 mr-1" />
                  <p className="text-lg font-bold text-red-600">SHORT</p>
                </>
              )}
            </div>
          </div>

          {/* Card: P&L Não Realizado */}
          <div className="p-4 bg-gray-50 rounded-lg border border-gray-100">
            <div className="flex items-center mb-2">
              <span className="text-sm font-medium text-gray-600">P&L Não Realizado</span>
            </div>
            <p className={`text-lg font-bold ${posicao.unrealizedProfitLoss >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              {formatarMoeda(posicao.unrealizedProfitLoss)}
            </p>
          </div>
        </div>

        {/* Cards de informações adicionais */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          {/* Card: Informações da Posição */}
          <div className="p-4 bg-gray-50 rounded-lg border border-gray-100">
            <h4 className="text-md font-medium text-gray-700 mb-3">Informações da Posição</h4>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Série da Opção:</span>
                <span className="text-sm font-medium">{posicao.optionSeriesCode}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Tipo de Opção:</span>
                <span className="text-sm font-medium">{posicao.optionType}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Corretora:</span>
                <span className="text-sm font-medium">{posicao.brokerageName}</span>
              </div>
              {posicao.analysisHouseName && (
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Casa de Análise:</span>
                  <span className="text-sm font-medium">{posicao.analysisHouseName}</span>
                </div>
              )}
            </div>
          </div>

          {/* Card: Valores */}
          <div className="p-4 bg-gray-50 rounded-lg border border-gray-100">
            <h4 className="text-md font-medium text-gray-700 mb-3">Valores</h4>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Capital Investido (Total):</span>
                <span className="text-sm font-medium">{formatarMoeda(posicao.totalInvested)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Capital Investido (Atual):</span>
                <span className="text-sm font-medium">{formatarMoeda(posicao.currentInvested)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Lucro/Prejuízo Realizado:</span>
                <span className={`text-sm font-medium ${posicao.realizedProfitLoss >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                  {formatarMoeda(posicao.realizedProfitLoss)}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Lucro/Prejuízo Não Realizado:</span>
                <span className={`text-sm font-medium ${posicao.unrealizedProfitLoss >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                  {formatarMoeda(posicao.unrealizedProfitLoss)}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Abas para Lotes e Histórico */}
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

      {/* Conteúdo da aba ativa */}
      {activeTab === 'lotes' ? (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Data
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Preço Unitário
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Quantidade Original
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Quantidade Restante
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Valor Total
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {lotes.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-4 text-center text-sm text-gray-500">
                    Nenhum lote encontrado
                  </td>
                </tr>
              ) : (
                lotes.map(lote => (
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
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Sequência
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Tipo
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Data
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Quantidade
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Preço Unitário
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Valor Total
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {historico.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-4 text-center text-sm text-gray-500">
                    Nenhuma operação encontrada
                  </td>
                </tr>
              ) : (
                historico.map(operacao => (
                  <tr key={operacao.id}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {operacao.sequence}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
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
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}