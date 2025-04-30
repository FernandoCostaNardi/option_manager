import React from 'react';
import { Eye } from 'lucide-react';
import { OperacaoFinalizada } from '../../../types/operacao/operacoes.types';
import { formatarData, formatarMoeda } from '../../../utils/formatadores';

interface OperacaoFinalizadaItemProps {
  operacao: OperacaoFinalizada;
  isAlternate: boolean;
  onView: (id: string) => void;
}

export const OperacaoFinalizadaItem: React.FC<OperacaoFinalizadaItemProps> = ({
  operacao,
  isAlternate,
  onView
}) => {
  return (
    <tr className={isAlternate ? "bg-purple-50" : ""}>
      <td className="px-6 py-4 whitespace-nowrap">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full overflow-hidden bg-gray-100 flex items-center justify-center">
            <img src={operacao.baseAssetLogoUrl} alt={operacao.optionSerieCode} className="w-6 h-6 object-contain" />
          </div>
          <span className="text-sm font-semibold text-gray-900">{operacao.optionSerieCode.toUpperCase()}</span>
        </div>
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-700 text-center">
        {formatarData(operacao.entryDate)}
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-700 text-center">
        {formatarData(operacao.exitDate)}
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-700">
        {operacao.analysisHouseName || '-'}
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-700">
        {operacao.brokerageName}
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-700 text-center">
        {operacao.quantity}
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-700 text-right">
        {formatarMoeda(operacao.entryTotalValue)}
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-center">
        <span className={`px-3 py-1 text-xs font-semibold rounded-full ${
          operacao.profitLoss > 0 
            ? 'bg-green-100 text-green-800' 
            : 'bg-red-100 text-red-800'
        }`}>
          {operacao.profitLoss > 0 ? 'Lucro' : 'Prejuízo'} ({operacao.profitLossPercentage.toFixed(2)}%)
        </span>
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-sm flex justify-center">
        <button
          onClick={() => onView(operacao.id)}
          className="text-indigo-600 hover:text-indigo-900 p-1 hover:bg-indigo-50 rounded-full transition-colors"
          title="Visualizar"
        >
          <Eye className="h-5 w-5" />
        </button>
      </td>
    </tr>
  );
};