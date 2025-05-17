// components/posicao/detail/ExitRecordsTable.tsx

import React from 'react';
import { 
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../ui/table';
import { Badge } from '../../ui/badge';
import { ExitRecord, ExitStrategy } from '../../../types/posicao/posicoes.types';
import { formatarMoeda, formatarData } from '../../../utils/formatadores';

interface ExitRecordsTableProps {
  saidas: ExitRecord[];
}

export const ExitRecordsTable: React.FC<ExitRecordsTableProps> = ({ saidas }) => {
  // Helper para formatar a estratégia
  const formatStrategy = (strategy: ExitStrategy) => {
    switch (strategy) {
      case ExitStrategy.FIFO:
        return 'FIFO';
      case ExitStrategy.LIFO:
        return 'LIFO';
      case ExitStrategy.AUTO:
        return 'Auto (LIFO/FIFO)';
      default:
        return strategy;
    }
  };

  // Helper para determinar a cor da estratégia
  const getStrategyColor = (strategy: ExitStrategy) => {
    switch (strategy) {
      case ExitStrategy.FIFO:
        return 'bg-blue-100 text-blue-700';
      case ExitStrategy.LIFO:
        return 'bg-purple-100 text-purple-700';
      case ExitStrategy.AUTO:
        return 'bg-green-100 text-green-700';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  };

  return (
    <div className="w-full overflow-auto bg-white rounded">
      <Table className="text-sm">
        <TableHeader>
          <TableRow>
            <TableHead>Data de Saída</TableHead>
            <TableHead>Lote #</TableHead>
            <TableHead>Data Entrada</TableHead>
            <TableHead className="text-right">Quantidade</TableHead>
            <TableHead className="text-right">Preço de Entrada</TableHead>
            <TableHead className="text-right">Preço de Saída</TableHead>
            <TableHead className="text-right">Lucro/Prejuízo</TableHead>
            <TableHead className="text-right">%</TableHead>
            <TableHead>Estratégia</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {saidas.length === 0 ? (
            <TableRow>
              <TableCell colSpan={9} className="text-center text-gray-500 py-8">
                Nenhum registro de saída encontrado.
              </TableCell>
            </TableRow>
          ) : (
            <>
              {saidas.map((saida) => (
                <TableRow key={saida.id} className="hover:bg-gray-50">
                  <TableCell>{formatarData(saida.exitDate)}</TableCell>
                  <TableCell>{saida.sequenceNumber}</TableCell>
                  <TableCell>{formatarData(saida.entryDate)}</TableCell>
                  <TableCell className="text-right">{saida.quantity}</TableCell>
                  <TableCell className="text-right">{formatarMoeda(saida.entryUnitPrice)}</TableCell>
                  <TableCell className="text-right">{formatarMoeda(saida.exitUnitPrice)}</TableCell>
                  <TableCell className={`text-right ${saida.profitLoss >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                    {formatarMoeda(saida.profitLoss)}
                  </TableCell>
                  <TableCell className={`text-right ${saida.profitLossPercentage >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                    {saida.profitLossPercentage.toFixed(2)}%
                  </TableCell>
                  <TableCell>
                    <Badge className={getStrategyColor(saida.appliedStrategy)}>
                      {formatStrategy(saida.appliedStrategy)}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))}
            </>
          )}
        </TableBody>
      </Table>
    </div>
  );
};
