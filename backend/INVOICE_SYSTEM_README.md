# Sistema de Importação de Notas de Corretagem - Fase 1

## 🎯 Visão Geral

Sistema completo para importação de notas de corretagem de diferentes corretoras brasileiras como **cópia fiel** dos documentos PDF. Esta é a **Fase 1** da implementação, focada na importação e armazenamento dos dados brutos.

## 📊 Funcionalidades Implementadas

### ✅ **Importação de Múltiplas Notas**
- Importação em lote (máximo 5 notas por vez)
- Validação automática de formato PDF
- Detecção de arquivos duplicados via hash SHA-256
- Processamento sequencial com logs detalhados

### ✅ **Armazenamento Estruturado**
- **Invoice**: Dados principais da nota de corretagem
- **InvoiceItem**: Operações individuais dentro da nota
- Relacionamentos com `Brokerage` e `User` existentes
- Campos para todos os impostos e taxas identificados

### ✅ **Validações Robustas**
- Formato de arquivo (PDF obrigatório)
- Tamanho máximo (10MB por arquivo)
- Hash de integridade
- Estrutura mínima do conteúdo

### ✅ **APIs RESTful Completas**
- `POST /api/invoices/import` - Importação
- `GET /api/invoices` - Listagem com filtros
- `GET /api/invoices/{id}` - Busca por ID
- `GET /api/invoices/latest` - Últimas importadas
- Suporte a paginação e ordenação

## 🏗️ Arquitetura Implementada

### **Entidades Principais**

```sql
invoices:
  - Metadados da nota (número, datas, cliente)
  - Resumo financeiro (valores brutos, líquidos, custos)
  - Impostos detalhados (IRRF, taxas, emolumentos)
  - Dados brutos (conteúdo completo + hash)

invoice_items:
  - Dados da operação (C/V, ativo, quantidade, preços)
  - Tipo de mercado (VISTA, OPÇÃO DE COMPRA, etc.)
  - Observações (Day Trade, negócio direto)
  - Dados de opções (vencimento, strike)
```

### **Serviços Implementados**

1. **InvoiceImportService** - Orquestração da importação
2. **InvoiceParserService** - Extração de dados dos PDFs
3. **InvoiceQueryService** - Consultas e filtros
4. **InvoiceFileValidator** - Validações de arquivo
5. **InvoiceMapperService** - Conversão Entity ↔ DTO

### **Padrões Utilizados**

- **Strategy Pattern**: Parser específico por corretora
- **Factory Pattern**: Criação de contextos e DTOs
- **Validation Pattern**: Validações em camadas
- **Repository Pattern**: Acesso a dados

## 📋 Como Usar

### **1. Importar Notas**

```bash
POST /api/invoices/import
Content-Type: application/json

{
  "brokerageId": "uuid-da-corretora",
  "files": [
    {
      "fileName": "nota_123456.pdf",
      "fileContent": "conteudo-base64-do-pdf",
      "fileHash": "sha256-hash-do-arquivo"
    }
  ]
}
```

### **2. Listar Notas**

```bash
GET /api/invoices?page=0&size=20&sortBy=tradingDate&sortDirection=DESC
GET /api/invoices?brokerageId=uuid&startDate=2025-01-01&endDate=2025-12-31
```

### **3. Buscar Nota Específica**

```bash
GET /api/invoices/{invoice-id}
```

## 🔧 Configurações

### **Arquivo application-invoice.yml**

```yaml
app:
  invoice:
    import-config:
      max-files-per-import: 5
      processing-timeout-seconds: 300
      validate-duplicates: true
    
    file-config:
      max-file-size-bytes: 10485760  # 10MB
      min-content-length: 1000
      allowed-extensions: [pdf]
```

## 🚀 Próximos Passos (Fase 2)

### **Processamento Automático**
- [ ] Validar se operações já existem no sistema
- [ ] Criar `Operations` automaticamente
- [ ] Detectar Day Trade vs Swing Trade
- [ ] Integrar com sistema de posições existente

### **Parsers Específicos**
- [ ] Parser BTG Pactual
- [ ] Parser Clear/XP
- [ ] Parser Rico
- [ ] Parser Toro

### **Melhorias**
- [ ] OCR para PDFs com texto não extraível
- [ ] Dashboard de importações
- [ ] Relatórios de inconsistências
- [ ] Processamento em background

## 📁 Estrutura de Arquivos

```
backend/src/main/java/com/olisystem/optionsmanager/
├── entity/invoice/
│   ├── Invoice.java
│   └── InvoiceItem.java
├── repository/invoice/
│   ├── InvoiceRepository.java
│   └── InvoiceItemRepository.java
├── service/invoice/
│   ├── InvoiceImportService.java
│   ├── InvoiceQueryService.java
│   ├── parser/BasicInvoiceParserService.java
│   └── mapper/InvoiceMapperService.java
├── controller/invoice/
│   └── InvoiceController.java
├── dto/invoice/
│   ├── InvoiceImportRequest.java
│   ├── InvoiceImportResponse.java
│   └── InvoiceData.java
├── exception/invoice/
│   ├── InvoiceParsingException.java
│   └── InvoiceExceptionHandler.java
├── validator/invoice/
│   └── InvoiceFileValidator.java
└── util/invoice/
    └── InvoiceUtils.java
```

## 🔍 Logs e Monitoramento

### **Logs Principais**

```
=== INICIANDO IMPORTAÇÃO DE NOTAS ===
Usuário: admin, Corretora: btg-uuid, Arquivos: 3
✅ Validação da requisição concluída
Processando arquivo: nota_123456.pdf
✅ Arquivo nota_123456.pdf importado com sucesso. ID: uuid
=== IMPORTAÇÃO CONCLUÍDA ===
Total: 3, Sucessos: 2, Duplicados: 1, Erros: 0
```

### **Métricas Capturadas**

- Arquivos processados por importação
- Taxa de sucesso vs erro
- Tempo de processamento por arquivo
- Detecção de duplicatas
- Tipos de erro mais comuns

## 🧪 Testes

### **Executar Testes**

```bash
cd backend
mvn test -Dtest=InvoiceUtilsTest
mvn test -Dtest="*Invoice*"
```

### **Cobertura Atual**

- ✅ InvoiceUtils (100%)
- 🔄 InvoiceFileValidator (planejado)
- 🔄 InvoiceImportService (planejado)

## 🛡️ Segurança

### **Validações Implementadas**

- Verificação de propriedade (usuário só vê suas notas)
- Validação de hash de arquivo
- Limite de tamanho de arquivo
- Sanitização de dados de entrada
- Logs de auditoria completos

### **Prevenção de Ataques**

- Hash SHA-256 previne manipulação de conteúdo
- Validação de extensão previne upload de malware
- Limite de tamanho previne DoS
- Autenticação obrigatória em todos os endpoints

## 📈 Performance

### **Otimizações Implementadas**

- Índices no banco para consultas frequentes
- Lazy loading em relacionamentos
- Paginação em todas as listagens
- Validação fail-fast para economizar recursos

### **Métricas Esperadas**

- Importação: ~2-5 segundos por arquivo PDF
- Consultas: <100ms para listagens paginadas
- Armazenamento: ~50KB por nota no banco

---

## 🎉 **Sistema Pronto para Uso!**

A **Fase 1** está completa e funcional. O sistema já pode importar e gerenciar notas de corretagem de forma robusta. A **Fase 2** irá adicionar o processamento automático para criação de operações no sistema principal.
