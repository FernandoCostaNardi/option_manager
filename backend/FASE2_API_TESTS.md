# 🚀 TESTE DAS APIS DA FASE 2 - PROCESSAMENTO DE INVOICES

## ENDPOINTS IMPLEMENTADOS:

### 🔄 Processar todas as invoices não processadas
POST http://localhost:8080/api/invoice-processing/process-all

### 🎯 Processar invoice específica
POST http://localhost:8080/api/invoice-processing/process/{invoiceId}

### 📦 Processar múltiplas invoices
POST http://localhost:8080/api/invoice-processing/process-batch
Content-Type: application/json
{
  "invoiceIds": ["uuid1", "uuid2", "uuid3"]
}

## RESPONSE FORMAT:
{
  "success": boolean,
  "partialSuccess": boolean,
  "processedInvoices": number,
  "createdOperations": number,
  "skippedOperations": number,
  "errorCount": number,
  "errors": ["string array"],
  "summary": "string"
}

## STATUS CODES:
- 200: Sucesso total
- 206: Sucesso parcial
- 400: Parâmetros inválidos
- 404: Invoice não encontrada
- 422: Falha no processamento
- 500: Erro interno
