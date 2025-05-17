import React from 'react';
import { 
  TrendingUp, 
  TrendingDown, 
  ArrowRight, 
  DollarSign, 
  Calendar, 
  BarChart, 
  PieChart 
} from 'lucide-react';

interface OperationStatsProps {
  loading?: boolean;
}

const OperationStats: React.FC<OperationStatsProps> = ({ loading = false }) => {
  // Em uma implementação real, estes dados viriam de uma API
  const statsData = {
    totalInvestido: 48250.75,
    totalOperacoes: 23,
    operacoesAtivas: 12,
    rendimentoMedio: 8.2,
    rendimentoTotal: 3950.25,
    operacoesVencidas: 11,
    operacoesGanhas: 8,
    operacoesPerdidas: 3,
    proximoVencimento: '15/05/2025',
    proximoAtivo: 'PETR4'
  };

  if (loading) {
    return (
      <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
        <div className="animate-pulse">
          <div className="h-6 bg-gray-200 rounded w-1/3 mb-4"></div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="h-24 bg-gray-200 rounded"></div>
            <div className="h-24 bg-gray-200 rounded"></div>
            <div className="h-24 bg-gray-200 rounded"></div>
            <div className="h-24 bg-gray-200 rounded"></div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
            <div className="h-36 bg-gray-200 rounded"></div>
            <div className="h-36 bg-gray-200 rounded"></div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl shadow-sm overflow-hidden border border-gray-100">
      <div className="px-6 py-4 border-b border-gray-100">
        <h2 className="font-bold text-xl text-gray-800">Resumo de Operações</h2>
      </div>

      <div className="p-6">
        {/* Cards de métricas principais */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {/* Card 1 - Total Investido */}
          <div className="bg-gray-50 rounded-xl p-4 border border-gray-100">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-sm text-gray-500 mb-1">Total Investido</p>
                <h3 className="text-2xl font-bold text-gray-800">
                  R$ {statsData.totalInvestido.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                </h3>
              </div>
              <div className="bg-indigo-100 p-2 rounded-lg">
                <DollarSign className="h-6 w-6 text-indigo-600" />
              </div>
            </div>
          </div>

          {/* Card 2 - Operações Ativas */}
          <div className="bg-gray-50 rounded-xl p-4 border border-gray-100">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-sm text-gray-500 mb-1">Operações Ativas</p>
                <h3 className="text-2xl font-bold text-gray-800">{statsData.operacoesAtivas}</h3>
                <p className="text-sm text-gray-600 mt-1">
                  de {statsData.totalOperacoes} operações
                </p>
              </div>
              <div className="bg-blue-100 p-2 rounded-lg">
                <BarChart className="h-6 w-6 text-blue-600" />
              </div>
            </div>
          </div>

          {/* Card 3 - Rendimento */}
          <div className="bg-gray-50 rounded-xl p-4 border border-gray-100">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-sm text-gray-500 mb-1">Rendimento Médio</p>
                <h3 className="text-2xl font-bold text-green-600">
                  +{statsData.rendimentoMedio}%
                </h3>
                <p className="text-sm text-gray-600 mt-1">
                  R$ {statsData.rendimentoTotal.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                </p>
              </div>
              <div className="bg-green-100 p-2 rounded-lg">
                <TrendingUp className="h-6 w-6 text-green-600" />
              </div>
            </div>
          </div>

          {/* Card 4 - Próximo Vencimento */}
          <div className="bg-gray-50 rounded-xl p-4 border border-gray-100">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-sm text-gray-500 mb-1">Próximo Vencimento</p>
                <h3 className="text-xl font-bold text-gray-800">{statsData.proximoVencimento}</h3>
                <p className="text-sm text-gray-600 mt-1">
                  {statsData.proximoAtivo}
                </p>
              </div>
              <div className="bg-amber-100 p-2 rounded-lg">
                <Calendar className="h-6 w-6 text-amber-600" />
              </div>
            </div>
          </div>
        </div>

        {/* Performance e Gráficos */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
          {/* Desempenho de Operações */}
          <div className="bg-gray-50 rounded-xl p-4 border border-gray-100">
            <h3 className="font-semibold text-gray-700 mb-3">Desempenho</h3>
            <div className="flex items-center gap-4">
              <div className="w-24 h-24 rounded-full bg-gradient-to-r from-indigo-500 to-purple-600 flex items-center justify-center text-white">
                <div className="text-center">
                  <span className="text-2xl font-bold block">
                    {Math.round((statsData.operacoesGanhas / statsData.operacoesVencidas) * 100)}%
                  </span>
                  <span className="text-xs">Sucesso</span>
                </div>
              </div>
              <div className="flex-1">
                <div className="flex justify-between items-center mb-2">
                  <span className="text-sm text-gray-600">Operações Ganhas</span>
                  <span className="text-sm font-medium text-green-600 flex items-center">
                    <TrendingUp className="h-4 w-4 mr-1" />
                    {statsData.