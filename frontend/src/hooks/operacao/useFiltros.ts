import { useState, useCallback } from 'react';
import { FiltrosOperacao } from '../../types/operacao/operacoes.types'
import { formatarData } from '../../utils/formatadores';

export const useFiltros = () => {
  const [acordeonAberto, setAcordeonAberto] = useState(false);
  const [filtros, setFiltros] = useState<FiltrosOperacao>({
    entryDateStart: null,
    entryDateEnd: null,
    exitDateStart: null,
    exitDateEnd: null,
    analysisHouseName: null,
    brokerageName: null,
    transactionType: null,
    optionType: null,
    tradeType: null,
    status: null
  });

  const limparFiltros = useCallback(() => {
    setFiltros({
      entryDateStart: null,
      entryDateEnd: null,
      exitDateStart: null,
      exitDateEnd: null,
      analysisHouseName: null,
      brokerageName: null,
      transactionType: null,
      optionType: null,
      tradeType: null,
      status: null
    });
  }, []);

  const temFiltrosAtivos = useCallback(() => {
    return Object.values(filtros).some(valor => valor !== null && valor !== '');
  }, [filtros]);

  // Função para obter texto dos filtros ativos
  const obterTextoFiltrosAtivos = () => {
    const filtrosAtivos = [] as string[];
    
    if (filtros.entryDateStart || filtros.entryDateEnd) {
      let textoData = 'Data de Entrada: ';
      if (filtros.entryDateStart && filtros.entryDateEnd) {
        textoData += `${formatarData(filtros.entryDateStart)} até ${formatarData(filtros.entryDateEnd)}`;
      } else if (filtros.entryDateStart) {
        textoData += `a partir de ${formatarData(filtros.entryDateStart)}`;
      } else if (filtros.entryDateEnd) {
        textoData += `até ${formatarData(filtros.entryDateEnd)}`;
      }
      filtrosAtivos.push(textoData);
    }
    
    if (filtros.exitDateStart || filtros.exitDateEnd) {
      let textoData = 'Data de Saída: ';
      if (filtros.exitDateStart && filtros.exitDateEnd) {
        textoData += `${formatarData(filtros.exitDateStart)} até ${formatarData(filtros.exitDateEnd)}`;
      } else if (filtros.exitDateStart) {
        textoData += `a partir de ${formatarData(filtros.exitDateStart)}`;
      } else if (filtros.exitDateEnd) {
        textoData += `até ${formatarData(filtros.exitDateEnd)}`;
      }
      filtrosAtivos.push(textoData);
    }
    
    if (filtros.analysisHouseName) {
      filtrosAtivos.push(`Casa de Análise: ${filtros.analysisHouseName}`);
    }
    
    if (filtros.brokerageName) {
      filtrosAtivos.push(`Corretora: ${filtros.brokerageName}`);
    }
    
    if (filtros.transactionType) {
      const tipoTraduzido = filtros.transactionType === 'BUY' ? 'COMPRA' : 'VENDA';
      filtrosAtivos.push(`Tipo: ${tipoTraduzido}`);
    }
    
    if (filtros.optionType) {
      filtrosAtivos.push(`Tipo de Opção: ${filtros.optionType}`);
    }
    
    if (filtros.tradeType) {
      const tipoTradeTraduzido = filtros.tradeType === 'SWING' ? 'SwingTrade' : 'DayTrade';
      filtrosAtivos.push(`Tipo de Trade: ${tipoTradeTraduzido}`);
    }
    
    if (filtros.status) {
      const statusTraduzido = filtros.status === 'WINNER' ? 'Ganhadora' : 'Perdedora';
      filtrosAtivos.push(`Status: ${statusTraduzido}`);
    }
    return filtrosAtivos.join(' | ');
  };

  return {
    acordeonAberto,
    setAcordeonAberto,
    filtros,
    setFiltros,
    limparFiltros,
    temFiltrosAtivos,
    obterTextoFiltrosAtivos
  };
};