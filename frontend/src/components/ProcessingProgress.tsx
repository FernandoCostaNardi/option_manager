import React, { useState, useEffect } from 'react';
import { CheckCircle, AlertCircle, RefreshCw } from 'lucide-react';
import toast from 'react-hot-toast';
import { cleanSpecialCharacters, decodeUTF8 } from '../utils/textCleaner';
import '../styles/processing-progress.css';

interface ProcessingProgressProps {
  sessionId: string;
  onComplete?: (result: any) => void;
  onError?: (error: any) => void;
  onClose?: () => void;
}

interface ProgressState {
  percentage: number;
  message: string;
  current: number;
  total: number;
  status: 'PENDING' | 'STARTED' | 'COMPLETED' | 'FINISHED' | 'ERROR';
  type?: string;
  details?: string;
  operationsCreated?: number;
  operationsSkipped?: number;
  operationsUpdated?: number;
  processingTimeMs?: number;
}

export function ProcessingProgress({ sessionId, onComplete, onError, onClose }: ProcessingProgressProps) {
  const [progress, setProgress] = useState<ProgressState>({
    percentage: 0,
    message: 'Iniciando processamento...',
    current: 0,
    total: 0,
    status: 'PENDING'
  });
  
  const [isVisible, setIsVisible] = useState(false);
  const [eventSource, setEventSource] = useState<EventSource | null>(null);
  const [completedOperations, setCompletedOperations] = useState(0);

  useEffect(() => {
    if (!sessionId) return;

    console.log('🔗 Conectando ao SSE para sessão:', sessionId);
    setIsVisible(true);
    
    // Conectar ao SSE quando iniciar processamento
    const token = localStorage.getItem('token');
    if (!token) {
      console.error('❌ Token não encontrado para SSE');
      toast.error('Token de autenticação não encontrado');
      return;
    }

    const baseUrl = 'http://localhost:8080';
    const encodedToken = encodeURIComponent(token);
    const url = `${baseUrl}/api/processing/progress/${sessionId}?token=${encodedToken}`;
    
    console.log('🔗 Conectando ao SSE:', url.substring(0, url.indexOf('?token=')) + '?token=***');
    
    const newEventSource = new EventSource(url);
    setEventSource(newEventSource);

    // Listener para evento 'connected'
    newEventSource.addEventListener('connected', (event: any) => {
      console.log('✅ Conectado ao progresso em tempo real');
      try {
        const data = JSON.parse(event.data);
        console.log('📨 Dados de conexão:', data);
        updateProgressUI({
          type: 'STARTED',
          message: 'Conectado ao progresso em tempo real',
          current: 0,
          total: 0,
          status: 'STARTED',
          percentage: 0
        });
      } catch (error) {
        console.error('❌ Erro ao parsear dados de conexão:', error);
      }
    });

    // Listener para evento 'progress'
    newEventSource.addEventListener('progress', (event: any) => {
      console.log('📩 EVENTO PROGRESS RECEBIDO:', event);
      try {
        const eventData = JSON.parse(event.data);
        console.log('📨 Dados de progresso:', eventData);
        updateProgressUI(eventData);
      } catch (error) {
        console.error('❌ Erro ao parsear dados do progresso:', error);
      }
    });

    // Listener para evento 'complete'
    newEventSource.addEventListener('complete', (event: any) => {
      console.log('🏁 EVENTO COMPLETE RECEBIDO:', event);
      try {
        const data = JSON.parse(event.data);
        console.log('📨 Dados de conclusão:', data);
        newEventSource.close();
        handleProcessingComplete(data);
      } catch (error) {
        console.error('❌ Erro ao parsear dados do evento complete:', error);
      }
    });

    // Listener para evento 'finished'
    newEventSource.addEventListener('finished', (event: any) => {
      console.log('🏁 EVENTO FINISHED RECEBIDO:', event);
      try {
        const data = JSON.parse(event.data);
        console.log('📨 Dados de finalização:', data);
        newEventSource.close();
        handleProcessingComplete(data);
      } catch (error) {
        console.error('❌ Erro ao parsear dados do evento finished:', error);
      }
    });

    // Listener para evento 'message' (fallback)
    newEventSource.addEventListener('message', (event: any) => {
      console.log('📩 EVENTO MESSAGE RECEBIDO:', event);
      try {
        const data = JSON.parse(event.data);
        console.log('📨 Dados de mensagem:', data);
        
        // Se for um evento de finalização
        if (data.type === 'FINISHED' || data.status === 'FINISHED') {
          console.log('🏁 Detectado evento de finalização via message');
          newEventSource.close();
          handleProcessingComplete(data);
        } else {
          updateProgressUI(data);
        }
      } catch (error) {
        console.error('❌ Erro ao parsear dados do evento message:', error);
      }
    });

    // Listener para evento 'error' (erro no processamento)
    newEventSource.addEventListener('error', (event: any) => {
      console.log('❌ EVENTO ERROR RECEBIDO:', event);
      try {
        const data = JSON.parse(event.data);
        console.log('📨 Dados de erro:', data);
        newEventSource.close();
        handleProcessingComplete(data);
      } catch (error) {
        console.error('❌ Erro ao parsear dados do evento error:', error);
      }
    });

    newEventSource.onerror = (error) => {
      console.error('❌ ERRO EventSource:', error);
      console.error('   ReadyState:', newEventSource.readyState);
      
      if (newEventSource.readyState === EventSource.CLOSED) {
        console.error('💀 EventSource fechado definitivamente');
        toast.error('Conexão perdida com o servidor');
      } else {
        console.warn('🔄 EventSource tentando reconectar...');
      }
    };

    // Timeout de segurança para fechar o modal se não receber evento de finalização
    const safetyTimeout = setTimeout(() => {
      console.log('⏰ Timeout de segurança - fechando modal');
      if (newEventSource) {
        newEventSource.close();
      }
      handleProcessingComplete({ type: 'TIMEOUT', message: 'Processamento concluído (timeout)' });
    }, 30000); // 30 segundos

    return () => {
      clearTimeout(safetyTimeout);
      if (newEventSource) {
        newEventSource.close();
      }
    };
  }, [sessionId]);

  const updateProgressUI = (eventData: any) => {
    console.log('🔄 Atualizando UI de progresso:', eventData);
    
    // Extrair dados do evento
    const { 
      type,
      message, 
      current, 
      total, 
      percentage, 
      status,
      operationsCreated,
      operationsSkipped,
      operationsUpdated,
      processingTimeMs,
      details
    } = eventData;
    
    // Limpar a mensagem de caracteres especiais
    let cleanMessage = message || '';
    
    // Primeiro tentar decodificar UTF-8
    if (cleanMessage) {
      cleanMessage = decodeUTF8(cleanMessage);
      console.log('📝 Mensagem após UTF-8:', cleanMessage);
    }
    
    // Depois aplicar limpeza de caracteres especiais
    if (cleanMessage) {
      cleanMessage = cleanSpecialCharacters(cleanMessage);
      console.log('📝 Mensagem após limpeza:', cleanMessage);
    }
    
    console.log('📝 Mensagem original:', message);
    console.log('📝 Mensagem final:', cleanMessage);
    
    setProgress(prev => ({
      ...prev,
      percentage: percentage ?? prev.percentage,
      message: cleanMessage || prev.message,
      current: current ?? prev.current,
      total: total ?? prev.total,
      status: status ?? prev.status,
      operationsCreated: operationsCreated ?? prev.operationsCreated,
      operationsSkipped: operationsSkipped ?? prev.operationsSkipped,
      operationsUpdated: operationsUpdated ?? prev.operationsUpdated,
      processingTimeMs: processingTimeMs ?? prev.processingTimeMs
    }));

    // Verificar se o processamento terminou
    if (percentage === 100 || status === 'FINISHED' || type === 'FINISHED') {
      console.log('🏁 Processamento terminou - fechando modal em 3 segundos');
      setTimeout(() => {
        handleProcessingComplete(eventData);
      }, 3000);
    }
    
    // Log específico baseado no conteúdo
    if (message) {
      console.log('📝 Nova mensagem limpa:', cleanMessage);
    }
    
    if (operationsCreated !== undefined || operationsSkipped !== undefined) {
      console.log('📊 Contadores atualizados:', { operationsCreated, operationsSkipped, operationsUpdated });
    }
    
    // Ações específicas por tipo de evento
    switch (type) {
      case 'STARTED':
        console.log('🚀 Processamento iniciado');
        setCompletedOperations(0);
        break;
      case 'PROCESSING':
        console.log('⚙️ Processando operação:', current, 'de', total);
        break;
      case 'COMPLETED':
        console.log('✅ Operação concluída:', current);
        // Incrementar contador de operações completadas
        setCompletedOperations(prev => prev + 1);
        break;
      case 'FINISHED':
        console.log('🏁 Processamento finalizado');
        // Definir todas as operações como criadas no final
        setProgress(prev => ({
          ...prev,
          operationsCreated: completedOperations + 1, // +1 para a última operação
          operationsSkipped: 0,
          operationsUpdated: 0
        }));
        break;
      case 'ERROR':
        console.error('❌ Erro no processamento:', details);
        break;
    }
  };

  const handleProcessingComplete = (result: any) => {
    console.log('📊 Resultado final do processamento:', result);
    
    // Fechar o EventSource se ainda estiver aberto
    if (eventSource) {
      eventSource.close();
      setEventSource(null);
    }
    
    if (result.success || result.partialSuccess || result.type === 'FINISHED') {
      const totalOperations = result.current || completedOperations || 0;
      const message = cleanSpecialCharacters(result.summary || result.message || `Processamento concluído! ${totalOperations} operações criadas.`);
      toast.success(message);
      onComplete?.(result);
    } else {
      const errorMessage = cleanSpecialCharacters(result.error || 'Processamento falhou');
      const totalOperations = result.current || completedOperations || 0;
      const processingTime = result.processingTimeMs ? `${Math.round(result.processingTimeMs / 1000)}s` : '';
      
      console.error('❌ Processamento falhou:', result);
      toast.error(`${errorMessage}. ${totalOperations} operações processadas. Tempo: ${processingTime}`);
      onError?.(result);
    }
    
    // Garantir que o modal seja fechado
    setIsVisible(false);
  };

  const cancelProcessing = () => {
    if (eventSource) {
      eventSource.close();
    }
    toast.success('Processamento cancelado');
    onClose?.();
  };

  if (!isVisible) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-xl max-w-2xl w-full p-6">
        {/* HEADER */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <RefreshCw className={`h-6 w-6 text-purple-600 ${progress.status === 'STARTED' ? 'animate-spin' : ''}`} />
            <div>
              <h3 className="text-lg font-semibold">Processamento em Andamento</h3>
              <p className="text-sm text-gray-500">Sessão: {sessionId.substring(0, 8)}...</p>
            </div>
          </div>
          
          <button
            onClick={cancelProcessing}
            className="text-gray-400 hover:text-gray-600"
          >
            ✕
          </button>
        </div>

        {/* PROGRESS BAR */}
        <div className="mb-6">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-gray-700">Progresso</span>
            <span className="text-sm text-gray-500">{progress.percentage}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-3">
            <div 
              className="bg-purple-600 h-3 rounded-full transition-all duration-500"
              style={{ width: `${progress.percentage}%` }}
            />
          </div>
        </div>

        {/* MESSAGE */}
        <div className="mb-6">
          <div className="text-center">
            <div className="text-lg font-medium text-gray-900 mb-2">
              {progress.message}
            </div>
            <div className="text-sm text-gray-500">
              {progress.current} de {progress.total} operações
            </div>
          </div>
        </div>

        {/* COUNTERS */}
        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="text-center">
            <div className="text-lg font-bold text-green-600">
              {progress.status === 'FINISHED' ? (progress.operationsCreated || completedOperations) : completedOperations}
            </div>
            <div className="text-sm text-gray-500">Completadas</div>
          </div>
          <div className="text-center">
            <div className="text-lg font-bold text-blue-600">
              {progress.current || 0}
            </div>
            <div className="text-sm text-gray-500">Atual</div>
          </div>
          <div className="text-center">
            <div className="text-lg font-bold text-yellow-600">
              {progress.total || 0}
            </div>
            <div className="text-sm text-gray-500">Total</div>
          </div>
        </div>

        {/* STATUS */}
        <div className="text-center">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium">
            {progress.status === 'ERROR' ? (
              <>
                <AlertCircle className="h-4 w-4 text-red-500" />
                <span className="text-red-700">Erro no Processamento</span>
              </>
            ) : progress.status === 'FINISHED' ? (
              <>
                <CheckCircle className="h-4 w-4 text-green-500" />
                <span className="text-green-700">Processamento Concluído</span>
              </>
            ) : (
              <>
                <RefreshCw className="h-4 w-4 text-purple-500 animate-spin" />
                <span className="text-purple-700">Processando...</span>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
} 