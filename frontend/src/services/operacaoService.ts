// operacaoService.ts
import api from './api';
import { FiltrosOperacao } from '../types/operacao/operacoes.types';
import { ApiService } from './api';
import axios from 'axios';

export class OperacaoService {
    // ... outros métodos existentes
  
    static async exportarOperacoes(
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

    /**
     * Finaliza uma operação existente
     * @param operationId ID da operação a ser finalizada
     * @param exitData Dados de saída da operação
     * @returns Dados da operação finalizada
     */
    static async finalizarOperacao(
      operationId: string, 
      exitData: {
        exitDate: string;
        exitUnitPrice: number;
        quantity?: number; // opcional para saídas parciais
      }
    ) {
      console.log('[DEBUG] OperacaoService - Chamando API para finalizar operação:', operationId, exitData);
      
      // Verificar se o token está disponível
      const token = localStorage.getItem('token');
      console.log('[DEBUG] OperacaoService - Token está presente?', !!token);
      
      try {
        // Preparando o payload para enviar
        const payload = {
          operationId,
          ...exitData
        };
        
        // O caminho completo da API deve incluir o prefixo /api
        const apiUrl = 'http://localhost:8080/api/operations/finalize';
        console.log('[DEBUG] OperacaoService - URL da API:', apiUrl);
        console.log('[DEBUG] OperacaoService - Payload completo:', JSON.stringify(payload));
        
        // Chamando a API diretamente com fetch para garantir que seja chamada
        const response = await fetch(apiUrl, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          },
          body: JSON.stringify(payload)
        });
        
        console.log('[DEBUG] OperacaoService - Resposta da API status:', response.status);
        
        if (!response.ok) {
          throw new Error(`Erro ao finalizar operação: ${response.status} ${response.statusText}`);
        }
        
        const data = await response.json();
        console.log('[DEBUG] OperacaoService - Resposta da API dados:', data);
        
        return data;
      } catch (error) {
        console.error('[DEBUG] OperacaoService - Erro ao finalizar operação:', error);
        throw error;
      }
    }
  }