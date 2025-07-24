import React, { useState, useEffect, useMemo, useRef } from 'react';
import { 
  FileText, Play, Eye, CheckCircle, Clock, AlertCircle, 
  ChevronLeft, ChevronRight, RefreshCw, Download
} from 'lucide-react';
import { SimpleInvoiceData, InvoiceProcessingService } from '../services/invoiceProcessingService';

// ===== PROPS INTERFACES =====
interface InvoicesTabProps {
  invoices: SimpleInvoiceData[];
  currentPage: number;
  totalPages: number;
  totalItems: number;
  selectedInvoiceIds: string[];
  onPageChange: (page: number) => void;
  onProcessSingle: (invoiceId: string) => void;
  onSelectionChange: (selectedIds: string[]) => void;
  onTabChange?: (tab: 'pendentes' | 'processadas') => void;
  activeTab?: 'pendentes' | 'processadas';
  tabLoading?: boolean;
  forceRefreshCounts?: boolean; // ✅ NOVO: Força recarregamento dos contadores
}

interface InvoiceDetailsModalProps {
  invoice: SimpleInvoiceData | null;
  isOpen: boolean;
  onClose: () => void;
  activeTab?: 'pendentes' | 'processadas';
}

// ===== COMPONENTE PRINCIPAL =====
export function InvoicesTab({ 
  invoices, 
  currentPage, 
  totalPages, 
  totalItems, 
  selectedInvoiceIds,
  onPageChange, 
  onProcessSingle,
  onSelectionChange,
  onTabChange,
  activeTab = 'pendentes',
  tabLoading = false,
  forceRefreshCounts = false
}: InvoicesTabProps) {
  console.log('🔄 InvoicesTab montado/remontado com activeTab:', activeTab);
  const [selectedInvoice, setSelectedInvoice] = useState<SimpleInvoiceData | null>(null);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);
  const [counts, setCounts] = useState({ pendentes: 0, processadas: 0 });
  const countsLoadedRef = useRef(false);

  // Usar invoices diretamente, já que o carregamento é controlado pelo handleTabChange
  // Mas limitar baseado no contador se houver inconsistência
  let filteredInvoices = invoices;
  
  // Se estamos na aba pendentes e há inconsistência, limitar aos primeiros N itens
  if (activeTab === 'pendentes' && invoices.length > counts.pendentes && counts.pendentes > 0) {
    console.log('🔧 Aplicando correção: limitando a', counts.pendentes, 'itens pendentes');
    filteredInvoices = invoices.slice(0, counts.pendentes);
  }
  
  // Se estamos na aba processadas e há inconsistência, limitar aos primeiros N itens
  if (activeTab === 'processadas' && invoices.length > counts.processadas && counts.processadas > 0) {
    console.log('🔧 Aplicando correção: limitando a', counts.processadas, 'itens processadas');
    filteredInvoices = invoices.slice(0, counts.processadas);
  }
  
  // Log para debug
  console.log('🔄 InvoicesTab - invoices recebidas:', invoices.length, 'activeTab:', activeTab, 'tabLoading:', tabLoading);
  console.log('🔄 InvoicesTab - primeiras 3 invoices:', invoices.slice(0, 3).map(inv => ({ id: inv.id, number: inv.invoiceNumber })));
  
  // ✅ CORREÇÃO: Mostrar estado de carregamento quando lista estiver vazia e tabLoading for true
  if (tabLoading && invoices.length === 0) {
    console.log('🔄 Mostrando estado de carregamento...');
  }
  
  // Verificar inconsistência entre contadores e dados reais
  if (activeTab === 'pendentes' && invoices.length !== counts.pendentes) {
    console.log('⚠️ INCONSISTÊNCIA DETECTADA - PENDENTES:', {
      'contador-pendentes': counts.pendentes,
      'dados-reais': invoices.length,
      'diferenca': invoices.length - counts.pendentes
    });
  }
  
  if (activeTab === 'processadas' && invoices.length !== counts.processadas) {
    console.log('⚠️ INCONSISTÊNCIA DETECTADA - PROCESSADAS:', {
      'contador-processadas': counts.processadas,
      'dados-reais': invoices.length,
      'diferenca': invoices.length - counts.processadas
    });
  }
  
  // ✅ CORREÇÃO: Limpar seleções quando mudar de aba para evitar inconsistências
  useEffect(() => {
    if (selectedInvoiceIds.length > 0) {
      console.log('🔄 Limpando seleções ao mudar de aba para evitar inconsistências');
      onSelectionChange([]);
    }
  }, [activeTab]); // Executa quando a aba muda



  // Função para formatar moeda
  const formatCurrency = (value: number | undefined | null) => {
    if (value === null || value === undefined || isNaN(value)) {
      return 'R$ 0,00';
    }
    return value.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    });
  };

  // Função para formatar data
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('pt-BR');
  };

  // Função para obter status da invoice baseado na aba ativa
  const getInvoiceStatusBadge = () => {
    if (activeTab === 'processadas') {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
          <CheckCircle className="h-3 w-3 mr-1" />
          Processada
        </span>
      );
    } else {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
          <Clock className="h-3 w-3 mr-1" />
          Pendente
        </span>
      );
    }
  };

  // Função para abrir detalhes
  const handleViewDetails = (invoice: SimpleInvoiceData) => {
    setSelectedInvoice(invoice);
    setDetailsModalOpen(true);
  };

  // ===== FUNÇÕES DE SELEÇÃO =====
  const handleSelectAll = () => {
    if (selectedInvoiceIds.length === filteredInvoices.length) {
      onSelectionChange([]);
    } else {
      // ✅ CORREÇÃO: Garantir que apenas invoices da aba atual sejam selecionadas
      const selectableInvoices = filteredInvoices;
      console.log('🔍 Selecionando todas as invoices:', {
        'aba-ativa': activeTab,
        'invoices-disponiveis': selectableInvoices.length,
        'ids-para-selecionar': selectableInvoices.map(inv => inv.id)
      });
      onSelectionChange(selectableInvoices.map(inv => inv.id));
    }
  };

  const handleSelectInvoice = (invoiceId: string) => {
    if (selectedInvoiceIds.includes(invoiceId)) {
      onSelectionChange(selectedInvoiceIds.filter(id => id !== invoiceId));
    } else {
      onSelectionChange([...selectedInvoiceIds, invoiceId]);
    }
  };

  // ✅ CORREÇÃO: Carregar contadores ao montar e quando forceRefreshCounts mudar
  useEffect(() => {
    console.log('🔄 useEffect contadores - forceRefreshCounts:', forceRefreshCounts, 'countsLoaded:', countsLoadedRef.current);
    
    // Se forceRefreshCounts é true, recarregar contadores mesmo se já foram carregados
    if (countsLoadedRef.current && !forceRefreshCounts) {
      console.log('🔄 Contadores já carregados, pulando...');
      return;
    }

    const loadCounts = async () => {
      try {
        console.log('🔍 Carregando contadores das abas...', forceRefreshCounts ? '(forçado)' : '');
        
        // Usar os métodos específicos de contagem
        const [pendingCount, processedCount] = await Promise.all([
          InvoiceProcessingService.getPendingCount(),
          InvoiceProcessingService.getProcessedCount()
        ]);
        
        console.log('✅ Contadores carregados via métodos específicos:', {
          pendentes: pendingCount,
          processadas: processedCount,
          'forcado': forceRefreshCounts
        });
        
        setCounts({
          pendentes: pendingCount,
          processadas: processedCount
        });
        
        // Marcar como carregado para evitar chamadas duplicadas
        countsLoadedRef.current = true;
        
      } catch (error) {
        console.error('❌ Erro ao carregar contadores:', error);
        setCounts({ pendentes: 0, processadas: 0 });
        countsLoadedRef.current = true; // Marcar como carregado mesmo em caso de erro
      }
    };

    // Usar um timeout para evitar chamadas simultâneas
    const timeoutId = setTimeout(() => {
      loadCounts();
    }, 100);

    return () => clearTimeout(timeoutId);
  }, [forceRefreshCounts]); // ✅ Executa quando forceRefreshCounts mudar

  const isAllSelected = selectedInvoiceIds.length > 0 && selectedInvoiceIds.length === filteredInvoices.length;
  const isIndeterminate = selectedInvoiceIds.length > 0 && selectedInvoiceIds.length < filteredInvoices.length;

  return (
    <div className="space-y-6">
      {/* FILTROS */}
      <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <h3 className="text-lg font-semibold">Notas de Corretagem</h3>
            
            <div className="flex rounded-lg border border-gray-200 p-1">
              {[
                { key: 'pendentes', label: 'Pendentes', count: counts.pendentes },
                { key: 'processadas', label: 'Processadas', count: counts.processadas }
              ].map(({ key, label, count }) => (
                <button
                  key={key}
                  onClick={() => {
                    if (onTabChange && !tabLoading) {
                      onTabChange(key as 'pendentes' | 'processadas');
                    }
                  }}
                  disabled={tabLoading}
                  className={`px-3 py-1 text-sm rounded-md transition-colors ${
                    tabLoading 
                      ? 'cursor-not-allowed opacity-50' 
                      : 'cursor-pointer'
                  } ${
                    activeTab === key
                      ? 'bg-purple-100 text-purple-700'
                      : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  {label} ({count})
                  {tabLoading && activeTab === key && (
                    <span className="ml-2 inline-block animate-spin">⏳</span>
                  )}
                </button>
              ))}
            </div>
          </div>

          <div className="text-sm text-gray-500">
            {selectedInvoiceIds.length > 0 ? (
              <span className="text-purple-600 font-medium">
                {selectedInvoiceIds.length} selecionada(s) • Total: {filteredInvoices.length} notas
              </span>
            ) : (
              `Total: ${filteredInvoices.length} notas`
            )}
          </div>
        </div>
      </div>
      {/* TABELA DE INVOICES */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">

        {tabLoading ? (
          <div className="text-center py-12">
            <div className="h-12 w-12 text-purple-600 mx-auto mb-4 animate-spin">⏳</div>
            <p className="text-lg font-medium text-gray-900 mb-2">Carregando...</p>
            <p className="text-gray-500">Buscando notas de corretagem.</p>
          </div>
        ) : filteredInvoices.length === 0 ? (
          <div className="text-center py-12">
            <FileText className="h-12 w-12 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500">Nenhuma nota encontrada com os filtros aplicados.</p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left">
                      <input
                        type="checkbox"
                        checked={isAllSelected}
                        ref={(input) => {
                          if (input) input.indeterminate = isIndeterminate;
                        }}
                        onChange={handleSelectAll}
                        className="h-4 w-4 text-purple-600 focus:ring-purple-500 border-gray-300 rounded"
                      />
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Número / Data
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Corretora
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Itens / Valor
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {activeTab === 'processadas' ? 'Visualizar' : 'Processar'}
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 bg-white">
                  {/* ✅ CORREÇÃO: Mostrar estado de carregamento quando lista estiver vazia */}
                  {tabLoading && filteredInvoices.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="px-6 py-12 text-center">
                        <div className="flex items-center justify-center gap-3">
                          <div className="h-6 w-6 animate-spin text-purple-600">⏳</div>
                          <span className="text-gray-500">Carregando {activeTab}...</span>
                        </div>
                      </td>
                    </tr>
                  ) : filteredInvoices.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="px-6 py-12 text-center text-gray-500">
                        Nenhuma nota {activeTab === 'pendentes' ? 'pendente' : 'processada'} encontrada.
                      </td>
                    </tr>
                  ) : (
                    filteredInvoices.map((invoice) => (
                    <tr key={invoice.id} className={`hover:bg-gray-50 ${
                      selectedInvoiceIds.includes(invoice.id) ? 'bg-purple-50 border-purple-200' : ''
                    }`}>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <input
                          type="checkbox"
                          checked={selectedInvoiceIds.includes(invoice.id)}
                          onChange={() => handleSelectInvoice(invoice.id)}
                          className="h-4 w-4 text-purple-600 focus:ring-purple-500 border-gray-300 rounded"
                        />
                      </td>
                      
                      <td className="px-6 py-4 whitespace-nowrap">
                        {getInvoiceStatusBadge()}
                      </td>
                      
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div>
                          <div className="text-sm font-medium text-gray-900">
                            {invoice.invoiceNumber}
                          </div>
                          <div className="text-sm text-gray-500">
                            {formatDate(invoice.tradingDate)}
                          </div>
                        </div>
                      </td>
                      
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-900">{invoice.brokerageName}</div>
                      </td>
                      
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div>
                          <div className="text-sm font-medium text-gray-900">
                            {invoice.itemsCount} itens
                          </div>
                          <div className="text-sm text-gray-500">
                            {formatCurrency(invoice.grossOperationsValue)}
                          </div>
                        </div>
                      </td>
                      
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-500">
                          {activeTab === 'processadas' ? 'Processada' : 'Não processada'}
                        </div>
                      </td>
                      
                      <td className="px-6 py-4 whitespace-nowrap text-center">
                        <div className="flex items-center justify-center space-x-2">
                          {/* Aba "Processadas": Mostrar apenas botão de visualizar */}
                          {activeTab === 'processadas' && (
                            <button
                              onClick={() => handleViewDetails(invoice)}
                              className="text-indigo-600 hover:text-indigo-900 p-1"
                              title="Ver detalhes"
                            >
                              <Eye className="h-4 w-4" />
                            </button>
                          )}
                          
                          {/* Aba "Pendentes": Mostrar apenas botão de processar */}
                          {activeTab === 'pendentes' && (
                            <button
                              onClick={() => onProcessSingle(invoice.id)}
                              className="text-purple-600 hover:text-purple-900 p-1"
                              title="Processar esta nota"
                            >
                              <Play className="h-4 w-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
                </tbody>
              </table>
            </div>

            {/* PAGINAÇÃO */}
            {totalPages > 1 && (
              <div className="px-6 py-3 flex items-center justify-between border-t border-gray-200">
                <div className="text-sm text-gray-500">
                  Mostrando {currentPage * 20 + 1} a {Math.min((currentPage + 1) * 20, totalItems)} de {totalItems} resultados
                </div>
                
                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => onPageChange(currentPage - 1)}
                    disabled={currentPage === 0}
                    className={`p-2 rounded-md ${
                      currentPage === 0
                        ? 'text-gray-400 cursor-not-allowed'
                        : 'text-gray-600 hover:bg-gray-100'
                    }`}
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </button>
                  
                  <span className="text-sm text-gray-600">
                    Página {currentPage + 1} de {totalPages}
                  </span>
                  
                  <button
                    onClick={() => onPageChange(currentPage + 1)}
                    disabled={currentPage === totalPages - 1}
                    className={`p-2 rounded-md ${
                      currentPage === totalPages - 1
                        ? 'text-gray-400 cursor-not-allowed'
                        : 'text-gray-600 hover:bg-gray-100'
                    }`}
                  >
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* MODAL DE DETALHES */}
      <InvoiceDetailsModal 
        invoice={selectedInvoice}
        isOpen={detailsModalOpen}
        onClose={() => {
          setDetailsModalOpen(false);
          setSelectedInvoice(null);
        }}
        activeTab={activeTab}
      />
    </div>
  );
}
// ===== MODAL DE DETALHES =====
function InvoiceDetailsModal({ invoice, isOpen, onClose, activeTab }: InvoiceDetailsModalProps) {
  if (!isOpen || !invoice) return null;

  // Determinar se a nota está processada baseado na aba ativa
  const isProcessed = activeTab === 'processadas';

  const formatCurrency = (value: number | undefined | null) => {
    if (value === null || value === undefined || isNaN(value)) {
      return 'R$ 0,00';
    }
    return value.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    });
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('pt-BR');
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-xl max-w-2xl w-full max-h-[90vh] overflow-hidden">
        {/* HEADER */}
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-semibold">Detalhes da Nota #{invoice.invoiceNumber}</h3>
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
            {/* INFORMAÇÕES BÁSICAS */}
            <div>
              <h4 className="font-medium text-gray-900 mb-3">Informações Básicas</h4>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-gray-500">Data do Pregão:</span>
                  <div className="font-medium">{formatDate(invoice.tradingDate)}</div>
                </div>
                <div>
                  <span className="text-gray-500">Corretora:</span>
                  <div className="font-medium">{invoice.brokerageName}</div>
                </div>
                <div>
                  <span className="text-gray-500">Quantidade de Itens:</span>
                  <div className="font-medium">{invoice.itemsCount}</div>
                </div>
                <div>
                  <span className="text-gray-500">Valor Líquido:</span>
                  <div className="font-medium">{formatCurrency(invoice.netSettlementValue)}</div>
                </div>
                <div>
                  <span className="text-gray-500">Valor Bruto:</span>
                  <div className="font-medium">{formatCurrency(invoice.grossOperationsValue)}</div>
                </div>
                <div>
                  <span className="text-gray-500">Total de Custos:</span>
                  <div className="font-medium">{formatCurrency(invoice.totalCosts)}</div>
                </div>
              </div>
            </div>

            {/* STATUS DE PROCESSAMENTO */}
            <div>
              <h4 className="font-medium text-gray-900 mb-3">Status de Processamento</h4>
              {isProcessed ? (
                <div className="text-center p-6 bg-green-50 rounded-lg">
                  <CheckCircle className="h-12 w-12 text-green-500 mx-auto mb-3" />
                  <p className="text-green-700 font-medium">Nota processada com sucesso</p>
                  <p className="text-sm text-green-600 mt-2">
                    As operações foram criadas e estão disponíveis no sistema
                  </p>
                </div>
              ) : (
                <div className="text-center p-6 bg-yellow-50 rounded-lg">
                  <Clock className="h-12 w-12 text-yellow-500 mx-auto mb-3" />
                  <p className="text-yellow-700 font-medium">Esta nota ainda não foi processada</p>
                  <p className="text-sm text-yellow-600 mt-2">
                    Clique no botão "Processar" para criar as operações
                  </p>
                </div>
              )}
            </div>

            {/* INFORMAÇÕES ADICIONAIS */}
            <div>
              <h4 className="font-medium text-gray-900 mb-3">Informações Adicionais</h4>
              <div className="text-sm text-gray-600">
                <div>Importada em: {formatDate(invoice.importedAt)}</div>
                <div>Valor para Liquidação: {formatCurrency(invoice.netSettlementValue)}</div>
              </div>
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
}