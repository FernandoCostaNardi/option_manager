// hooks/posicao/usePosicaoDetalhes.ts

import { useState, useEffect } from 'react';
import { PosicaoService } from '../../services/posicaoService';
import { 
  EntryLot, 
  ExitRecord, 
  Posicao 
} from '../../types/posicao/posicoes.types';

export const usePosicaoDetalhes = (positionId: string | null) => {
  const [posicao, setPosicao] = useState<Posicao | null>(null);
  const [lotes, setLotes] = useState<EntryLot[]>([]);
  const [saidas, setSaidas] = useState<ExitRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!positionId) return;

    const carregarDetalhes = async () => {
      setLoading(true);
      setError(null);
      try {
        // Carregar todos os detalhes em paralelo
        const [posicaoData, lotesData, saidasData] = await Promise.all([
          PosicaoService.buscarPosicaoPorId(positionId),
          PosicaoService.buscarLotesPorPosicao(positionId),
          PosicaoService.buscarSaidasPorPosicao(positionId)
        ]);
        
        setPosicao(posicaoData);
        setLotes(lotesData);
        setSaidas(saidasData);
      } catch (error) {
        console.error('Erro ao carregar detalhes da posição:', error);
        setError('Não foi possível carregar os detalhes da posição.');
      } finally {
        setLoading(false);
      }
    };

    carregarDetalhes();
  }, [positionId]);

  return {
    posicao,
    lotes,
    saidas,
    loading,
    error
  };
};
