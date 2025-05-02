// operacaoService.ts
import api from './api';
import { FiltrosOperacao } from '../types/operacao/operacoes.types';

export class OperacaoService {
    // ... outros métodos existentes
  
    static async exportarOperacoes(
      filtros: FiltrosOperacao,
      status: string[],
      formato: 'excel' | 'pdf'
    ): Promise<Blob> {
      // Monta os parâmetros de query
      const params: any = {
        ...filtros,
      };
      // Remove filtros nulos
      Object.keys(params).forEach(
        (key) => (params[key] == null || params[key] === '') && delete params[key]
      );
      // Adiciona status[] como array se houver
      if (status && status.length > 0) {
        params['status[]'] = status;
      }

      const endpoint = `/operations/export/${formato}`;
      const response = await api.get(endpoint, {
        params,
        responseType: 'blob',
        headers: {
          Accept:
            formato === 'excel'
              ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
              : 'application/pdf',
        },
      });
      return response.data;
    }
  }