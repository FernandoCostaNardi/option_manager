export interface Posicao {
  id: string;
  optionSeriesCode: string;
  optionType: string;
  baseAssetLogoUrl: string;
  direction: PositionDirection;
  status: PositionStatus;
  creationDate: string;
  lastUpdateDate: string;
  averageEntryPrice: number;
  averageExitPrice: number | null;
  totalQuantity: number;
  remainingQuantity: number;
  totalInvested: number;
  currentInvested: number;
  realizedProfitLoss: number;
  unrealizedProfitLoss: number;
  brokerageName: string;
  analysisHouseName: string | null;
}

export interface EntryLot {
  id: string;
  positionId: string;
  operationId: string;
  entryDate: string;
  unitPrice: number;
  originalQuantity: number;
  remainingQuantity: number;
  totalValue: number;
  dayKey: string;
}

export enum PositionDirection {
  LONG = 'LONG',
  SHORT = 'SHORT'
}

export enum PositionStatus {
  ACTIVE = 'ACTIVE',
  PARTIALLY_CLOSED = 'PARTIALLY_CLOSED',
  CLOSED = 'CLOSED'
}

export interface PositionOperation {
  id: string;
  positionId: string;
  operationId: string;
  type: 'ENTRY' | 'EXIT';
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  date: string;
  sequence: number;
}