import React, { useState, useEffect } from 'react';
import { X, Loader2, Target } from 'lucide-react';
import api from '../../../services/api';

interface VisualizarTargetsModalProps {
  isOpen: boolean;
  operationId: string | null;
  onClose: () => void;
}

interface OperationTarget {
  id: string;
  type: 'TARGET' | 'STOP_LOSS';
  sequence: number;
  value: string;
  reached: boolean;
}

export function VisualizarTargetsModal({ isOpen, operationId, onClose }: VisualizarTargetsModalProps) {
  const [targets, setTargets] = useState<OperationTarget[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Adicionando console.log para depuração
  useEffect(() => {
    console.log('Modal aberto:', isOpen);
    console.log('ID da operação:', operationId);
    
    if (isOpen && operationId) {
      console.log('Iniciando carregamento de targets para operação:', operationId);
      carregarTargets();
    } else {
      // Limpar targets quando o modal for fechado
      setTargets([]);
    }
  }, [isOpen, operationId]);

  const carregarTargets = async () => {
    if (!operationId) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await api.get(`/operations/${operationId}/targets`);
      setTargets(response.data);
    } catch (error) {
      console.error('Erro ao carregar targets:', error);
      setError('Não foi possível carregar os targets da operação.');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div 
      role="dialog" 
      aria-modal="true" 
      className="fixed inset-0 flex items-center justify-center z-50 p-4"
    >
      {/* Overlay do modal */}
      <div className="fixed inset-0 bg-gray-900 bg-opacity-75" onClick={onClose} />
      
      {/* Conteúdo do modal */}
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md overflow-hidden relative z-10">
        {/* Cabeçalho do modal */}
        <div className="flex justify-between items-center p-4 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-800 flex items-center gap-2">
            <Target className="h-5 w-5 text-purple-500" />
            Targets e Stop Loss
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 focus:outline-none"
            aria-label="Fechar modal"
          >
            <X className="h-6 w-6" />
          </button>
        </div>
        
        {/* Corpo do modal */}
        <div className="p-6">
          {error && (
            <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-sm flex items-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
              </svg>
              {error}
            </div>
          )}
          
          {loading ? (
            <div className="flex justify-center items-center py-10">
              <Loader2 className="animate-spin w-8 h-8 text-purple-600" />
              <span className="ml-3 text-gray-600">Carregando targets...</span>
            </div>
          ) : (
            <div className="space-y-4">
              {targets.length === 0 ? (
                <p className="text-center text-gray-500 py-4">Nenhum target ou stop loss definido para esta operação.</p>
              ) : (
                <div className="space-y-3">
                  {targets
                    .sort((a, b) => {
                      // Primeiro ordenar por tipo (STOP_LOSS primeiro, depois TARGET)
                      if (a.type !== b.type) {
                        return a.type === 'STOP_LOSS' ? -1 : 1;
                      }
                      // Depois ordenar por sequência
                      return a.sequence - b.sequence;
                    })
                    .map((target, index) => (
                      <div 
                        key={target.id || index}
                        className={`p-3 rounded-lg border ${
                          target.type === 'TARGET' 
                            ? 'border-green-200 bg-green-50' 
                            : 'border-red-200 bg-red-50'
                        }`}
                      >
                        <div className="flex justify-between items-center">
                          <div>
                            <span className={`text-sm font-medium ${
                              target.type === 'TARGET' ? 'text-green-700' : 'text-red-700'
                            }`}>
                              {target.type === 'TARGET' ? `Target ${target.sequence}` : 'Stop Loss'}
                            </span>
                            <p className="text-lg font-bold mt-1">
                              R$ {parseFloat(target.value).toFixed(2).replace('.', ',')}
                            </p>
                          </div>
                          {target.reached && (
                            <span className="px-2 py-1 text-xs font-medium bg-blue-100 text-blue-800 rounded-full">
                              Atingido
                            </span>
                          )}
                        </div>
                      </div>
                    ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}