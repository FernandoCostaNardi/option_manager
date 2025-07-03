import React, { useState, useEffect } from 'react';
import { 
  TrendingUp, TrendingDown, BarChart3, PieChart, 
  Calendar, Download, RefreshCw, Target, Clock, 
  AlertTriangle, CheckCircle, Building2, Zap
} from 'lucide-react';
import { InvoiceProcessingService } from '../services/invoiceProcessingService';

// ===== INTERFACES =====
interface ProcessingPerformance {
  totalProcessed: number;
  successRate: number;
  averageOperationsPerInvoice: number;
  mostCommonErrors: Array<{ error: string; count: number }>;
  processingTrend: Array<{ date: string; processed: number; errors: number }>;
}

interface BrokerageStats {
  brokerageName: string;
  totalInvoices: number;
  processedInvoices: number;
  totalOperationsCreated: number;
  successRate: number;
  avgProcessingTime: number;
}

interface TradeTypeAnalysis {
  dayTradeOperations: number;
  swingTradeOperations: number;
  detectionAccuracy: number;
  manualReviewNeeded: number;
}

// ===== COMPONENTE PRINCIPAL =====
export function AnalyticsTab() {
  const [performance, setPerformance] = useState<ProcessingPerformance | null>(null);
  const [brokerageStats, setBrokerageStats] = useState<BrokerageStats[]>([]);
  const [tradeTypeAnalysis, setTradeTypeAnalysis] = useState<TradeTypeAnalysis | null>(null);
  const [selectedPeriod, setSelectedPeriod] = useState<'WEEK' | 'MONTH' | 'QUARTER'>('MONTH');
  const [loading, setLoading] = useState(true);

  // ===== CARREGAMENTO DE DADOS =====
  useEffect(() => {
    loadAnalyticsData();
  }, [selectedPeriod]);

  const loadAnalyticsData = async () => {
    setLoading(true);
    try {
      const [performanceData, brokerageData, tradeTypeData] = await Promise.all([
        InvoiceProcessingService.getProcessingPerformance(selectedPeriod),
        InvoiceProcessingService.getBrokerageStats(),
        InvoiceProcessingService.getTradeTypeAnalysis()
      ]);

      setPerformance(performanceData);
      setBrokerageStats(brokerageData);
      setTradeTypeAnalysis(tradeTypeData);
    } catch (error) {
      console.error('Erro ao carregar analytics:', error);
    } finally {
      setLoading(false);
    }
  };

  // ===== FUNÇÕES DE FORMATAÇÃO =====
  const formatPercent = (value: number) => `${(value * 100).toFixed(1)}%`;
  const formatNumber = (value: number) => value.toLocaleString('pt-BR');
  const formatDate = (dateString: string) => new Date(dateString).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' });

  // ===== RENDERIZAÇÃO =====
  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <RefreshCw className="h-12 w-12 text-purple-500 animate-spin mx-auto mb-4" />
          <p className="text-gray-500">Carregando analytics...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* HEADER COM CONTROLES */}
      <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold mb-1">Análises e Relatórios</h3>
            <p className="text-gray-500 text-sm">Insights sobre o processamento de notas de corretagem</p>
          </div>

          <div className="flex items-center gap-3">
            {/* SELETOR DE PERÍODO */}
            <div className="flex rounded-lg border border-gray-200 p-1">
              {[
                { key: 'WEEK', label: 'Semana' },
                { key: 'MONTH', label: 'Mês' },
                { key: 'QUARTER', label: 'Trimestre' }
              ].map(({ key, label }) => (
                <button
                  key={key}
                  onClick={() => setSelectedPeriod(key as any)}
                  className={`px-3 py-1 text-sm rounded-md transition-colors ${
                    selectedPeriod === key
                      ? 'bg-purple-100 text-purple-700'
                      : 'text-gray-500 hover:text-gray-700'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>

            <button
              onClick={loadAnalyticsData}
              className="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg"
              title="Atualizar dados"
            >
              <RefreshCw className="h-4 w-4" />
            </button>

            <button className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 flex items-center gap-2">
              <Download className="h-4 w-4" />
              Exportar
            </button>
          </div>
        </div>
      </div>

      {/* MÉTRICAS PRINCIPAIS */}
      {performance && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-500">Total Processadas</p>
                <p className="text-2xl font-bold text-gray-900">{formatNumber(performance.totalProcessed)}</p>
              </div>
              <BarChart3 className="h-8 w-8 text-blue-500" />
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-500">Taxa de Sucesso</p>
                <p className="text-2xl font-bold text-green-600">{formatPercent(performance.successRate)}</p>
              </div>
              <Target className="h-8 w-8 text-green-500" />
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-500">Média de Ops/Nota</p>
                <p className="text-2xl font-bold text-purple-600">{performance.averageOperationsPerInvoice.toFixed(1)}</p>
              </div>
              <Zap className="h-8 w-8 text-purple-500" />
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-500">Principais Erros</p>
                <p className="text-2xl font-bold text-red-600">{performance.mostCommonErrors.length}</p>
              </div>
              <AlertTriangle className="h-8 w-8 text-red-500" />
            </div>
          </div>
        </div>
      )}

      {/* GRÁFICO DE TENDÊNCIA */}
      {performance && performance.processingTrend.length > 0 && (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h4 className="text-lg font-semibold mb-4">Tendência de Processamento</h4>
          <div className="space-y-4">
            {/* LEGENDA */}
            <div className="flex items-center gap-6 text-sm">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                <span className="text-gray-600">Processadas com Sucesso</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                <span className="text-gray-600">Erros</span>
              </div>
            </div>

            {/* GRÁFICO SIMPLES (implementação básica) */}
            <div className="space-y-2">
              {performance.processingTrend.map((point, index) => {
                const maxValue = Math.max(...performance.processingTrend.map(p => p.processed + p.errors));
                const successWidth = (point.processed / maxValue) * 100;
                const errorWidth = (point.errors / maxValue) * 100;

                return (
                  <div key={index} className="flex items-center gap-4">
                    <div className="w-16 text-xs text-gray-500 text-right">
                      {formatDate(point.date)}
                    </div>
                    <div className="flex-1 flex items-center gap-1">
                      <div className="flex-1 bg-gray-100 rounded-full h-6 flex overflow-hidden">
                        <div 
                          className="bg-green-500 h-full" 
                          style={{ width: `${successWidth}%` }}
                        ></div>
                        <div 
                          className="bg-red-500 h-full" 
                          style={{ width: `${errorWidth}%` }}
                        ></div>
                      </div>
                      <div className="w-12 text-xs text-gray-600 text-right">
                        {point.processed + point.errors}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* ANÁLISE POR CORRETORA */}
      <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <h4 className="text-lg font-semibold mb-4">Performance por Corretora</h4>
        {brokerageStats.length === 0 ? (
          <p className="text-gray-500 text-center py-8">Nenhum dado disponível</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="text-left border-b border-gray-200">
                  <th className="pb-3 text-sm font-medium text-gray-500">Corretora</th>
                  <th className="pb-3 text-sm font-medium text-gray-500 text-center">Notas</th>
                  <th className="pb-3 text-sm font-medium text-gray-500 text-center">Processadas</th>
                  <th className="pb-3 text-sm font-medium text-gray-500 text-center">Operações</th>
                  <th className="pb-3 text-sm font-medium text-gray-500 text-center">Taxa Sucesso</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {brokerageStats.map((brokerage, index) => (
                  <tr key={index} className="hover:bg-gray-50">
                    <td className="py-3">
                      <div className="flex items-center gap-2">
                        <Building2 className="h-4 w-4 text-gray-400" />
                        <span className="font-medium text-gray-900">{brokerage.brokerageName}</span>
                      </div>
                    </td>
                    <td className="py-3 text-center text-gray-900">{formatNumber(brokerage.totalInvoices)}</td>
                    <td className="py-3 text-center text-gray-900">{formatNumber(brokerage.processedInvoices)}</td>
                    <td className="py-3 text-center text-gray-900">{formatNumber(brokerage.totalOperationsCreated)}</td>
                    <td className="py-3 text-center">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        brokerage.successRate > 0.9 
                          ? 'bg-green-100 text-green-800'
                          : brokerage.successRate > 0.7
                            ? 'bg-yellow-100 text-yellow-800'
                            : 'bg-red-100 text-red-800'
                      }`}>
                        {formatPercent(brokerage.successRate)}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* ANÁLISE DE TRADE TYPES */}
      {tradeTypeAnalysis && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <h4 className="text-lg font-semibold mb-4">Análise de Trade Types</h4>
            <div className="space-y-4">
              <div className="flex items-center justify-between p-3 bg-blue-50 rounded-lg">
                <div className="flex items-center gap-3">
                  <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
                  <span className="text-blue-700 font-medium">Day Trade</span>
                </div>
                <span className="text-blue-800 font-bold">{formatNumber(tradeTypeAnalysis.dayTradeOperations)}</span>
              </div>

              <div className="flex items-center justify-between p-3 bg-green-50 rounded-lg">
                <div className="flex items-center gap-3">
                  <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                  <span className="text-green-700 font-medium">Swing Trade</span>
                </div>
                <span className="text-green-800 font-bold">{formatNumber(tradeTypeAnalysis.swingTradeOperations)}</span>
              </div>

              <div className="pt-3 border-t border-gray-200">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-600">Precisão da Detecção:</span>
                  <span className="font-medium">{formatPercent(tradeTypeAnalysis.detectionAccuracy)}</span>
                </div>
                <div className="flex items-center justify-between text-sm mt-1">
                  <span className="text-gray-600">Revisão Manual:</span>
                  <span className="font-medium text-orange-600">{formatNumber(tradeTypeAnalysis.manualReviewNeeded)}</span>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <h4 className="text-lg font-semibold mb-4">Erros Mais Comuns</h4>
            {performance && performance.mostCommonErrors.length > 0 ? (
              <div className="space-y-3">
                {performance.mostCommonErrors.slice(0, 5).map((error, index) => (
                  <div key={index} className="flex items-center justify-between p-3 bg-red-50 border border-red-100 rounded-lg">
                    <div className="flex items-start gap-2 flex-1">
                      <AlertTriangle className="h-4 w-4 text-red-500 mt-0.5 flex-shrink-0" />
                      <span className="text-sm text-red-700 leading-tight">{error.error}</span>
                    </div>
                    <span className="text-red-800 font-bold ml-3">{error.count}</span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-8">
                <CheckCircle className="h-8 w-8 text-green-500 mx-auto mb-2" />
                <p className="text-green-700 font-medium">Nenhum erro registrado!</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}