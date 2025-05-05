import React, { useState, useEffect } from 'react';
import { X, Loader2 } from 'lucide-react';
import { Label } from '../../../components/ui/label';
import { Switch } from '../../../components/ui/switch';

interface FinalizarOperacaoModalProps {
  isOpen: boolean;
  operacao: any | null;
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

export function FinalizarOperacaoModal({ isOpen, operacao, onClose, onSuccess }: FinalizarOperacaoModalProps) {
  // Estado para os detalhes da operação
  const [operacaoAtiva, setOperacaoAtiva] = useState<OperacaoAtiva | null>(null);
  
  // Estados para os campos do formulário
  const [valorUnitarioSaida, setValorUnitarioSaida] = useState('');
  const [dataSaida, setDataSaida] = useState('');
  const [saidaTotal, setSaidaTotal] = useState(true);
  const [quantidadeParcial, setQuantidadeParcial] = useState('');
  
  // Estados para controle de carregamento e erros
  const [salvando, setSalvando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Preencher campos ao abrir o modal
  useEffect(() => {
    if (isOpen && operacao) {
      setValorUnitarioSaida('');
      setSaidaTotal(true);
      setQuantidadeParcial('');
      // Data de saída padrão: hoje
      const hoje = new Date();
      setDataSaida(hoje.toISOString().split('T')[0]);
    }
  }, [isOpen, operacao]);

  // Validar quantidade parcial
  const validarQuantidadeParcial = (quantidade: string) => {
    const qtd = parseInt(quantidade);
    if (isNaN(qtd) || qtd <= 0) {
      return 'A quantidade deve ser um número positivo';
    }
    if (qtd >= operacao.quantity) {
      return 'A quantidade parcial deve ser menor que a quantidade total da operação';
    }
    return null;
  };

  // Função para salvar a finalização da operação
  const salvarFinalizacao = async () => {
    if (!operacao) return;
    if (!valorUnitarioSaida || !dataSaida) {
      setError('Por favor, preencha todos os campos.');
      return;
    }

    if (!saidaTotal) {
      const validacaoQtd = validarQuantidadeParcial(quantidadeParcial);
      if (validacaoQtd) {
        setError(validacaoQtd);
        return;
      }
    }

    setSalvando(true);
    setError(null);
    try {
      // Simulação de chamada de API para facilitar teste
      await new Promise(resolve => setTimeout(resolve, 1000));
      onSuccess();
    } catch (error) {
      setError('Não foi possível finalizar a operação. Tente novamente.');
    } finally {
      setSalvando(false);
    }
  };

  // Cálculo dos valores de saída
  const calcularValores = () => {
    if (!operacao || !valorUnitarioSaida) return null;
    const valorUnitarioSaidaNum = parseFloat(valorUnitarioSaida);
    const quantidadeFinal = !saidaTotal && quantidadeParcial ? parseInt(quantidadeParcial) : operacao.quantity;
    const valorTotalSaida = quantidadeFinal * valorUnitarioSaidaNum;
    const valorLucroPrejuizo = valorTotalSaida - (quantidadeFinal * operacao.entryUnitPrice);
    const percentualLucroPrejuizo = (valorLucroPrejuizo / (quantidadeFinal * operacao.entryUnitPrice)) * 100;
    return {
      valorTotalSaida,
      valorLucroPrejuizo,
      percentualLucroPrejuizo
    };
  };

  const formatarMoeda = (valor: number) => {
    return valor.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    });
  };

  if (!isOpen || !operacao) return null;
  const valores = calcularValores();

  return (
    <div 
      role="dialog" 
      aria-modal="true" 
      aria-labelledby="finalizar-modal-title"
      className="fixed inset-0 flex items-center justify-center z-50 p-4"
    >
      {/* Overlay do modal */}
      <div className="fixed inset-0 bg-gray-900 bg-opacity-75" onClick={onClose} />
      
      {/* Conteúdo do modal */}
      <div className="relative bg-white rounded-xl shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Cabeçalho do modal */}
        <div className="flex justify-between items-center p-4 border-b border-gray-200">
          <h2 id="finalizar-modal-title" className="text-xl font-semibold text-gray-800">Finalizar Operação</h2>
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
          
          <div className="space-y-6">
            {/* Detalhes da operação */}
            <div className="space-y-4">
              <div className="flex items-center gap-3">
                <img src={operacao.baseAssetLogoUrl} alt={operacao.optionSeriesCode} className="h-10 w-10 rounded-full object-contain" />
                <div>
                  <h3 className="font-bold text-lg">{operacao.optionSeriesCode}</h3>
                  <p className="text-sm text-gray-500">{operacao.brokerageName}</p>
                </div>
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-gray-500">Quantidade</p>
                  <p className="font-medium">{operacao.quantity}</p>
                </div>
                <div>
                  <p className="text-gray-500">Valor de Entrada</p>
                  <p className="font-medium">{formatarMoeda(operacao.entryTotalValue)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Preço Unitário de Entrada</p>
                  <p className="font-medium">{formatarMoeda(operacao.entryUnitPrice)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Tipo de Opção</p>
                  <p className="font-medium">{operacao.optionType}</p>
                </div>
                <div>
                  <p className="text-gray-500">Data de Entrada</p>
                  <p className="font-medium">{new Date(operacao.entryDate).toLocaleDateString('pt-BR')}</p>
                </div>
              </div>
            </div>
            
            <hr className="border-gray-200" />
            
            {/* Formulário de finalização */}
            <div className="space-y-4">
              <div>
                <Label htmlFor="dataSaida" className="text-sm font-medium text-gray-700">
                  Data de Saída
                </Label>
                <input
                  type="date"
                  id="dataSaida"
                  value={dataSaida}
                  onChange={(e) => setDataSaida(e.target.value)}
                  className="mt-1 w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                  disabled={salvando}
                />
              </div>

              <div className="flex items-center justify-between space-x-4 mb-4">
                <Label htmlFor="saidaParcial" className="text-sm font-medium text-gray-700">
                  Deseja fazer uma saída parcial?
                </Label>
                <div className="switch-container" style={{ minWidth: '44px', height: '24px' }}>
                  <Switch
                    id="saidaParcial"
                    checked={!saidaTotal}
                    onCheckedChange={(checked: boolean) => {
                      setSaidaTotal(!checked);
                      if (!checked) {
                        setQuantidadeParcial('');
                      }
                    }}
                    disabled={salvando}
                    className="finalizar-switch"
                  />
                </div>
              </div>

              {!saidaTotal && (
                <div>
                  <Label htmlFor="quantidadeParcial" className="text-sm font-medium text-gray-700">
                    Quantidade Parcial
                  </Label>
                  <input
                    type="number"
                    id="quantidadeParcial"
                    value={quantidadeParcial}
                    onChange={(e) => setQuantidadeParcial(e.target.value)}
                    className="mt-1 w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                    placeholder="0"
                    min="1"
                    max={operacao.quantity - 1}
                    disabled={salvando}
                  />
                </div>
              )}
                
              <div>
                <Label htmlFor="valorUnitarioSaida" className="text-sm font-medium text-gray-700">
                  Valor Unitário de Saída (R$)
                </Label>
                <input
                  type="number"
                  id="valorUnitarioSaida"
                  value={valorUnitarioSaida}
                  onChange={(e) => setValorUnitarioSaida(e.target.value)}
                  className="mt-1 w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                  placeholder="0.00"
                  step="0.01"
                  min="0.01"
                  disabled={salvando}
                />
              </div>
              
              {/* Seção de resultado da operação - só aparece se valorUnitarioSaida estiver preenchido */}
              {valores && (
                <div className="p-4 bg-purple-50 rounded-lg space-y-2">
                  <div className="flex justify-between">
                    <span className="text-gray-500">Valor Total de Saída:</span>
                    <span className="font-medium">{formatarMoeda(valores.valorTotalSaida)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Lucro / Prejuízo:</span>
                    <span className={valores.valorLucroPrejuizo >= 0 ? 'text-green-600 font-medium' : 'text-red-600 font-medium'}>
                      {formatarMoeda(valores.valorLucroPrejuizo)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Percentual:</span>
                    <span className={valores.percentualLucroPrejuizo >= 0 ? 'text-green-600 font-medium' : 'text-red-600 font-medium'}>
                      {valores.percentualLucroPrejuizo.toFixed(2)}%
                    </span>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
        
        {/* Rodapé com botões */}
        <div className="flex justify-end gap-3 p-4 border-t border-gray-200">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 mr-3"
            disabled={salvando}
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={salvarFinalizacao}
            className={`px-4 py-2 bg-purple-600 text-white rounded-lg flex items-center justify-center min-w-[100px] ${salvando || !operacao ? 'opacity-70 cursor-not-allowed' : 'hover:bg-purple-700'}`}
            disabled={salvando || !operacao || !valorUnitarioSaida || !dataSaida}
          >
            {salvando ? (
              <>
                <Loader2 className="animate-spin mr-2 h-4 w-4" />
                <span>Processando...</span>
              </>
            ) : (
              'Finalizar Operação'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}