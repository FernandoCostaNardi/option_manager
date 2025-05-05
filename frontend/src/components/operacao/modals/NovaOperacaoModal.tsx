import React, { useState, useEffect } from 'react';
import { X, Loader2, Plus, Edit, Trash } from 'lucide-react';
import api from '../../../services/api';
import { OperacaoAtiva } from '../../../types/operacao/operacoes.types';
import { Switch } from '../../../components/ui/switch';
import { Label } from '../../../components/ui/label';

interface NovaOperacaoModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  operacaoExistente?: OperacaoAtiva;
}

interface OptionData {
  optionCode: string;
  optionExpirationDate: number[];
  optionStrikePrice: number;
  optionType: string;
  baseAsset: string;
  baseAssetName: string;
  baseAssetUrlLogo: string;
  baseAssetType: string;
}

export function NovaOperacaoModal({ isOpen, onClose, onSuccess, operacaoExistente }: NovaOperacaoModalProps) {
  // Estado para controlar o passo atual
  const [passoAtual, setPassoAtual] = useState(1);
  
  // Estado para o código da opção
  const [codigoOpcao, setCodigoOpcao] = useState('');
  
  // Estado para os dados da opção
  const [dadosOpcao, setDadosOpcao] = useState<OptionData | null>(null);
  
  // Estados para os campos do passo 2
  const [quantidade, setQuantidade] = useState('');
  const [valorUnitario, setValorUnitario] = useState('');
  
  // Estado para o passo 3
  const [corretoras, setCorretoras] = useState<{id: string, name: string}[]>([]);
  const [corretoraSelecionada, setCorretoraSelecionada] = useState('');
  const [carregandoCorretoras, setCarregandoCorretoras] = useState(false);
  const [casasAnalise, setCasasAnalise] = useState<{id: string, name: string}[]>([]);
  const [casaAnaliseSelecionada, setCasaAnaliseSelecionada] = useState('');
  const [carregandoCasasAnalise, setCarregandoCasasAnalise] = useState(false);
  const [temCasaAnalise, setTemCasaAnalise] = useState(false);
  
  // Estados para targets e stop loss
  const [targets, setTargets] = useState<{type: 'TARGET' | 'STOP_LOSS', sequence: number, value: string, editing?: boolean}[]>([]);
  const [targetType, setTargetType] = useState<'TARGET' | 'STOP_LOSS'>('TARGET');
  const [targetValue, setTargetValue] = useState('');
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  
  // Estado para controle de carregamento e erros
  const [loading, setLoading] = useState(false);
  const [loadingTargets, setLoadingTargets] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Adicionar novo estado para o tipo de operação (Compra ou Venda)
  const [tipoOperacao, setTipoOperacao] = useState<'BUY' | 'SELL' | ''>('');
  // Novo estado para a data de entrada
  const [dataEntrada, setDataEntrada] = useState(() => {
    const hoje = new Date();
    return hoje.toISOString().split('T')[0];
  });

  // Função para buscar targets da operação
  const buscarTargets = async (operationId: string) => {
    setLoadingTargets(true);
    try {
      const response = await api.get(`/operations/${operationId}/targets`);
      const targetsFormatados = response.data.map((target: any) => ({
        type: target.type,
        sequence: target.sequence,
        value: target.value.toString(),
        editing: false
      }));
      setTargets(targetsFormatados);
    } catch (error) {
      console.error('Erro ao buscar targets:', error);
      setError('Não foi possível carregar os targets da operação.');
    } finally {
      setLoadingTargets(false);
    }
  };

  // Efeito para preencher os campos quando uma operação existente é fornecida
  useEffect(() => {
    if (operacaoExistente) {
      setCodigoOpcao(operacaoExistente.optionSeriesCode);
      setTipoOperacao(operacaoExistente.transactionType as 'BUY' | 'SELL');
      
      // Formatar a data de entrada para o formato esperado pelo input type="date"
      const dataEntradaFormatada = new Date(operacaoExistente.entryDate).toISOString().split('T')[0];
      setDataEntrada(dataEntradaFormatada);
      
      setQuantidade(operacaoExistente.quantity.toString());
      setValorUnitario(operacaoExistente.entryUnitPrice.toString());
      setTemCasaAnalise(!!operacaoExistente.analysisHouseName);
      setCorretoraSelecionada(operacaoExistente.brokerageId);
      setCasaAnaliseSelecionada(operacaoExistente.analysisHouseId || '');
      
      // Buscar dados da opção
      buscarDadosOpcao(operacaoExistente.optionSeriesCode);
      
      // Carregar corretoras e casas de análise
      carregarCorretoras();
      if (operacaoExistente.analysisHouseName) {
        carregarCasasAnalise();
      }
    }
  }, [operacaoExistente]);

  // Efeito para carregar targets quando chegar no passo 4
  useEffect(() => {
    if (passoAtual === 4 && operacaoExistente?.id) {
      buscarTargets(operacaoExistente.id);
    }
  }, [passoAtual, operacaoExistente]);

  // Função para buscar dados da opção com código pré-definido
  const buscarDadosOpcao = async (codigo?: string) => {
    const codigoParaBuscar = codigo || codigoOpcao;
    if (!codigoParaBuscar.trim()) {
      setError('Por favor, informe o código da opção');
      return;
    }
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await api.get(`/option-series/${codigoParaBuscar.trim().toLowerCase()}`);
      console.log('Dados da opção retornados:', response.data);
      setDadosOpcao(response.data);    
      setPassoAtual(2);
    } catch (error) {
      console.error('Erro ao buscar dados da opção:', error);
      setError('Não foi possível encontrar os dados da opção. Verifique o código e tente novamente.');
    } finally {
      setLoading(false);
    }
  };

  // Função para adicionar ou editar target/stop loss
  const adicionarOuEditarTarget = () => {
    if (!targetValue) {
      setError('Informe o valor do Target ou Stop Loss');
      return;
    }
    if (targetType === 'STOP_LOSS' && targets.some(t => t.type === 'STOP_LOSS' && editingIndex === null)) {
      setError('Só é permitido um Stop Loss por operação');
      return;
    }
    if (editingIndex !== null) {
      // Editando
      const novosTargets = [...targets];
      novosTargets[editingIndex] = {
        ...novosTargets[editingIndex],
        type: targetType,
        value: targetValue
      };
      setTargets(novosTargets);
      setEditingIndex(null);
    } else {
      // Adicionando
      const sequence = targetType === 'TARGET'
        ? 1 + Math.max(0, ...targets.filter(t => t.type === 'TARGET').map(t => t.sequence))
        : 1;
      setTargets([...targets, { type: targetType, sequence, value: targetValue }]);
    }
    setTargetValue('');
    setTargetType('TARGET');
    setError(null);
  };
  
  // Função para editar um target/stop loss
  const editarTarget = (idx: number) => {
    setEditingIndex(idx);
    setTargetType(targets[idx].type);
    setTargetValue(targets[idx].value);
  };
  
  // Função para cancelar edição
  const cancelarEdicaoTarget = () => {
    setEditingIndex(null);
    setTargetType('TARGET');
    setTargetValue('');
  };

  // Função para limpar o formulário
  const limparFormulario = () => {
    setPassoAtual(1);
    setCodigoOpcao('');
    setDadosOpcao(null);
    setTipoOperacao(''); 
    setQuantidade('');
    setValorUnitario('');
    setCorretoraSelecionada('');
    setCasaAnaliseSelecionada('');
    setTemCasaAnalise(false);
    setTargets([]);
    setTargetType('TARGET');
    setTargetValue('');
    setEditingIndex(null);
    setError(null);
  };

  // Função para fechar o modal
  const handleClose = () => {
    limparFormulario();
    onClose();
  };

  // Função para avançar para o próximo passo
  const avancarPasso = () => {
    console.log('avancarPasso chamado', { passoAtual, tipoOperacao, quantidade, valorUnitario, dadosOpcao });
    if (passoAtual === 1) {
      buscarDadosOpcao();
    } else if (passoAtual === 2) {
      // Validar se os campos estão preenchidos antes de avançar
      if (!tipoOperacao) {
        setError('Por favor, selecione o tipo de operação (Compra ou Venda)');
        return;
      }
      if (!dataEntrada) {
        setError('Por favor, selecione a data de entrada da operação');
        return;
      }
      if (!quantidade || quantidade === '0') {
        setError('Por favor, preencha a quantidade');
        return;
      }
      if (!valorUnitario || valorUnitario === '0') {
        setError('Por favor, preencha o valor unitário');
        return;
      }
      setError(null);
      setPassoAtual(3);
      console.log('Mudando para passo 3');
      // Carregar corretoras ao avançar para o passo 3
      if (corretoras.length === 0) {
        carregarCorretoras();
      }
    } else if (passoAtual === 3) {
      if (!corretoraSelecionada) {
        setError('Por favor, selecione uma corretora');
        return;
      }
      if (temCasaAnalise && !casaAnaliseSelecionada) {
        setError('Por favor, selecione uma casa de análise');
        return;
      }
      
      if (temCasaAnalise) {
        // Se tem casa de análise, vai para o passo 4 (targets e stop loss)
        setPassoAtual(4);
      } else {
        // Se não tem casa de análise, salva a operação diretamente
        salvarOperacao();
      }
    }
  };

  // Função para voltar ao passo anterior
  const voltarPasso = () => {
    if (passoAtual > 1) {
      setPassoAtual(passoAtual - 1);
    }
  };

  // Função para carregar corretoras
  const carregarCorretoras = async () => {
    setCarregandoCorretoras(true);
    
    try {
      const response = await api.get('/brokerages?page=0&size=100');
      const lista = response.data.content || response.data;
      setCorretoras(Array.isArray(lista) ? lista.map((c: any) => ({ id: c.id, name: c.name })) : []);
    } catch (error) {
      console.error('Erro ao carregar corretoras:', error);
      setError('Não foi possível carregar a lista de corretoras.');
    } finally {
      setCarregandoCorretoras(false);
    }
  };

  // Função para carregar casas de análise
  const carregarCasasAnalise = async () => {
    setCarregandoCasasAnalise(true);
    
    try {
      const response = await api.get('/analysis-houses?page=0&size=100');
      const lista = response.data.content || response.data;
      setCasasAnalise(Array.isArray(lista) ? lista.map((c: any) => ({ id: c.id, name: c.name })) : []);
    } catch (error) {
      console.error('Erro ao carregar casas de análise:', error);
      setError('Não foi possível carregar a lista de casas de análise.');
    } finally {
      setCarregandoCasasAnalise(false);
    }
  };

  // Monitorar mudanças em temCasaAnalise para carregar as casas de análise
  useEffect(() => {
    if (temCasaAnalise && casasAnalise.length === 0) {
      carregarCasasAnalise();
    }
  }, [temCasaAnalise]);

  // Função para salvar a operação
  const salvarOperacao = async () => {
    if (!dadosOpcao || !quantidade || !valorUnitario || !corretoraSelecionada || !tipoOperacao || !dataEntrada) {
      setError('Por favor, preencha todos os campos obrigatórios');
      return;
    }
  
    if (temCasaAnalise && !casaAnaliseSelecionada) {
      setError('Por favor, selecione uma casa de análise');
      return;
    }
  
    setLoading(true);
    setError(null);
  
    try {
      // Monta o DTO conforme OperationDataRequest
      const payload = {
        id: operacaoExistente?.id,
        baseAssetCode: dadosOpcao.baseAsset,
        baseAssetName: dadosOpcao.baseAssetName,
        baseAssetType: dadosOpcao.baseAssetType,
        baseAssetLogoUrl: dadosOpcao.baseAssetUrlLogo,
  
        optionSeriesCode: dadosOpcao.optionCode,
        optionSeriesType: dadosOpcao.optionType,
        optionSeriesStrikePrice: dadosOpcao.optionStrikePrice,
        optionSeriesExpirationDate: `${dadosOpcao.optionExpirationDate[0]}-${String(dadosOpcao.optionExpirationDate[1]).padStart(2, '0')}-${String(dadosOpcao.optionExpirationDate[2]).padStart(2, '0')}`,
  
        targets: targets.map(t => ({
          type: t.type,
          sequence: t.sequence,
          value: parseFloat(t.value)
        })),
  
        brokerageId: corretoraSelecionada,
        analysisHouseId: temCasaAnalise ? casaAnaliseSelecionada : undefined,
        transactionType: tipoOperacao,
        entryDate: dataEntrada,
        exitDate: operacaoExistente?.exitDate || null,
        quantity: parseInt(quantidade),
        entryUnitPrice: parseFloat(valorUnitario)
      };
  
      const token = localStorage.getItem('token');
      
      await api.post('/operations', payload, {
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        }
      });
  
      onSuccess();
      handleClose();
    } catch (error) {
      console.error('Erro ao salvar operação:', error);
      setError('Não foi possível salvar a operação. Tente novamente.');
    } finally {
      setLoading(false);
    }
  };

  // Função para formatar a data de expiração
  const formatarDataExpiracao = (data: number[]) => {
    if (!data || data.length !== 3) return '';
    return `${data[2].toString().padStart(2, '0')}/${data[1].toString().padStart(2, '0')}/${data[0]}`;
  };

  // Função para formatar valor monetário
  const formatarMoeda = (valor: number) => {
    return valor.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    });
  };

  // Se o modal não estiver aberto, não renderiza nada
  if (!isOpen) return null;

  return (
    <div 
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
      className="fixed inset-0 flex items-center justify-center z-50 p-4"
    >
      {/* Overlay do modal */}
      <div className="fixed inset-0 bg-gray-900 bg-opacity-75" onClick={handleClose} />
      
      {/* Conteúdo do modal */}
      <div className="bg-white rounded-xl shadow-xl w-full max-w-3xl overflow-hidden relative z-10">
        {/* Cabeçalho do modal */}
        <div className="flex justify-between items-center p-4 border-b border-gray-200">
          <h2 id="modal-title" className="text-xl font-semibold text-gray-800">Nova Operação</h2>
          <button
            onClick={handleClose}
            className="text-gray-400 hover:text-gray-600 focus:outline-none"
            aria-label="Fechar modal"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        <div className="flex">
          {/* Barra lateral com os passos */}
          <div className="w-56 bg-gray-50 p-4 border-r border-gray-200 min-h-96">
            <div className="space-y-6">
              <div 
                className={`flex items-center ${passoAtual === 1 ? 'text-purple-600 font-medium' : 'text-gray-500'}`}
              >
                <div 
                  className={`flex items-center justify-center w-8 h-8 rounded-full mr-3 
                    ${passoAtual === 1 ? 'bg-purple-100 text-purple-600' : 'bg-gray-200 text-gray-500'}`}
                >
                  1
                </div>
                <span>Código da Opção</span>
              </div>
              
              <div 
                className={`flex items-center ${passoAtual === 2 ? 'text-purple-600 font-medium' : 'text-gray-500'}`}
              >
                <div 
                  className={`flex items-center justify-center w-8 h-8 rounded-full mr-3 
                    ${passoAtual === 2 ? 'bg-purple-100 text-purple-600' : 'bg-gray-200 text-gray-500'}`}
                >
                  2
                </div>
                <span>Detalhes da Operação</span>
              </div>
              
              <div 
                className={`flex items-center ${passoAtual === 3 ? 'text-purple-600 font-medium' : 'text-gray-500'}`}
              >
                <div 
                  className={`flex items-center justify-center w-8 h-8 rounded-full mr-3 
                    ${passoAtual === 3 ? 'bg-purple-100 text-purple-600' : 'bg-gray-200 text-gray-500'}`}
                >
                  3
                </div>
                <span>Corretora e Casa de Análise</span>
              </div>
              
              {temCasaAnalise && (
                <div 
                  className={`flex items-center ${passoAtual === 4 ? 'text-purple-600 font-medium' : 'text-gray-500'}`}
                >
                  <div 
                    className={`flex items-center justify-center w-8 h-8 rounded-full mr-3 
                      ${passoAtual === 4 ? 'bg-purple-100 text-purple-600' : 'bg-gray-200 text-gray-500'}`}
                  >
                    4
                  </div>
                  <span>Targets e Stop Loss</span>
                </div>
              )}
            </div>
          </div>

          {/* Conteúdo do formulário */}
          <div className="flex-1 p-6">
            {error && (
              <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-sm flex items-center">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
                {error}
              </div>
            )}

            {/* Passo 1: Código da Opção */}
            {passoAtual === 1 && (
              <div className="space-y-4">
                <div>
                  <label htmlFor="codigoOpcao" className="block text-sm font-medium text-gray-700 mb-1">
                    Código da Opção
                  </label>
                  <input
                    type="text"
                    id="codigoOpcao"
                    value={codigoOpcao}
                    onChange={(e) => setCodigoOpcao(e.target.value.toUpperCase())}
                    className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                    placeholder="Ex: PETR4"
                    disabled={loading}
                    autoFocus
                  />
                  <p className="mt-2 text-sm text-gray-500">
                    Digite o código da opção que deseja adicionar.
                  </p>
                </div>
              </div>
            )}

            {/* Passo 2: Detalhes da Operação */}
            {passoAtual === 2 && dadosOpcao && (
              <div className="space-y-6">
                {/* Informações do Ativo Base */}
                <div className="p-4 bg-gray-50 rounded-lg">
                  <h3 className="text-sm font-semibold text-gray-700 mb-3">Ativo Base</h3>
                  <div className="flex items-center">
                    <img 
                      src={dadosOpcao.baseAssetUrlLogo} 
                      alt={dadosOpcao.baseAsset} 
                      className="w-10 h-10 mr-3 object-contain" 
                    />
                    <div>
                      <p className="font-medium">{dadosOpcao.baseAsset}</p>
                      <p className="text-sm text-gray-500">{dadosOpcao.baseAssetName}</p>
                    </div>
                  </div>
                </div>

                {/* Informações da Opção */}
                <div className="p-4 bg-gray-50 rounded-lg">
                  <h3 className="text-sm font-semibold text-gray-700 mb-3">Detalhes da Opção</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm text-gray-500">Código</p>
                      <p className="font-medium">{dadosOpcao.optionCode.toUpperCase()}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500">Tipo</p>
                      <p className="font-medium">{dadosOpcao.optionType}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500">Strike</p>
                      <p className="font-medium">{formatarMoeda(dadosOpcao.optionStrikePrice)}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-500">Data de Expiração</p>
                      <p className="font-medium">{formatarDataExpiracao(dadosOpcao.optionExpirationDate)}</p>
                    </div>
                  </div>
                </div>

                {/* Campos de entrada */}
                <div className="space-y-4">
                {/* Tipo de Operação e Data de Entrada (na mesma linha) */}
<div className="flex gap-4">
  <div className="flex-1">
    <label htmlFor="tipoOperacao" className="block text-sm font-medium text-gray-700 mb-1">
      Tipo de Operação *
    </label>
    <div className="relative">
      <select
        id="tipoOperacao"
        value={tipoOperacao}
        onChange={(e) => setTipoOperacao(e.target.value as 'BUY' | 'SELL' | '')}
        className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent appearance-none bg-white"
        disabled={loading}
        required
      >
        <option value="">Selecione o tipo</option>
        <option value="BUY">Compra</option>
        <option value="SELL">Venda</option>
      </select>
      <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none">
        <svg className="h-5 w-5 text-gray-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
          <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
        </svg>
      </div>
    </div>
  </div>
  
  <div className="flex-1">
    <label htmlFor="dataEntrada" className="block text-sm font-medium text-gray-700 mb-1">
      Data de Entrada *
    </label>
    <input
      type="date"
      id="dataEntrada"
      value={dataEntrada}
      onChange={(e) => setDataEntrada(e.target.value)}
      className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
      disabled={loading}
      required
    />
  </div>
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
                      Preço Unitário (R$)
                    </label>
                    <input
                      type="number"
                      id="valorUnitario"
                      value={valorUnitario}
                      onChange={(e) => setValorUnitario(e.target.value)}
                      className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                      placeholder="Ex: 0.50"
                      step="0.01"
                      min="0.01"
                      disabled={loading}
                    />
                  </div>

                  {quantidade && valorUnitario && parseFloat(quantidade) > 0 && parseFloat(valorUnitario) > 0 && (
                    <div className="p-3 bg-purple-50 rounded-lg">
                      <p className="text-sm text-purple-800">
                        Valor Total: <strong>{formatarMoeda(parseFloat(quantidade) * parseFloat(valorUnitario))}</strong>
                      </p>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Passo 3: Seleção de Corretora e Casa de Análise */}
            {passoAtual === 3 && (
              <div className="space-y-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Corretora</label>
                  {carregandoCorretoras ? (
                    <div className="flex items-center p-2 border border-gray-300 rounded-lg bg-gray-50">
                      <Loader2 className="animate-spin w-5 h-5 mr-2 text-purple-600" />
                      <span className="text-gray-500">Carregando corretoras...</span>
                    </div>
                  ) : (
                    <div className="relative">
                      <select
                        value={corretoraSelecionada}
                        onChange={(e) => setCorretoraSelecionada(e.target.value)}
                        className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent appearance-none bg-white"
                        disabled={carregandoCorretoras}
                      >
                        <option value="">Selecione a corretora</option>
                        {corretoras.map(c => (
                          <option key={c.id} value={c.id}>
                            {c.name}
                          </option>
                        ))}
                      </select>
                      <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none">
                        <svg className="h-5 w-5 text-gray-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                          <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
                        </svg>
                      </div>
                    </div>
                  )}
                </div>
                
                <div className="flex items-center space-x-4">
                  <div className="switch-container">
                    <Switch
                      id="temCasaAnalise"
                      checked={temCasaAnalise}
                      onCheckedChange={setTemCasaAnalise}
                      disabled={loading}
                      className="nova-operacao-switch"
                    />
                  </div>
                  <Label htmlFor="temCasaAnalise" className="cursor-pointer text-sm font-medium text-gray-700">
                    Existe casa de análise recomendando a operação?
                  </Label>
                </div>
                
                {temCasaAnalise && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Casa de Análise</label>
                    {carregandoCasasAnalise ? (
                      <div className="flex items-center p-2 border border-gray-300 rounded-lg bg-gray-50">
                        <Loader2 className="animate-spin w-5 h-5 mr-2 text-purple-600" />
                        <span className="text-gray-500">Carregando casas de análise...</span>
                      </div>
                    ) : (
                      <div className="relative">
                        <select
                          value={casaAnaliseSelecionada}
                          onChange={(e) => setCasaAnaliseSelecionada(e.target.value)}
                          className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent appearance-none bg-white"
                          disabled={carregandoCasasAnalise}
                        >
                          <option value="">Selecione a casa de análise</option>
                          {casasAnalise.map(c => (
                            <option key={c.id} value={c.id}>
                              {c.name}
                            </option>
                          ))}
                        </select>
                        <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none">
                          <svg className="h-5 w-5 text-gray-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                            <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
                          </svg>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Passo 4: Targets e Stop Loss */}
            {passoAtual === 4 && (
              <div className="space-y-6">
                <h3 className="text-lg font-semibold mb-4">Targets e Stop Loss</h3>
                
                {loadingTargets ? (
                  <div className="flex items-center justify-center py-8">
                    <div className="flex items-center space-x-2">
                      <Loader2 className="w-6 h-6 text-purple-600 animate-spin" />
                      <span className="text-gray-600">Carregando targets...</span>
                    </div>
                  </div>
                ) : (
                  <>
                    {/* Formulário para adicionar targets/stop loss */}
                    <div className="flex flex-col sm:flex-row gap-4 mb-4">
                      <div className="sm:w-1/3">
                        <select
                          className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent appearance-none bg-white"
                          value={targetType}
                          onChange={e => setTargetType(e.target.value as 'TARGET' | 'STOP_LOSS')}
                          disabled={editingIndex !== null && targets[editingIndex || 0]?.type === 'STOP_LOSS'}
                        >
                          <option value="TARGET">Target</option>
                          <option 
                            value="STOP_LOSS" 
                            disabled={targets.some(t => t.type === 'STOP_LOSS') && editingIndex === null}
                          >
                            Stop Loss
                          </option>
                        </select>
                      </div>
                      
                      <div className="sm:w-1/3">
                        <input
                          type="number"
                          className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                          placeholder="Valor (R$)"
                          value={targetValue}
                          onChange={e => setTargetValue(e.target.value)}
                          step="0.01"
                          min="0.01"
                        />
                      </div>
                      
                      <div className="flex gap-2 sm:w-1/3">
                        <button
                          className="flex-1 p-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors flex items-center justify-center"
                          onClick={adicionarOuEditarTarget}
                          disabled={!targetValue}
                        >
                          {editingIndex !== null ? (
                            <>
                              <Edit className="w-4 h-4 mr-1" /> Atualizar
                            </>
                          ) : (
                            <>
                              <Plus className="w-4 h-4 mr-1" /> Adicionar
                            </>
                          )}
                        </button>
                        
                        {editingIndex !== null && (
                          <button 
                            className="p-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition-colors"
                            onClick={cancelarEdicaoTarget}
                          >
                            Cancelar
                          </button>
                        )}
                      </div>
                    </div>
                    
                    {/* Lista de targets/stop loss */}
                    {targets.length > 0 ? (
                      <div className="overflow-x-auto">
                        <table className="min-w-full bg-white rounded-lg shadow">
                          <thead>
                            <tr className="bg-gray-100 text-gray-700">
                              <th className="px-4 py-2 text-left">Tipo</th>
                              <th className="px-4 py-2 text-right">Valor</th>
                              <th className="px-4 py-2 text-center">Ações</th>
                            </tr>
                          </thead>
                          <tbody>
                            {targets.map((target, idx) => (
                              <tr key={idx} className="border-b hover:bg-gray-50">
                                <td className="px-4 py-2">
                                  {target.type === 'TARGET' ? (
                                    <span className="text-green-600 font-medium">Target {target.sequence}</span>
                                  ) : (
                                    <span className="text-red-600 font-medium">Stop Loss</span>
                                  )}
                                </td>
                                <td className="px-4 py-2 text-right">
                                  {parseFloat(target.value).toLocaleString('pt-BR', { 
                                    style: 'currency', 
                                    currency: 'BRL' 
                                  })}
                                </td>
                                <td className="px-4 py-2 flex gap-2 justify-center">
                                  <button 
                                    title="Editar" 
                                    className="p-1 rounded hover:bg-gray-100"
                                    onClick={() => editarTarget(idx)}
                                    disabled={editingIndex !== null}
                                  >
                                    <Edit className="w-4 h-4 text-blue-600" />
                                  </button>
                                  <button 
                                    title="Remover" 
                                    className="p-1 rounded hover:bg-gray-100"
                                    onClick={() => {
                                      const novosTargets = [...targets];
                                      novosTargets.splice(idx, 1);
                                      setTargets(novosTargets);
                                    }}
                                    disabled={editingIndex !== null}
                                  >
                                    <Trash className="w-4 h-4 text-red-600" />
                                  </button>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    ) : (
                      <div className="text-center py-6 bg-gray-50 rounded-lg text-gray-500">
                        Nenhum target ou stop loss adicionado ainda.
                      </div>
                    )}
                  </>
                )}
              </div>
            )}

            {/* Botões de navegação - Visíveis em todos os passos */}
            <div className="mt-6 flex justify-end space-x-3">
              {/* Botão Voltar - Visível em todos os passos exceto o primeiro */}
              {passoAtual > 1 && (
                <button
                  type="button"
                  onClick={voltarPasso}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
                  disabled={loading}
                >
                  Voltar
                </button>
              )}

              {/* Botão Próximo/Salvar - Lógica condicional baseada no passo atual */}
              {passoAtual === 1 && (
                <button
                  type="button"
                  onClick={avancarPasso}
                  className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
                  disabled={loading || !codigoOpcao.trim()}
                >
                  Próximo
                </button>
              )}

              {passoAtual === 2 && (
                <button
                  type="button"
                  onClick={avancarPasso}
                  className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
                  disabled={
                    loading ||
                    !tipoOperacao ||
                    !quantidade ||
                    !valorUnitario ||
                    isNaN(Number(quantidade)) ||
                    isNaN(Number(valorUnitario)) ||
                    Number(quantidade) <= 0 ||
                    Number(valorUnitario) <= 0
                  }
                >
                  Próximo
                </button>
              )}

              {passoAtual === 3 && (
                <button
                  type="button"
                  onClick={avancarPasso}
                  className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
                  disabled={loading || !corretoraSelecionada || (temCasaAnalise && !casaAnaliseSelecionada)}
                >
                  {temCasaAnalise ? 'Próximo' : 'Salvar'}
                </button>
              )}

              {passoAtual === 4 && (
                <button
                  type="button"
                  onClick={salvarOperacao}
                  className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
                  disabled={loading}
                >
                  Salvar
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}