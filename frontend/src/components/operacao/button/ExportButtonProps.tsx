// Primeiro, vamos criar um componente para o botão de exportação
import React, { useState } from 'react';
import { Download, FileSpreadsheet, FileText, Loader2 } from 'lucide-react';
import { OperacaoService } from '../../../services/operacaoService';

interface ExportButtonProps {
  finalizada: boolean;
}

const ExportButton: React.FC<ExportButtonProps> = ({ finalizada }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleExport = async (formato: 'excel' | 'pdf') => {
    try {
      setIsLoading(true);
      
      // Verificar se há filtros ativos que precisam ser passados
      const statusArray = finalizada ? ['WINNER', 'LOSER'] : ['ACTIVE'];
      
      // Obtenha os filtros ativos do contexto ou props, se necessário
      const filtrosAtivos = {}; // Substitua por seus filtros reais
      
      const response = await OperacaoService.exportarOperacoes(statusArray, formato, formato);
      
      // Cria um URL para o blob e faz o download
      const url = window.URL.createObjectURL(new Blob([response]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `operacoes-${finalizada ? 'finalizadas' : 'ativas'}.${formato === 'excel' ? 'xlsx' : 'pdf'}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error('Erro ao exportar:', error);
      alert('Erro ao exportar os dados. Tente novamente.');
    } finally {
      setIsLoading(false);
      setIsOpen(false);
    }
  };

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-3 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700"
      >
        <Download size={16} />
        Exportar
      </button>
      
      {isOpen && (
        <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg z-10 border border-gray-200">
          <button
            onClick={() => handleExport('excel')}
            disabled={isLoading}
            className="flex items-center gap-2 w-full px-4 py-2 text-left hover:bg-gray-100 border-b border-gray-200"
          >
            {isLoading ? <Loader2 className="animate-spin h-4 w-4" /> : <FileSpreadsheet size={16} />}
            Exportar para Excel
          </button>
          <button
            onClick={() => handleExport('pdf')}
            disabled={isLoading}
            className="flex items-center gap-2 w-full px-4 py-2 text-left hover:bg-gray-100"
          >
            {isLoading ? <Loader2 className="animate-spin h-4 w-4" /> : <FileText size={16} />}
            Exportar para PDF
          </button>
        </div>
      )}
    </div>
  );
};

export default ExportButton;