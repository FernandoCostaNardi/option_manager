export interface OperacaoAtiva {
  id: string;
  entryDate: string;
  transactionType: string;
  exitDate: string;
  analysisHouseName: string;
  analysisHouseId: string;
  brokerageName: string;
  brokerageId: string;
  optionSeriesCode: string;
  baseAssetLogoUrl: string;
  quantity: number;
  entryUnitPrice: number;
  entryTotalValue: number;
  exitUnitPrice: number;
  exitTotalValue: number;
  profitLoss: number;
  profitLossPercentage: number;
  status: string;
  optionType: string;
  targets?: {
    type: 'TARGET' | 'STOP_LOSS';
    sequence: number;
    value: number;
  }[];
  
  // Novos campos para integração com posições
  positionId?: string;  // ID da posição associada
  isMultipleEntry?: boolean;  // Indicador se faz parte de múltiplas entradas
  hasPartialExits?: boolean;  // Indicador se possui saídas parciais
}

export interface OperacaoFinalizada {
  optionSerieCode: string;
  id: string;
  type: string;
  optionSeriesCode: string;
  entryDate: string;
  exitDate: string;
  entryUnitPrice: number;
  entryTotalValue: number;
  exitUnitPrice: number;
  exitTotalValue: number;
  result: number | null;
  status: string;
  analysisHouseName: string | null;
  brokerageName: string | null;
  profitLoss: number | null;
  profitLossPercentage: number | null;
  baseAssetLogoUrl: string | null;
  tradeType: string;
  transactionType: string;
  optionType: string;
  
  // Novos campos para integração com posições
  positionId?: string;  // ID da posição associada
  isPartialExit?: boolean;  // Indicador se é uma saída parcial
  isConsolidated?: boolean;  // Indicador se é uma operação consolidada
  hasDetailedExits?: boolean;  // Indicador se possui saídas detalhadas para expandir
  roleType?: string;  // Tipo de papel na estrutura (Original, Parcial, Resultado)
  sequenceNumber?: number;  // Número de sequência na estrutura
  groupId?: string;  // ID do grupo relacionado
  originalOperationId?: string;  // ID da operação original
}

export type SortField = 
  | 'optionSerieCode' 
  | 'optionType' 
  | 'transactionType' 
  | 'entryDate' 
  | 'exitDate' 
  | 'entryUnitPrice' 
  | 'exitUnitPrice' 
  | 'profitLoss'
  | 'profitLossPercentage'
  | 'entryTotalValue'
  | 'analysisHouseName' 
  | 'brokerageName'
  | 'tradeType'  // Adicionando o campo tradeType
  | 'status';    // Adicionando o campo status
export type SortDirection = 'asc' | 'desc';

export interface FiltrosOperacao {
  entryDateStart: string | null;
  entryDateEnd: string | null;
  exitDateStart: string | null;
  exitDateEnd: string | null;
  analysisHouseName: string | null;
  brokerageName: string | null;
  transactionType: string | null;
  optionType: string | null;
  tradeType: string | null;
  status: string | null;
  entryTotalValue: number | null;
}
