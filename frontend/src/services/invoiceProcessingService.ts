import { ApiService } from './api';

// ===== INTERFACE SIMPLIFICADA PARA INVOICES =====

/**
 * Invoice simplificada baseada no InvoiceData do backend
 */
export interface SimpleInvoiceData {
  id: string;
  invoiceNumber: string;
  tradingDate: string;
  brokerageName: string;
  itemsCount: number;
  netOperationsValue: number;
  grossOperationsValue: number;
  totalCosts: number;
  netSettlementValue: number;
  importedAt: string;
  processingStatus?: 'PENDING' | 'SUCCESS' | 'FAILED'; // ✅ NOVO: Status de processamento
  items?: any[]; // ✅ NOVO: Campo items para verificar se foi processada
}

/**
 * Resposta da API com dados completos
 */
export interface ApiInvoiceResponse {
  content: SimpleInvoiceData[];
  totalPages: number;
  totalElements: number;
}

// ===== INTERFACES PARA FASE 2 - PROCESSAMENTO (FUTURAS) =====

/**
 * Resposta do processamento de invoices
 */
export interface InvoiceProcessingResponse {
  success: boolean;
  partialSuccess: boolean;
  processedInvoices: number;
  createdOperations: number;
  skippedOperations: number;
  errorCount: number;
  errors: string[];
  summary: string;
  sessionId?: string; // ID da sessão para processamento assíncrono
}

/**
 * Request para processamento em lote
 */
export interface ProcessBatchRequest {
  invoiceIds: string[];
}

/**
 * Estatísticas do dashboard
 */
export interface DashboardStats {
  totalInvoices: number;
  unprocessedInvoices: number;
  processedInvoices: number;
  totalOperationsCreated: number;
  totalOperationsSkipped: number;
  successRate: number;
  lastProcessingDate?: string;
}

/**
 * Invoice com status de processamento (FASE 2 - FUTURO)
 */
export interface InvoiceWithStatus {
  id: string;
  invoiceNumber: string;
  tradingDate: string;
  brokerageName: string;
  itemsCount: number;
  operationNetValue: number;
  isProcessed: boolean;
  operationsCreated: number;
  operationsSkipped: number;
  processingErrors: string[];
  lastProcessingDate?: string;
}

/**
 * Resultado de reconciliação
 */
export interface ReconciliationResult {
  totalInvoiceOperations: number;
  totalSystemOperations: number;
  matchedOperations: number;
  unmatchedInvoiceOperations: number;
  unmatchedSystemOperations: number;
  discrepancies: OperationDiscrepancy[];
}

export interface OperationDiscrepancy {
  type: 'MISSING_IN_SYSTEM' | 'MISSING_IN_INVOICE' | 'VALUE_MISMATCH' | 'DATE_MISMATCH';
  invoiceOperation?: any;
  systemOperation?: any;
  description: string;
}

// ===== SERVIÇO PRINCIPAL =====

export class InvoiceProcessingService {
  
  // ===== MÉTODOS FUNCIONAIS (FASE 1) =====
  
  /**
   * Lista invoices simples (funcionando)
   */
  static async getSimpleInvoices(page: number = 0, size: number = 20, processingStatus?: string): Promise<ApiInvoiceResponse> {
    let url = `/invoices-v2?page=${page}&size=${size}`;
    
    if (processingStatus) {
      url += `&processingStatus=${processingStatus}`;
    }
    
    console.log('🔍 API - URL chamada:', url);
    console.log('🔍 API - processingStatus:', processingStatus);
    console.log('🔍 API - Parâmetros completos:', { page, size, processingStatus });
    
    try {
      const response = await ApiService.get(url);
      console.log('✅ API - Resposta recebida:', response.content.length, 'invoices, totalElements:', response.totalElements);
      
      // ✅ DEBUG: Verificar se o filtro está funcionando
      if (processingStatus) {
        console.log('🔍 DEBUG - Verificando filtro:', {
          'status-solicitado': processingStatus,
          'invoices-recebidas': response.content.length,
          'primeiras-3-invoices': response.content.slice(0, 3).map((inv: SimpleInvoiceData) => ({ 
            id: inv.id, 
            number: inv.invoiceNumber,
            processingStatus: inv.processingStatus || 'N/A',
            // ✅ DEBUG: Verificar se há outros campos que indiquem status
            hasItems: inv.itemsCount > 0,
            itemsArray: inv.items ? inv.items.length : 0,
            importedAt: inv.importedAt
          }))
        });
        
        // ✅ DEBUG: Verificar dados brutos da primeira invoice
        if (response.content.length > 0) {
          console.log('🔍 DEBUG - Dados brutos da primeira invoice:', JSON.stringify(response.content[0], null, 2));
          
          // ✅ DEBUG: Verificar se o campo items está presente
          const firstInvoice = response.content[0] as any;
          console.log('🔍 DEBUG - Campo items presente?', 'items' in firstInvoice);
          console.log('🔍 DEBUG - Tipo do campo items:', typeof firstInvoice.items);
          console.log('🔍 DEBUG - Items é array?', Array.isArray(firstInvoice.items));
          console.log('🔍 DEBUG - Quantidade de items:', firstInvoice.items ? firstInvoice.items.length : 'undefined');
        }
      }
      
      return response;
    } catch (error) {
      console.error('❌ Erro na API:', error);
      throw error;
    }
  }

  /**
   * Obtém detalhes de uma invoice específica
   */
  static async getInvoiceDetails(invoiceId: string): Promise<SimpleInvoiceData> {
    return ApiService.get(`/invoices-v2/${invoiceId}`);
  }

  /**
   * Obtém contador de invoices não processadas
   */
  static async getUnprocessedCount(): Promise<number> {
    return ApiService.get('/invoices-v2/count');
  }

  /**
   * Obtém contador de invoices pendentes
   */
  static async getPendingCount(): Promise<number> {
    try {
      // Tentar o endpoint específico primeiro
      return await ApiService.get('/invoices-v2/count/pending');
    } catch (error) {
      console.log('⚠️ Endpoint /count/pending não disponível, usando fallback');
      // Fallback: buscar invoices pendentes e contar
      const response = await InvoiceProcessingService.getSimpleInvoices(0, 1000, 'PENDING');
      console.log('✅ Fallback getPendingCount - Total de invoices pendentes:', response.totalElements);
      return response.totalElements;
    }
  }

  /**
   * Obtém contador de invoices processadas com sucesso
   */
  static async getProcessedCount(): Promise<number> {
    try {
      // Tentar o endpoint específico primeiro
      return await ApiService.get('/invoices-v2/count/success');
    } catch (error) {
      console.log('⚠️ Endpoint /count/success não disponível, usando fallback');
      // Fallback: buscar invoices processadas e contar
      const response = await InvoiceProcessingService.getSimpleInvoices(0, 1000, 'SUCCESS');
      console.log('✅ Fallback getProcessedCount - Total de invoices processadas:', response.totalElements);
      return response.totalElements;
    }
  }

  // ===== MÉTODOS DE PROCESSAMENTO (FASE 2 - IMPLEMENTADOS) =====
  
  /**
   * Estima o processamento de invoices
   */
  static async estimateProcessing(invoiceIds: string[]): Promise<any> {
    // ✅ VERIFICAR SE AS INVOICES EXISTEM ANTES DE ESTIMAR
    console.log('🔍 Verificando se as invoices existem:', invoiceIds);
    
    try {
      // Verificar cada invoice individualmente
      for (const invoiceId of invoiceIds) {
        try {
          await this.getInvoiceDetails(invoiceId);
          console.log(`✅ Invoice ${invoiceId} encontrada`);
        } catch (error) {
          console.error(`❌ Invoice ${invoiceId} não encontrada:`, error);
          throw new Error(`Invoice ${invoiceId} não encontrada no sistema`);
        }
      }
      
      console.log('✅ Todas as invoices verificadas, prosseguindo com estimativa...');
      return ApiService.post('/processing/estimate', { invoiceIds });
    } catch (error) {
      console.error('❌ Erro na verificação de invoices:', error);
      throw error;
    }
  }

  /**
   * Processa múltiplas invoices
   */
  static async processBatch(invoiceIds: string[], options: {
    dryRun?: boolean;
    maxOperations?: number;
    skipDuplicates?: boolean;
  } = {}): Promise<InvoiceProcessingResponse> {
    // ✅ VERIFICAR SE AS INVOICES EXISTEM ANTES DE PROCESSAR
    console.log('🔍 Verificando invoices antes do processamento:', invoiceIds);
    
    try {
      // Verificar cada invoice individualmente
      for (const invoiceId of invoiceIds) {
        try {
          await this.getInvoiceDetails(invoiceId);
          console.log(`✅ Invoice ${invoiceId} encontrada para processamento`);
        } catch (error) {
          console.error(`❌ Invoice ${invoiceId} não encontrada:`, error);
          throw new Error(`Invoice ${invoiceId} não encontrada no sistema`);
        }
      }
      
      console.log('✅ Todas as invoices verificadas, iniciando processamento...');
      return ApiService.post('/invoice/processing/real/process', {
        invoiceIds,
        ...options
      });
    } catch (error) {
      console.error('❌ Erro na verificação de invoices para processamento:', error);
      throw error;
    }
  }

  /**
   * Processa uma invoice específica
   */
  static async processInvoice(invoiceId: string, options: {
    dryRun?: boolean;
    maxOperations?: number;
  } = {}): Promise<InvoiceProcessingResponse> {
    return ApiService.post(`/invoice/processing/real/process/${invoiceId}`, options);
  }

  /**
   * Obtém status de uma sessão específica
   */
  static async getProcessingStatus(sessionId: string): Promise<any> {
    return ApiService.get(`/api/invoice/processing/real/status/${sessionId}`);
  }

  /**
   * Obtém status de processamento de uma invoice específica
   */
  static async getInvoiceProcessingStatus(invoiceId: string): Promise<any> {
    return ApiService.get(`/invoice/processing/status/${invoiceId}`);
  }

  /**
   * Cancela uma sessão de processamento
   */
  static async cancelProcessing(sessionId: string): Promise<void> {
    return ApiService.post(`/api/invoice/processing/real/status/${sessionId}/cancel`, {});
  }

  /**
   * Obtém status de processamento via SSE
   */
  static createProcessingEventSource(sessionId: string): EventSource {
    const token = localStorage.getItem('token');
    
    if (!token) {
      throw new Error('Token de autenticação não encontrado');
    }

    const baseUrl = ApiService.getBaseUrl().replace(/\/$/, '');
    const encodedToken = encodeURIComponent(token);
    const url = `${baseUrl}/api/processing/progress/${sessionId}?token=${encodedToken}`;
    
    console.log('🔗 Criando EventSource para sessão:', sessionId);
    console.log('   URL (sem token):', url.substring(0, url.indexOf('?token=')) + '?token=***');
    
    const eventSource = new EventSource(url);
    
    eventSource.addEventListener('open', () => {
      console.log('✅ EventSource aberto com sucesso para sessão:', sessionId);
    });
    
    eventSource.addEventListener('error', (e) => {
      console.error('❌ EventSource erro para sessão:', sessionId);
      console.error('   ReadyState:', eventSource.readyState);
      console.error('   Event:', e);
    });
    
    return eventSource;
  }

  /**
   * Obtém operações criadas por uma nota específica
   */
  static async getInvoiceOperations(invoiceId: string): Promise<{
    operations: any[];
    totalOperations: number;
    processedAt: string;
    status: 'PROCESSED' | 'PENDING' | 'FAILED';
  }> {
    try {
      const response = await ApiService.get(`/invoices-v2/${invoiceId}/operations`);
      return response;
    } catch (error) {
      console.error('Erro ao buscar operações da nota:', error);
      // Retornar dados mockados se a API não existir ainda
      return {
        operations: [],
        totalOperations: 0,
        processedAt: new Date().toISOString(),
        status: 'PENDING'
      };
    }
  }

  // ===== DASHBOARD E ESTATÍSTICAS (FUTURO) =====

  /**
   * Obtém estatísticas gerais do dashboard (FUTURO)
   */
  static async getDashboardStats(): Promise<DashboardStats> {
    // TODO: Implementar na Fase 2
    throw new Error('Funcionalidade de estatísticas ainda não implementada');
  }

  /**
   * Lista invoices com status de processamento (FUTURO)
   */
  static async getInvoicesWithStatus(page: number = 0, size: number = 20): Promise<{
    content: InvoiceWithStatus[];
    totalPages: number;
    totalElements: number;
  }> {
    // TODO: Implementar na Fase 2
    throw new Error('Funcionalidade de status ainda não implementada');
  }

  /**
   * Lista apenas invoices não processadas (FUTURO)
   */
  static async getUnprocessedInvoices(): Promise<InvoiceWithStatus[]> {
    // TODO: Implementar na Fase 2 - Por enquanto retorna todas como não processadas
    const result = await this.getSimpleInvoices(0, 1000);
    return result.content.map(invoice => ({
      ...invoice,
      operationNetValue: invoice.netOperationsValue,
      isProcessed: false,
      operationsCreated: 0,
      operationsSkipped: 0,
      processingErrors: [],
      lastProcessingDate: undefined
    }));
  }

  /**
   * Obtém lista de invoices disponíveis para processamento
   */
  static async getAvailableInvoices(): Promise<SimpleInvoiceData[]> {
    const result = await this.getSimpleInvoices(0, 1000);
    return result.content;
  }

  // ===== UTILITÁRIOS TEMPORÁRIOS =====

  /**
   * Converte SimpleInvoiceData para InvoiceWithStatus (temporário)
   */
  static convertToInvoiceWithStatus(invoice: SimpleInvoiceData): InvoiceWithStatus {
    return {
      id: invoice.id,
      invoiceNumber: invoice.invoiceNumber,
      tradingDate: invoice.tradingDate,
      brokerageName: invoice.brokerageName,
      itemsCount: invoice.itemsCount,
      operationNetValue: invoice.netOperationsValue,
      isProcessed: false, // Sempre false na Fase 1
      operationsCreated: 0,
      operationsSkipped: 0,
      processingErrors: [],
      lastProcessingDate: undefined
    };
  }
}