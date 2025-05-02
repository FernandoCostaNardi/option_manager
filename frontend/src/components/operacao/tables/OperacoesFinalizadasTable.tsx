import React from 'react';
import { OperacaoFinalizada, SortField, SortDirection } from '../../../types/operacao/operacoes.types';
import { OperacaoFinalizadaItem } from './OperacaoFinalizadaItem';
import { Loader2 } from 'lucide-react';
import { SortIcon } from '../../SortIcon';

interface OperacoesFinalizadasTableProps {
  operacoes: OperacaoFinalizada[];
  loading: boolean;
  onView: (id: string) => void;
  onRemove: (id: string) => void;
  onViewTargets: (id: string) => void;
  onSort: (field: SortField) => void;
  sortField: SortField;
  sortDirection: SortDirection;
}

export function OperacoesFinalizadasTable({
  operacoes,
  loading,
  onView,
  onRemove,
  onViewTargets,
  onSort,
  sortField,
  sortDirection
}: OperacoesFinalizadasTableProps) {
  return (
    <div className="overflow-x-auto rounded-lg border border-gray-100">
      <table className="min-w-full divide-y divide-gray-100">
        <thead className="bg-gray-50">
          <tr>
            <th 
              className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('optionSerieCode')}
            >
              <span className="flex items-center justify-center">
                Opção <SortIcon currentField={sortField} field="optionSerieCode" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('optionType')}
            >
              <span className="flex items-center justify-center">
                Tipo <SortIcon currentField={sortField} field="optionType" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('entryDate')}
            >
              <span className="flex items-center justify-center">
                Data Entrada <SortIcon currentField={sortField} field="entryDate" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('exitDate')}
            >
              <span className="flex items-center justify-center">
                Data Saída <SortIcon currentField={sortField} field="exitDate" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('tradeType')}
            >
              <span className="flex items-center justify-center">
                Tipo de trade <SortIcon currentField={sortField} field="tradeType" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('entryUnitPrice')}
            >
              <span className="flex items-center justify-center">
                Preço Entrada <SortIcon currentField={sortField} field="entryUnitPrice" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('exitUnitPrice')}
            >
              <span className="flex items-center justify-center">
                Preço Saída <SortIcon currentField={sortField} field="exitUnitPrice" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('profitLoss')}
            >
              <span className="flex items-center justify-end">
                Resultado <SortIcon currentField={sortField} field="profitLoss" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('profitLossPercentage')}
            >
              <span className="flex items-center justify-center">
                % <SortIcon currentField={sortField} field="profitLossPercentage" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('status')}
            >
              <span className="flex items-center justify-center">
                Status <SortIcon currentField={sortField} field="status" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('analysisHouseName')}
            >
              <span className="flex items-center">
                Casa de Análise <SortIcon currentField={sortField} field="analysisHouseName" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-50"
              onClick={() => onSort('brokerageName')}
            >
              <span className="flex items-center">
                Corretora <SortIcon currentField={sortField} field="brokerageName" direction={sortDirection} />
              </span>
            </th>
            <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
              Ações
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-100">
          {loading ? (
            <tr>
              <td colSpan={13} className="px-4 py-8 text-center">
                <div className="flex items-center justify-center">
                  <Loader2 className="w-6 h-6 text-purple-600 animate-spin mr-2" />
                  <span className="text-gray-500">Carregando operações...</span>
                </div>
              </td>
            </tr>
          ) : operacoes.length === 0 ? (
            <tr>
              <td colSpan={13} className="px-4 py-8 text-center text-gray-500">
                Nenhuma operação finalizada encontrada.
              </td>
            </tr>
          ) : (
            operacoes.map((operacao, index) => (
              <OperacaoFinalizadaItem
                key={operacao.id}
                operacao={operacao}
                onView={onView}
                onRemove={onRemove}
                onViewTargets={onViewTargets}
                index={index}
              />
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}