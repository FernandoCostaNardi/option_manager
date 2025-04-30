import React from 'react';
import { BarChart2, Plus } from 'lucide-react';

interface OperacoesHeaderProps {
  onNovaOperacao: () => void;
}

export const OperacoesHeader: React.FC<OperacoesHeaderProps> = ({ onNovaOperacao }) => {
  return (
    <div className="flex items-center justify-between mb-6">
      <h1 className="text-2xl font-bold flex items-center gap-2">
        <BarChart2 className="w-7 h-7 text-purple-600" /> Operações
      </h1>
      <button
        className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
        onClick={onNovaOperacao}
      >
        <Plus className="w-5 h-5" /> Nova Operação
      </button>
    </div>
  );
};