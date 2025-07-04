import React, { useState, useEffect, useRef } from 'react';
import { 
  X, Play, Pause, CheckCircle, AlertCircle, Clock, 
  RefreshCw, BarChart3, FileText, Target, Zap
} from 'lucide-react';
import { InvoiceProcessingService } from '../services/invoiceProcessingService';
import toast from 'react-hot-toast';

// ===== INTERFACES =====
interface ProcessingModalProps {
  isOpen: boolean;
  onClose: () => void;
  invoiceIds: string[];
  selectedInvoices: Array<{
    id: string;
    invoiceNumber: string;
    brokerageName: string;
    itemsCount: number;
    grossOperationsValue: number;
  }>;
  onSuccess: () => void;
}

interface ProcessingProgress {
  sessionId: string;
  status: string;
  currentInvoice: number;
  totalInvoices: number;
  currentStep: string;
  operationsCreated: number;
  operationsSkipped: number;
  operationsUpdated: number;
  estimatedDuration: number;
  elapsedTime: number;
  messages: string[];
  errors: string[];
}

interface ProcessingEstimate {
  estimatedDuration: number;
  complexity: string;
  potentialOperations: number;
  warnings: string[];
  canProceed: boolean;
}

// ===== COMPONENTE PRINCIPAL =====
export function ProcessingModal({ 
  isOpen, 
  onClose, 
  invoiceIds, 
  selectedInvoices, 
  onSuccess 
}: ProcessingModalProps) {
  // ===== ESTADOS =====
  const [step, setStep] = useState<'estimate' | 'confirm' | 'processing' | 'completed'>('estimate');
  const [estimate, setEstimate] = useState<ProcessingEstimate | null>(null);
  const [progress, setProgress] = useState<ProcessingProgress | null>(null);
  const [processing, setProcessing] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [estimateLoading, setEstimateLoading] = useState(false);
  
  // ===== REFS =====
  const eventSourceRef = useRef<EventSource | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // ===== EFEITOS =====
  useEffect(() => {
    if (isOpen && step === 'estimate') {
      loadEstimate();
    }
  }, [isOpen]);

  useEffect(() => {
    return () => {
      // Cleanup EventSource ao desmontar
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, []);

  useEffect(() => {
    // Auto-scroll para final das mensagens
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [progress?.messages]);

  // ===== FUNÇÕES =====
  const loadEstimate = async () => {
    setEstimateLoading(true);
    try {
      const result = await InvoiceProcessingService.estimateProcessing(invoiceIds);
      setEstimate(result);
      setStep('confirm');
    } catch (error) {
      console.error('Erro ao estimar processamento:', error);
      toast.error('Erro ao estimar processamento');
      onClose();
    } finally {
      setEstimateLoading(false);
    }
  };

  const startProcessing = async () => {
    setProcessing(true);
    setStep('processing');
    
    try {
      const response = await InvoiceProcessingService.processBatch(invoiceIds, {
        maxOperations: 10, // Limite para teste
        skipDuplicates: true
      });
      
      if (response.sessionId) {
        // Conectar ao SSE para acompanhar progresso
        connectToProgressStream(response.sessionId);
      } else {
        // Processamento síncrono (rápido)
        handleProcessingComplete(response);
      }
    } catch (error) {
      console.error('Erro ao iniciar processamento:', error);
      toast.error('Erro ao iniciar processamento');
      setStep('confirm');
      setProcessing(false);
    }
  };

  const connectToProgressStream = (sessionId: string) => {
    console.log('🔗 Conectando ao SSE para sessão:', sessionId);
    
    try {
      const eventSource = InvoiceProcessingService.createProcessingEventSource(sessionId);
      eventSourceRef.current = eventSource;

      eventSource.onopen = (event) => {
        console.log('✅ EventSource ABERTO:', event);
        console.log('   ReadyState:', eventSource.readyState);
        console.log('   URL:', eventSource.url.substring(0, eventSource.url.indexOf('?token=')) + '?token=***');
        console.log('🔗 Conectado ao stream de progresso');
        
        setProgress(prev => prev ? { ...prev, sessionId } : {
          sessionId,
          status: 'PROCESSING',
          currentInvoice: 0,
          totalInvoices: invoiceIds.length,
          currentStep: 'Iniciando processamento...',
          operationsCreated: 0,
          operationsSkipped: 0,
          operationsUpdated: 0,
          estimatedDuration: estimate?.estimatedDuration || 0,
          elapsedTime: 0,
          messages: ['Processamento iniciado'],
          errors: []
        });
      };

      eventSource.onmessage = (event) => {
        console.log('📩 EVENTO SSE RECEBIDO:', event);
        console.log('   Type:', event.type);
        console.log('   Data:', event.data);
        
        try {
          const data = JSON.parse(event.data);
          console.log('📨 Progresso recebido:', data);
          
          setProgress(prev => ({
            ...prev,
            ...data,
            messages: [...(prev?.messages || []), ...data.newMessages || []],
            errors: [...(prev?.errors || []), ...data.newErrors || []]
          }));

          if (data.status === 'COMPLETED' || data.status === 'ERROR') {
            console.log('🏁 Processamento finalizado:', data.status);
            eventSource.close();
            handleProcessingComplete(data);
          }
        } catch (error) {
          console.error('❌ Erro ao parsear dados do SSE:', error);
          console.error('   Dados recebidos:', event.data);
        }
      };

      eventSource.onerror = (error) => {
        console.error('❌ ERRO EventSource:', error);
        console.error('   ReadyState:', eventSource.readyState);
        console.error('   URL:', eventSource.url?.substring(0, eventSource.url.indexOf('?token=')) + '?token=***');
        console.error('   Error event:', error);
        
        // Só fechar se realmente falhou (não reconexão automática)
        if (eventSource.readyState === EventSource.CLOSED) {
          console.error('💀 EventSource fechado definitivamente');
          toast.error('Conexão perdida com o servidor');
        } else {
          console.warn('🔄 EventSource tentando reconectar...');
        }
      };
      
    } catch (error) {
      console.error('❌ Erro ao criar EventSource:', error);
      toast.error('Erro ao conectar ao stream de progresso');
    }
  };

  const handleProcessingComplete = (result: any) => {
    setProcessing(false);
    setStep('completed');
    
    if (result.success || result.partialSuccess) {
      toast.success(
        result.summary || 
        `Processamento concluído! ${result.operationsCreated || 0} operações criadas.`
      );
      onSuccess();
    } else {
      toast.error('Processamento falhou com erros');
    }
  };

  const cancelProcessing = async () => {
    if (!progress?.sessionId) return;
    
    setCancelling(true);
    try {
      await InvoiceProcessingService.cancelProcessing(progress.sessionId);
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
      toast.success('Processamento cancelado');
      onClose();
    } catch (error) {
      console.error('Erro ao cancelar:', error);
      toast.error('Erro ao cancelar processamento');
    } finally {
      setCancelling(false);
    }
  };

  const formatCurrency = (value: number) => {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  };

  const formatDuration = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const getProgressPercentage = () => {
    if (!progress) return 0;
    if (progress.totalInvoices === 0) return 100;
    return Math.round((progress.currentInvoice / progress.totalInvoices) * 100);
  };

  // ===== RENDER =====
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-xl max-w-4xl w-full max-h-[90vh] overflow-hidden">
        {/* HEADER */}
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Zap className="h-6 w-6 text-purple-600" />
              <div>
                <h3 className="text-lg font-semibold">Processamento de Notas</h3>
                <p className="text-sm text-gray-500">
                  {selectedInvoices.length} nota(s) selecionada(s)
                </p>
              </div>
            </div>
            
            <button
              onClick={onClose}
              disabled={processing && !cancelling}
              className="text-gray-400 hover:text-gray-600 disabled:opacity-50"
            >
              <X className="h-6 w-6" />
            </button>
          </div>
        </div>

        {/* CONTEÚDO */}
        <div className="p-6 overflow-y-auto max-h-[calc(90vh-180px)]">
          {/* STEP: ESTIMATE LOADING */}
          {step === 'estimate' && (
            <div className="text-center py-12">
              <RefreshCw className="h-12 w-12 text-purple-600 mx-auto mb-4 animate-spin" />
              <p className="text-lg font-medium mb-2">Analisando notas...</p>
              <p className="text-gray-500">Estimando complexidade e duração do processamento</p>
            </div>
          )}

          {/* STEP: CONFIRM */}
          {step === 'confirm' && estimate && (
            <div className="space-y-6">
              {/* RESUMO */}
              <div className="bg-gray-50 rounded-lg p-4">
                <h4 className="font-medium mb-3">Resumo do Processamento</h4>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-gray-500">Notas a processar:</span>
                    <div className="font-medium">{selectedInvoices.length}</div>
                  </div>
                  <div>
                    <span className="text-gray-500">Operações estimadas:</span>
                    <div className="font-medium">{estimate.potentialOperations}</div>
                  </div>
                  <div>
                    <span className="text-gray-500">Complexidade:</span>
                    <div className="font-medium capitalize">{estimate.complexity}</div>
                  </div>
                  <div>
                    <span className="text-gray-500">Duração estimada:</span>
                    <div className="font-medium">{formatDuration(estimate.estimatedDuration)}</div>
                  </div>
                </div>
              </div>

              {/* NOTAS SELECIONADAS */}
              <div>
                <h4 className="font-medium mb-3">Notas Selecionadas</h4>
                <div className="space-y-2 max-h-40 overflow-y-auto">
                  {selectedInvoices.map((invoice) => (
                    <div key={invoice.id} className="flex items-center justify-between p-3 bg-blue-50 rounded-lg">
                      <div className="flex items-center gap-3">
                        <FileText className="h-4 w-4 text-blue-600" />
                        <div>
                          <div className="font-medium">#{invoice.invoiceNumber}</div>
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
              </div>

              {/* WARNINGS */}
              {estimate.warnings && estimate.warnings.length > 0 && (
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                  <div className="flex">
                    <AlertCircle className="h-5 w-5 text-yellow-600 flex-shrink-0 mt-0.5" />
                    <div className="ml-3">
                      <h4 className="text-sm font-medium text-yellow-800">Avisos importantes:</h4>
                      <ul className="mt-2 text-sm text-yellow-700 list-disc list-inside">
                        {estimate.warnings.map((warning, index) => (
                          <li key={index}>{warning}</li>
                        ))}
                      </ul>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* STEP: PROCESSING */}
          {step === 'processing' && (
            <div className="space-y-6">
              {/* BARRA DE PROGRESSO */}
              {progress && (
                <div className="bg-gray-50 rounded-lg p-6">
                  <div className="flex items-center justify-between mb-4">
                    <div>
                      <h4 className="font-medium">Progresso do Processamento</h4>
                      <p className="text-sm text-gray-500">{progress.currentStep}</p>
                    </div>
                    <div className="text-right">
                      <div className="text-2xl font-bold text-purple-600">
                        {getProgressPercentage()}%
                      </div>
                      <div className="text-sm text-gray-500">
                        {progress.currentInvoice} de {progress.totalInvoices}
                      </div>
                    </div>
                  </div>
                  
                  <div className="w-full bg-gray-200 rounded-full h-3 mb-4">
                    <div 
                      className="bg-purple-600 h-3 rounded-full transition-all duration-500"
                      style={{ width: `${getProgressPercentage()}%` }}
                    />
                  </div>

                  <div className="grid grid-cols-3 gap-4 text-sm">
                    <div className="text-center">
                      <div className="text-lg font-bold text-green-600">
                        {progress.operationsCreated}
                      </div>
                      <div className="text-gray-500">Criadas</div>
                    </div>
                    <div className="text-center">
                      <div className="text-lg font-bold text-blue-600">
                        {progress.operationsUpdated}
                      </div>
                      <div className="text-gray-500">Atualizadas</div>
                    </div>
                    <div className="text-center">
                      <div className="text-lg font-bold text-yellow-600">
                        {progress.operationsSkipped}
                      </div>
                      <div className="text-gray-500">Ignoradas</div>
                    </div>
                  </div>
                </div>
              )}

              {/* LOG DE MENSAGENS */}
              {progress && progress.messages.length > 0 && (
                <div className="bg-gray-900 rounded-lg p-4 text-green-400 font-mono text-sm max-h-60 overflow-y-auto">
                  {progress.messages.map((message, index) => (
                    <div key={index} className="mb-1">
                      <span className="text-gray-500">[{new Date().toLocaleTimeString()}]</span> {message}
                    </div>
                  ))}
                  <div ref={messagesEndRef} />
                </div>
              )}

              {/* ERROS */}
              {progress && progress.errors.length > 0 && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                  <h4 className="font-medium text-red-800 mb-2">Erros encontrados:</h4>
                  <ul className="text-sm text-red-700 space-y-1">
                    {progress.errors.map((error, index) => (
                      <li key={index}>• {error}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}

          {/* STEP: COMPLETED */}
          {step === 'completed' && progress && (
            <div className="text-center py-8">
              <CheckCircle className="h-16 w-16 text-green-600 mx-auto mb-4" />
              <h3 className="text-xl font-semibold mb-2">Processamento Concluído!</h3>
              <p className="text-gray-600 mb-6">
                {progress.operationsCreated} operações criadas, {progress.operationsUpdated} atualizadas
              </p>
              
              <div className="bg-green-50 rounded-lg p-4 text-left">
                <h4 className="font-medium text-green-800 mb-2">Resumo dos resultados:</h4>
                <ul className="text-sm text-green-700 space-y-1">
                  <li>✅ {progress.operationsCreated} novas operações criadas</li>
                  <li>🔄 {progress.operationsUpdated} operações existentes atualizadas</li>
                  <li>⏭️ {progress.operationsSkipped} itens ignorados (duplicatas)</li>
                  <li>⏱️ Processamento concluído em {formatDuration(progress.elapsedTime)}</li>
                </ul>
              </div>
            </div>
          )}
        </div>

        {/* FOOTER */}
        <div className="p-6 border-t border-gray-200 bg-gray-50">
          <div className="flex justify-between">
            <div>
              {step === 'processing' && (
                <button
                  onClick={cancelProcessing}
                  disabled={cancelling}
                  className="px-4 py-2 text-red-600 border border-red-300 rounded-lg hover:bg-red-50 disabled:opacity-50"
                >
                  {cancelling ? (
                    <>
                      <RefreshCw className="h-4 w-4 animate-spin inline mr-2" />
                      Cancelando...
                    </>
                  ) : (
                    'Cancelar'
                  )}
                </button>
              )}
            </div>
            
            <div className="flex gap-3">
              {step === 'confirm' && (
                <>
                  <button
                    onClick={onClose}
                    className="px-4 py-2 text-gray-600 border border-gray-300 rounded-lg hover:bg-gray-50"
                  >
                    Cancelar
                  </button>
                  <button
                    onClick={startProcessing}
                    disabled={!estimate?.canProceed || processing}
                    className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:bg-gray-400 flex items-center gap-2"
                  >
                    <Play className="h-4 w-4" />
                    Iniciar Processamento
                  </button>
                </>
              )}
              
              {(step === 'completed') && (
                <button
                  onClick={onClose}
                  className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700"
                >
                  Concluir
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}