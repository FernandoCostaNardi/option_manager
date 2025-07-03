import React, { useState, useEffect } from 'react';
import { 
  Shield, AlertTriangle, CheckCircle, Search, Calendar, 
  RefreshCw, Download, Target, TrendingUp, Eye, 
  FileText, Activity, Clock, XCircle
} from 'lucide-react';
import { InvoiceProcessingService, ReconciliationResult, OperationDiscrepancy } from '../services/invoiceProcessingService';

// ===== INTERFACES =====
interface ReconciliationFilters {
  startDate: string;
  endDate: string;
  includePerfectMatches: boolean;
  includeDiscrepancies: boolean;
}

// ===== COMPONENTE PRINCIPAL =====
export function ReconciliationTab() {
  const [reconciliationResult, setReconciliationResult] = useState<ReconciliationResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [running, setRunning] = useState(false);
  const [filters, setFilters] = useState<ReconciliationFilters>({
    startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0], // 30 dias atrás
    endDate: new Date().toISOString().split('T')[0], // hoje
    includePerfectMatches: true,
    includeDiscrepancies: true
  });
  const [selectedDiscrepancy, setSelectedDiscrepancy] = useState<OperationDiscrepancy | null>(null);

  // ===== CARREGAMENTO INICIAL =====
  useEffect(() => {
    loadExistingReconciliation();
  }, []);

  // ===== FUNÇÕES =====
  const loadExistingReconciliation = async () => {
    setLoading(true);
    try {
      const result = await InvoiceProcessingService.getReconciliationReport(filters.startDate, filters.endDate);
      setReconciliationResult(result);
    } catch (error) {
      console.error('Erro ao carregar reconciliação:', error);
    } finally {
      setLoading(false);
    }
  };

  const runReconciliation = async () => {
    setRunning(true);
    try {
      const result = await InvoiceProcessingService.runReconciliation(filters.startDate, filters.endDate);
      setReconciliationResult(result);
    } catch (error) {
      console.error('Erro ao executar reconciliação:', error);
      alert('Erro ao executar reconciliação');
    } finally {
      setRunning(false);
    }
  };

  const exportReconciliation = async () => {
    try {
      const blob = await InvoiceProcessingService.exportProcessingData(filters.startDate, filters.endDate);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `reconciliacao_${filters.startDate}_${filters.endDate}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (error) {
      console.error('Erro ao exportar:', error);
      alert('Erro ao exportar dados');
    }
  };

  // ===== FORMATAÇÃO =====
  const formatDate = (dateString: string) => new Date(dateString).toLocaleDateString('pt-BR');
  const formatPercent = (value: number) => `${(value * 100).toFixed(1)}%`;
  const formatNumber = (value: number) => value.toLocaleString('pt-BR');

  const getDiscrepancyIcon = (type: string) => {
    switch (type) {
      case 'MISSING_IN_SYSTEM': return <AlertTriangle className="h-4 w-4 text-red-500" />;
      case 'MISSING_IN_INVOICE': return <XCircle className="h-4 w-4 text-orange-500" />;
      case 'VALUE_MISMATCH': return <TrendingUp className="h-4 w-4 text-yellow-500" />;
      case 'DATE_MISMATCH': return <Clock className="h-4 w-4 text-blue-500" />;
      default: return <AlertTriangle className="h-4 w-4 text-gray-500" />;
    }
  };

  const getDiscrepancyLabel = (type: string) => {
    switch (type) {
      case 'MISSING_IN_SYSTEM': return 'Ausente no Sistema';
      case 'MISSING_IN_INVOICE': return 'Ausente na Nota';
      case 'VALUE_MISMATCH': return 'Divergência de Valor';
      case 'DATE_MISMATCH': return 'Divergência de Data';
      default: return 'Discrepância Desconhecida';
    }
  };

  const getDiscrepancyColor = (type: string) => {
    switch (type) {
      case 'MISSING_IN_SYSTEM': return 'bg-red-50 border-red-200';
      case 'MISSING_IN_INVOICE': return 'bg-orange-50 border-orange-200';
      case 'VALUE_MISMATCH': return 'bg-yellow-50 border-yellow-200';
      case 'DATE_MISMATCH': return 'bg-blue-50 border-blue-200';
      default: return 'bg-gray-50 border-gray-200';
    }
  };

  // ===== RENDERIZAÇÃO =====
  return (
    <div className="space-y-6">
      {/* HEADER COM CONTROLES */}
      <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div>
            <h3 className="text-lg font-semibold mb-1">Reconciliação e Auditoria</h3>
            <p className="text-gray-500 text-sm">Compare operações das notas com as operações do sistema</p>
          </div>

          <div className="flex items-center gap-3">
            {/* FILTROS DE DATA */}
            <div className="flex items-center gap-2">
              <Calendar className="h-4 w-4 text-gray-400" />
              <input
                type="date"
                value={filters.startDate}
                onChange={(e) => setFilters(prev => ({ ...prev, startDate: e.target.value }))}
                className="px-3 py-1 border border-gray-300 rounded text-sm"
              />
              <span className="text-gray-400">até</span>
              <input
                type="date"
                value={filters.endDate}
                onChange={(e) => setFilters(prev => ({ ...prev, endDate: e.target.value }))}
                className="px-3 py-1 border border-gray-300 rounded text-sm"
              />
            </div>

            <button
              onClick={runReconciliation}
              disabled={running}
              className="px-4 py-2 bg-purple-500 text-white rounded-lg hover:bg-purple-600 disabled:bg-gray-300 disabled:cursor-not-allowed flex items-center gap-2"
            >
              {running ? (
                <>
                  <RefreshCw className="h-4 w-4 animate-spin" />
                  Executando...
                </>
              ) : (
                <>
                  <Search className="h-4 w-4" />
                  Reconciliar
                </>
              )}
            </button>

            {reconciliationResult && (
              <button
                onClick={exportReconciliation}
                className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 flex items-center gap-2"
              >
                <Download className="h-4 w-4" />
                Exportar
              </button>
            )}
          </div>
        </div>
      </div>

      {/* RESULTADO DA RECONCILIAÇÃO */}
      {loading ? (
        <div className="flex items-center justify-center min-h-[300px]">
          <div className="text-center">
            <RefreshCw className="h-12 w-12 text-purple-500 animate-spin mx-auto mb-4" />
            <p className="text-gray-500">Carregando dados de reconciliação...</p>
          </div>
        </div>
      ) : reconciliationResult ? (
        <>
          {/* RESUMO EXECUTIVO */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
            <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 text-center">
              <FileText className="h-8 w-8 text-blue-500 mx-auto mb-2" />
              <div className="text-2xl font-bold text-gray-900">{formatNumber(reconciliationResult.totalInvoiceOperations)}</div>
              <div className="text-sm text-gray-500">Ops. nas Notas</div>
            </div>

            <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 text-center">
              <Target className="h-8 w-8 text-green-500 mx-auto mb-2" />
              <div className="text-2xl font-bold text-gray-900">{formatNumber(reconciliationResult.totalSystemOperations)}</div>
              <div className="text-sm text-gray-500">Ops. no Sistema</div>
            </div>

            <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 text-center">
              <CheckCircle className="h-8 w-8 text-green-500 mx-auto mb-2" />
              <div className="text-2xl font-bold text-green-600">{formatNumber(reconciliationResult.matchedOperations)}</div>
              <div className="text-sm text-gray-500">Reconciliadas</div>
            </div>

            <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 text-center">
              <AlertTriangle className="h-8 w-8 text-red-500 mx-auto mb-2" />
              <div className="text-2xl font-bold text-red-600">{formatNumber(reconciliationResult.unmatchedInvoiceOperations)}</div>
              <div className="text-sm text-gray-500">Não Encontradas</div>
            </div>

            <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 text-center">
              <XCircle className="h-8 w-8 text-orange-500 mx-auto mb-2" />
              <div className="text-2xl font-bold text-orange-600">{formatNumber(reconciliationResult.unmatchedSystemOperations)}</div>
              <div className="text-sm text-gray-500">Extras no Sistema</div>
            </div>
          </div>

          {/* INDICADOR DE SAÚDE */}
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <h4 className="text-lg font-semibold mb-4">Índice de Reconciliação</h4>
            <div className="space-y-4">
              {(() => {
                const total = reconciliationResult.totalInvoiceOperations;
                const matched = reconciliationResult.matchedOperations;
                const matchRate = total > 0 ? matched / total : 0;
                
                return (
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-medium text-gray-700">Taxa de Correspondência</span>
                      <span className={`text-sm font-bold ${
                        matchRate >= 0.95 ? 'text-green-600' : 
                        matchRate >= 0.85 ? 'text-yellow-600' : 'text-red-600'
                      }`}>
                        {formatPercent(matchRate)}
                      </span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-3">
                      <div 
                        className={`h-3 rounded-full ${
                          matchRate >= 0.95 ? 'bg-green-500' : 
                          matchRate >= 0.85 ? 'bg-yellow-500' : 'bg-red-500'
                        }`}
                        style={{ width: `${matchRate * 100}%` }}
                      ></div>
                    </div>
                    <div className="mt-2 text-xs text-gray-500">
                      {matchRate >= 0.95 && "Excelente! Alta concordância entre notas e sistema."}
                      {matchRate >= 0.85 && matchRate < 0.95 && "Boa reconciliação, algumas discrepâncias menores."}
                      {matchRate < 0.85 && "Atenção! Muitas discrepâncias encontradas."}
                    </div>
                  </div>
                );
              })()}
            </div>
          </div>

          {/* DISCREPÂNCIAS ENCONTRADAS */}
          {reconciliationResult.discrepancies.length > 0 && (
            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <h4 className="text-lg font-semibold mb-4">Discrepâncias Encontradas ({reconciliationResult.discrepancies.length})</h4>
              
              <div className="space-y-3">
                {reconciliationResult.discrepancies.map((discrepancy, index) => (
                  <div 
                    key={index} 
                    className={`p-4 border rounded-lg cursor-pointer hover:shadow-sm transition-shadow ${getDiscrepancyColor(discrepancy.type)}`}
                    onClick={() => setSelectedDiscrepancy(discrepancy)}
                  >
                    <div className="flex items-start gap-3">
                      {getDiscrepancyIcon(discrepancy.type)}
                      <div className="flex-1">
                        <div className="flex items-center justify-between">
                          <span className="font-medium text-gray-900">{getDiscrepancyLabel(discrepancy.type)}</span>
                          <Eye className="h-4 w-4 text-gray-400" />
                        </div>
                        <p className="text-sm text-gray-600 mt-1">{discrepancy.description}</p>
                        
                        {/* PREVIEW DOS DADOS */}
                        <div className="mt-2 grid grid-cols-1 lg:grid-cols-2 gap-3 text-xs">
                          {discrepancy.invoiceOperation && (
                            <div className="bg-white bg-opacity-50 p-2 rounded">
                              <div className="font-medium text-gray-700">Na Nota:</div>
                              <div className="text-gray-600">
                                {discrepancy.invoiceOperation.asset} • 
                                {discrepancy.invoiceOperation.quantity} • 
                                R$ {discrepancy.invoiceOperation.price}
                              </div>
                            </div>
                          )}
                          
                          {discrepancy.systemOperation && (
                            <div className="bg-white bg-opacity-50 p-2 rounded">
                              <div className="font-medium text-gray-700">No Sistema:</div>
                              <div className="text-gray-600">
                                {discrepancy.systemOperation.optionSeries?.code} • 
                                {discrepancy.systemOperation.quantity} • 
                                R$ {discrepancy.systemOperation.entryUnitPrice}
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* NENHUMA DISCREPÂNCIA */}
          {reconciliationResult.discrepancies.length === 0 && (
            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <div className="text-center py-8">
                <CheckCircle className="h-16 w-16 text-green-500 mx-auto mb-4" />
                <h3 className="text-lg font-semibold text-green-800 mb-2">Perfeita Reconciliação!</h3>
                <p className="text-green-600">
                  Todas as operações das notas correspondem perfeitamente às operações do sistema.
                </p>
              </div>
            </div>
          )}
        </>
      ) : (
        /* ESTADO INICIAL */
        <div className="bg-white p-12 rounded-xl shadow-sm border border-gray-100">
          <div className="text-center">
            <Shield className="h-16 w-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">Reconciliação de Dados</h3>
            <p className="text-gray-500 mb-6 max-w-md mx-auto">
              Execute uma reconciliação para comparar as operações das notas de corretagem 
              com as operações registradas no sistema.
            </p>
            <button
              onClick={runReconciliation}
              disabled={running}
              className="px-6 py-3 bg-purple-500 text-white rounded-lg hover:bg-purple-600 disabled:bg-gray-300 flex items-center gap-2 mx-auto"
            >
              {running ? (
                <>
                  <RefreshCw className="h-5 w-5 animate-spin" />
                  Executando Reconciliação...
                </>
              ) : (
                <>
                  <Search className="h-5 w-5" />
                  Iniciar Reconciliação
                </>
              )}
            </button>
          </div>
        </div>
      )}

      {/* MODAL DE DETALHES DA DISCREPÂNCIA */}
      {selectedDiscrepancy && (
        <DiscrepancyDetailsModal 
          discrepancy={selectedDiscrepancy}
          onClose={() => setSelectedDiscrepancy(null)}
        />
      )}
    </div>
  );
}

// ===== MODAL DE DETALHES =====
interface DiscrepancyDetailsModalProps {
  discrepancy: OperationDiscrepancy;
  onClose: () => void;
}

function DiscrepancyDetailsModal({ discrepancy, onClose }: DiscrepancyDetailsModalProps) {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-xl max-w-4xl w-full max-h-[90vh] overflow-hidden">
        {/* HEADER */}
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              {getDiscrepancyIcon(discrepancy.type)}
              <h3 className="text-lg font-semibold">{getDiscrepancyLabel(discrepancy.type)}</h3>
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600"
            >
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        {/* CONTEÚDO */}
        <div className="p-6 overflow-y-auto max-h-[calc(90vh-120px)]">
          <div className="space-y-6">
            {/* DESCRIÇÃO */}
            <div>
              <h4 className="font-medium text-gray-900 mb-2">Descrição</h4>
              <p className="text-gray-600">{discrepancy.description}</p>
            </div>

            {/* DADOS COMPARATIVOS */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* DADOS DA NOTA */}
              {discrepancy.invoiceOperation && (
                <div>
                  <h4 className="font-medium text-gray-900 mb-3">Dados da Nota de Corretagem</h4>
                  <div className="bg-blue-50 p-4 rounded-lg space-y-2">
                    {Object.entries(discrepancy.invoiceOperation).map(([key, value]) => (
                      <div key={key} className="flex justify-between text-sm">
                        <span className="text-gray-600 capitalize">{key.replace(/([A-Z])/g, ' $1').toLowerCase()}:</span>
                        <span className="font-medium text-gray-900">{String(value)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* DADOS DO SISTEMA */}
              {discrepancy.systemOperation && (
                <div>
                  <h4 className="font-medium text-gray-900 mb-3">Dados do Sistema</h4>
                  <div className="bg-green-50 p-4 rounded-lg space-y-2">
                    {Object.entries(discrepancy.systemOperation).map(([key, value]) => (
                      <div key={key} className="flex justify-between text-sm">
                        <span className="text-gray-600 capitalize">{key.replace(/([A-Z])/g, ' $1').toLowerCase()}:</span>
                        <span className="font-medium text-gray-900">{
                          typeof value === 'object' && value !== null ? JSON.stringify(value) : String(value)
                        }</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* FOOTER */}
        <div className="p-6 border-t border-gray-200 bg-gray-50">
          <div className="flex justify-end">
            <button
              onClick={onClose}
              className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200"
            >
              Fechar
            </button>
          </div>
        </div>
      </div>
    </div>
  );

  function getDiscrepancyIcon(type: string) {
    switch (type) {
      case 'MISSING_IN_SYSTEM': return <AlertTriangle className="h-5 w-5 text-red-500" />;
      case 'MISSING_IN_INVOICE': return <XCircle className="h-5 w-5 text-orange-500" />;
      case 'VALUE_MISMATCH': return <TrendingUp className="h-5 w-5 text-yellow-500" />;
      case 'DATE_MISMATCH': return <Clock className="h-5 w-5 text-blue-500" />;
      default: return <AlertTriangle className="h-5 w-5 text-gray-500" />;
    }
  }

  function getDiscrepancyLabel(type: string) {
    switch (type) {
      case 'MISSING_IN_SYSTEM': return 'Operação Ausente no Sistema';
      case 'MISSING_IN_INVOICE': return 'Operação Ausente na Nota';
      case 'VALUE_MISMATCH': return 'Divergência de Valor';
      case 'DATE_MISMATCH': return 'Divergência de Data';
      default: return 'Discrepância Desconhecida';
    }
  }
}