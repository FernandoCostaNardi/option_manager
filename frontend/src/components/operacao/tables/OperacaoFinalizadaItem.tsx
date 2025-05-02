import React from 'react';
import { Eye, Trash2, Target } from 'lucide-react';
import { OperacaoFinalizada } from '../../../types/operacao/operacoes.types';
import { formatarData, formatarMoeda } from '../../../utils/formatadores';

interface OperacaoFinalizadaItemProps {
  operacao: OperacaoFinalizada;
  onView: (id: string) => void;
  onRemove: (id: string) => void;
  onViewTargets: (id: string) => void;
  index: number; // Adicionar índice para alternar cores das linhas
}

export function OperacaoFinalizadaItem({ operacao, onView, onRemove, onViewTargets, index }: OperacaoFinalizadaItemProps) {
  // Função para formatar o resultado (lucro/prejuízo)
  const formatarResultado = () => {
    // Verificar se o resultado existe antes de chamar toFixed
    if (operacao.profitLoss === null || operacao.profitLoss === undefined) {
      return 'R$ 0,00';
    }
    
    // Converter para número se for string
    const valorNumerico = typeof operacao.profitLoss === 'string' 
      ? parseFloat(operacao.profitLoss) 
      : operacao.profitLoss;
    
    // Verificar se é um número válido
    if (isNaN(valorNumerico)) {
      return 'R$ 0,00';
    }
    
    const valor = parseFloat(valorNumerico.toFixed(2));
    const formatado = formatarMoeda(Math.abs(valor));
    
    if (valor > 0) {
      return <span className="text-green-600 font-medium">+{formatado}</span>;
    } else if (valor < 0) {
      return <span className="text-red-600 font-medium">-{formatado}</span>;
    } else {
      return <span className="text-gray-600">{formatado}</span>;
    }
  };

  const formatarPercentualResultado = () => {
      // Verificar se o % resultado existe antes de chamar toFixed
      if (operacao.profitLossPercentage === null || operacao.profitLossPercentage === undefined) {
        return '0%';
      }
      
      // Converter para número se for string
      const valorNumerico = typeof operacao.profitLossPercentage === 'string' 
        ? parseFloat(operacao.profitLossPercentage) 
        : operacao.profitLossPercentage;
      
      // Verificar se é um número válido
      if (isNaN(valorNumerico)) {
        return '0%';
      }
      
      const valor = parseFloat(valorNumerico.toFixed(2));
      
      if (valor > 0) {
        return <span className="text-green-600 font-medium">+{valor}</span>;
      } else if (valor < 0) {
        return <span className="text-red-600 font-medium">{valor}</span>;
      } else {
        return <span className="text-gray-600">{valor}</span>;
      }
    };
  

  // Traduzir status
  const traduzirStatus = (status: string) => {
    switch (status) {
      case 'WINNER':
        return 'Ganhadora';
      case 'LOSER':
        return 'Perdedora';
      default:
        return status || '-';
    }
  };

  // Traduzir trade type
  const traduzirTradeType = (status: string) => {
    switch (status) {
      case 'DAY':
        return 'Daytrade';
      case 'SWING':
        return 'Swingtrade';
      default:
        return status || '-';
    }
  };




  return (
    <tr className={`border-b border-gray-100 hover:bg-gray-50 transition-colors ${index % 2 === 0 ? 'bg-white' : 'bg-gray-50'}`}>
      <td className="px-4 py-3 text-sm font-medium text-gray-800">
        <div className="flex items-center gap-2">
          {operacao.baseAssetLogoUrl && (
            <img 
              src={operacao.baseAssetLogoUrl} 
              alt={operacao.optionSeriesCode || '-'}
              className="h-8 w-8 rounded-full object-contain"
            />
          )}
          {operacao.optionSeriesCode || '-'}
        </div>
      </td>
      <td className="px-4 py-3 text-sm text-gray-700 text-center">
        <span className={`px-2 py-1 rounded-full text-xs font-medium ${
          operacao.optionType === 'CALL' ? 'bg-gray-200 text-green-800' : 
          operacao.optionType === 'PUT' ? 'bg-gray-200 text-red-800' : 
          'bg-gray-100 text-gray-800'
        }`}>
          {operacao.optionType || '-'}
        </span>
      </td>
      <td className="px-2 py-3 text-sm text-gray-700 text-center">{operacao.entryDate ? formatarData(operacao.entryDate) : '-'}</td>
      <td className="px-2 py-3 text-sm text-gray-700 text-center">{operacao.exitDate ? formatarData(operacao.exitDate) : '-'}</td>
      <td className="px-2 py-3 text-sm text-gray-700 text-center">
        <span className={`px-3 py-1 rounded-full text-xs font-medium ${
          operacao.tradeType === 'SWING' ? 'bg-gray-200 text-purple-600' : 
          operacao.tradeType === 'DAY' ? 'bg-gray-200 text-blue-600' : 
          'bg-gray-100 text-gray-800'
        }`}>
          {traduzirTradeType(operacao.tradeType) || '-'}
        </span>
      </td>
      <td className="px-1 py-3 text-sm text-gray-700 text-right">{operacao.entryUnitPrice !== null && operacao.entryUnitPrice !== undefined ? formatarMoeda(operacao.entryUnitPrice) : '-'}</td>
      <td className="px-1 py-3 text-sm text-gray-700 text-right">{operacao.exitUnitPrice !== null && operacao.exitUnitPrice !== undefined ? formatarMoeda(operacao.exitUnitPrice) : '-'}</td>
      <td className="px-4 py-3 text-sm font-medium text-right">{formatarResultado()}</td>
      <td className="px-1 py-3 text-sm font-medium text-right">{formatarPercentualResultado()}</td>
      <td className="px-2 py-3 text-sm text-center">
        <span className={`px-2 py-1 rounded-full text-xs font-medium ${
          operacao.status === 'WINNER' ? 'bg-green-100 text-green-800' : 
          operacao.status === 'LOSER' ? 'bg-red-100 text-red-800' : 
          'bg-gray-100 text-gray-800'
        }`}>
          {traduzirStatus(operacao.status)}
        </span>
      </td>
      <td className="px-6 py-3 text-sm text-gray-700">{operacao.analysisHouseName || '-'}</td>
      <td className="px-6 py-3 text-sm text-gray-700">{operacao.brokerageName || '-'}</td>
      <td className="px-4 py-3 text-center">
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={() => onView(operacao.id)}
            className="p-1 text-purple-500 hover:text-purple-700 transition-colors"
            title="Visualizar detalhes"
          >
            <Eye className="w-5 h-5" />
          </button>
          <button
            onClick={() => onViewTargets(operacao.id)}
            className="p-1 text-blue-500 hover:text-blue-700 transition-colors"
            title="Visualizar targets"
          >
            <Target className="w-5 h-5" />
          </button>
          <button
            onClick={() => onRemove(operacao.id)}
            className="p-1 text-red-500 hover:text-red-700 transition-colors"
            title="Remover operação"
          >
            <Trash2 className="w-5 h-5" />
          </button>
        </div>
      </td>
    </tr>
  );
}