import React, { useState, useEffect } from 'react';
import { X } from 'lucide-react';

interface FinalizarOperacaoModalProps {
  isOpen: boolean;
  operacaoId: string | null;
  onClose: () => void;
  onSuccess: () => void;
}

interface OperacaoAtiva {
  id: string;
  dataEntrada: string;
  casaAnalise: string;
  corretora: string;
  opcao: string;
  logoEmpresa: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
}

export function FinalizarOperacaoModal({ isOpen, operacaoId, onClose, onSuccess }: FinalizarOperacaoModalProps) {
  // Estado para armazenar os dados da operação ativa
  const [operacao, setOperacao] = useState<OperacaoAtiva | null>(null);
  
  // Estados para os campos do formulário
  const [dataSaida, setDataSaida] = useState('');
  const [valorUnitarioSaida, setValorUnitarioSaida] = useState('');
  
  // Estados calculados
  const [valorTotalSaida, setValorTotalSaida] = useState(0);
  const [valorLucroPrejuizo, setValorLucroPrejuizo] = useState(0);
  const [percentualLucroPrejuizo, setPercentualLucroPrejuizo] = useState(0);
  const [status, setStatus] = useState<'Vencedor' | 'Perdedor'>('Vencedor');
  
  // Estado para controle de carregamento
  const [loading, setLoading] = useState(false);
  const [loadingOperacao, setLoadingOperacao] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Carregar os dados da operação quando o modal for aberto
  useEffect(() => {
    if (isOpen && operacaoId) {
      carregarOperacao(operacaoId);
    }
  }, [isOpen, operacaoId]);

  // Definir a data de saída como a data atual quando o modal for aberto
  useEffect(() => {
    if (isOpen) {
      const hoje = new Date();
      const dataFormatada = hoje.toISOString().split('T')[0]; // Formato YYYY-MM-DD
      setDataSaida(dataFormatada);
    }
  }, [isOpen]);

  // Calcular os valores quando o valor unitário de saída mudar
  useEffect(() => {
    if (operacao && valorUnitarioSaida) {
      const valorUnitSaida = parseFloat(valorUnitarioSaida);
      const valorTotalSaida = operacao.quantidade * valorUnitSaida;
      const lucroPrejuizo = valorTotalSaida - operacao.valorTotal;
      const percentual = (lucroPrejuizo / operacao.valorTotal) * 100;
      
      setValorTotalSaida(valorTotalSaida);
      setValorLucroPrejuizo(lucroPrejuizo);
      setPercentualLucroPrejuizo(percentual);
      setStatus(lucroPrejuizo >= 0 ? 'Vencedor' : 'Perdedor');
    }
  }, [operacao, valorUnitarioSaida]);

  // Função para carregar os dados da operação
  const carregarOperacao = async (id: string) => {
    setLoadingOperacao(true);
    setError(null);

    try {
      // Aqui seria a chamada para a API real
      // Por enquanto, simulamos com dados mockados
      await new Promise(resolve => setTimeout(resolve, 500));
      
      // Dados mockados para exemplo
      const operacaoMock: OperacaoAtiva = {
        id: id,
        dataEntrada: '2023-05-15',
        casaAnalise: 'Empiricus',
        corretora: 'XP Investimentos',
        opcao: 'PETR4',
        logoEmpresa: 'https://logodownload.org/wp-content/uploads/2014/07/petrobras-logo-1-1.png',
        quantidade: 100,
        valorUnitario: 32.50,
        valorTotal: 3250.00
      };
      
      setOperacao(operacaoMock);
    } catch (error) {
      console.error('Erro ao carregar operação:', error);
      setError('Não foi possível carregar os dados da operação. Tente novamente.');
    } finally {
      setLoadingOperacao(false);
    }
  };

  // Função para limpar o formulário
  const limparFormulario = () => {
    setDataSaida('');
    setValorUnitarioSaida('');
    setValorTotalSaida(0);
    setValorLucroPrejuizo(0);
    setPercentualLucroPrejuizo(0);
    setStatus('Vencedor');
    setOperacao(null);
    setError(null);
  };

  // Função para fechar o modal
  const handleClose = () => {
    limparFormulario();
    onClose();
  };

  // Função para finalizar a operação
  const handleFinalizar = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    // Validação básica
    if (!dataSaida || !valorUnitarioSaida) {
      setError('Todos os campos são obrigatórios');
      setLoading(false);
      return;
    }

    try {
      // Aqui seria a chamada para a API real
      // Por enquanto, apenas simulamos uma chamada bem-sucedida
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Notificar sucesso e fechar o modal
      onSuccess();
      handleClose();
    } catch (error) {
      console.error('Erro ao finalizar operação:', error);
      setError('Não foi possível finalizar a operação. Tente novamente.');
    } finally {
      setLoading(false);
    }
  };

  // Função para formatar valores em reais
  const formatarMoeda = (valor: number) => {
    return valor.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    });
  };

  // Se o modal não estiver aberto, não renderiza nada
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md">
        <div className="flex justify-between items-center p-6 border-b border-gray-100">
          <h2 className="text-xl font-semibold text-gray-800">Finalizar Operação</h2>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        {loadingOperacao ? (
          <div className="p-6 flex flex-col items-center justify-center">
            <div className="animate-spin h-8 w-8 border-4 border-purple-500 rounded-full border-t-transparent mb-4"></div>
            <p className="text-gray-500">Carregando dados da operação...</p>
          </div>
        ) : error && !operacao ? (
          <div className="p-6">
            <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-sm">
              {error}
            </div>
            <div className="flex justify-end">
              <button
                onClick={handleClose}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300"
              >
                Fechar
              </button>
            </div>
          </div>
        ) : operacao ? (
          <form onSubmit={handleFinalizar} className="p-6">
            {error && (
              <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-sm">
                {error}
              </div>
            )}

            <div className="mb-4 p-3 bg-gray-50 rounded-lg">
              <h3 className="font-medium text-gray-700 mb-2">Dados da Operação</h3>
              <div className="grid grid-cols-2 gap-2 text-sm">
                <div>
                  <span className="text-gray-500">Opção:</span>
                  <p className="font-medium">{operacao.opcao}</p>
                </div>
                <div>
                  <span className="text-gray-500">Quantidade:</span>
                  <p className="font-medium">{operacao.quantidade}</p>
                </div>
                <div>
                  <span className="text-gray-500">Valor Unitário:</span>
                  <p className="font-medium">{formatarMoeda(operacao.valorUnitario)}</p>
                </div>
                <div>
                  <span className="text-gray-500">Valor Total:</span>
                  <p className="font-medium">{formatarMoeda(operacao.valorTotal)}</p>
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <div>
                <label htmlFor="dataSaida" className="block text-sm font-medium text-gray-700 mb-1">
                  Data de Saída
                </label>
                <input
                  type="date"
                  id="dataSaida"
                  value={dataSaida}
                  onChange={(e) => setDataSaida(e.target.value)}
                  className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                  disabled={loading}
                />
              </div>

              <div>
                <label htmlFor="valorUnitarioSaida" className="block text-sm font-medium text-gray-700 mb-1">
                  Valor Unitário de Saída (R$)
                </label>
                <input
                  type="number"
                  id="valorUnitarioSaida"
                  value={valorUnitarioSaida}
                  onChange={(e) => setValorUnitarioSaida(e.target.value)}
                  className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                  placeholder="Ex: 35.75"
                  step="0.01"
                  min="0.01"
                  disabled={loading}
                />
              </div>

              {valorUnitarioSaida && (
                <div className="p-3 bg-gray-50 rounded-lg space-y-2">
                  <div className="flex justify-between">
                    <span className="text-gray-600">Valor Total de Saída:</span>
                    <span className="font-medium">{formatarMoeda(valorTotalSaida)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Lucro/Prejuízo:</span>
                    <span className={`font-medium ${valorLucroPrejuizo >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                      {formatarMoeda(valorLucroPrejuizo)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Percentual:</span>
                    <span className={`font-medium ${percentualLucroPrejuizo >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                      {percentualLucroPrejuizo.toFixed(2)}%
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Status:</span>
                    <span className={`px-2 py-1 text-xs font-semibold rounded-full ${status === 'Vencedor' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                      {status}
                    </span>
                  </div>
                </div>
              )}
            </div>

            <div className="mt-6 flex justify-end space-x-3">
              <button
                type="button"
                onClick={handleClose}
                className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                disabled={loading}
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="px-4 py-2 bg-purple-500 text-white rounded-lg hover:bg-purple-600 flex items-center justify-center min-w-[100px]"
                disabled={loading}
              >
                {loading ? (
                  <span className="animate-pulse">Finalizando...</span>
                ) : (
                  'Finalizar'
                )}
              </button>
            </div>
          </form>
        ) : null}
      </div>
    </div>
  );
}