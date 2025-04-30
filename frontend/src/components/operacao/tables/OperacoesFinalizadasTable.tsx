import React from 'react';
import { OperacaoFinalizada, SortField, SortDirection } from '../../../types/operacao/operacoes.types';
import { SortIcon } from '../../SortIcon';
import { OperacaoFinalizadaItem } from './OperacaoFinalizadaItem';

interface OperacoesFinalizadasTableProps {
  operacoes: OperacaoFinalizada[];
  sortField: SortField;
  sortDirection: SortDirection;
  onSort: (field: SortField) => void;
  onView: (id: string) => void;
}

export const OperacoesFinalizadasTable: React.FC<OperacoesFinalizadasTableProps> = ({
  operacoes,
  sortField,
  sortDirection,
  onSort,
  onView
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
              className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer"
              onClick={() => onSort('entryDate')}
            >
              <span className="flex items-center justify-center">
                Data Entrada <SortIcon currentField={sortField} field="entryDate" direction={sortDirection} />
              </span>
            </th>
            <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
              Data Saída
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
              Resultado
            </th>
            <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
              Ações
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200">
          {operacoes.length === 0 ? (
            <tr>
              <td colSpan={9} className="text-center py-6 text-gray-500">Nenhuma operação finalizada encontrada.</td>
            </tr>
          ) : (
            operacoes.map((operacao, index) => (
              <OperacaoFinalizadaItem
                key={operacao.id}
                operacao={operacao}
                isAlternate={index % 2 === 1}
                onView={onView}
              />
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};