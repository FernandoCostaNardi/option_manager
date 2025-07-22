import React, { useState, useEffect, useRef } from 'react';
import { 
  FileText, Upload, Settings, TrendingUp, CheckCircle, 
  Clock, BarChart3, RefreshCw, Download, Eye, Play, Pause,
  Target, Shield, Activity, DollarSign, Zap
} from 'lucide-react';
import { InvoiceProcessingService, SimpleInvoiceData } from '../../services/invoiceProcessingService';
import { ApiService } from '../../services/api';
import { ImportarNotaModal } from '../../components/ImportarNotaModal';
import { InvoicesTab } from '../../components/InvoicesTab';
import { ProcessingModal } from '../../components/ProcessingModal';
import toast from 'react-hot-toast';
// import { AnalyticsTab } from '../../components/AnalyticsTab';
// import { ReconciliationTab } from '../../components/ReconciliationTab';

export function DashboardInvoiceProcessing() {
  // ===== ESTADOS PRINCIPAIS =====
  const [simpleInvoices, setSimpleInvoices] = useState<SimpleInvoiceData[]>([]);
  
  // ===== ESTADOS DE UI =====
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);
  const [tabLoading, setTabLoading] = useState(false);
  const [selectedTab, setSelectedTab] = useState<'overview' | 'invoices'>('overview');
  const [modalOpen, setModalOpen] = useState(false);
  const [processingModalOpen, setProcessingModalOpen] = useState(false);
  const [processingEstimate, setProcessingEstimate] = useState<any>(null);
  const [activeInvoiceTab, setActiveInvoiceTab] = useState<'pendentes' | 'processadas'>('pendentes');
  const [overviewDataLoaded, setOverviewDataLoaded] = useState(false);
  const loadingRef = useRef(false);

  // Função wrapper para setActiveInvoiceTab
  const handleTabChange = async (tab: 'pendentes' | 'processadas') => {
    // Não fazer nada se já estiver na aba selecionada
    if (tab === activeInvoiceTab) return;
    
    // Verificar autenticação antes de fazer a requisição
    if (!ApiService.isAuthenticated()) {
      console.log('❌ Usuário não autenticado. Redirecionando para login...');
      window.location.href = '/login';
      return;
    }
    
    console.log('🔄 Mudando para aba:', tab);
    
    // Carregar dados ANTES de mudar a aba
    try {
      setTabLoading(true);
      let processingStatus: string | undefined;
      if (tab === 'pendentes') {
        processingStatus = 'PENDING';
      } else if (tab === 'processadas') {
        processingStatus = 'SUCCESS';
      }
      
      console.log('🔍 Carregando dados para aba:', tab, 'com status:', processingStatus);
      const response = await InvoiceProcessingService.getSimpleInvoices(0, 1000, processingStatus);
      
      console.log('✅ Dados carregados com sucesso:', response.content.length, 'invoices');
      
      // Só mudar a aba após os dados estarem carregados
      setSimpleInvoices(response.content);
      setTotalPages(response.totalPages);
      setTotalItems(response.totalElements);
      setActiveInvoiceTab(tab);
      
    } catch (error) {
      console.error('❌ Erro ao carregar dados para aba:', tab, error);
      
      // Verificar se é erro de autenticação
      if (error instanceof Error && error.message.includes('Acesso negado')) {
        console.log('❌ Erro de autenticação detectado. Redirecionando para login...');
        window.location.href = '/login';
        return;
      }
      
      toast.error('Erro ao carregar dados. Tente novamente.');
      // Em caso de erro, limpar os dados
      setSimpleInvoices([]);
      setTotalPages(0);
      setTotalItems(0);
    } finally {
      setTabLoading(false);
    }
  };
  
  // ===== PAGINAÇÃO =====
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [selectedInvoiceIds, setSelectedInvoiceIds] = useState<string[]>([]);

  // ===== CARREGAMENTO INICIAL =====
  useEffect(() => {
    // Verificar se o usuário está autenticado antes de carregar dados
    if (!ApiService.isAuthenticated()) {
      console.log('❌ Usuário não autenticado. Redirecionando para login...');
      window.location.href = '/login';
      return;
    }
    
    console.log('✅ Usuário autenticado. Verificação de autenticação concluída.');
    // Removido: carregamento inicial de dados - agora só carrega quando aba for selecionada
  }, []);

  // Carregar dados quando a aba "Visão Geral" for selecionada
  useEffect(() => {
    console.log('🔄 useEffect selectedTab mudou para:', selectedTab);
    if (selectedTab === 'overview') {
      console.log('🔄 Aba Visão Geral selecionada, carregando dados...');
      loadDashboardData();
    } else {
      // Resetar flag quando mudar para outra aba
      console.log('🔄 Mudando para outra aba, resetando flag...');
      setOverviewDataLoaded(false);
      loadingRef.current = false; // Resetar também o loading ref
    }
  }, [selectedTab]);

  // ===== FUNÇÕES DE CARREGAMENTO =====
  const loadDashboardData = async () => {
    // Evitar chamadas simultâneas
    if (loadingRef.current) {
      console.log('🔄 Carregamento já em andamento, pulando...');
      return;
    }

    // Evitar carregar se já foi carregado recentemente
    if (overviewDataLoaded && selectedTab === 'overview') {
      console.log('🔄 Dados da Visão Geral já carregados, pulando...');
      return;
    }

    loadingRef.current = true;
    setLoading(true);
    
    try {
      console.log('🔍 Carregando dados da Visão Geral com filtro ALL...');
      const response = await InvoiceProcessingService.getSimpleInvoices(0, 1000, 'ALL'); // Carrega todas com filtro ALL
      console.log('✅ Dados da Visão Geral carregados:', response.content.length, 'invoices');
      setSimpleInvoices(response.content);
      setOverviewDataLoaded(true);
    } catch (error) {
      console.error('❌ Erro ao carregar dados do dashboard:', error);
    } finally {
      setLoading(false);
      loadingRef.current = false;
    }
  };

  const loadSimpleInvoices = async () => {
    try {
      let processingStatus: string | undefined;
      if (activeInvoiceTab === 'pendentes') {
        processingStatus = 'PENDING';
      } else if (activeInvoiceTab === 'processadas') {
        processingStatus = 'SUCCESS';
      }
      
      const response = await InvoiceProcessingService.getSimpleInvoices(0, 1000, processingStatus);
      
      setSimpleInvoices(response.content);
      setTotalPages(response.totalPages);
      setTotalItems(response.totalElements);
    } catch (error) {
      console.error('Erro ao carregar invoices:', error);
      // Em caso de erro, limpar os dados para evitar mostrar dados antigos
      setSimpleInvoices([]);
      setTotalPages(0);
      setTotalItems(0);
    }
  };

  // ===== AÇÕES DE PROCESSAMENTO (FASE 2 - IMPLEMENTADO) =====
  const handleProcessAll = async () => {
    if (simpleInvoices.length === 0) {
      toast.error('Nenhuma nota disponível para processamento');
      return;
    }
    
    const allInvoiceIds = simpleInvoices.map(inv => inv.id);
    setSelectedInvoiceIds(allInvoiceIds);
    
    // Carregar estimativa ANTES de abrir o modal
    try {
      setProcessing(true);
      console.log('🔍 Carregando estimativa para processamento de todas:', allInvoiceIds);
      const estimate = await InvoiceProcessingService.estimateProcessing(allInvoiceIds);
      console.log('✅ Estimativa carregada com sucesso:', estimate);
      
      // Armazenar estimativa e abrir modal
      setProcessingEstimate(estimate);
      setProcessingModalOpen(true);
    } catch (error) {
      console.error('❌ Erro ao carregar estimativa:', error);
      toast.error('Erro ao preparar processamento. Tente novamente.');
    } finally {
      setProcessing(false);
    }
  };

  const handleProcessSingle = async (invoiceId: string) => {
    setSelectedInvoiceIds([invoiceId]);
    
    // Carregar estimativa ANTES de abrir o modal
    try {
      setProcessing(true);
      console.log('🔍 Carregando estimativa para processamento individual:', invoiceId);
      const estimate = await InvoiceProcessingService.estimateProcessing([invoiceId]);
      console.log('✅ Estimativa carregada com sucesso:', estimate);
      
      // Armazenar estimativa e abrir modal
      setProcessingEstimate(estimate);
      setProcessingModalOpen(true);
    } catch (error) {
      console.error('❌ Erro ao carregar estimativa:', error);
      toast.error('Erro ao preparar processamento. Tente novamente.');
    } finally {
      setProcessing(false);
    }
  };

  const handleProcessSelected = async () => {
    if (selectedInvoiceIds.length === 0) {
      toast.error('Selecione pelo menos uma nota para processar');
      return;
    }
    
    if (selectedInvoiceIds.length > 5) {
      toast.error('Máximo 5 notas por vez. Selecione menos notas.');
      return;
    }
    
    // Carregar estimativa ANTES de abrir o modal
    try {
      setProcessing(true);
      console.log('🔍 Carregando estimativa para processamento selecionado:', selectedInvoiceIds);
      const estimate = await InvoiceProcessingService.estimateProcessing(selectedInvoiceIds);
      console.log('✅ Estimativa carregada com sucesso:', estimate);
      
      // Armazenar estimativa e abrir modal
      setProcessingEstimate(estimate);
      setProcessingModalOpen(true);
    } catch (error) {
      console.error('❌ Erro ao carregar estimativa:', error);
      toast.error('Erro ao preparar processamento. Tente novamente.');
    } finally {
      setProcessing(false);
    }
  };

  const handleProcessingSuccess = () => {
    // Recarregar dados após sucesso
    loadDashboardData();
    if (selectedTab === 'invoices') {
      loadSimpleInvoices();
    }
    setSelectedInvoiceIds([]);
    setProcessingModalOpen(false);
    setProcessingEstimate(null);
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

  // Log das estatísticas calculadas
  console.log('📊 Estatísticas da Visão Geral:', {
    totalInvoices,
    totalValue: formatCurrency(totalValue),
    averageValue: formatCurrency(averageValue),
    selectedTab
  });

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
            
            {selectedInvoiceIds.length > 0 && (
              <button
                onClick={handleProcessSelected}
                disabled={processing}
                className="bg-purple-600 text-white px-4 py-2 rounded-lg hover:bg-purple-700 disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center gap-2"
              >
                {processing ? (
                  <RefreshCw className="h-4 w-4 animate-spin" />
                ) : (
                  <Zap className="h-4 w-4" />
                )}
                Processar Selecionadas ({selectedInvoiceIds.length})
              </button>
            )}
            
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
        <>
          {console.log('🔄 Renderizando InvoicesTab com selectedTab:', selectedTab)}
          <InvoicesTab
            key={`invoices-tab-${selectedTab}`} // Forçar re-montagem apenas quando selectedTab muda
            invoices={simpleInvoices}
            currentPage={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            selectedInvoiceIds={selectedInvoiceIds}
            onPageChange={setCurrentPage}
            onProcessSingle={handleProcessSingle}
            onSelectionChange={setSelectedInvoiceIds}
            activeTab={activeInvoiceTab}
            onTabChange={handleTabChange}
            tabLoading={tabLoading}
          />
        </>
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

      {/* MODAL DE PROCESSAMENTO */}
      <ProcessingModal
        isOpen={processingModalOpen}
        onClose={() => {
          setProcessingModalOpen(false);
          setProcessingEstimate(null);
        }}
        invoiceIds={selectedInvoiceIds}
        selectedInvoices={simpleInvoices.filter(inv => selectedInvoiceIds.includes(inv.id))}
        onSuccess={handleProcessingSuccess}
        initialEstimate={processingEstimate}
      />
    </div>
  );
}

