import React from 'react';
import { 
  ListChecks, 
  BarChart, 
  DollarSign, 
  Percent, 
  TrendingUp, 
  TrendingDown,
  Loader2
} from 'lucide-react';
import { PosicoesDashboard } from '../../../services/posicaoService';

interface PosicoesDashboardProps {
  dashboardData: PosicoesDashboard;
  loading: boolean;
}

export function PosicoesDashboardCards({ dashboardData, loading }: PosicoesDashboardProps) {
  // Helper para formatar moeda
  const formatarMoeda = (valor: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(valor);
  };

  if (loading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
        {[...Array(4)].map((_, index) => (
          <div key={index} className="bg-white rounded-xl shadow-sm p-6 border border-gray-100 animate-pulse">
            <div className="h-5 bg-gray-200 rounded w-24 mb-3"></div>
            <div className="h-7 bg-gray-300 rounded w-20"></div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
      {/* Card: Total de Posições */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
        <div className="flex justify-between items-start">
          <div>
            <p className="text-sm text-gray-500 mb-1">Posições Ativas</p>
            <h3 className="text-2xl font-bold text-gray-800">
              {dashboardData.totalPositions}
            </h3>
          </div>
          <div className="p-3 bg-blue-100 rounded-full">
            <ListChecks className="h-5 w-5 text-blue-600" />
          </div>
        </div>
      </div>

      {/* Card: LONG / SHORT */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
        <div className="flex justify-between items-start">
          <div>
            <p className="text-sm text-gray-500 mb-1">LONG / SHORT</p>
            <h3 className="text-2xl font-bold text-gray-800">
              {dashboardData.totalLongPositions} / {dashboardData.totalShortPositions}
            </h3>
          </div>
          <div className="p-3 bg-purple-100 rounded-full">
            <BarChart className="h-5 w-5 text-purple-600" />
          </div>
        </div>
        <div className="mt-3 h-2 bg-gray-100 rounded-full overflow-hidden">
          <div 
            className="h-full bg-blue-600" 
            style={{ 
              width: `${dashboardData.totalPositions ? 
                (dashboardData.totalLongPositions / dashboardData.totalPositions) * 100 : 0}%` 
            }}
          ></div>
        </div>
        <div className="flex justify-between mt-1">
          <div className="flex items-center">
            <div className="w-3 h-3 bg-blue-600 rounded-full mr-1"></div>
            <span className="text-xs text-gray-500">LONG</span>
          </div>
          <div className="flex items-center">
            <div className="w-3 h-3 bg-gray-300 rounded-full mr-1"></div>
            <span className="text-xs text-gray-500">SHORT</span>
          </div>
        </div>
      </div>

      {/* Card: Capital Investido */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
        <div className="flex justify-between items-start">
          <div>
            <p className="text-sm text-gray-500 mb-1">Capital Investido</p>
            <h3 className="text-2xl font-bold text-gray-800">
              {formatarMoeda(dashboardData.totalInvested)}
            </h3>
          </div>
          <div className="p-3 bg-green-100 rounded-full">
            <DollarSign className="h-5 w-5 text-green-600" />
          </div>
        </div>
      </div>

      {/* Card: Lucro Não Realizado */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
        <div className="flex justify-between items-start">
          <div>
            <p className="text-sm text-gray-500 mb-1">Lucro Não Realizado</p>
            <h3 className={`text-2xl font-bold ${
              dashboardData.totalUnrealizedProfitLoss >= 0 ? 'text-green-600' : 'text-red-600'
            }`}>
              {formatarMoeda(dashboardData.totalUnrealizedProfitLoss)}
            </h3>
            <p className={`text-sm mt-1 ${
              dashboardData.totalUnrealizedProfitLossPercentage >= 0 ? 'text-green-600' : 'text-red-600'
            }`}>
              {dashboardData.totalUnrealizedProfitLossPercentage >= 0 ? (
                <span className="flex items-center">
                  <TrendingUp className="h-3 w-3 mr-1" />
                  {dashboardData.totalUnrealizedProfitLossPercentage.toFixed(2)}%
                </span>
              ) : (
                <span className="flex items-center">
                  <TrendingDown className="h-3 w-3 mr-1" />
                  {Math.abs(dashboardData.totalUnrealizedProfitLossPercentage).toFixed(2)}%
                </span>
              )}
            </p>
          </div>
          <div className="p-3 bg-blue-100 rounded-full">
            <Percent className="h-5 w-5 text-blue-600" />
          </div>
        </div>
      </div>
    </div>
  );
}