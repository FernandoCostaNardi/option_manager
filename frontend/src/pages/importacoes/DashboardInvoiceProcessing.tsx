import React, { useState, useEffect } from 'react';
import { 
  FileText, Upload, Settings, TrendingUp, AlertCircle, CheckCircle, 
  Clock, BarChart3, RefreshCw, Download, Eye, Play, Pause,
  Target, Shield, Activity, DollarSign
} from 'lucide-react';
import { InvoiceProcessingService, SimpleInvoiceData } from '../../services/invoiceProcessingService';
import { ImportarNotaModal } from '../../components/ImportarNotaModal';
import { InvoicesTab } from '../../components/InvoicesTab';
// import { AnalyticsTab } from '../../components/AnalyticsTab';
// import { ReconciliationTab } from '../../components/ReconciliationTab';

export function DashboardInvoiceProcessing() {
  // ===== ESTADOS PRINCIPAIS =====
  const [simpleInvoices, setSimpleInvoices] = useState<SimpleInvoiceData[]>([]);
  
  // ===== ESTADOS DE UI =====
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);
  const [selectedTab, setSelectedTab] = useState<'overview' | 'invoices'>('overview');
  const [modalOpen, setModalOpen] = useState(false);
  
  // ===== PAGINAÇÃO =====
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);

  // ===== CARREGAMENTO INICIAL =====
  useEffect(() => {
    loadDashboardData();
  }, []);

  useEffect(() => {
    if (selectedTab === 'invoices') {
      loadSimpleInvoices();
    }
  }, [selectedTab, currentPage]);

  // ===== FUNÇÕES DE CARREGAMENTO =====
  const loadDashboardData = async () => {
    setLoading(true);
    try {
      const response = await InvoiceProcessingService.getSimpleInvoices(0, 1000); // Carrega todas para stats
      setSimpleInvoices(response.content);
    } catch (error) {
      console.error('Erro ao carregar dados do dashboard:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadSimpleInvoices = async () => {
    try {
      const response = await InvoiceProcessingService.getSimpleInvoices(currentPage, 20);
      setSimpleInvoices(response.content);
      setTotalPages(response.totalPages);
      setTotalItems(response.totalElements);
    } catch (error) {
      console.error('Erro ao carregar invoices:', error);
    }
  };

  // ===== AÇÕES DE PROCESSAMENTO (FASE 2) =====
  const handleProcessAll = async () => {
    alert('Funcionalidade de processamento será implementada na Fase 2 do sistema');
  };

  const handleProcessSingle = async (invoiceId: string) => {
    alert('Funcionalidade de processamento será implementada na Fase 2 do sistema');
  };

  // ===== UTILITÁRIOS =====
  const formatCurrency = (value: number) => {
    return value.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    });
  };

  // ===== ESTATÍSTICAS BÁSICAS (FASE 1) =====
  const totalInvoices = simpleInvoices.length;
  const totalValue = simpleInvoices.reduce((sum, inv) => sum + inv.grossOperationsValue, 0);
  const totalCosts = simpleInvoices.reduce((sum, inv) => sum + inv.totalCosts, 0);
  const averageValue = totalInvoices > 0 ? totalValue / totalInvoices : 0;

  // ===== RENDERIZAÇÃO =====
  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="flex items-center gap-3">
          <RefreshCw className="h-6 w-6 animate-spin text-purple-600" />
          <span className="text-lg">Carregando dashboard...</span>
        </div>
      </div>
    );
  }
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* HEADER */}
      <div className="mb-8">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Dashboard de Processamento</h1>
            <p className="text-gray-600 mt-1">Importação e processamento automático de notas de corretagem</p>
          </div>
          
          <div className="flex items-center gap-3">
            <button
              onClick={() => setModalOpen(true)}
              className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 flex items-center gap-2"
            >
              <Upload className="h-4 w-4" />
              Importar Notas
            </button>
            
            <button
              onClick={handleProcessAll}
              disabled={processing || totalInvoices === 0}
              className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center gap-2"
            >
              {processing ? (
                <RefreshCw className="h-4 w-4 animate-spin" />
              ) : (
                <Play className="h-4 w-4" />
              )}
              Processar Todas ({totalInvoices})
            </button>
          </div>
        </div>
      </div>

      {/* NAVEGAÇÃO DE ABAS */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="-mb-px flex space-x-8">
          {[
            { key: 'overview', label: 'Visão Geral', icon: BarChart3 },
            { key: 'invoices', label: 'Notas de Corretagem', icon: FileText }
          ].map(({ key, label, icon: Icon }) => (
            <button
              key={key}
              onClick={() => setSelectedTab(key as any)}
              className={`py-2 px-1 border-b-2 font-medium text-sm whitespace-nowrap flex items-center gap-2 ${
                selectedTab === key
                  ? 'border-purple-500 text-purple-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <Icon className="h-4 w-4" />
              {label}
            </button>
          ))}
        </nav>
      </div>

      {/* CONTEÚDO DAS ABAS */}
      {selectedTab === 'overview' && (
        <div className="space-y-6">
          {/* ESTATÍSTICAS BÁSICAS */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <FileText className="h-8 w-8 text-blue-600" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Total de Notas
                    </dt>
                    <dd className="text-2xl font-bold text-gray-900">
                      {totalInvoices}
                    </dd>
                  </dl>
                </div>
              </div>
            </div>

            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <Clock className="h-8 w-8 text-yellow-600" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Não Processadas
                    </dt>
                    <dd className="text-2xl font-bold text-gray-900">
                      {totalInvoices}
                    </dd>
                  </dl>
                </div>
              </div>
            </div>

            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <DollarSign className="h-8 w-8 text-green-600" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Valor Total
                    </dt>
                    <dd className="text-2xl font-bold text-gray-900">
                      {formatCurrency(totalValue)}
                    </dd>
                  </dl>
                </div>
              </div>
            </div>

            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <TrendingUp className="h-8 w-8 text-purple-600" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Valor Médio
                    </dt>
                    <dd className="text-2xl font-bold text-gray-900">
                      {formatCurrency(averageValue)}
                    </dd>
                  </dl>
                </div>
              </div>
            </div>
          </div>

          {/* AVISO SOBRE FASE 2 */}
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <div className="flex">
              <div className="flex-shrink-0">
                <AlertCircle className="h-5 w-5 text-yellow-400" />
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-yellow-800">
                  Sistema em Fase 1 - Importação
                </h3>
                <div className="mt-2 text-sm text-yellow-700">
                  <p>
                    Atualmente, o sistema está importando e exibindo as notas de corretagem. 
                    O processamento automático para criar operações será implementado na Fase 2.
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* ÚLTIMAS NOTAS IMPORTADAS */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-100">
            <div className="px-6 py-4 border-b border-gray-200">
              <h3 className="text-lg font-medium text-gray-900">Últimas Notas Importadas</h3>
            </div>
            <div className="p-6">
              {simpleInvoices.slice(0, 5).length === 0 ? (
                <p className="text-gray-500 text-center py-8">
                  Nenhuma nota importada ainda. Clique em "Importar Notas" para começar.
                </p>
              ) : (
                <div className="space-y-3">
                  {simpleInvoices.slice(0, 5).map((invoice) => (
                    <div key={invoice.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center gap-3">
                        <FileText className="h-5 w-5 text-blue-600" />
                        <div>
                          <div className="font-medium">Nota #{invoice.invoiceNumber}</div>
                          <div className="text-sm text-gray-500">{invoice.brokerageName}</div>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="font-medium">{formatCurrency(invoice.grossOperationsValue)}</div>
                        <div className="text-sm text-gray-500">{invoice.itemsCount} itens</div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {selectedTab === 'invoices' && (
        <InvoicesTab
          invoices={simpleInvoices}
          currentPage={currentPage}
          totalPages={totalPages}
          totalItems={totalItems}
          onPageChange={setCurrentPage}
          onProcessSingle={handleProcessSingle}
        />
      )}

      {/* MODAL DE IMPORTAÇÃO */}
      <ImportarNotaModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        onSuccess={() => {
          setModalOpen(false);
          loadDashboardData();
        }}
      />
    </div>
  );
}