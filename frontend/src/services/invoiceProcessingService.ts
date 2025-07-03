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
  static async getSimpleInvoices(page: number = 0, size: number = 20): Promise<{
    content: SimpleInvoiceData[];
    totalPages: number;
    totalElements: number;
  }> {
    return ApiService.get(`/invoices-v2?page=${page}&size=${size}`);
  }

  /**
   * Obtém detalhes de uma invoice específica
   */
  static async getInvoiceDetails(invoiceId: string): Promise<SimpleInvoiceData> {
    return ApiService.get(`/invoices-v2/${invoiceId}`);
  }

  // ===== MÉTODOS FUTUROS (FASE 2) =====
  
  /**
   * Processa todas as invoices não processadas (FUTURO)
   */
  static async processAllUnprocessed(): Promise<InvoiceProcessingResponse> {
    // TODO: Implementar na Fase 2
    throw new Error('Funcionalidade de processamento ainda não implementada');
  }

  /**
   * Processa uma invoice específica (FUTURO)
   */
  static async processInvoice(invoiceId: string): Promise<InvoiceProcessingResponse> {
    // TODO: Implementar na Fase 2
    throw new Error('Funcionalidade de processamento ainda não implementada');
  }

  /**
   * Processa múltiplas invoices (FUTURO)
   */
  static async processBatch(invoiceIds: string[]): Promise<InvoiceProcessingResponse> {
    // TODO: Implementar na Fase 2
    throw new Error('Funcionalidade de processamento ainda não implementada');
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