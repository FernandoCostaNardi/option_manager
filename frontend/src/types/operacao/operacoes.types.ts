export interface OperacaoAtiva {
    id: string;
    entryDate: string;
    exitDate: string;
    analysisHouseName: string;
    brokerageName: string;
    optionSerieCode: string;
    baseAssetLogoUrl: string;
    quantity: number;
    entryUnitPrice: number;
    entryTotalValue: number;
    exitUnitPrice: number;
    exitTotalValue: number;
    profitLoss: number;
    profitLossPercentage: number;
    status: string;
  }
  
  export interface OperacaoFinalizada extends OperacaoAtiva {
    dataSaida: string;
    status: 'Vencedor' | 'Perdedor';
    valorUnitarioSaida: number;
    valorTotalSaida: number;
    valorLucroPrejuizo: number;
    percentualLucroPrejuizo: number;
  }
  
  export type SortField = 'optionSerieCode' | 'entryDate' | 'entryTotalValue' | null;
  export type SortDirection = 'asc' | 'desc';
  
  export interface FiltrosOperacao {
    entryDateStart: string | null;
    entryDateEnd: string | null;
    exitDateStart: string | null;
    exitDateEnd: string | null;
    analysisHouseName: string | null;
    brokerageName: string | null;
  }