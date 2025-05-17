import { ApiService } from './api';
import { Posicao, EntryLot, PositionOperation } from '../types/posicao/posicoes.types';

export interface PosicaoPaginated {
  content: Posicao[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export interface PosicoesDashboard {
  totalPositions: number;
  totalLongPositions: number;
  totalShortPositions: number;
  totalInvested: number;
  totalUnrealizedProfitLoss: number;
  totalUnrealizedProfitLossPercentage: number;
}

export interface PosicaoResponse {
  positions: Posicao[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  dashboard: PosicoesDashboard;
}

export class PosicaoService {
  /**
   * Busca todas as posições ativas do usuário
   */
  static async buscarPosicoesAtivas(page: number = 0, size: number = 10): Promise<PosicaoResponse> {
    const response = await ApiService.get('/positions', {
      params: { status: 'ACTIVE,PARTIALLY_CLOSED', page, size }
    });
    return response;
  }

  /**
   * Busca uma posição específica com seus lotes e histórico
   */
  static async buscarPosicaoPorId(id: string): Promise<Posicao> {
    const response = await ApiService.get(`/positions/${id}`);
    return response;
  }

  /**
   * Busca os lotes de entrada de uma posição
   */
  static async buscarLotesPorPosicao(positionId: string): Promise<EntryLot[]> {
    const response = await ApiService.get(`/positions/${positionId}/lots`);
    return response;
  }

  /**
   * Busca o histórico de operações de uma posição
   */
  static async buscarHistoricoPorPosicao(positionId: string): Promise<PositionOperation[]> {
    const response = await ApiService.get(`/positions/${positionId}/operations`);
    return response;
  }

  /**
   * Realiza saída (total ou parcial) de uma posição
   */
  static async realizarSaidaPosicao(
    positionId: string,
    exitData: {
      exitDate: string;
      exitUnitPrice: number;
      quantity?: number;  // opcional para saídas parciais
      applyFifoLifo?: boolean;  // opcional para configurar regra FIFO/LIFO
    }
  ): Promise<any> {
    const response = await ApiService.post(`/positions/${positionId}/exit`, exitData);
    return response;
  }

  /**
   * Adiciona uma entrada a uma posição existente
   */
  static async adicionarEntradaPosicao(
    positionId: string,
    entryData: {
      entryDate: string;
      entryUnitPrice: number;
      quantity: number;
      brokerageId: string;
      analysisHouseId?: string;
    }
  ): Promise<any> {
    const response = await ApiService.post(`/positions/${positionId}/entry`, entryData);
    return response;
  }

  /**
   * Detecta se uma nova operação pode ser adicionada a uma posição existente
   */
  static async detectarPosicaoCompativel(optionSeriesCode: string, transactionType: string): Promise<Posicao | null> {
    const response = await ApiService.get(`/positions/compatible`, {
      params: { optionSeriesCode, transactionType }
    });
    return response;
  }
}