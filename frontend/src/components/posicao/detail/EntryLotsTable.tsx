// components/posicao/detail/EntryLotsTable.tsx

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
import { EntryLot } from '../../../types/posicao/posicoes.types';
import { formatarMoeda, formatarData } from '../../../utils/formatadores';

interface EntryLotsTableProps {
  lotes: EntryLot[];
}

export const EntryLotsTable: React.FC<EntryLotsTableProps> = ({ lotes }) => {
  return (
    <div className="w-full overflow-auto bg-white rounded">
      <Table className="text-sm">
        <TableHeader>
          <TableRow>
            <TableHead className="w-12">#</TableHead>
            <TableHead>Data de Entrada</TableHead>
            <TableHead className="text-right">Quantidade</TableHead>
            <TableHead className="text-right">Quantidade Restante</TableHead>
            <TableHead className="text-right">Preço Unitário</TableHead>
            <TableHead className="text-right">Valor Total</TableHead>
            <TableHead>Status</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {lotes.length === 0 ? (
            <TableRow>
              <TableCell colSpan={7} className="text-center text-gray-500 py-8">
                Nenhum lote de entrada encontrado.
              </TableCell>
            </TableRow>
          ) : (
            <>
              {lotes.map((lote) => (
                <TableRow key={lote.id} className="hover:bg-gray-50">
                  <TableCell className="font-medium">{lote.sequenceNumber}</TableCell>
                  <TableCell>{formatarData(lote.entryDate)}</TableCell>
                  <TableCell className="text-right">{lote.quantity}</TableCell>
                  <TableCell className="text-right">{lote.remainingQuantity}</TableCell>
                  <TableCell className="text-right">{formatarMoeda(lote.unitPrice)}</TableCell>
                  <TableCell className="text-right">{formatarMoeda(lote.totalValue)}</TableCell>
                  <TableCell>
                    {lote.isFullyConsumed ? (
                      <Badge className="bg-gray-100 text-gray-700">Totalmente Consumido</Badge>
                    ) : lote.remainingQuantity < lote.quantity ? (
                      <Badge className="bg-yellow-100 text-yellow-700">Parcialmente Consumido</Badge>
                    ) : (
                      <Badge className="bg-green-100 text-green-700">Disponível</Badge>
                    )}
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
