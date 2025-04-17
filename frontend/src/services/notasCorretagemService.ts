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
    // Cria um FormData para enviar os arquivos
    const formData = new FormData();
    
    // Adiciona cada arquivo ao FormData
    files.forEach(file => {
      formData.append('files', file);
    });
    
    // Envia a requisição para a API
    return ApiService.post('/ocr/upload', formData);
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
