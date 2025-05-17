import React, { useState, useEffect } from 'react';
import { Loader2, X } from 'lucide-react';
import { Posicao } from '../../../types/posicao/posicoes.types';
import { PosicaoService } from '../../../services/posicaoService';

interface NovaEntradaModalProps {
  isOpen: boolean;
  posicao: Posicao | null;
  onClose: () => void;
  onSuccess: () => void;
}

export function NovaEntradaModal({ isOpen, posicao, onClose, onSuccess }: NovaEntradaModalProps) {
  // Estados do formulário
  const [form, setForm] = useState({
    entryDate: '',
    entryUnitPrice: '',
    quantity: '',
    brokerageId: '',
    analysisHouseId: ''
  });
  
  // Estados de UI
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [brokerages, setBrokerages] = useState<any[]>([]);
  const [analysisHouses, setAnalysisHouses] = useState<any[]>([]);
  
  // Efeito para resetar o formulário quando o modal é aberto
  useEffect(() => {
    if (isOpen && posicao) {
      setForm({
        entryDate: new Date().toISOString().split('T')[0],
        entryUnitPrice: '',
        quantity: '',
        brokerageId: posicao.brokerageName ? '' : '', // Idealmente, pegar o ID da corretora atual
        analysisHouseId: ''
      });
      setError(null);
      
      // Em uma implementação real, buscar corretoras e casas de análise disponíveis
      // Ex: BrokerageService.getBrokerages().then(data => setBrokerages(data))
    }
  }, [isOpen, posicao]);
  
  if (!isOpen || !posicao) {
    return null;
  }
  
  // Handler para mudanças nos campos
  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    
    if (name === 'entryUnitPrice' || name === 'quantity') {
      // Para campos numéricos, validar e formatar
      const numValue = value.replace(/[^0-9.,]/g, '').replace(',', '.');
      setForm(prev => ({
        ...prev,
        [name]: numValue
      }));
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
    if (!form.entryDate || !form.entryUnitPrice || !form.quantity || !form.brokerageId) {
      setError('Preencha todos os campos obrigatórios.');
      return;
    }
    
    setLoading(true);
    
    try {
      // Preparar dados
      const entryData = {
        entryDate: form.entryDate,
        entryUnitPrice: parseFloat(form.entryUnitPrice),
        quantity: parseInt(form.quantity, 10),
        brokerageId: form.brokerageId,
        ...(form.analysisHouseId ? { analysisHouseId: form.analysisHouseId } : {})
      };
      
      // Enviar requisição
      await PosicaoService.adicionarEntradaPosicao(posicao.id, entryData);
      
      // Em caso de sucesso
      onSuccess();
    } catch (err) {
      console.error('Erro ao adicionar entrada:', err);
      setError('Não foi possível adicionar a entrada. Tente novamente.');
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
            Nova Entrada para {posicao.optionSeriesCode}
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
              {/* Data de entrada */}
              <div>
                <label htmlFor="entryDate" className="block text-sm font-medium text-gray-700 mb-1">
                  Data de Entrada *
                </label>
                <input
                  type="date"
                  id="entryDate"
                  name="entryDate"
                  value={form.entryDate}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                  required
                />
              </div>
              
              {/* Preço unitário */}
              <div>
                <label htmlFor="entryUnitPrice" className="block text-sm font-medium text-gray-700 mb-1">
                  Preço Unitário (R$) *
                </label>
                <input
                  type="text"
                  id="entryUnitPrice"
                  name="entryUnitPrice"
                  value={form.entryUnitPrice}
                  onChange={handleChange}
                  placeholder="0,00"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                  required
                />
              </div>
              
              {/* Quantidade */}
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
                  placeholder="0"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                  required
                />
              </div>
              
              {/* Corretora */}
              <div>
                <label htmlFor="brokerageId" className="block text-sm font-medium text-gray-700 mb-1">
                  Corretora *
                </label>
                <select
                  id="brokerageId"
                  name="brokerageId"
                  value={form.brokerageId}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                  required
                >
                  <option value="">Selecione uma corretora</option>
                  {brokerages.map(broker => (
                    <option key={broker.id} value={broker.id}>
                      {broker.name}
                    </option>
                  ))}
                </select>
              </div>
              
              {/* Casa de análise (opcional) */}
              <div>
                <label htmlFor="analysisHouseId" className="block text-sm font-medium text-gray-700 mb-1">
                  Casa de Análise (opcional)
                </label>
                <select
                  id="analysisHouseId"
                  name="analysisHouseId"
                  value={form.analysisHouseId}
                  onChange={handleChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                >
                  <option value="">Selecione uma casa de análise</option>
                  {analysisHouses.map(house => (
                    <option key={house.id} value={house.id}>
                      {house.name}
                    </option>
                  ))}
                </select>
              </div>
              
              {/* Resumo */}
              <div className="bg-gray-50 p-4 rounded-md mt-4">
                <h4 className="text-sm font-medium text-gray-800 mb-2">Resumo da Operação</h4>
                <div className="grid grid-cols-2 gap-2 text-sm">
                  <div className="text-gray-600">Ativo:</div>
                  <div className="font-medium">{posicao.optionSeriesCode}</div>
                  <div className="text-gray-600">Tipo:</div>
                  <div className="font-medium">{posicao.optionType}</div>
                  <div className="text-gray-600">Direção:</div>
                  <div className="font-medium">{posicao.direction}</div>
                  <div className="text-gray-600">Preço médio atual:</div>
                  <div className="font-medium">
                    R$ {posicao.averageEntryPrice.toFixed(2).replace('.', ',')}
                  </div>
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
                  'Adicionar Entrada'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}