# 🔄 FLUXOGRAMA DE PROCESSAMENTO DE INVOICE

## **📋 Visão Geral do Sistema**

```
┌─────────────────────────────────────────────────────────────┐
│                    INVOICE PROCESSING SYSTEM               │
│                                                             │
│  ETAPA 1: Estrutura Base    ETAPA 2: Validação            │
│  ETAPA 3: Detecção          ETAPA 4: Integração            │
│  ETAPA 5: Orquestrador      ETAPA 6: Controller (Futuro)  │
└─────────────────────────────────────────────────────────────┘
```

## **🎼 FLUXO PRINCIPAL - InvoiceProcessingOrchestrator**

### **1. INÍCIO DO PROCESSAMENTO**
```
┌─────────────────────────────────────────────────────────────┐
│                    PROCESSAMENTO INICIADO                  │
│                                                             │
│  Input: List<UUID> invoiceIds + User user                 │
│  Output: OrchestrationResult                              │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    PROGRESSO 0%                            │
│              "Validando invoices..."                       │
└─────────────────────────────────────────────────────────────┘
```

### **2. ETAPA DE VALIDAÇÃO (0-20%)**
```
┌─────────────────────────────────────────────────────────────┐
│                    VALIDAÇÃO INICIAL                       │
│                                                             │
│  Para cada Invoice ID:                                     │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 1. InvoiceValidationService.validateInvoice()     │   │
│  │    ├─ Dados básicos (número, datas, cliente)     │   │
│  │    ├─ Itens individuais (código, tipo, preços)   │   │
│  │    ├─ Consistência (preço × quantidade = total)  │   │
│  │    └─ Regras de negócio (limites, valores)       │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    DETECÇÃO DE DUPLICATAS                  │
│                                                             │
│  DuplicateDetectionService.detectDuplicates()             │
│  ├─ Verificar OperationSourceMapping existente            │
│  ├─ Verificar regras de negócio (mesmo ativo + data)     │
│  ├─ Verificar duplicatas exatas (todos campos iguais)    │
│  └─ Tolerância de 1% para preços similares               │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                VALIDAÇÃO DE REPROCESSAMENTO                │
│                                                             │
│  ReprocessingValidationService.validateReprocessing()     │
│  ├─ Verificar histórico de processamento                  │
│  ├─ Verificar processamento recente (< 5 minutos)        │
│  ├─ Verificar limite de tentativas (máx 5)               │
│  └─ Verificar operações existentes                        │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    DECISÃO DE CONTINUAÇÃO                  │
│                                                             │
│  IF (validCount > 0 && !hasDuplicates)                   │
│  │  └─ CONTINUAR PARA DETECÇÃO                            │
│  ELSE                                                      │
│  │  └─ PARAR COM ERRO                                     │
└─────────────────────────────────────────────────────────────┘
```

### **3. BUSCA DE INVOICES VÁLIDAS (20-40%)**
```
┌─────────────────────────────────────────────────────────────┐
│                    PROGRESSO 20%                           │
│              "Buscando invoices válidas..."                │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    CARREGAMENTO                            │
│                                                             │
│  fetchValidInvoices(validInvoiceIds)                      │
│  ├─ Buscar invoices do banco de dados                     │
│  ├─ Carregar itens relacionados                           │
│  ├─ Verificar integridade dos dados                       │
│  └─ Preparar para detecção                                │
└─────────────────────────────────────────────────────────────┘
```

### **4. DETECÇÃO DE OPERAÇÕES (40-60%)**
```
┌─────────────────────────────────────────────────────────────┐
│                    PROGRESSO 40%                           │
│                "Detectando operações..."                   │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    OPERATION DETECTION ENGINE               │
│                                                             │
│  detectionEngine.detectOperations(validInvoices, user)    │
│  │                                                         │
│  ├─ 1. DETECÇÃO INICIAL                                   │
│  │   ├─ Para cada InvoiceItem:                            │
│  │   │  ├─ ActiveOperationDetector.detectActiveOperations()│
│  │   │  ├─ TradeTypeAnalyzer.analyzeTradeType()           │
│  │   │  └─ OperationMatchingService.matchOperations()     │
│  │   └─ Resultado: List<DetectedOperation>                │
│  │                                                         │
│  ├─ 2. CLASSIFICAÇÃO                                       │
│  │   ├─ Para cada DetectedOperation:                      │
│  │   │  ├─ OperationClassifier.classify()                 │
│  │   │  ├─ Determinar tipo (NEW, EXIT, DAY_TRADE)        │
│  │   │  └─ Aplicar regras de negócio                      │
│  │   └─ Resultado: List<ClassifiedOperation>              │
│  │                                                         │
│  ├─ 3. CONSOLIDAÇÃO                                        │
│  │   ├─ InvoiceItemGrouper.groupItems()                   │
│  │   ├─ OperationConsolidator.consolidate()               │
│  │   ├─ Agrupar operações similares                       │
│  │   └─ Calcular totais e médias                          │
│  └─ Resultado: List<ConsolidatedOperation>                │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    VERIFICAÇÃO DE SUCESSO                  │
│                                                             │
│  IF (detectionResult.isSuccess())                          │
│  │  └─ CONTINUAR PARA INTEGRAÇÃO                           │
│  ELSE                                                       │
│  │  └─ PARAR COM ERRO DE DETECÇÃO                          │
└─────────────────────────────────────────────────────────────┘
```

### **5. VALIDAÇÃO PARA INTEGRAÇÃO (60-80%)**
```
┌─────────────────────────────────────────────────────────────┐
│                    PROGRESSO 60%                           │
│        "Validando operações para integração..."            │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    VALIDAÇÃO FINAL                         │
│                                                             │
│  integrationProcessor.validateOperationsForIntegration()   │
│  │                                                         │
│  ├─ Para cada ConsolidatedOperation:                       │
│  │  ├─ OperationValidationService.validateOperation()     │
│  │  │  ├─ Validações básicas (campos obrigatórios)       │
│  │  │  ├─ Validações de negócio (limites, permissões)    │
│  │  │  ├─ Validações de integridade (cálculos)           │
│  │  │  └─ Sistema de avisos para operações suspeitas     │
│  │  └─ Resultado: ValidationResult                        │
│  │                                                         │
│  ├─ Agrupar operações válidas vs inválidas               │
│  ├─ Calcular estatísticas de validação                    │
│  └─ Resultado: ValidationSummary                          │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    VERIFICAÇÃO DE OPERAÇÕES VÁLIDAS        │
│                                                             │
│  IF (integrationValidation.getValidCount() > 0)            │
│  │  └─ CONTINUAR PARA INTEGRAÇÃO                           │
│  ELSE                                                       │
│  │  └─ PARAR: "Nenhuma operação válida para integração"   │
└─────────────────────────────────────────────────────────────┘
```

### **6. INTEGRAÇÃO DE OPERAÇÕES (80-100%)**
```
┌─────────────────────────────────────────────────────────────┐
│                    PROGRESSO 80%                           │
│                "Integrando operações..."                   │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    OPERATION INTEGRATION PROCESSOR          │
│                                                             │
│  integrationProcessor.processIntegration()                 │
│  │                                                         │
│  ├─ Para cada ConsolidatedOperation:                       │
│  │  ├─ 1. VALIDAÇÃO INDIVIDUAL                            │
│  │  │   ├─ OperationValidationService.validateOperation() │
│  │  │   └─ Verificar se pode ser processada               │
│  │  │                                                      │
│  │  ├─ 2. MAPEAMENTO                                      │
│  │  │   ├─ OperationMappingService.createMapping()        │
│  │  │   ├─ Determinar tipo de mapeamento                  │
│  │  │   │  ├─ NEW_OPERATION                               │
│  │  │   │  ├─ DAY_TRADE_ENTRY                             │
│  │  │   │  ├─ EXISTING_OPERATION_EXIT                     │
│  │  │   │  └─ EXISTING_OPERATION_UPDATE                   │
│  │  │   └─ Criar OperationSourceMapping                   │
│  │  │                                                      │
│  │  ├─ 3. PROCESSAMENTO                                   │
│  │  │   ├─ IF (NEW_OPERATION)                             │
│  │  │   │  ├─ OperationService.createOperation()          │
│  │  │   │  └─ Criar nova operação no sistema             │
│  │  │   ├─ IF (EXISTING_OPERATION_EXIT)                   │
│  │  │   │  ├─ OperationService.createExitOperation()     │
│  │  │   │  └─ Finalizar operação existente               │
│  │  │   ├─ IF (DAY_TRADE_ENTRY)                           │
│  │  │   │  ├─ DayTradeProcessor.process()                 │
│  │  │   │  └─ Processar como day trade                    │
│  │  │   └─ IF (EXISTING_OPERATION_UPDATE)                 │
│  │  │     ├─ OperationService.updateOperation()           │
│  │  │     └─ Atualizar operação existente                 │
│  │  │                                                      │
│  │  └─ 4. RESULTADO                                        │
│  │    ├─ ProcessedOperation (sucesso/falha)               │
│  │    ├─ InvoiceOperationMapping criado                   │
│  │    └─ Logs detalhados                                  │
│  │                                                         │
│  ├─ Consolidar resultados                                 │
│  ├─ Calcular estatísticas                                 │
│  └─ Resultado: IntegrationResult                          │
└─────────────────────────────────────────────────────────────┘
```

### **7. FINALIZAÇÃO (100%)**
```
┌─────────────────────────────────────────────────────────────┐
│                    PROGRESSO 100%                          │
│              "Processamento concluído!"                    │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    CÁLCULO DE ESTATÍSTICAS                 │
│                                                             │
│  calculateFinalStatistics(result)                          │
│  ├─ Estatísticas de validação                             │
│  │  ├─ validInvoicesCount                                 │
│  │  └─ invalidInvoicesCount                               │
│  │                                                         │
│  ├─ Estatísticas de detecção                              │
│  │  ├─ detectedOperationsCount                            │
│  │  └─ consolidatedOperationsCount                        │
│  │                                                         │
│  ├─ Estatísticas de integração                            │
│  │  ├─ createdOperationsCount                             │
│  │  └─ failedOperationsCount                              │
│  │                                                         │
│  └─ Taxa de sucesso geral                                 │
│    └─ overallSuccessRate = (created / total) * 100        │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    RESULTADO FINAL                          │
│                                                             │
│  OrchestrationResult                                       │
│  ├─ success: boolean                                       │
│  ├─ errorMessage: String                                   │
│  ├─ totalInvoices: int                                     │
│  ├─ overallSuccessRate: double                             │
│  ├─ validInvoicesCount: int                                │
│  ├─ invalidInvoicesCount: int                              │
│  ├─ detectedOperationsCount: int                           │
│  ├─ consolidatedOperationsCount: int                       │
│  ├─ createdOperationsCount: int                            │
│  ├─ failedOperationsCount: int                             │
│  ├─ validationResult: ValidationOrchestrationResult       │
│  ├─ detectionResult: DetectionResult                       │
│  ├─ integrationValidation: ValidationSummary               │
│  ├─ integrationResult: IntegrationResult                   │
│  └─ processedInvoices: List<Invoice>                       │
└─────────────────────────────────────────────────────────────┘
```

## **🚨 PONTOS DE FALHA E DECISÕES**

### **1. FALHA NA VALIDAÇÃO**
```
┌─────────────────────────────────────────────────────────────┐
│                    ERRO DE VALIDAÇÃO                       │
│                                                             │
│  IF (!validationResult.isCanProceed())                     │
│  │  ├─ success = false                                    │
│  │  ├─ errorMessage = "Falha na validação: " + reason     │
│  │  └─ RETURN OrchestrationResult                         │
└─────────────────────────────────────────────────────────────┘
```

### **2. FALHA NA DETECÇÃO**
```
┌─────────────────────────────────────────────────────────────┐
│                    ERRO DE DETECÇÃO                        │
│                                                             │
│  IF (!detectionResult.isSuccess())                         │
│  │  ├─ success = false                                    │
│  │  ├─ errorMessage = "Falha na detecção: " + error       │
│  │  └─ RETURN OrchestrationResult                         │
└─────────────────────────────────────────────────────────────┘
```

### **3. NENHUMA OPERAÇÃO VÁLIDA**
```
┌─────────────────────────────────────────────────────────────┐
│                NENHUMA OPERAÇÃO VÁLIDA                     │
│                                                             │
│  IF (integrationValidation.getValidCount() == 0)           │
│  │  ├─ success = false                                    │
│  │  ├─ errorMessage = "Nenhuma operação válida"           │
│  │  └─ RETURN OrchestrationResult                         │
└─────────────────────────────────────────────────────────────┘
```

### **4. EXCEÇÃO GERAL**
```
┌─────────────────────────────────────────────────────────────┐
│                    EXCEÇÃO GERAL                           │
│                                                             │
│  CATCH (Exception e)                                       │
│  │  ├─ success = false                                    │
│  │  ├─ errorMessage = "Erro na orquestração: " + message  │
│  │  ├─ Log error com stack trace                          │
│  │  └─ RETURN OrchestrationResult                         │
└─────────────────────────────────────────────────────────────┘
```

## **📊 MÉTRICAS E ESTATÍSTICAS**

### **OrchestrationResult**
- **totalInvoices**: Total de invoices processadas
- **overallSuccessRate**: Taxa de sucesso geral (%)
- **validInvoicesCount**: Invoices válidas
- **invalidInvoicesCount**: Invoices inválidas
- **detectedOperationsCount**: Operações detectadas
- **consolidatedOperationsCount**: Operações consolidadas
- **createdOperationsCount**: Operações criadas com sucesso
- **failedOperationsCount**: Operações que falharam

### **ValidationOrchestrationResult**
- **validCount**: Invoices válidas
- **invalidCount**: Invoices inválidas
- **hasDuplicates**: Se há duplicatas
- **validationErrors**: Lista de erros de validação

### **DetectionResult**
- **detectedOperations**: Operações detectadas inicialmente
- **classifiedOperations**: Operações classificadas
- **consolidatedOperations**: Operações consolidadas finais
- **detectionRate**: Taxa de detecção (%)
- **consolidationRate**: Taxa de consolidação (%)

### **IntegrationResult**
- **createdOperations**: Operações criadas
- **updatedOperations**: Operações atualizadas
- **failedOperations**: Operações que falharam
- **totalMappings**: Total de mapeamentos criados
- **successRate**: Taxa de sucesso da integração (%)

## **🔍 LOGS E DEBUGGING**

### **Logs Principais**
```
🎼 Iniciando orquestração de X invoices (User: email)
🔍 Validando X invoices
✅ Validação concluída: X válidas, Y inválidas, duplicatas: sim/não
📋 Buscando X invoices válidas
🎯 Detectando operações...
✅ Detecção concluída: X operações detectadas, Y consolidadas
🔍 Validando operações para integração...
✅ Validação para integração: X válidas, Y inválidas
⚙️ Integrando operações...
✅ Integração concluída: X criadas, Y atualizadas, Z falharam
✅ Orquestração concluída: X operações criadas, Y erros
```

### **Logs de Erro**
```
❌ Validação falhou: motivo
❌ Detecção falhou: motivo
❌ Nenhuma operação válida para integração
❌ Erro durante orquestração: motivo
```

## **🎯 CENÁRIOS DE TESTE**

### **Cenário 1: Sucesso Completo**
1. ✅ Validação passa
2. ✅ Sem duplicatas
3. ✅ Reprocessamento permitido
4. ✅ Detecção bem-sucedida
5. ✅ Operações válidas para integração
6. ✅ Integração bem-sucedida
7. ✅ Resultado: success = true

### **Cenário 2: Falha na Validação**
1. ❌ Invoice inválida
2. ❌ Parar processamento
3. ✅ Resultado: success = false, errorMessage = "Falha na validação"

### **Cenário 3: Duplicatas Detectadas**
1. ✅ Validação passa
2. ❌ Duplicatas detectadas
3. ❌ Parar processamento
4. ✅ Resultado: success = false, errorMessage = "Duplicatas detectadas"

### **Cenário 4: Falha na Detecção**
1. ✅ Validação passa
2. ✅ Sem duplicatas
3. ✅ Reprocessamento permitido
4. ❌ Detecção falha
5. ❌ Parar processamento
6. ✅ Resultado: success = false, errorMessage = "Falha na detecção"

### **Cenário 5: Nenhuma Operação Válida**
1. ✅ Validação passa
2. ✅ Sem duplicatas
3. ✅ Reprocessamento permitido
4. ✅ Detecção bem-sucedida
5. ❌ Nenhuma operação válida para integração
6. ❌ Parar processamento
7. ✅ Resultado: success = false, errorMessage = "Nenhuma operação válida"

## **🚀 BENEFÍCIOS DO FLUXO**

### **Para Desenvolvedores**
- ✅ **Fluxo claro e documentado** - fácil de entender e manter
- ✅ **Pontos de falha bem definidos** - fácil de debugar
- ✅ **Logs estruturados** - rastreabilidade completa
- ✅ **Testes abrangentes** - cobertura de todos os cenários

### **Para o Sistema**
- ✅ **Processamento robusto** - tratamento de erros em cada etapa
- ✅ **Rollback automático** - consistência de dados
- ✅ **Estatísticas detalhadas** - monitoramento completo
- ✅ **Progresso em tempo real** - feedback imediato

### **Para Usuários**
- ✅ **Feedback claro** - sabem exatamente o que aconteceu
- ✅ **Mensagens amigáveis** - erros compreensíveis
- ✅ **Processamento confiável** - validações em múltiplas camadas
- ✅ **Recuperação automática** - retry para erros recuperáveis 