// components/posicao/tables/PosicoesTable.tsx

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
import { Button } from '../../ui/button';
import { Posicao, PositionStatus } from '../../../types/posicao/posicoes.types';
import { ChevronDown, ChevronUp, PlusCircle, LogOut, Eye } from 'lucide-react';
import { formatarMoeda, formatarData } from '../../../utils/formatadores';
import { PosicaoDetailPanel } from '../detail/PosicaoDetailPanel';

interface PosicoesTableProps {
  posicoes: Posicao[];
  loading: boolean;
  expandedPositionId: string | null;
  onViewDetails: (id: string) => void;
  onAddEntry: (posicao: Posicao) => void;
  onExit: (posicao: Posicao) => void;
}

export const PosicoesTable: React.FC<PosicoesTableProps> = ({
  posicoes,
  loading,
  expandedPositionId,
  onViewDetails,
  onAddEntry,
  onExit
}) => {
  // Helper para determinar a cor do status
  const getStatusColor = (status: PositionStatus) => {
    switch (status) {
      case PositionStatus.ACTIVE:
        return 'bg-green-100 text-green-700';
      case PositionStatus.PARTIALLY_CLOSED:
        return 'bg-yellow-100 text-yellow-700';
      case PositionStatus.CLOSED:
        return 'bg-gray-100 text-gray-700';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  };

  // Helper para formatar o status
  const formatStatus = (status: PositionStatus) => {
    switch (status) {
      case PositionStatus.ACTIVE:
        return 'Aberta';
      case PositionStatus.PARTIALLY_CLOSED:
        return 'Parcial';
      case PositionStatus.CLOSED:
        return 'Fechada';
      default:
        return status;
    }
  };

  return (
    <div className="w-full overflow-auto">
      <Table className="text-sm">
        <TableHeader>
          <TableRow>
            <TableHead className="w-36">Opção</TableHead>
            <TableHead>Tipo</TableHead>
            <TableHead>Direção</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Corretora</TableHead>
            <TableHead className="text-right">Quantidade Total</TableHead>
            <TableHead className="text-right">Quantidade Restante</TableHead>
            <TableHead className="text-right">Preço Médio</TableHead>
            <TableHead className="text-right">Lucro/Prejuízo</TableHead>
            <TableHead className="text-right">%</TableHead>
            <TableHead>Data Abertura</TableHead>
            <TableHead>Dias Aberto</TableHead>
            <TableHead className="text-center">Ações</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {posicoes.length === 0 ? (
            <TableRow>
              <TableCell colSpan={13} className="text-center text-gray-500 py-8">
                {loading ? 'Carregando posições...' : 'Nenhuma posição encontrada.'}
              </TableCell>
            </TableRow>
          ) : (
            <>
              {posicoes.map((posicao) => (
                <React.Fragment key={posicao.id}>
                  <TableRow className="hover:bg-gray-50">
                    <TableCell className="whitespace-nowrap font-medium">
                      <div className="flex items-center">
                        {posicao.baseAssetLogoUrl && (
                          <img 
                            src={posicao.baseAssetLogoUrl} 
                            alt={posicao.baseAssetCode} 
                            className="w-6 h-6 mr-2" 
                          />
                        )}
                        {posicao.optionSeriesCode}
                      </div>
                    </TableCell>
                    <TableCell>{posicao.optionType}</TableCell>
                    <TableCell>{posicao.direction === 'BUY' ? 'Compra' : 'Venda'}</TableCell>
                    <TableCell>
                      <div className="flex items-center">
                        <Badge 
                          className={`${getStatusColor(posicao.status)}`}
                        >
                          {formatStatus(posicao.status)}
                        </Badge>
                        {posicao.hasMultipleEntries && (
                          <Badge className="ml-1 bg-blue-100 text-blue-700" variant="outline">
                            Múltiplas Entradas
                          </Badge>
                        )}
                        {posicao.hasPartialExits && (
                          <Badge className="ml-1 bg-yellow-100 text-yellow-700" variant="outline">
                            Saídas Parciais
                          </Badge>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>{posicao.brokerageName}</TableCell>
                    <TableCell className="text-right">{posicao.totalQuantity}</TableCell>
                    <TableCell className="text-right">{posicao.remainingQuantity}</TableCell>
                    <TableCell className="text-right">{formatarMoeda(posicao.averagePrice)}</TableCell>
                    <TableCell className={`text-right ${posicao.totalRealizedProfit >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                      {formatarMoeda(posicao.totalRealizedProfit)}
                    </TableCell>
                    <TableCell className={`text-right ${posicao.totalRealizedProfitPercentage >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                      {posicao.totalRealizedProfitPercentage !== null ? `${posicao.totalRealizedProfitPercentage.toFixed(2)}%` : '-'}
                    </TableCell>
                    <TableCell>{formatarData(posicao.openDate)}</TableCell>
                    <TableCell className="text-center">{posicao.daysOpen}</TableCell>
                    <TableCell>
                      <div className="flex items-center justify-center gap-2">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => onViewDetails(posicao.id)}
                          title="Ver detalhes"
                        >
                          {expandedPositionId === posicao.id ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                        </Button>
                        {posicao.status !== PositionStatus.CLOSED && (
                          <>
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => onAddEntry(posicao)}
                              title="Adicionar entrada"
                            >
                              <PlusCircle size={16} />
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => onExit(posicao)}
                              title="Realizar saída"
                            >
                              <LogOut size={16} />
                            </Button>
                          </>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                  {expandedPositionId === posicao.id && (
                    <TableRow>
                      <TableCell colSpan={13} className="p-0">
                        <PosicaoDetailPanel 
                          posicaoId={posicao.id} 
                          onClose={() => onViewDetails("")}
                        />
                      </TableCell>
                    </TableRow>
                  )}
                </React.Fragment>
              ))}
            </>
          )}
        </TableBody>
      </Table>
    </div>
  );
};
