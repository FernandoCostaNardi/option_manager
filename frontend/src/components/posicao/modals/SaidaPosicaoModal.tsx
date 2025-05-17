import React, { useState, useEffect } from 'react';
import { Loader2, X, HelpCircle } from 'lucide-react';
import { Posicao } from '../../../types/posicao/posicoes.types';
import { PosicaoService } from '../../../services/posicaoService';

interface SaidaPosicaoModalProps {
  isOpen: boolean;
  posicao: Posicao | null;
  onClose: () => void;
  onSuccess: () => void;
}

export function SaidaPosicaoModal({ isOpen, posicao, onClose, onSuccess }: SaidaPosicaoModalProps) {
  // Estados do formulário
  const [form, setForm] = useState({
    exitDate: '',
    exitUnitPrice: '',
    quantity: '',
    isTotalExit: true,
    applyFifoLifo: true
  });
  
  // Estados de UI
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Efeito para resetar o formulário quando o modal é aberto
  useEffect(() => {
    if (isOpen && posicao) {
      setForm({
        exitDate: new Date().toISOString().split('T')[0],
        exitUnitPrice: '',
        quantity: posicao.remainingQuantity.toString(),
        isTotalExit: true,
        applyFifoLifo: true
      });
      setError(null);
    }
  }, [isOpen, posicao]);
  
  if (!isOpen || !posicao) {
    return null;
  }
  
  // Handler para mudanças nos campos
  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target as HTMLInputElement;
    
    if (name === 'exitUnitPrice' || name === 'quantity') {
      // Para campos numéricos, validar e formatar
      const numValue = value.replace(/[^0-9.,]/g, '').replace(',', '.');
      setForm(prev => ({
        ...prev,
        [name]: numValue
      }));
    } else if (type === 'checkbox') {
      const isChecked = (e.target as HTMLInputElement).checked;
      
      // Atualizar estado baseado no checkbox
      setForm(prev => ({
        ...prev,
        [name]: isChecked
      }));
      
      // Se alterou para saída total, atualizar a quantidade
      if (name === 'isTotalExit' && isChecked) {
        setForm(prev => ({
          ...prev,
          quantity: posicao.remainingQuantity.toString()
        }));
      }
    } else {
      // Para outros campos
      setForm(prev => ({
        ...prev,
        [name]: value
      }));
    }
  };
  
  // Handler para submit do formulário
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    
    // Validação
    if (!form.exitDate || !form.exitUnitPrice || (!form.isTotalExit && !form.quantity)) {
      setError('Preencha todos os campos obrigatórios.');
      return;
    }
    
    // Validar quantidade para saída parcial
    if (!form.isTotalExit) {
      const quantity = parseInt(form.quantity, 10);
      if (isNaN(quantity) || quantity <= 0 || quantity > posicao.remainingQuantity) {
        setError(`A quantidade deve ser maior que zero e menor ou igual a ${posicao.remainingQuantity}.`);
        return;
      }
    }
    
    setLoading(true);
    
    try {
      // Preparar dados
      const exitData = {
        exitDate: form.exitDate,
        exitUnitPrice: parseFloat(form.exitUnitPrice),
        ...(form.isTotalExit ? {} : { quantity: parseInt(form.quantity, 10) }),
        applyFifoLifo: form.applyFifoLifo
      };
      
      // Enviar requisição
      await PosicaoService.realizarSaidaPosicao(posicao.id, exitData);
      
      // Em caso de sucesso
      onSuccess();
    } catch (err) {
      console.error('Erro ao realizar saída:', err);
      setError('Não foi possível realizar a saída. Tente novamente.');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md overflow-hidden">
        {/* Cabeçalho */}
        <div className="flex justify-between items-center px-6 py-4 bg-gray-50 border-b">
          <h3 className="text-lg font-semibold text-gray-800">
            Realizar Saída - {posicao.optionSeriesCode}
          </h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X className="h-5 w-5" />
          </button>
        </div>
        
        {/* Corpo do modal */}
        <div className="p-6">
          {error && (
            <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-md text-sm">
              {error}
            </div>
          )}
          
          <form onSubmit={handleSubmit}>
            <div className="space-y-4">
              {/* Data de saída */}
              <div>
                <label htmlFor="exitDate" className="block text-sm font-medium text-gray-700 mb-1">
                  Data de Saída *
                </label>
                <input
                  type="date"
                  id="exitDate"
                  name="exitDate"
                  value={form.exitDate}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                  required
                />
              </div>
              
              {/* Preço unitário */}
              <div>
                <label htmlFor="exitUnitPrice" className="block text-sm font-medium text-gray-700 mb-1">
                  Preço Unitário (R$) *
                </label>
                <input
                  type="text"
                  id="exitUnitPrice"
                  name="exitUnitPrice"
                  value={form.exitUnitPrice}
                  onChange={handleChange}
                  placeholder="0,00"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                  required
                />
              </div>
              
              {/* Tipo de saída */}
              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="isTotalExit"
                  name="isTotalExit"
                  checked={form.isTotalExit}
                  onChange={handleChange}
                  className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                />
                <label htmlFor="isTotalExit" className="ml-2 block text-sm text-gray-700">
                  Saída total da posição
                </label>
              </div>
              
              {/* Quantidade (para saída parcial) */}
              {!form.isTotalExit && (
                <div>
                  <label htmlFor="quantity" className="block text-sm font-medium text-gray-700 mb-1">
                    Quantidade *
                  </label>
                  <input
                    type="text"
                    id="quantity"
                    name="quantity"
                    value={form.quantity}
                    onChange={handleChange}
                    placeholder={`Max: ${posicao.remainingQuantity}`}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                    required
                  />
                  <p className="mt-1 text-xs text-gray-500">
                    Máximo disponível: {posicao.remainingQuantity}
                  </p>
                </div>
              )}
              
              {/* Opção de FIFO/LIFO (para múltiplas entradas) */}
              {posicao.remainingQuantity !== posicao.totalQuantity && (
                <div className="mt-4 p-3 bg-blue-50 rounded-md">
                  <div className="flex items-start mb-2">
                    <HelpCircle className="h-5 w-5 text-blue-500 mr-2 mt-0.5" />
                    <div>
                      <p className="text-sm font-medium text-blue-700">
                        Posição com múltiplas entradas
                      </p>
                      <p className="text-xs text-blue-600 mt-1">
                        Esta posição possui múltiplas entradas com preços diferentes.
                      </p>
                    </div>
                  </div>
                  
                  <div className="flex items-center mt-2">
                    <input
                      type="checkbox"
                      id="applyFifoLifo"
                      name="applyFifoLifo"
                      checked={form.applyFifoLifo}
                      onChange={handleChange}
                      className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                    />
                    <label htmlFor="applyFifoLifo" className="ml-2 block text-sm text-gray-700">
                      Aplicar regras automáticas (FIFO/LIFO)
                    </label>
                  </div>
                  <p className="mt-1 text-xs text-gray-500">
                    FIFO para operações de dias diferentes, LIFO para operações do mesmo dia.
                  </p>
                </div>
              )}
              
              {/* Resumo */}
              <div className="bg-gray-50 p-4 rounded-md mt-4">
                <h4 className="text-sm font-medium text-gray-800 mb-2">Resumo da Posição</h4>
                <div className="grid grid-cols-2 gap-2 text-sm">
                  <div className="text-gray-600">Ativo:</div>
                  <div className="font-medium">{posicao.optionSeriesCode}</div>
                  <div className="text-gray-600">Tipo:</div>
                  <div className="font-medium">{posicao.optionType}</div>
                  <div className="text-gray-600">Direção:</div>
                  <div className="font-medium">{posicao.direction}</div>
                  <div className="text-gray-600">Preço médio:</div>
                  <div className="font-medium">
                    R$ {posicao.averageEntryPrice.toFixed(2).replace('.', ',')}
                  </div>
                  <div className="text-gray-600">Quantidade total:</div>
                  <div className="font-medium">{posicao.totalQuantity}</div>
                  <div className="text-gray-600">Quantidade restante:</div>
                  <div className="font-medium">{posicao.remainingQuantity}</div>
                </div>
              </div>
            </div>
            
            {/* Botões de ação */}
            <div className="mt-6 flex justify-end gap-3">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50"
                disabled={loading}
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="px-4 py-2 bg-indigo-600 text-white rounded-md text-sm font-medium hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                disabled={loading}
              >
                {loading ? (
                  <span className="flex items-center">
                    <Loader2 className="animate-spin h-4 w-4 mr-2" />
                    Processando...
                  </span>
                ) : (
                  'Realizar Saída'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}