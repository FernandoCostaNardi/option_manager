import React, { useState, useEffect, useCallback } from 'react';
import { 
  X, Play, CheckCircle, BarChart3, FileText, Zap
} from 'lucide-react';
import { InvoiceProcessingService } from '../services/invoiceProcessingService';
import { ProcessingProgress } from './ProcessingProgress';
import { cleanSpecialCharacters } from '../utils/textCleaner';
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
  initialEstimate?: any; // Estimativa já carregada
}

interface ProcessingEstimate {
  success: boolean;
  totalInvoices: number;
  estimatedOperations: number;
  complexity: string;
  estimatedTimeMs: number;
  estimatedTimeSeconds: number;
  estimatedTimeFormatted: string;
  estimatedRemainingTime: number;
  message: string;
}

// ===== COMPONENTE PRINCIPAL =====
export function ProcessingModal({ 
  isOpen, 
  onClose, 
  invoiceIds, 
  selectedInvoices, 
  onSuccess,
  initialEstimate
}: ProcessingModalProps) {
  // ===== ESTADOS =====
  const [step, setStep] = useState<'estimate' | 'confirm' | 'completed'>('estimate');
  const [estimate, setEstimate] = useState<ProcessingEstimate | null>(null);
  const [processing, setProcessing] = useState(false);
  const [estimateLoading, setEstimateLoading] = useState(false);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [showProgress, setShowProgress] = useState(false);
  const [isCompleted, setIsCompleted] = useState(false); // Proteção contra múltiplos toasts

  // ===== FUNÇÕES =====
  const loadEstimate = useCallback(async () => {
    console.log('🔍 Carregando estimativa para invoices:', invoiceIds);
    setEstimateLoading(true);
    try {
      const result = await InvoiceProcessingService.estimateProcessing(invoiceIds);
      console.log('✅ Estimativa carregada:', result);
      setEstimate(result);
      setStep('confirm');
    } catch (error) {
      console.error('❌ Erro ao estimar processamento:', error);
      
      if (error instanceof Error) {
        if (error.message.includes('não encontrada')) {
          toast.error(`Erro: ${error.message}`);
        } else {
          toast.error('Erro ao estimar processamento. Verifique se as notas selecionadas existem.');
        }
      } else {
        toast.error('Erro ao estimar processamento');
      }
      
      onClose();
    } finally {
      setEstimateLoading(false);
    }
  }, [invoiceIds, onClose]);

  // ===== EFEITOS =====
  useEffect(() => {
    if (isOpen) {
      // Resetar estado de conclusão quando modal abrir
      setIsCompleted(false);
      
      // ✅ DEBUG: Log para verificar dados recebidos pelo modal
      console.log('🔍 ProcessingModal - Dados recebidos:', {
        'invoiceIds': invoiceIds,
        'selectedInvoices': selectedInvoices,
        'selectedInvoices-length': selectedInvoices.length,
        'initialEstimate': initialEstimate
      });
      
      if (initialEstimate) {
        console.log('🔄 Modal aberto - Usando estimativa pré-carregada:', initialEstimate);
        setEstimate(initialEstimate);
        setStep('confirm');
      } else {
        console.log('🔄 Modal aberto - Carregando estimativa para:', invoiceIds);
        loadEstimate();
      }
    }
  }, [isOpen, initialEstimate, loadEstimate, invoiceIds, selectedInvoices]);

  const startProcessing = async () => {
    setProcessing(true);
    
    try {
      const response = await InvoiceProcessingService.processBatch(invoiceIds, {
        maxOperations: 10,
        skipDuplicates: true
      });
      
      if (response.sessionId) {
        setSessionId(response.sessionId);
        setShowProgress(true);
      } else {
        handleProcessingComplete(response);
      }
    } catch (error) {
      console.error('Erro ao iniciar processamento:', error);
      
      if (error instanceof Error) {
        if (error.message.includes('não encontrada')) {
          toast.error(`Erro: ${error.message}`);
        } else {
          toast.error('Erro ao iniciar processamento. Verifique se as notas selecionadas existem.');
        }
      } else {
        toast.error('Erro ao iniciar processamento');
      }
      
      setProcessing(false);
    }
  };

  const handleProcessingComplete = (result: any) => {
    // Proteção contra múltiplos toasts
    if (isCompleted) {
      console.log('🛡️ Processamento já foi completado no modal, ignorando...');
      return;
    }
    
    setIsCompleted(true);
    setProcessing(false);
    setStep('completed');
    
    console.log('📊 Resultado final do processamento:', result);
    
    // Verificar se o processamento foi bem-sucedido
    const isSuccess = result.success === true;
    const isPartialSuccess = result.partialSuccess === true;
    const hasOperationsCreated = (result.operationsCreated || 0) > 0;
    
    if (isSuccess || isPartialSuccess || hasOperationsCreated) {
      // Processamento bem-sucedido
      const message = cleanSpecialCharacters(result.summary || `Processamento concluído! ${result.operationsCreated || 0} operações criadas.`);
      toast.success(message);
      onSuccess();
    } else {
      // Apenas mostrar erro se realmente falhou
      const errorMessage = cleanSpecialCharacters(result.error || 'Processamento falhou');
      const operationsCreated = result.operationsCreated || 0;
      const operationsSkipped = result.operationsSkipped || 0;
      const processingTime = result.processingTimeMs ? `${Math.round(result.processingTimeMs / 1000)}s` : '';
      
      console.error('❌ Processamento falhou:', result);
      toast.error(`${errorMessage}. ${operationsCreated} operações criadas, ${operationsSkipped} ignoradas. Tempo: ${processingTime}`);
      onSuccess();
    }
  };

  const handleProgressComplete = (result: any) => {
    setShowProgress(false);
    setSessionId(null);
    handleProcessingComplete(result);
  };

  const handleProgressError = (error: any) => {
    console.error('❌ Erro no progresso do processamento:', error);
    setShowProgress(false);
    setSessionId(null);
    setProcessing(false);
    
    // Verificar se é realmente um erro ou se o processamento foi concluído
    if (error && typeof error === 'object') {
      const hasSuccess = error.success === true;
      const hasPartialSuccess = error.partialSuccess === true;
      const hasOperations = (error.operationsCreated || 0) > 0;
      const hasError = error.error && error.error.trim() !== '';
      
      if (hasSuccess || hasPartialSuccess || hasOperations) {
        // Se tem indicadores de sucesso, não é um erro real
        console.log('🔄 Erro contém indicadores de sucesso, tratando como conclusão');
        handleProcessingComplete(error);
      } else if (hasError) {
        // Apenas mostrar erro se realmente há um erro
        console.log('❌ Erro real detectado');
        toast.error('Erro no processamento');
      } else {
        // Processamento concluído sem erro específico
        console.log('✅ Processamento concluído sem erro específico');
        handleProcessingComplete(error);
      }
    } else {
      // Erro genérico
      console.log('❌ Erro genérico no processamento');
      toast.error('Erro no processamento');
    }
  };

  const handleProgressClose = () => {
    setShowProgress(false);
    setSessionId(null);
    setProcessing(false);
  };

  const formatCurrency = (value: number) => {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  };

  // ===== RENDER =====
  if (!isOpen) return null;

  return (
    <>
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
                disabled={processing}
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
                <div className="h-12 w-12 text-purple-600 mx-auto mb-4 animate-spin">⏳</div>
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
                      <div className="font-medium">{estimate.estimatedOperations}</div>
                    </div>
                    <div>
                      <span className="text-gray-500">Complexidade:</span>
                      <div className="font-medium capitalize">{estimate.complexity}</div>
                    </div>
                    <div>
                      <span className="text-gray-500">Duração estimada:</span>
                      <div className="font-medium">{estimate.estimatedTimeFormatted}</div>
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

                {/* MENSAGEM DA ESTIMATIVA */}
                {estimate.message && (
                  <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                    <div className="flex">
                      <BarChart3 className="h-5 w-5 text-blue-600 flex-shrink-0 mt-0.5" />
                      <div className="ml-3">
                        <h4 className="text-sm font-medium text-blue-800">Informação da estimativa:</h4>
                        <p className="mt-2 text-sm text-blue-700">{estimate.message}</p>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* STEP: COMPLETED */}
            {step === 'completed' && (
              <div className="text-center py-8">
                <CheckCircle className="h-16 w-16 text-green-600 mx-auto mb-4" />
                <h3 className="text-xl font-semibold mb-2">Processamento Concluído!</h3>
                <p className="text-gray-600 mb-6">
                  Processamento realizado com sucesso
                </p>
                
                <div className="bg-green-50 rounded-lg p-4 text-left">
                  <h4 className="font-medium text-green-800 mb-2">Resumo dos resultados:</h4>
                  <ul className="text-sm text-green-700 space-y-1">
                    <li>✅ Processamento concluído com sucesso</li>
                    <li>⏱️ Operações processadas</li>
                  </ul>
                </div>
              </div>
            )}
          </div>

          {/* FOOTER */}
          <div className="p-6 border-t border-gray-200 bg-gray-50">
            <div className="flex justify-end gap-3">
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
                    disabled={!estimate?.success || processing}
                    className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:bg-gray-400 flex items-center gap-2"
                  >
                    <Play className="h-4 w-4" />
                    Iniciar Processamento
                  </button>
                </>
              )}
              
              {step === 'completed' && (
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

      {/* COMPONENTE DE PROGRESSO */}
      {showProgress && sessionId && (
        <ProcessingProgress
          sessionId={sessionId}
          onComplete={handleProgressComplete}
          onError={handleProgressError}
          onClose={handleProgressClose}
        />
      )}
    </>
  );
}