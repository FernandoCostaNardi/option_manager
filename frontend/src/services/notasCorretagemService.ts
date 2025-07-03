import { ApiService } from './api';

// Interfaces para os tipos de dados
export interface NotaCorretagemSummary {
  id: string;
  tradingDate: string;
  invoiceNumber: string;
  operationNetValue: number;
  brokerageName: string;
}

export interface NotaCorretagemDetalhada {
  id: string;
  invoiceNumber: string;
  tradingDate: string;
  cpf: string;
  operationValue: number;
  operationNetValue: number;
  brokerageName: string;
  items: NotaCorretagemItem[];
  // outros campos...
}

export interface NotaCorretagemItem {
  id: string;
  transactionType: 'BUY' | 'SELL';
  asset: string;
  quantity: number;
  price: number;
  operationValue: number;
  // outros campos...
}

export interface NotaCorretagemPaginada {
  content: NotaCorretagemSummary[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  // outros campos de paginação...
}

export class NotasCorretagemService {
  /**
   * Importa notas de corretagem a partir de arquivos PDF
   */
  static async importarNotas(files: File[]): Promise<any> {
    const results = [];
    
    // Processa cada arquivo individualmente
    for (const file of files) {
      try {
        console.log('[DEBUG] Processando arquivo:', file.name, 'Tamanho:', file.size, 'Tipo:', file.type);
        
        // Cria um FormData para enviar um arquivo por vez
        const formData = new FormData();
        formData.append('file', file); // ✅ Corrigido: 'file' (singular) em vez de 'files'
        
        // Log do FormData (verificar se o arquivo foi anexado)
        console.log('[DEBUG] FormData criado com:', {
          hasFile: formData.has('file'),
          entries: Array.from(formData.entries()).map(([key, value]) => [key, value instanceof File ? `File: ${value.name}` : value])
        });
        
        const endpoint = '/ocr/upload';
        console.log('[DEBUG] Enviando requisição para:', endpoint);
        console.log('[DEBUG] URL completa será:', `http://localhost:8080/api${endpoint}`);
        console.log('[DEBUG] Timestamp:', new Date().toISOString());
        
        // Envia a requisição para a API (sem Content-Type para multipart/form-data)
        const result = await ApiService.post(endpoint, formData, {});
        
        console.log('[DEBUG] Resposta recebida:', result);
        
        results.push({
          file: file.name,
          success: true,
          result: result
        });
      } catch (error) {
        console.error(`Erro ao processar arquivo ${file.name}:`, error);
        results.push({
          file: file.name,
          success: false,
          error: error instanceof Error ? error.message : 'Erro desconhecido'
        });
      }
    }
    
    // Verifica se pelo menos um arquivo foi processado com sucesso
    const hasSuccess = results.some(r => r.success);
    const hasError = results.some(r => !r.success);
    
    if (!hasSuccess) {
      // Se nenhum arquivo foi processado com sucesso
      throw new Error(`Falha ao processar todos os arquivos: ${results.map(r => r.error).join(', ')}`);
    } else if (hasError) {
      // Se alguns arquivos falharam
      const errorFiles = results.filter(r => !r.success).map(r => r.file);
      console.warn(`Alguns arquivos falharam: ${errorFiles.join(', ')}`);
    }
    
    return {
      processedFiles: results.filter(r => r.success).length,
      totalFiles: files.length,
      results: results
    };
  }
  
  /**
   * Obtém um resumo de todas as notas de corretagem, com paginação
   */
  static async obterNotas(page: number = 0, size: number = 10): Promise<NotaCorretagemPaginada> {
    return ApiService.get(`/invoices?page=${page}&size=${size}`);
  }
  
  /**
   * Obtém os detalhes de uma nota de corretagem específica
   */
  static async obterNotaPorId(id: string): Promise<NotaCorretagemDetalhada> {
    return ApiService.get(`/invoices/${id}`);
  }
  
  /**
   * Exclui uma nota de corretagem
   */
  static async excluirNota(id: string): Promise<boolean> {
    return ApiService.delete(`/invoices/${id}`);
  }
}
