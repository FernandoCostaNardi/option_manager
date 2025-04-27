import React, { useState } from 'react';
import { X } from 'lucide-react';

interface NovaOperacaoModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function NovaOperacaoModal({ isOpen, onClose, onSuccess }: NovaOperacaoModalProps) {
  // Estados para os campos do formulário
  const [casaAnalise, setCasaAnalise] = useState('');
  const [corretora, setCorretora] = useState('');
  const [opcao, setOpcao] = useState('');
  const [logoEmpresa, setLogoEmpresa] = useState('');
  const [quantidade, setQuantidade] = useState('');
  const [valorUnitario, setValorUnitario] = useState('');
  
  // Estado para controle de carregamento
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Função para limpar o formulário
  const limparFormulario = () => {
    setCasaAnalise('');
    setCorretora('');
    setOpcao('');
    setLogoEmpresa('');
    setQuantidade('');
    setValorUnitario('');
    setError(null);
  };

  // Função para fechar o modal
  const handleClose = () => {
    limparFormulario();
    onClose();
  };

  // Função para salvar a operação
  const handleSalvar = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    // Validação básica
    if (!casaAnalise || !corretora || !opcao || !quantidade || !valorUnitario) {
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
      console.error('Erro ao salvar operação:', error);
      setError('Não foi possível salvar a operação. Tente novamente.');
    } finally {
      setLoading(false);
    }
  };

  // Se o modal não estiver aberto, não renderiza nada
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md">
        <div className="flex justify-between items-center p-6 border-b border-gray-100">
          <h2 className="text-xl font-semibold text-gray-800">Nova Operação</h2>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        <form onSubmit={handleSalvar} className="p-6">
          {error && (
            <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-sm">
              {error}
            </div>
          )}

          <div className="space-y-4">
            <div>
              <label htmlFor="casaAnalise" className="block text-sm font-medium text-gray-700 mb-1">
                Casa de Análise
              </label>
              <input
                type="text"
                id="casaAnalise"
                value={casaAnalise}
                onChange={(e) => setCasaAnalise(e.target.value)}
                className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                placeholder="Ex: Empiricus"
                disabled={loading}
              />
            </div>

            <div>
              <label htmlFor="corretora" className="block text-sm font-medium text-gray-700 mb-1">
                Corretora
              </label>
              <input
                type="text"
                id="corretora"
                value={corretora}
                onChange={(e) => setCorretora(e.target.value)}
                className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                placeholder="Ex: XP Investimentos"
                disabled={loading}
              />
            </div>

            <div>
              <label htmlFor="opcao" className="block text-sm font-medium text-gray-700 mb-1">
                Opção
              </label>
              <input
                type="text"
                id="opcao"
                value={opcao}
                onChange={(e) => setOpcao(e.target.value)}
                className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                placeholder="Ex: PETR4"
                disabled={loading}
              />
            </div>

            <div>
              <label htmlFor="logoEmpresa" className="block text-sm font-medium text-gray-700 mb-1">
                URL do Logo da Empresa
              </label>
              <input
                type="text"
                id="logoEmpresa"
                value={logoEmpresa}
                onChange={(e) => setLogoEmpresa(e.target.value)}
                className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                placeholder="Ex: https://exemplo.com/logo.png"
                disabled={loading}
              />
            </div>

            <div>
              <label htmlFor="quantidade" className="block text-sm font-medium text-gray-700 mb-1">
                Quantidade
              </label>
              <input
                type="number"
                id="quantidade"
                value={quantidade}
                onChange={(e) => setQuantidade(e.target.value)}
                className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                placeholder="Ex: 100"
                min="1"
                disabled={loading}
              />
            </div>

            <div>
              <label htmlFor="valorUnitario" className="block text-sm font-medium text-gray-700 mb-1">
                Valor Unitário (R$)
              </label>
              <input
                type="number"
                id="valorUnitario"
                value={valorUnitario}
                onChange={(e) => setValorUnitario(e.target.value)}
                className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                placeholder="Ex: 32.50"
                step="0.01"
                min="0.01"
                disabled={loading}
              />
            </div>
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
                <span className="animate-pulse">Salvando...</span>
              ) : (
                'Salvar'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}