import React from 'react';
import { Edit, CheckSquare, Trash, Target } from 'lucide-react';
import { OperacaoAtiva } from '../../../types/operacao/operacoes.types';
import { formatarData, formatarMoeda } from '../../../utils/formatadores';

interface OperacaoAtivaItemProps {
  operacao: OperacaoAtiva;
  isAlternate: boolean;
  onEdit: (id: string) => void;
  onFinalize: (id: string) => void;
  onRemove: (id: string) => void;
  onViewTargets: (id: string) => void;
}

export const OperacaoAtivaItem: React.FC<OperacaoAtivaItemProps> = ({
  operacao,
  isAlternate,
  onEdit,
  onFinalize,
  onRemove,
  onViewTargets
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
      <td className="px-6 py-4 whitespace-nowrap text-sm text-right space-x-2 flex justify-center">
        <button
          onClick={() => onViewTargets(operacao.id)}
          className="text-blue-600 hover:text-blue-800 p-1 hover:bg-blue-50 rounded-full transition-colors"
          title="Visualizar Targets"
        >
          <Target className="h-5 w-5" />
        </button>
        <button
          onClick={() => onEdit(operacao.id)}
          className="text-indigo-600 hover:text-indigo-900 p-1 hover:bg-indigo-50 rounded-full transition-colors"
          title="Editar"
        >
          <Edit className="h-5 w-5" />
        </button>
        <button
          onClick={() => onFinalize(operacao.id)}
          className="text-green-600 hover:text-green-900 p-1 hover:bg-green-50 rounded-full transition-colors"
          title="Finalizar"
        >
          <CheckSquare className="h-5 w-5" />
        </button>
        <button
          onClick={() => onRemove(operacao.id)}
          className="text-red-600 hover:text-red-900 p-1 hover:bg-red-50 rounded-full transition-colors"
          title="Remover"
        >
          <Trash className="h-5 w-5" />
        </button>
      </td>
    </tr>
  );
};