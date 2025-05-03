import React from 'react';
import { OperacaoAtiva, SortField, SortDirection } from '../../../types/operacao/operacoes.types';
import { SortIcon } from '../../SortIcon';
import { OperacaoAtivaItem } from './OperacaoAtivaItem';

interface OperacoesAtivasTableProps {
  operacoes: OperacaoAtiva[];
  sortField: SortField;
  sortDirection: SortDirection;
  onSort: (field: SortField) => void;
  onEdit: (id: string) => void;
  onFinalize: (operacao: OperacaoAtiva) => void;
  onRemove: (id: string) => void;
  onViewTargets: (id: string) => void;
}

export const OperacoesAtivasTable: React.FC<OperacoesAtivasTableProps> = ({
  operacoes,
  sortField,
  sortDirection,
  onSort,
  onEdit,
  onFinalize,
  onRemove,
  onViewTargets
}) => {
  return (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead className="bg-gray-50">
          <tr>
            <th 
              className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer"
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
              className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer"
              onClick={() => onSort('transactionType')}
            >
              <span className="flex items-center justify-center">
                Tipo de transação <SortIcon currentField={sortField} field="transactionType" direction={sortDirection} />
              </span>
            </th>
            <th 
              className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer"
              onClick={() => onSort('entryDate')}
            >
              <span className="flex items-center justify-center">
                Data Entrada <SortIcon currentField={sortField} field="entryDate" direction={sortDirection} />
              </span>
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Casa de Análise
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Corretora
            </th>
            <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
              Quantidade
            </th>
            <th 
              className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer"
              onClick={() => onSort('entryTotalValue')}
            >
              <span className="flex items-center justify-center">
                Valor Total <SortIcon currentField={sortField} field="entryTotalValue" direction={sortDirection} />
              </span>
            </th>
            <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
              Ações
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200">
          {operacoes.length === 0 ? (
            <tr>
              <td colSpan={7} className="text-center py-6 text-gray-500">Nenhuma operação ativa encontrada.</td>
            </tr>
          ) : (
            operacoes.map((operacao, index) => (
              <OperacaoAtivaItem
                key={operacao.id}
                operacao={operacao}
                isAlternate={index % 2 === 1}
                onEdit={onEdit}
                onFinalize={onFinalize}
                onRemove={onRemove}
                onViewTargets={onViewTargets}
              />
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};